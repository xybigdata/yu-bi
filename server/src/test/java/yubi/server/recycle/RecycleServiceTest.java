package yubi.server.recycle;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecycleServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-07T06:00:00Z");

    @Test
    void shouldKeepExecutableItemsWhenAnotherRootIsBlocked() {
        RecycleResourceAdapter adapter = new StubAdapter(
                RecycleResourceType.SOURCE,
                Set.of("blocked-source")
        );
        RecycleService service = new DefaultRecycleService(
                List.of(adapter),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        RecycleAccess access = RecycleAccess.authenticated("user-1", "org-1", false);

        RecyclePreflight result = service.preflight(
                access,
                new RecyclePreflightCommand(
                        RecycleResourceType.SOURCE,
                        List.of("ready-source", "blocked-source")
                )
        );

        assertNotNull(result.operationToken());
        assertFalse(result.operationToken().isBlank());
        assertEquals(NOW.plusSeconds(300), result.expiresAt());
        assertEquals(2, result.items().size());
        assertEquals(RecycleItemStatus.SUCCESS, result.items().get(0).status());
        assertTrue(result.items().get(0).dependencies().isEmpty());
        assertEquals(RecycleItemStatus.BLOCKED, result.items().get(1).status());
        assertEquals("存在前置依赖，请先解除依赖", result.items().get(1).message());
        assertEquals(List.of(new RecycleDependency(
                        "view-1",
                        "订单数据视图",
                        RecycleResourceType.VIEW,
                        RecycleDependencyDepth.DIRECT,
                        true
                )),
                result.items().get(1).dependencies());
    }

    @Test
    void shouldRecheckAndExecuteReadyRootsIdempotently() {
        StubAdapter adapter = new StubAdapter(
                RecycleResourceType.SOURCE,
                Set.of("blocked-source")
        );
        RecycleService service = new DefaultRecycleService(
                List.of(adapter),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        RecycleAccess access = RecycleAccess.authenticated("user-1", "org-1", false);
        RecyclePreflight preflight = service.preflight(
                access,
                new RecyclePreflightCommand(
                        RecycleResourceType.SOURCE,
                        List.of("ready-source", "blocked-source")
                )
        );

        RecycleBatch first = service.moveToRecycle(
                access,
                new RecycleExecutionCommand(RecycleResourceType.SOURCE, preflight.operationToken(), "request-1")
        );
        RecycleBatch retried = service.moveToRecycle(
                access,
                new RecycleExecutionCommand(RecycleResourceType.SOURCE, preflight.operationToken(), "request-1")
        );

        assertEquals(first, retried);
        assertEquals(RecycleBatchState.COMPLETED, first.state());
        assertEquals(2, first.items().size());
        assertEquals(RecycleItemStatus.SUCCESS, first.items().get(0).status());
        assertEquals(RecycleItemStatus.BLOCKED, first.items().get(1).status());
        assertEquals(1, adapter.archiveCalls.get());
        assertEquals(5, adapter.preflightCalls.get());
    }

    @Test
    void shouldListAndRestoreADeletedRootToItsOriginalLocation() {
        StubAdapter adapter = new StubAdapter(RecycleResourceType.SOURCE, Set.of());
        RecycleService service = new DefaultRecycleService(
                List.of(adapter), Clock.fixed(NOW, ZoneOffset.UTC));
        RecycleAccess manager = RecycleAccess.authenticated("manager-1", "org-1", false);
        RecyclePreflight prepared = service.preflight(manager,
                new RecyclePreflightCommand(RecycleResourceType.SOURCE, List.of("source-1")));
        RecycleBatch moved = service.moveToRecycle(manager,
                new RecycleExecutionCommand(RecycleResourceType.SOURCE, prepared.operationToken(), "move-1"));
        String recordId = moved.items().getFirst().recordId();

        assertEquals(List.of(recordId), service.list(manager, RecycleResourceType.SOURCE).stream()
                .map(RecycleEntry::id).toList());

        RecycleBatch restored = service.restore(manager,
                new RecycleBulkCommand(RecycleResourceType.SOURCE, List.of(recordId), "restore-1"));

        assertEquals(RecycleItemStatus.SUCCESS, restored.items().getFirst().status());
        assertEquals(1, adapter.restoreCalls.get());
        assertTrue(service.list(manager, RecycleResourceType.SOURCE).isEmpty());
    }

    @Test
    void shouldExposeTheDeleterUsernameWithoutLosingTheUserId() {
        StubAdapter adapter = new StubAdapter(RecycleResourceType.SOURCE, Set.of());
        RecycleService service = new DefaultRecycleService(
                List.of(adapter), Clock.fixed(NOW, ZoneOffset.UTC),
                new InMemoryRecycleStore(), Runnable::run,
                actorId -> "user-1".equals(actorId) ? "alice" : null);
        RecycleAccess user = RecycleAccess.authenticated("user-1", "org-1", false);
        RecyclePreflight prepared = service.preflight(user,
                new RecyclePreflightCommand(RecycleResourceType.SOURCE, List.of("source-1")));
        service.moveToRecycle(user,
                new RecycleExecutionCommand(
                        RecycleResourceType.SOURCE, prepared.operationToken(), "move-1"));

        RecycleEntry entry = service.list(user, RecycleResourceType.SOURCE).getFirst();

        assertEquals("user-1", entry.deletedBy());
        assertEquals("alice", entry.deletedByName());
    }

    @Test
    void shouldAllowOnlyOrganizationOwnerToPermanentlyDelete() {
        StubAdapter adapter = new StubAdapter(RecycleResourceType.SOURCE, Set.of());
        RecycleService service = new DefaultRecycleService(
                List.of(adapter), Clock.fixed(NOW, ZoneOffset.UTC));
        RecycleAccess manager = RecycleAccess.authenticated("manager-1", "org-1", false);
        RecycleAccess owner = RecycleAccess.authenticated("owner-1", "org-1", true);
        RecyclePreflight prepared = service.preflight(manager,
                new RecyclePreflightCommand(RecycleResourceType.SOURCE, List.of("source-1")));
        String recordId = service.moveToRecycle(manager,
                        new RecycleExecutionCommand(RecycleResourceType.SOURCE, prepared.operationToken(), "move-1"))
                .items().getFirst().recordId();

        assertThrows(SecurityException.class, () -> service.permanentlyDelete(
                manager, new RecycleBulkCommand(RecycleResourceType.SOURCE, List.of(recordId), "delete-1")));

        RecycleBatch deleted = service.permanentlyDelete(
                owner, new RecycleBulkCommand(RecycleResourceType.SOURCE, List.of(recordId), "delete-2"));

        assertEquals(RecycleItemStatus.SUCCESS, deleted.items().getFirst().status());
        assertEquals(1, adapter.permanentDeleteCalls.get());
        assertTrue(service.list(owner, RecycleResourceType.SOURCE).isEmpty());
    }

    @Test
    void shouldRecalculateExistingExpiryWhenOwnerChangesPolicy() {
        StubAdapter adapter = new StubAdapter(RecycleResourceType.SOURCE, Set.of());
        RecycleService service = new DefaultRecycleService(
                List.of(adapter), Clock.fixed(NOW, ZoneOffset.UTC));
        RecycleAccess manager = RecycleAccess.authenticated("manager-1", "org-1", false);
        RecycleAccess owner = RecycleAccess.authenticated("owner-1", "org-1", true);
        RecyclePreflight prepared = service.preflight(manager,
                new RecyclePreflightCommand(RecycleResourceType.SOURCE, List.of("source-1")));
        service.moveToRecycle(manager,
                new RecycleExecutionCommand(RecycleResourceType.SOURCE, prepared.operationToken(), "move-1"));

        assertEquals(new RecyclePolicy(true, 30),
                service.getPolicy(owner, RecycleResourceType.SOURCE));
        assertThrows(SecurityException.class, () -> service.updatePolicy(
                manager, RecycleResourceType.SOURCE, new RecyclePolicy(true, 7)));

        service.updatePolicy(owner, RecycleResourceType.SOURCE, new RecyclePolicy(true, 7));
        assertEquals(NOW.plusSeconds(7 * 86_400L),
                service.list(owner, RecycleResourceType.SOURCE).getFirst().expiresAt());

        service.updatePolicy(owner, RecycleResourceType.SOURCE, new RecyclePolicy(false, 7));
        assertEquals(null, service.list(owner, RecycleResourceType.SOURCE).getFirst().expiresAt());
    }

    @Test
    void shouldUndoAMoveBatchWithTheServerSideToken() {
        StubAdapter adapter = new StubAdapter(RecycleResourceType.SOURCE, Set.of());
        RecycleService service = new DefaultRecycleService(
                List.of(adapter), Clock.fixed(NOW, ZoneOffset.UTC));
        RecycleAccess manager = RecycleAccess.authenticated("manager-1", "org-1", false);
        RecyclePreflight prepared = service.preflight(manager,
                new RecyclePreflightCommand(RecycleResourceType.SOURCE, List.of("source-1")));
        RecycleBatch moved = service.moveToRecycle(manager,
                new RecycleExecutionCommand(RecycleResourceType.SOURCE, prepared.operationToken(), "move-1"));

        RecycleBatch undone = service.undo(
                manager, moved.id(), moved.undoToken());

        assertEquals(RecycleOperation.UNDO, undone.operation());
        assertEquals(RecycleItemStatus.SUCCESS, undone.items().getFirst().status());
        assertEquals(1, adapter.restoreCalls.get());
        assertTrue(service.list(manager, RecycleResourceType.SOURCE).isEmpty());
    }

    @Test
    void shouldRunExpandedBatchesOverOneHundredItemsAsynchronously() {
        StubAdapter adapter = new StubAdapter(
                RecycleResourceType.SOURCE, Set.of(), 101);
        List<Runnable> pendingTasks = new ArrayList<>();
        RecycleService service = new DefaultRecycleService(
                List.of(adapter), Clock.fixed(NOW, ZoneOffset.UTC),
                new InMemoryRecycleStore(), pendingTasks::add);
        RecycleAccess manager = RecycleAccess.authenticated("manager-1", "org-1", false);
        RecyclePreflight prepared = service.preflight(manager,
                new RecyclePreflightCommand(RecycleResourceType.SOURCE, List.of("folder-1")));

        RecycleBatch accepted = service.moveToRecycle(manager,
                new RecycleExecutionCommand(RecycleResourceType.SOURCE, prepared.operationToken(), "move-large-1"));

        assertEquals(RecycleBatchState.PROCESSING, accepted.state());
        assertEquals(0, adapter.archiveCalls.get());
        assertEquals(1, pendingTasks.size());

        pendingTasks.getFirst().run();
        RecycleBatch completed = service.getBatch(
                manager, RecycleResourceType.SOURCE, accepted.id());

        assertEquals(RecycleBatchState.COMPLETED, completed.state());
        assertEquals(RecycleItemStatus.SUCCESS, completed.items().getFirst().status());
        assertNotNull(completed.undoToken());
        assertEquals(1, adapter.archiveCalls.get());
    }

    @Test
    void shouldRejectAnOperationTokenUsedThroughAnotherBusinessModule() {
        StubAdapter adapter = new StubAdapter(RecycleResourceType.SOURCE, Set.of());
        RecycleService service = new DefaultRecycleService(
                List.of(adapter), Clock.fixed(NOW, ZoneOffset.UTC));
        RecycleAccess manager = RecycleAccess.authenticated("manager-1", "org-1", false);
        RecyclePreflight prepared = service.preflight(manager,
                new RecyclePreflightCommand(RecycleResourceType.SOURCE, List.of("source-1")));

        assertThrows(IllegalArgumentException.class, () -> service.moveToRecycle(
                manager,
                new RecycleExecutionCommand(
                        RecycleResourceType.DASHBOARD,
                        prepared.operationToken(),
                        "wrong-module-1")));
        assertEquals(0, adapter.archiveCalls.get());
    }

    @Test
    void shouldRetryABlockedRootAfterItsDependentRootWasMovedInTheSameBatch() {
        Set<String> archived = new java.util.HashSet<>();
        RecycleResourceAdapter adapter = new RecycleResourceAdapter() {
            @Override
            public RecycleResourceType type() {
                return RecycleResourceType.SOURCE;
            }

            @Override
            public RecycleItemPreflight preflight(RecycleAccess access,
                                                  String rootId,
                                                  Set<String> selectedRootIds) {
                if (rootId.equals("source-1") && !archived.contains("view-like-dependent")) {
                    return RecycleItemPreflight.blocked(
                            rootId, "存在同批依赖", List.of(new RecycleDependency(
                                    "view-like-dependent", "依赖项",
                                    RecycleResourceType.SOURCE,
                                    RecycleDependencyDepth.DIRECT, true)));
                }
                return RecycleItemPreflight.ready(rootId);
            }

            @Override
            public RecycleRootSnapshot moveToRecycle(RecycleAccess access, String rootId) {
                archived.add(rootId);
                return new RecycleRootSnapshot(rootId, rootId, null, 0D, false, 1);
            }
        };
        RecycleService service = new DefaultRecycleService(
                List.of(adapter), Clock.fixed(NOW, ZoneOffset.UTC));
        RecycleAccess manager = RecycleAccess.authenticated("manager-1", "org-1", false);
        RecyclePreflight prepared = service.preflight(manager,
                new RecyclePreflightCommand(
                        RecycleResourceType.SOURCE,
                        List.of("source-1", "view-like-dependent")));

        RecycleBatch batch = service.moveToRecycle(manager,
                new RecycleExecutionCommand(
                        RecycleResourceType.SOURCE,
                        prepared.operationToken(),
                        "topology-1"));

        assertEquals(List.of(RecycleItemStatus.SUCCESS, RecycleItemStatus.SUCCESS),
                batch.items().stream().map(RecycleItemResult::status).toList());
    }

    private static final class StubAdapter implements RecycleResourceAdapter {

        private final RecycleResourceType type;
        private final Set<String> blockedIds;
        private final AtomicInteger preflightCalls = new AtomicInteger();
        private final AtomicInteger archiveCalls = new AtomicInteger();
        private final AtomicInteger restoreCalls = new AtomicInteger();
        private final AtomicInteger permanentDeleteCalls = new AtomicInteger();
        private final int expandedItemCount;

        private StubAdapter(RecycleResourceType type, Set<String> blockedIds) {
            this(type, blockedIds, 1);
        }

        private StubAdapter(RecycleResourceType type,
                            Set<String> blockedIds,
                            int expandedItemCount) {
            this.type = type;
            this.blockedIds = blockedIds;
            this.expandedItemCount = expandedItemCount;
        }

        @Override
        public RecycleResourceType type() {
            return type;
        }

        @Override
        public RecycleItemPreflight preflight(RecycleAccess access,
                                              String rootId,
                                              Set<String> selectedRootIds) {
            preflightCalls.incrementAndGet();
            if (blockedIds.contains(rootId)) {
                return RecycleItemPreflight.blocked(
                        rootId,
                        "存在前置依赖，请先解除依赖",
                        List.of(new RecycleDependency(
                                "view-1",
                                "订单数据视图",
                                RecycleResourceType.VIEW,
                                RecycleDependencyDepth.DIRECT,
                                true
                        ))
                );
            }
            return RecycleItemPreflight.ready(rootId);
        }

        @Override
        public RecycleRootSnapshot moveToRecycle(RecycleAccess access, String rootId) {
            archiveCalls.incrementAndGet();
            return new RecycleRootSnapshot(rootId, rootId, null, 0D, false, 1);
        }

        @Override
        public int expandedItemCount(RecycleAccess access, String rootId) {
            return expandedItemCount;
        }

        @Override
        public RecycleItemResult restore(RecycleAccess access, RecycleRootSnapshot snapshot) {
            restoreCalls.incrementAndGet();
            return new RecycleItemResult(snapshot.rootId(), RecycleItemStatus.SUCCESS, null, null);
        }

        @Override
        public RecycleItemResult permanentlyDelete(RecycleAccess access,
                                                   RecycleRootSnapshot snapshot) {
            permanentDeleteCalls.incrementAndGet();
            return new RecycleItemResult(snapshot.rootId(), RecycleItemStatus.SUCCESS, null, null);
        }
    }
}
