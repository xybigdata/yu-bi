package yubi.server.recycle;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Function;

public final class DefaultRecycleService implements RecycleService {

    static final int MAX_PREFLIGHT_ROOTS = 2_000;
    static final int ASYNC_EXPANDED_ITEM_THRESHOLD = 100;
    private static final Duration OPERATION_TOKEN_TTL = Duration.ofMinutes(5);

    private final Map<RecycleResourceType, RecycleResourceAdapter> adapters;
    private final Clock clock;
    private final RecycleStore store;
    private final Executor asyncExecutor;
    private final Function<String, String> deletedByNameResolver;

    public DefaultRecycleService(List<RecycleResourceAdapter> adapters, Clock clock) {
        this(adapters, clock, new InMemoryRecycleStore(), Runnable::run, Function.identity());
    }

    public DefaultRecycleService(List<RecycleResourceAdapter> adapters,
                                 Clock clock,
                                 RecycleStore store) {
        this(adapters, clock, store, Runnable::run, Function.identity());
    }

    DefaultRecycleService(List<RecycleResourceAdapter> adapters,
                          Clock clock,
                          RecycleStore store,
                          Executor asyncExecutor) {
        this(adapters, clock, store, asyncExecutor, Function.identity());
    }

    DefaultRecycleService(List<RecycleResourceAdapter> adapters,
                          Clock clock,
                          RecycleStore store,
                          Executor asyncExecutor,
                          Function<String, String> deletedByNameResolver) {
        Objects.requireNonNull(adapters, "adapters");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.store = Objects.requireNonNull(store, "store");
        this.asyncExecutor = Objects.requireNonNull(asyncExecutor, "asyncExecutor");
        this.deletedByNameResolver = Objects.requireNonNull(
                deletedByNameResolver, "deletedByNameResolver");
        this.adapters = new EnumMap<>(RecycleResourceType.class);
        for (RecycleResourceAdapter adapter : adapters) {
            RecycleResourceAdapter previous = this.adapters.put(adapter.type(), adapter);
            if (previous != null) {
                throw new IllegalArgumentException("资源类型存在重复适配器: " + adapter.type());
            }
        }
    }

    @Override
    public RecyclePreflight preflight(RecycleAccess access, RecyclePreflightCommand command) {
        Objects.requireNonNull(access, "access");
        Objects.requireNonNull(command, "command");
        if (command.rootIds().isEmpty()) {
            throw new IllegalArgumentException("至少选择一个删除根项");
        }
        if (command.rootIds().size() > MAX_PREFLIGHT_ROOTS) {
            throw new IllegalArgumentException("单次预检最多支持 2000 个删除根项");
        }

        RecycleResourceAdapter adapter = adapters.get(command.resourceType());
        if (adapter == null) {
            throw new IllegalArgumentException("不支持的资源类型: " + command.resourceType());
        }

        Set<String> selectedRootIds = new LinkedHashSet<>(command.rootIds());
        if (selectedRootIds.size() != command.rootIds().size()) {
            throw new IllegalArgumentException("删除根项不能重复");
        }
        List<RecycleItemPreflight> items = command.rootIds().stream()
                .map(rootId -> adapter.preflight(access, rootId, Set.copyOf(selectedRootIds)))
                .toList();
        Instant now = clock.instant();
        String operationToken = UUID.randomUUID().toString();
        Instant expiresAt = now.plus(OPERATION_TOKEN_TTL);
        store.savePrepared(new RecyclePreparedOperation(
                operationToken,
                access.actorId(),
                access.organizationId(),
                command.resourceType(),
                command.rootIds(),
                expiresAt
        ));
        return new RecyclePreflight(operationToken, expiresAt, items);
    }

    @Override
    public RecycleBatch moveToRecycle(RecycleAccess access, RecycleExecutionCommand command) {
        Objects.requireNonNull(access, "access");
        Objects.requireNonNull(command, "command");
        return store.findBatchByRequest(
                        access.actorId(), access.organizationId(), command.clientRequestId())
                .orElseGet(() -> executeMove(access, command));
    }

    private RecycleBatch executeMove(RecycleAccess access, RecycleExecutionCommand command) {
        RecyclePreparedOperation prepared = store.findPrepared(command.operationToken())
                .filter(operation -> operation.actorId().equals(access.actorId()))
                .filter(operation -> operation.organizationId().equals(access.organizationId()))
                .filter(operation -> operation.expiresAt().isAfter(clock.instant()))
                .orElseThrow(() -> new IllegalArgumentException("操作令牌不存在或已过期"));
        if (prepared.resourceType() != command.resourceType()) {
            throw new IllegalArgumentException("操作令牌不属于当前业务模块");
        }
        RecycleResourceAdapter adapter = adapters.get(prepared.resourceType());
        Instant now = clock.instant();
        long expandedItemCount = prepared.rootIds().stream()
                .mapToLong(rootId -> adapter.expandedItemCount(access, rootId))
                .sum();
        if (expandedItemCount > ASYNC_EXPANDED_ITEM_THRESHOLD) {
            return scheduleMove(access, command, prepared, adapter, now);
        }
        RecycleBatch batch = completeMove(
                access, prepared, adapter, UUID.randomUUID().toString(), now, now);
        RecycleBatch saved = store.saveBatch(
                access.actorId(), access.organizationId(), command.clientRequestId(), batch);
        auditBatch(access, saved);
        return saved;
    }

    private RecycleBatch scheduleMove(RecycleAccess access,
                                      RecycleExecutionCommand command,
                                      RecyclePreparedOperation prepared,
                                      RecycleResourceAdapter adapter,
                                      Instant createdAt) {
        RecycleBatch pending = new RecycleBatch(
                UUID.randomUUID().toString(), prepared.resourceType(),
                RecycleOperation.MOVE_TO_RECYCLE, RecycleBatchState.PROCESSING,
                createdAt, null, null, List.of());
        RecycleBatch saved = store.saveBatch(
                access.actorId(), access.organizationId(), command.clientRequestId(), pending);
        if (!saved.id().equals(pending.id())) {
            return saved;
        }
        try {
            asyncExecutor.execute(() -> {
                Instant completedAt = clock.instant();
                RecycleBatch completed = completeMove(
                        access, prepared, adapter, pending.id(), createdAt, completedAt);
                store.completeBatch(
                        access.actorId(), access.organizationId(), completed, completedAt);
                auditBatch(access, completed);
            });
        } catch (RuntimeException exception) {
            List<RecycleItemResult> failed = prepared.rootIds().stream()
                    .map(rootId -> new RecycleItemResult(
                            rootId, RecycleItemStatus.FAILED,
                            "异步任务提交失败，请稍后重试", null))
                    .toList();
            RecycleBatch completed = new RecycleBatch(
                    pending.id(), prepared.resourceType(), RecycleOperation.MOVE_TO_RECYCLE,
                    RecycleBatchState.COMPLETED, createdAt, null, null, failed);
            store.completeBatch(
                    access.actorId(), access.organizationId(), completed, clock.instant());
            auditBatch(access, completed);
        }
        return store.findBatch(access.actorId(), access.organizationId(), pending.id())
                .orElse(saved);
    }

    private RecycleBatch completeMove(RecycleAccess access,
                                      RecyclePreparedOperation prepared,
                                      RecycleResourceAdapter adapter,
                                      String batchId,
                                      Instant createdAt,
                                      Instant completedAt) {
        Set<String> selectedRootIds = Set.copyOf(prepared.rootIds());
        List<RecycleItemResult> items = executeRoots(
                access, adapter, prepared.rootIds(), selectedRootIds, completedAt);
        return new RecycleBatch(
                batchId,
                prepared.resourceType(),
                RecycleOperation.MOVE_TO_RECYCLE,
                RecycleBatchState.COMPLETED,
                createdAt,
                UUID.randomUUID().toString(),
                completedAt.plus(Duration.ofMinutes(10)),
                items
        );
    }

    private List<RecycleItemResult> executeRoots(RecycleAccess access,
                                                 RecycleResourceAdapter adapter,
                                                 List<String> rootIds,
                                                 Set<String> selectedRootIds,
                                                 Instant now) {
        Map<String, RecycleItemResult> results = new LinkedHashMap<>();
        List<String> pending = new ArrayList<>(rootIds);
        while (!pending.isEmpty()) {
            boolean changedDependencies = false;
            List<String> retry = new ArrayList<>();
            Map<String, RecycleItemResult> blocked = new LinkedHashMap<>();
            for (String rootId : pending) {
                RecycleItemResult result = executeRoot(
                        access, adapter, rootId, selectedRootIds, now);
                if (result.status() == RecycleItemStatus.BLOCKED) {
                    retry.add(rootId);
                    blocked.put(rootId, result);
                } else {
                    results.put(rootId, result);
                    changedDependencies |= result.status() == RecycleItemStatus.SUCCESS;
                }
            }
            if (!changedDependencies || retry.isEmpty()) {
                results.putAll(blocked);
                break;
            }
            pending = retry;
        }
        return rootIds.stream().map(results::get).toList();
    }

    @Override
    public RecycleBatch getBatch(RecycleAccess access,
                                 RecycleResourceType resourceType,
                                 String batchId) {
        Objects.requireNonNull(access, "access");
        Objects.requireNonNull(resourceType, "resourceType");
        Objects.requireNonNull(batchId, "batchId");
        return store.findBatch(access.actorId(), access.organizationId(), batchId)
                .filter(batch -> batch.resourceType() == resourceType)
                .orElseThrow(() -> new IllegalArgumentException("批次不存在或不属于当前模块"));
    }

    @Override
    public List<RecycleEntry> list(RecycleAccess access, RecycleResourceType resourceType) {
        Objects.requireNonNull(access, "access");
        Objects.requireNonNull(resourceType, "resourceType");
        RecycleResourceAdapter adapter = requireAdapter(resourceType);
        Map<String, String> deletedByNames = new HashMap<>();
        return store.listRecords(access.organizationId(), resourceType).stream()
                .filter(record -> access.organizationOwner()
                        || record.deletedBy().equals(access.actorId())
                        || adapter.canManageRecycle(access, record.snapshot()))
                .map(record -> RecycleEntry.from(
                        record,
                        deletedByNames.computeIfAbsent(
                                record.deletedBy(), deletedByNameResolver)))
                .toList();
    }

    @Override
    public RecycleBatch restore(RecycleAccess access, RecycleBulkCommand command) {
        return executeStored(access, command, RecycleOperation.RESTORE, false);
    }

    @Override
    public RecycleBatch permanentlyDelete(RecycleAccess access, RecycleBulkCommand command) {
        if (!access.organizationOwner()) {
            throw new SecurityException("仅组织所有者可以永久删除回收站内容");
        }
        return executeStored(access, command, RecycleOperation.PERMANENT_DELETE, true);
    }

    @Override
    public RecycleBatch undo(RecycleAccess access, String batchId, String undoToken) {
        Objects.requireNonNull(access, "access");
        Objects.requireNonNull(batchId, "batchId");
        Objects.requireNonNull(undoToken, "undoToken");
        RecycleBatch moved = store.findBatch(
                        access.actorId(), access.organizationId(), batchId)
                .filter(batch -> batch.operation() == RecycleOperation.MOVE_TO_RECYCLE)
                .filter(batch -> batch.state() == RecycleBatchState.COMPLETED)
                .filter(batch -> undoToken.equals(batch.undoToken()))
                .filter(batch -> batch.undoExpiresAt() != null
                        && !batch.undoExpiresAt().isBefore(clock.instant()))
                .orElseThrow(() -> new IllegalArgumentException("撤销令牌不存在或已过期"));
        RecycleResourceAdapter adapter = requireAdapter(moved.resourceType());
        List<RecycleItemResult> items = moved.items().stream()
                .filter(item -> item.status() == RecycleItemStatus.SUCCESS)
                .map(RecycleItemResult::recordId)
                .filter(Objects::nonNull)
                .map(store::findRecord)
                .flatMap(Optional::stream)
                .map(record -> executeStoredRoot(
                        access, adapter, record, RecycleOperation.RESTORE))
                .toList();
        if (items.isEmpty()) {
            throw new IllegalArgumentException("该批次没有可撤销的内容");
        }
        Instant now = clock.instant();
        RecycleBatch undone = new RecycleBatch(
                UUID.randomUUID().toString(), moved.resourceType(), RecycleOperation.UNDO,
                RecycleBatchState.COMPLETED, now, null, null, items);
        RecycleBatch saved = store.saveBatch(
                access.actorId(), access.organizationId(),
                "undo:" + moved.id(), undone);
        auditBatch(access, saved);
        return saved;
    }

    @Override
    public RecyclePolicy getPolicy(RecycleAccess access, RecycleResourceType resourceType) {
        Objects.requireNonNull(access, "access");
        Objects.requireNonNull(resourceType, "resourceType");
        return store.findPolicy(access.organizationId(), resourceType)
                .orElse(RecyclePolicy.defaults());
    }

    @Override
    public RecyclePolicy updatePolicy(RecycleAccess access,
                                      RecycleResourceType resourceType,
                                      RecyclePolicy policy) {
        Objects.requireNonNull(access, "access");
        Objects.requireNonNull(resourceType, "resourceType");
        Objects.requireNonNull(policy, "policy");
        if (!access.organizationOwner()) {
            throw new SecurityException("仅组织所有者可以修改自动清理策略");
        }
        Instant now = clock.instant();
        store.savePolicy(access.organizationId(), resourceType, policy, access.actorId(), now);
        store.recalculateExpiry(
                access.organizationId(), resourceType,
                policy.enabled() ? now.plus(Duration.ofDays(policy.retentionDays())) : null);
        store.saveAudit(new RecycleAuditEvent(
                UUID.randomUUID().toString(), null, null,
                access.organizationId(), resourceType, null,
                RecycleOperation.UPDATE_POLICY, "SUCCESS",
                "enabled=" + policy.enabled() + ", retentionDays=" + policy.retentionDays(),
                access.actorId(), now));
        return policy;
    }

    private RecycleBatch executeStored(RecycleAccess access,
                                       RecycleBulkCommand command,
                                       RecycleOperation operation,
                                       boolean ownerOnly) {
        Objects.requireNonNull(access, "access");
        Objects.requireNonNull(command, "command");
        if (ownerOnly && !access.organizationOwner()) {
            throw new SecurityException("仅组织所有者可以执行此操作");
        }
        return store.findBatchByRequest(
                        access.actorId(), access.organizationId(), command.clientRequestId())
                .orElseGet(() -> {
                    List<RecycleRecord> records = command.recordIds().stream()
                            .map(store::findRecord)
                            .flatMap(Optional::stream)
                            .filter(record -> record.organizationId().equals(access.organizationId()))
                            .toList();
                    if (records.isEmpty()) {
                        throw new IllegalArgumentException("未找到可操作的回收站内容");
                    }
                    RecycleResourceType type = command.resourceType();
                    if (records.stream().anyMatch(record -> record.resourceType() != type)) {
                        throw new IllegalArgumentException("单次操作只能处理一个业务模块");
                    }
                    RecycleResourceAdapter adapter = requireAdapter(type);
                    List<RecycleItemResult> items = records.stream()
                            .map(record -> executeStoredRoot(access, adapter, record, operation))
                            .toList();
                    Instant now = clock.instant();
                    RecycleBatch batch = new RecycleBatch(
                            UUID.randomUUID().toString(), type, operation,
                            RecycleBatchState.COMPLETED, now, null, null, items);
                    RecycleBatch saved = store.saveBatch(
                            access.actorId(), access.organizationId(), command.clientRequestId(), batch);
                    auditBatch(access, saved);
                    return saved;
                });
    }

    @Override
    public RecycleMaintenanceResult maintain(boolean cleanupEnabled, int limit) {
        if (!cleanupEnabled) {
            return new RecycleMaintenanceResult(0, 0, 0);
        }
        if (limit < 1) {
            throw new IllegalArgumentException("维护批量大小必须为正数");
        }
        Instant now = clock.instant();
        List<RecycleRecord> expired = store.findExpired(now, limit);
        int deleted = 0;
        int failed = 0;
        for (RecycleRecord record : expired) {
            RecycleAccess system = RecycleAccess.authenticated(
                    "SYSTEM", record.organizationId(), true);
            RecycleResourceAdapter adapter = requireAdapter(record.resourceType());
            RecycleItemResult item;
            try {
                item = adapter.permanentlyDelete(system, record.snapshot());
            } catch (RuntimeException exception) {
                item = new RecycleItemResult(
                        record.snapshot().rootId(), RecycleItemStatus.FAILED,
                        "自动清理失败，等待下次重试", record.id());
            }
            item = new RecycleItemResult(
                    item.rootId(), item.status(), item.message(), record.id());
            if (item.status() == RecycleItemStatus.SUCCESS) {
                store.deleteRecord(record.id());
                deleted++;
            } else {
                failed++;
            }
            RecycleBatch batch = new RecycleBatch(
                    UUID.randomUUID().toString(), record.resourceType(),
                    RecycleOperation.AUTO_CLEANUP, RecycleBatchState.COMPLETED,
                    now, null, null, List.of(item));
            RecycleBatch saved = store.saveBatch(
                    "SYSTEM", record.organizationId(),
                    "auto-cleanup:" + record.id() + ":" + now, batch);
            auditBatch(system, saved);
        }
        store.pruneAudit(now.minus(Duration.ofDays(90)));
        return new RecycleMaintenanceResult(expired.size(), deleted, failed);
    }

    private RecycleItemResult executeStoredRoot(RecycleAccess access,
                                                RecycleResourceAdapter adapter,
                                                RecycleRecord record,
                                                RecycleOperation operation) {
        if (!access.organizationOwner() && !adapter.canManageRecycle(access, record.snapshot())) {
            return new RecycleItemResult(
                    record.snapshot().rootId(), RecycleItemStatus.FORBIDDEN, "没有管理权限", record.id());
        }
        try {
            RecycleItemResult result = operation == RecycleOperation.RESTORE
                    ? adapter.restore(access, record.snapshot())
                    : adapter.permanentlyDelete(access, record.snapshot());
            if (result.status() == RecycleItemStatus.SUCCESS) {
                store.deleteRecord(record.id());
            }
            return new RecycleItemResult(
                    result.rootId(), result.status(), result.message(), record.id());
        } catch (RuntimeException exception) {
            return new RecycleItemResult(
                    record.snapshot().rootId(), RecycleItemStatus.FAILED,
                    operation == RecycleOperation.RESTORE ? "恢复失败" : "永久删除失败",
                    record.id());
        }
    }

    private RecycleResourceAdapter requireAdapter(RecycleResourceType resourceType) {
        RecycleResourceAdapter adapter = adapters.get(resourceType);
        if (adapter == null) {
            throw new IllegalArgumentException("不支持的资源类型: " + resourceType);
        }
        return adapter;
    }

    private void auditBatch(RecycleAccess access, RecycleBatch batch) {
        boolean successful = batch.items().stream()
                .allMatch(item -> item.status() == RecycleItemStatus.SUCCESS);
        store.saveAudit(new RecycleAuditEvent(
                UUID.randomUUID().toString(), batch.id(), null,
                access.organizationId(), batch.resourceType(), null,
                batch.operation(), successful ? "SUCCESS" : "FAILED",
                null, access.actorId(), batch.createdAt()));
        for (RecycleItemResult item : batch.items()) {
            store.saveAudit(new RecycleAuditEvent(
                    UUID.randomUUID().toString(), batch.id(), item.recordId(),
                    access.organizationId(), batch.resourceType(), item.rootId(),
                    batch.operation(), item.status().name(), item.message(),
                    access.actorId(), batch.createdAt()));
        }
    }

    private RecycleItemResult executeRoot(RecycleAccess access,
                                          RecycleResourceAdapter adapter,
                                          String rootId,
                                          Set<String> selectedRootIds,
                                          Instant now) {
        try {
            RecycleItemPreflight current = adapter.preflight(access, rootId, selectedRootIds);
            if (current.status() != RecycleItemStatus.SUCCESS) {
                return new RecycleItemResult(rootId, current.status(), current.message(), null);
            }
            RecycleRootSnapshot snapshot = adapter.moveToRecycle(access, rootId);
            String recordId = UUID.randomUUID().toString();
            RecyclePolicy policy = store.findPolicy(access.organizationId(), adapter.type())
                    .orElse(RecyclePolicy.defaults());
            store.saveRecord(new RecycleRecord(
                    recordId,
                    access.organizationId(),
                    adapter.type(),
                    snapshot,
                    access.actorId(),
                    now,
                    policy.enabled() ? now.plus(Duration.ofDays(policy.retentionDays())) : null
            ));
            return new RecycleItemResult(rootId, RecycleItemStatus.SUCCESS, null, recordId);
        } catch (RuntimeException exception) {
            return new RecycleItemResult(
                    rootId,
                    RecycleItemStatus.FAILED,
                    "移入回收站失败",
                    null
            );
        }
    }
}
