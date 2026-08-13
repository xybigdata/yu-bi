package yubi.server.recycle;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JdbcRecycleStoreTest {

    private static final Instant NOW = Instant.parse("2026-08-07T06:00:00Z");

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:recycle-store-" + java.util.UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        new ResourceDatabasePopulator(
                new ClassPathResource("db/migration/V2026.08.07__2.0.0.recycle-bin.sql"))
                .execute(source);
    }

    @Test
    void shouldPersistPreparedOperationAndIdempotentBatchAcrossServiceInstances() {
        AtomicInteger archiveCalls = new AtomicInteger();
        RecycleResourceAdapter adapter = new RecycleResourceAdapter() {
            @Override
            public RecycleResourceType type() {
                return RecycleResourceType.VIEW;
            }

            @Override
            public RecycleItemPreflight preflight(RecycleAccess access,
                                                  String rootId,
                                                  Set<String> selectedRootIds) {
                return RecycleItemPreflight.ready(rootId);
            }

            @Override
            public RecycleRootSnapshot moveToRecycle(RecycleAccess access, String rootId) {
                archiveCalls.incrementAndGet();
                return new RecycleRootSnapshot(rootId, "订单视图", "folder-1", 2D, false, 1);
            }
        };
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        RecycleAccess access = RecycleAccess.authenticated("user-1", "org-1", false);
        RecyclePreflight prepared = new DefaultRecycleService(
                List.of(adapter), clock, new JdbcRecycleStore(jdbc))
                .preflight(access, new RecyclePreflightCommand(
                        RecycleResourceType.VIEW, List.of("view-1")));

        RecycleBatch executed = new DefaultRecycleService(
                List.of(adapter), clock, new JdbcRecycleStore(jdbc))
                .moveToRecycle(access, new RecycleExecutionCommand(
                        RecycleResourceType.VIEW, prepared.operationToken(), "request-1"));
        RecycleBatch retried = new DefaultRecycleService(
                List.of(adapter), clock, new JdbcRecycleStore(jdbc))
                .moveToRecycle(access, new RecycleExecutionCommand(
                        RecycleResourceType.VIEW, prepared.operationToken(), "request-1"));

        assertEquals(executed, retried);
        assertEquals(1, archiveCalls.get());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM recycle_record", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM recycle_batch", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM recycle_operation_token", Integer.class));
    }

    @Test
    void shouldDeleteExpiredRecordsWriteAuditAndHonorGlobalSwitch() {
        AtomicInteger permanentDeleteCalls = new AtomicInteger();
        RecycleResourceAdapter adapter = new RecycleResourceAdapter() {
            @Override
            public RecycleResourceType type() {
                return RecycleResourceType.DASHBOARD;
            }

            @Override
            public RecycleItemPreflight preflight(RecycleAccess access,
                                                  String rootId,
                                                  Set<String> selectedRootIds) {
                return RecycleItemPreflight.ready(rootId);
            }

            @Override
            public RecycleRootSnapshot moveToRecycle(RecycleAccess access, String rootId) {
                return new RecycleRootSnapshot(rootId, "经营看板", null, 0D, false, 1);
            }

            @Override
            public RecycleItemResult permanentlyDelete(RecycleAccess access,
                                                       RecycleRootSnapshot snapshot) {
                permanentDeleteCalls.incrementAndGet();
                return new RecycleItemResult(
                        snapshot.rootId(), RecycleItemStatus.SUCCESS, null, null);
            }
        };
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        RecycleAccess manager = RecycleAccess.authenticated("manager-1", "org-1", false);
        DefaultRecycleService service = new DefaultRecycleService(
                List.of(adapter), clock, new JdbcRecycleStore(jdbc));
        RecyclePreflight prepared = service.preflight(manager,
                new RecyclePreflightCommand(RecycleResourceType.DASHBOARD, List.of("dashboard-1")));
        service.moveToRecycle(manager,
                new RecycleExecutionCommand(
                        RecycleResourceType.DASHBOARD, prepared.operationToken(), "move-1"));
        jdbc.update("UPDATE recycle_record SET expires_at = ?",
                java.sql.Timestamp.from(NOW.minusSeconds(1)));

        assertEquals(new RecycleMaintenanceResult(0, 0, 0), service.maintain(false, 100));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM recycle_record", Integer.class));

        assertEquals(new RecycleMaintenanceResult(1, 1, 0), service.maintain(true, 100));
        assertEquals(1, permanentDeleteCalls.get());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM recycle_record", Integer.class));
        assertEquals(4, jdbc.queryForObject("SELECT COUNT(*) FROM recycle_audit_event", Integer.class));
    }
}
