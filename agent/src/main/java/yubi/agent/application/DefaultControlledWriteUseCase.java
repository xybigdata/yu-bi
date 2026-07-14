package yubi.agent.application;

import yubi.agent.api.AgentExecutionContext;
import yubi.agent.api.ControlledWriteException;
import yubi.agent.api.ControlledWriteException.Code;
import yubi.agent.api.WriteApprovalUseCase;
import yubi.agent.api.WriteProposalUseCase;
import yubi.agent.domain.ControlledWriteModels.ControlledWriteOperation;
import yubi.agent.domain.ControlledWriteModels.NormalizedWriteProposal;
import yubi.agent.domain.ControlledWriteModels.PreparedWrite;
import yubi.agent.domain.ControlledWriteModels.TrustedWriteContext;
import yubi.agent.domain.ControlledWriteModels.WriteAuditEvent;
import yubi.agent.domain.ControlledWriteModels.WriteAuditEventType;
import yubi.agent.domain.ControlledWriteModels.WriteApprovalScope;
import yubi.agent.domain.ControlledWriteModels.WriteExecutionResult;
import yubi.agent.domain.ControlledWriteModels.WriteFailureCategory;
import yubi.agent.domain.ControlledWriteModels.WriteIdempotencyScope;
import yubi.agent.domain.ControlledWriteModels.WriteOperationState;
import yubi.agent.domain.ControlledWriteModels.WriteOperationView;
import yubi.agent.domain.ControlledWriteModels.WriteProposalCommand;
import yubi.agent.port.VisualizationWritePort;
import yubi.agent.port.WriteAuditPort;
import yubi.agent.port.WriteClockPort;
import yubi.agent.port.WriteIdGeneratorPort;
import yubi.agent.port.WriteOperationStorePort;
import yubi.agent.port.WriteProposalTool;
import yubi.agent.port.WriteProposalToolRegistry;
import yubi.query.api.QueryExecutionContext;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class DefaultControlledWriteUseCase implements WriteProposalUseCase, WriteApprovalUseCase {

    private static final int MAXIMUM_IDEMPOTENCY_KEY_LENGTH = 128;
    private static final int MAXIMUM_WORKSPACE_OPERATIONS = 100;
    private static final Duration MAXIMUM_APPROVAL_LIFETIME = Duration.ofHours(24);

    private final WriteProposalToolRegistry registry;
    private final VisualizationWritePort visualizationWrite;
    private final WriteOperationStorePort store;
    private final WriteAuditPort audit;
    private final WriteIdGeneratorPort idGenerator;
    private final WriteClockPort clock;
    private final Duration approvalLifetime;

    public DefaultControlledWriteUseCase(WriteProposalToolRegistry registry,
                                         VisualizationWritePort visualizationWrite,
                                         WriteOperationStorePort store,
                                         WriteAuditPort audit,
                                         WriteIdGeneratorPort idGenerator,
                                         WriteClockPort clock,
                                         Duration approvalLifetime) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.visualizationWrite = Objects.requireNonNull(visualizationWrite, "visualizationWrite");
        this.store = Objects.requireNonNull(store, "store");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.approvalLifetime = Objects.requireNonNull(approvalLifetime, "approvalLifetime");
        if (approvalLifetime.isZero() || approvalLifetime.isNegative()
                || approvalLifetime.compareTo(MAXIMUM_APPROVAL_LIFETIME) > 0) {
            throw new IllegalArgumentException("审批有效期必须在 24 小时内");
        }
    }

    @Override
    public WriteOperationView propose(WriteProposalCommand command, AgentExecutionContext context) {
        TrustedWriteContext trusted = trusted(context);
        if (command == null || command.toolName() == null || command.toolName().isBlank()
                || command.toolName().length() > 64
                || command.arguments() == null) {
            throw failure(Code.INVALID_WRITE_REQUEST, "写提案请求无效");
        }
        WriteProposalTool tool = registry.find(command.toolName())
                .orElseThrow(() -> failure(Code.UNKNOWN_WRITE_TOOL, "写工具未注册"));
        String key = normalizeIdempotencyKey(command.idempotencyKey());

        // Tool 映射必须先于任何业务能力调用拒绝未知或越权字段。
        NormalizedWriteProposal normalized = tool.normalize(command.arguments());
        if (!tool.schema().name().equals(normalized.action().toolName())) {
            throw failure(Code.INVALID_WRITE_REQUEST, "写工具与业务操作不匹配");
        }
        if (!WriteDigests.canonicalParameters(normalized.action()).equals(normalized.canonicalParameters())) {
            throw failure(Code.INVALID_WRITE_REQUEST, "写工具规范化参数不一致");
        }
        String parametersDigest = WriteDigests.structured(normalized.canonicalParameters());
        String keyDigest = WriteDigests.idempotencyKey(key);
        WriteIdempotencyScope idempotencyScope = new WriteIdempotencyScope(
                trusted.subjectId(), trusted.organizationId(), tool.schema().name(), keyDigest);

        var existing = store.findByIdempotency(idempotencyScope);
        if (existing.isPresent()) {
            Instant replayedAt = now();
            ControlledWriteOperation operation = validateReplay(existing.get(), idempotencyScope,
                    parametersDigest, trusted, replayedAt);
            operation = materializeExpiry(operation, operation.approvalScope(), trusted, replayedAt);
            record(operation, WriteAuditEventType.IDEMPOTENT_REPLAYED, replayedAt, trusted);
            return WriteOperationView.from(operation, true);
        }

        PreparedWrite prepared = visualizationWrite.prepare(normalized.action(), normalized.preview(), trusted);
        validatePreparation(normalized, prepared);
        Instant createdAt = now();
        Instant expiresAt;
        try {
            expiresAt = createdAt.plus(approvalLifetime);
        } catch (RuntimeException exception) {
            throw failure(Code.INVALID_WRITE_REQUEST, "审批有效期无效");
        }
        ControlledWriteOperation candidate = new ControlledWriteOperation(
                generatedId(), generatedId(), trusted, tool.schema().name(),
                normalized.canonicalParameters(), parametersDigest, WriteDigests.prepared(prepared),
                keyDigest, prepared,
                createdAt, expiresAt, WriteOperationState.PENDING, null, null, null);

        WriteOperationStorePort.CreateResult result = store.createPending(candidate);
        Instant recordedAt = result.created() ? createdAt : now();
        ControlledWriteOperation operation = validateReplay(result.operation(), idempotencyScope,
                parametersDigest, trusted, recordedAt);
        if (!result.created()) {
            operation = materializeExpiry(operation, operation.approvalScope(), trusted, recordedAt);
        }
        record(operation, result.created() ? WriteAuditEventType.PROPOSED
                : WriteAuditEventType.IDEMPOTENT_REPLAYED, recordedAt, trusted);
        return WriteOperationView.from(operation, !result.created());
    }

    @Override
    public List<WriteOperationView> listOperations(AgentExecutionContext context) {
        TrustedWriteContext trusted = trusted(context);
        WriteApprovalScope scope = new WriteApprovalScope(
                trusted.subjectId(), trusted.organizationId(), trusted.sessionId());
        List<ControlledWriteOperation> operations = store.listByScope(scope, MAXIMUM_WORKSPACE_OPERATIONS);
        if (operations == null || operations.size() > MAXIMUM_WORKSPACE_OPERATIONS) {
            throw failure(Code.APPROVAL_TAMPERED, "审批列表存储结果无效");
        }
        Instant current = now();
        return operations.stream()
                .map(operation -> materializeExpiry(operation, scope, trusted, current))
                .sorted(Comparator.comparing(ControlledWriteOperation::createdAt).reversed()
                        .thenComparing(ControlledWriteOperation::approvalId))
                .map(operation -> WriteOperationView.from(operation, false))
                .toList();
    }

    private ControlledWriteOperation materializeExpiry(ControlledWriteOperation operation,
                                                        WriteApprovalScope scope,
                                                        TrustedWriteContext trusted,
                                                        Instant current) {
        validateIntegrity(operation);
        if (!operation.approvalScope().equals(scope)) {
            throw failure(Code.APPROVAL_TAMPERED, "审批列表作用域异常");
        }
        if (operation.state() != WriteOperationState.PENDING || current.isBefore(operation.expiresAt())) {
            return operation;
        }

        ControlledWriteOperation locked = store.lockByApprovalId(operation.approvalId())
                .orElseThrow(() -> failure(Code.APPROVAL_TAMPERED, "审批列表记录不存在"));
        validateIntegrity(locked);
        if (!locked.approvalScope().equals(scope)) {
            throw failure(Code.APPROVAL_TAMPERED, "审批列表作用域异常");
        }
        if (locked.state() != WriteOperationState.PENDING || current.isBefore(locked.expiresAt())) {
            return locked;
        }
        ControlledWriteOperation expired = locked.expired(current);
        store.saveLocked(locked, expired);
        record(expired, WriteAuditEventType.EXPIRED, current, trusted);
        return expired;
    }

    @Override
    public WriteOperationView approve(String approvalId, AgentExecutionContext context) {
        TrustedWriteContext trusted = trusted(context);
        ControlledWriteOperation operation = locked(approvalId);
        validateApproval(operation, trusted);
        Instant current = now();

        if (operation.state() != WriteOperationState.PENDING) {
            record(operation, WriteAuditEventType.IDEMPOTENT_REPLAYED, current, trusted);
            return WriteOperationView.from(operation, true);
        }
        if (!current.isBefore(operation.expiresAt())) {
            ControlledWriteOperation expired = operation.expired(current);
            store.saveLocked(operation, expired);
            record(expired, WriteAuditEventType.EXPIRED, current, trusted);
            return WriteOperationView.from(expired, false);
        }

        // execute 必须使用持久化的 prepared operation，并在内部重新授权和核验目标状态。
        WriteExecutionResult execution = visualizationWrite.execute(operation.preparedWrite(), trusted);
        if (execution == null || execution.resourceId() == null || execution.resourceId().isBlank()) {
            throw failure(Code.INVALID_BUSINESS_RESULT, "业务写入结果无效");
        }
        Instant completedAt = now();
        ControlledWriteOperation succeeded = operation.succeeded(execution.resourceId(), completedAt);
        store.saveLocked(operation, succeeded);
        record(succeeded, WriteAuditEventType.SUCCEEDED, completedAt, trusted);
        return WriteOperationView.from(succeeded, false);
    }

    @Override
    public WriteOperationView reject(String approvalId, AgentExecutionContext context) {
        TrustedWriteContext trusted = trusted(context);
        ControlledWriteOperation operation = locked(approvalId);
        validateApproval(operation, trusted);
        Instant current = now();

        if (operation.state() == WriteOperationState.REJECTED
                || operation.state() == WriteOperationState.EXPIRED
                || operation.state() == WriteOperationState.FAILED) {
            record(operation, WriteAuditEventType.IDEMPOTENT_REPLAYED, current, trusted);
            return WriteOperationView.from(operation, true);
        }
        if (operation.state() != WriteOperationState.PENDING) {
            throw failure(Code.APPROVAL_STATE_INVALID, "审批状态不允许拒绝");
        }
        ControlledWriteOperation terminal = !current.isBefore(operation.expiresAt())
                ? operation.expired(current)
                : operation.rejected(current);
        store.saveLocked(operation, terminal);
        record(terminal, terminal.state() == WriteOperationState.EXPIRED
                ? WriteAuditEventType.EXPIRED : WriteAuditEventType.REJECTED, current, trusted);
        return WriteOperationView.from(terminal, false);
    }

    @Override
    public WriteOperationView markFailed(String approvalId,
                                         WriteFailureCategory failure,
                                         AgentExecutionContext context) {
        TrustedWriteContext trusted = trusted(context);
        ControlledWriteOperation operation = locked(approvalId);
        validateApproval(operation, trusted);
        if (failure == null) {
            throw failure(Code.INVALID_WRITE_REQUEST, "写入失败分类无效");
        }
        Instant current = now();
        if (operation.state() == WriteOperationState.FAILED) {
            record(operation, WriteAuditEventType.IDEMPOTENT_REPLAYED, current, trusted);
            return WriteOperationView.from(operation, true);
        }
        if (operation.state() != WriteOperationState.PENDING) {
            throw failure(Code.APPROVAL_STATE_INVALID, "审批状态不允许标记失败");
        }
        ControlledWriteOperation failed = operation.failed(failure, current);
        store.saveLocked(operation, failed);
        record(failed, WriteAuditEventType.FAILED, current, trusted);
        return WriteOperationView.from(failed, false);
    }

    private ControlledWriteOperation locked(String approvalId) {
        if (approvalId == null || approvalId.isBlank() || approvalId.length() > 128) {
            throw failure(Code.APPROVAL_NOT_FOUND, "审批不存在");
        }
        return store.lockByApprovalId(approvalId)
                .orElseThrow(() -> failure(Code.APPROVAL_NOT_FOUND, "审批不存在"));
    }

    private void validateApproval(ControlledWriteOperation operation, TrustedWriteContext trusted) {
        validateIntegrity(operation);
        TrustedWriteContext bound = operation.context();
        if (!bound.subjectId().equals(trusted.subjectId())
                || !bound.organizationId().equals(trusted.organizationId())
                || !bound.sessionId().equals(trusted.sessionId())) {
            throw failure(Code.APPROVAL_SCOPE_MISMATCH, "审批可信作用域不匹配");
        }
    }

    private ControlledWriteOperation validateReplay(ControlledWriteOperation operation,
                                                     WriteIdempotencyScope scope,
                                                     String parametersDigest,
                                                     TrustedWriteContext trusted,
                                                     Instant current) {
        validateIntegrity(operation);
        if (!operation.idempotencyScope().equals(scope)) {
            throw failure(Code.APPROVAL_TAMPERED, "幂等记录作用域异常");
        }
        if (!operation.parametersDigest().equals(parametersDigest)) {
            throw failure(Code.IDEMPOTENCY_CONFLICT, "幂等键已用于不同写操作");
        }
        if (operation.state() == WriteOperationState.PENDING && current.isBefore(operation.expiresAt())
                && !operation.context().sessionId().equals(trusted.sessionId())) {
            throw failure(Code.APPROVAL_SCOPE_MISMATCH, "待审批操作属于其他工作区会话");
        }
        return operation;
    }

    private void validateIntegrity(ControlledWriteOperation operation) {
        if (operation == null
                || !operation.toolName().equals(operation.preparedWrite().action().toolName())
                || !operation.canonicalParameters().equals(
                WriteDigests.canonicalParameters(operation.preparedWrite().action()))
                || !operation.parametersDigest().equals(
                WriteDigests.structured(operation.canonicalParameters()))
                || !operation.preparedDigest().equals(
                WriteDigests.prepared(operation.preparedWrite()))) {
            throw failure(Code.APPROVAL_TAMPERED, "审批记录完整性校验失败");
        }
    }

    private void validatePreparation(NormalizedWriteProposal normalized, PreparedWrite prepared) {
        if (prepared == null || !normalized.action().equals(prepared.action())
                || !extendsSafePreview(normalized.preview(), prepared.preview())
                || prepared.expectedStateDigest().length() > 256) {
            throw failure(Code.INVALID_BUSINESS_PREPARATION, "业务写入准备结果无效");
        }
    }

    private boolean extendsSafePreview(yubi.agent.domain.ControlledWriteModels.WritePreview required,
                                       yubi.agent.domain.ControlledWriteModels.WritePreview actual) {
        if (actual == null || !required.title().equals(actual.title())
                || !required.resourceType().equals(actual.resourceType())
                || !Objects.equals(required.targetId(), actual.targetId())
                || actual.fields().size() > 16 || actual.impacts().size() > 16
                || !actual.fields().containsAll(required.fields())
                || !actual.impacts().containsAll(required.impacts())) {
            return false;
        }
        boolean fieldsValid = actual.fields().stream()
                .allMatch(field -> field.label().length() <= 255 && field.value().length() <= 2_048);
        boolean impactsValid = actual.impacts().stream().allMatch(value -> value.length() <= 2_048);
        return fieldsValid && impactsValid;
    }

    private TrustedWriteContext trusted(AgentExecutionContext context) {
        Objects.requireNonNull(context, "context");
        QueryExecutionContext query = context.queryContext();
        return new TrustedWriteContext(query.subjectId(), query.organizationId(),
                context.sessionId(), context.requestId(), query.correlationId());
    }

    private String normalizeIdempotencyKey(String value) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isEmpty()
                || normalized.length() > MAXIMUM_IDEMPOTENCY_KEY_LENGTH) {
            throw failure(Code.INVALID_WRITE_REQUEST, "幂等键无效");
        }
        return normalized;
    }

    private String generatedId() {
        String value = idGenerator.generate();
        if (value == null || value.isBlank() || value.length() > 128) {
            throw failure(Code.INVALID_WRITE_REQUEST, "服务端标识生成失败");
        }
        return value;
    }

    private Instant now() {
        return Objects.requireNonNull(clock.now(), "clock.now");
    }

    private void record(ControlledWriteOperation operation,
                        WriteAuditEventType eventType,
                        Instant occurredAt,
                        TrustedWriteContext requestContext) {
        audit.record(new WriteAuditEvent(generatedId(), eventType,
                operation.approvalId(), operation.changeId(), requestContext.subjectId(),
                requestContext.organizationId(), requestContext.sessionId(), requestContext.requestId(),
                requestContext.correlationId(), operation.toolName(), operation.parametersDigest(),
                operation.preparedDigest(),
                operation.idempotencyKeyDigest(), operation.resourceId(), operation.state(),
                operation.failure(), occurredAt));
    }

    private ControlledWriteException failure(Code code, String message) {
        return new ControlledWriteException(code, message);
    }
}
