package yubi.server.agent.write;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import yubi.agent.domain.ControlledWriteModels.ControlledWriteOperation;
import yubi.agent.domain.ControlledWriteModels.CreateChartAction;
import yubi.agent.domain.ControlledWriteModels.PreparedWrite;
import yubi.agent.domain.ControlledWriteModels.PreviewField;
import yubi.agent.domain.ControlledWriteModels.RenameDashboardAction;
import yubi.agent.domain.ControlledWriteModels.TrustedWriteContext;
import yubi.agent.domain.ControlledWriteModels.WriteApprovalScope;
import yubi.agent.domain.ControlledWriteModels.WriteAuditEvent;
import yubi.agent.domain.ControlledWriteModels.WriteAuditEventType;
import yubi.agent.domain.ControlledWriteModels.WriteOperationState;
import yubi.agent.domain.ControlledWriteModels.WritePreview;
import yubi.agent.domain.StructuredValue;
import yubi.agent.domain.StructuredValue.ObjectValue;
import yubi.agent.port.WriteOperationStorePort.CreateResult;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcControlledWritePersistenceTest {

    private static final Instant NOW = Instant.parse("2026-07-12T02:00:00Z");

    private JdbcTemplate jdbcTemplate;
    private JdbcWriteOperationStore store;
    private JdbcWriteAuditAdapter audit;
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:agent-write-" + java.util.UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=5000;DB_CLOSE_DELAY=-1");
        dataSource = source;
        jdbcTemplate = new JdbcTemplate(source);
        createSchema();
        store = new JdbcWriteOperationStore(jdbcTemplate, new StructuredValueWebMapper());
        audit = new JdbcWriteAuditAdapter(jdbcTemplate);
    }

    @Test
    void shouldRoundTripAllowedActionsAndKeepSessionListsInsideTrustedScope() {
        ControlledWriteOperation chart = chart("approval-chart", "user-1", "org-1", "session-1",
                digest('1'), NOW);
        ControlledWriteOperation rename = rename("approval-rename", "user-1", "org-1", "session-1",
                digest('2'), NOW.plusSeconds(1));
        store.createPending(chart);
        store.createPending(rename);
        store.createPending(chart("approval-user-2", "user-2", "org-1", "session-1",
                digest('3'), NOW.plusSeconds(2)));
        store.createPending(chart("approval-org-2", "user-1", "org-2", "session-1",
                digest('4'), NOW.plusSeconds(3)));
        store.createPending(chart("approval-session-2", "user-1", "org-1", "session-2",
                digest('5'), NOW.plusSeconds(4)));

        assertEquals(chart, store.findByIdempotency(chart.idempotencyScope()).orElseThrow());
        assertTrue(store.findByIdempotency(new yubi.agent.domain.ControlledWriteModels.WriteIdempotencyScope(
                "user-2", "org-1", chart.toolName(), chart.idempotencyKeyDigest())).isEmpty());

        List<ControlledWriteOperation> scoped = store.listByScope(
                new WriteApprovalScope("user-1", "org-1", "session-1"), 10);
        assertEquals(List.of("approval-rename", "approval-chart"),
                scoped.stream().map(ControlledWriteOperation::approvalId).toList());
        assertInstanceOf(RenameDashboardAction.class, scoped.get(0).preparedWrite().action());
        assertInstanceOf(CreateChartAction.class, scoped.get(1).preparedWrite().action());
        assertEquals(List.of("approval-rename"), store.listByScope(
                        new WriteApprovalScope("user-1", "org-1", "session-1"), 1)
                .stream().map(ControlledWriteOperation::approvalId).toList());
        assertThrows(IllegalArgumentException.class, () -> store.listByScope(
                new WriteApprovalScope("user-1", "org-1", "session-1"), 201));
    }

    @Test
    void shouldReturnSingleExistingOperationWhenSameScopeIsCreatedConcurrently() throws Exception {
        ControlledWriteOperation first = chart("approval-concurrent-1", "user-1", "org-1", "session-1",
                digest('6'), NOW);
        ControlledWriteOperation second = chart("approval-concurrent-2", "user-1", "org-1", "session-2",
                digest('6'), NOW);
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<CreateResult> firstResult = executor.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                return store.createPending(first);
            });
            Future<CreateResult> secondResult = executor.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                return store.createPending(second);
            });

            List<CreateResult> results = List.of(firstResult.get(10, TimeUnit.SECONDS),
                    secondResult.get(10, TimeUnit.SECONDS));
            assertEquals(1, results.stream().filter(CreateResult::created).count());
            assertEquals(1, results.stream().filter(result -> !result.created()).count());
            assertEquals(1, results.stream().map(result -> result.operation().approvalId()).distinct().count());
            assertEquals(1, jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM agent_write_operation", Integer.class));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldHoldRowLockAndProtectTerminalUpdateFromStateRaces() throws Exception {
        ControlledWriteOperation pending = chart("approval-lock", "user-1", "org-1", "session-1",
                digest('7'), NOW);
        store.createPending(pending);
        TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> holder = executor.submit(() -> transaction.executeWithoutResult(status -> {
                store.lockByApprovalId(pending.approvalId()).orElseThrow();
                locked.countDown();
                await(release);
            }));
            assertTrue(locked.await(5, TimeUnit.SECONDS));
            Future<ControlledWriteOperation> waiter = executor.submit(() -> transaction.execute(status ->
                    store.lockByApprovalId(pending.approvalId()).orElseThrow()));

            assertThrows(TimeoutException.class, () -> waiter.get(200, TimeUnit.MILLISECONDS));
            release.countDown();
            holder.get(5, TimeUnit.SECONDS);
            assertEquals(pending, waiter.get(5, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            executor.shutdownNow();
        }

        ControlledWriteOperation succeeded = pending.succeeded("chart-created", NOW.plusSeconds(10));
        transaction.executeWithoutResult(status -> {
            ControlledWriteOperation lockedOperation = store.lockByApprovalId(pending.approvalId()).orElseThrow();
            store.saveLocked(lockedOperation, succeeded);
        });
        assertEquals(WriteOperationState.SUCCEEDED,
                store.lockByApprovalId(pending.approvalId()).orElseThrow().state());
        assertThrows(OptimisticLockingFailureException.class,
                () -> store.saveLocked(pending, pending.rejected(NOW.plusSeconds(11))));
        assertEquals("chart-created", jdbcTemplate.queryForObject("""
                SELECT resource_id FROM agent_write_operation WHERE approval_id = ?
                """, String.class, pending.approvalId()));
    }

    @Test
    void shouldRejectUnknownFieldsDamagedJsonAndHighRiskActions() throws Exception {
        ControlledWriteOperation operation = chart("approval-corrupt", "user-1", "org-1", "session-1",
                digest('8'), NOW);
        store.createPending(operation);
        String original = jdbcTemplate.queryForObject("""
                SELECT prepared_json FROM agent_write_operation WHERE approval_id = ?
                """, String.class, operation.approvalId());
        JsonMapper mapper = JsonMapper.builder().build();

        ObjectNode highRisk = (ObjectNode) mapper.readTree(original);
        ((ObjectNode) highRisk.get("action")).put("type", "delete_dashboard");
        updatePrepared(operation.approvalId(), mapper.writeValueAsString(highRisk));
        assertThrows(IllegalStateException.class,
                () -> store.lockByApprovalId(operation.approvalId()));

        ObjectNode unknownField = (ObjectNode) mapper.readTree(original);
        ((ObjectNode) unknownField.get("action")).put("sql", "DELETE FROM dashboard");
        updatePrepared(operation.approvalId(), mapper.writeValueAsString(unknownField));
        assertThrows(IllegalStateException.class,
                () -> store.lockByApprovalId(operation.approvalId()));

        updatePrepared(operation.approvalId(), original);
        jdbcTemplate.update("""
                UPDATE agent_write_operation SET arguments_json = ? WHERE approval_id = ?
                """, "not-json", operation.approvalId());
        assertThrows(IllegalStateException.class,
                () -> store.lockByApprovalId(operation.approvalId()));
    }

    @Test
    void shouldAppendOnlySafeAuditFieldsWithoutParameterValuesOrRawErrors() {
        WriteAuditEvent event = new WriteAuditEvent("event-1", WriteAuditEventType.PROPOSED,
                "approval-audit", "change-audit", "user-1", "org-1", "session-1",
                "request-1", "correlation-1", "create_chart", digest('a'), digest('b'),
                digest('c'), null, WriteOperationState.PENDING, null, NOW);

        audit.record(event);

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM agent_write_audit_event");
        String persisted = row.toString();
        assertEquals("PROPOSED", row.get("event_type"));
        assertEquals("CHART", row.get("resource_type"));
        assertEquals("change-audit", row.get("change_id"));
        assertEquals(digest('a'), row.get("arguments_digest"));
        assertEquals(digest('b'), row.get("prepared_digest"));
        assertFalse(persisted.contains("季度收入（机密）"));
        assertFalse(persisted.contains("raw-idempotency-key"));
        assertFalse(persisted.contains("SQLException"));
        assertFalse(row.containsKey("arguments_json"));
        assertFalse(row.containsKey("prepared_json"));
        assertFalse(row.containsKey("parameters_json"));
        assertThrows(DuplicateKeyException.class, () -> audit.record(event));
    }

    private ControlledWriteOperation chart(String approvalId,
                                            String subjectId,
                                            String organizationId,
                                            String sessionId,
                                            String idempotencyDigest,
                                            Instant createdAt) {
        CreateChartAction action = new CreateChartAction("季度收入（机密）", "view-1", "folder-1", "说明");
        WritePreview preview = new WritePreview("创建图表", "CHART", "view-1",
                List.of(new PreviewField("图表名称", action.name()),
                        new PreviewField("数据视图", action.viewId())),
                List.of("创建一个未发布图表草稿"));
        ObjectValue arguments = new ObjectValue(Map.of(
                "name", StructuredValue.text(action.name()),
                "viewId", StructuredValue.text(action.viewId()),
                "parentId", StructuredValue.text(action.parentId()),
                "description", StructuredValue.text(action.description())));
        return operation(approvalId, subjectId, organizationId, sessionId, idempotencyDigest,
                createdAt, arguments, new PreparedWrite(action, digest('d'), preview));
    }

    private ControlledWriteOperation rename(String approvalId,
                                             String subjectId,
                                             String organizationId,
                                             String sessionId,
                                             String idempotencyDigest,
                                             Instant createdAt) {
        RenameDashboardAction action = new RenameDashboardAction("dashboard-1", "经营驾驶舱");
        WritePreview preview = new WritePreview("重命名仪表盘", "DASHBOARD", "dashboard-1",
                List.of(new PreviewField("仪表盘", action.dashboardId()),
                        new PreviewField("新名称", action.newName())),
                List.of("同步更新仪表盘及其导航名称"));
        ObjectValue arguments = new ObjectValue(Map.of(
                "dashboardId", StructuredValue.text(action.dashboardId()),
                "newName", StructuredValue.text(action.newName())));
        return operation(approvalId, subjectId, organizationId, sessionId, idempotencyDigest,
                createdAt, arguments, new PreparedWrite(action, digest('e'), preview));
    }

    private ControlledWriteOperation operation(String approvalId,
                                                String subjectId,
                                                String organizationId,
                                                String sessionId,
                                                String idempotencyDigest,
                                                Instant createdAt,
                                                ObjectValue arguments,
                                                PreparedWrite prepared) {
        TrustedWriteContext context = new TrustedWriteContext(subjectId, organizationId, sessionId,
                "request-1", "correlation-1");
        return new ControlledWriteOperation(approvalId, "change-" + approvalId, context,
                prepared.action().toolName(), arguments, digest('a'), digest('b'), idempotencyDigest,
                prepared, createdAt, createdAt.plusSeconds(3_600), WriteOperationState.PENDING,
                null, null, null);
    }

    private String digest(char value) {
        return Character.toString(value).repeat(64);
    }

    private void updatePrepared(String approvalId, String value) {
        jdbcTemplate.update("""
                UPDATE agent_write_operation SET prepared_json = ? WHERE approval_id = ?
                """, value, approvalId);
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("等待行锁测试超时");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待行锁测试被中断");
        }
    }

    private void createSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE agent_write_operation (
                  approval_id varchar(36) NOT NULL PRIMARY KEY,
                  subject_id varchar(32) NOT NULL,
                  organization_id varchar(32) NOT NULL,
                  session_id varchar(36) NOT NULL,
                  request_id varchar(36) NOT NULL,
                  correlation_id varchar(36) NOT NULL,
                  tool_name varchar(32) NOT NULL,
                  arguments_digest char(64) NOT NULL,
                  prepared_digest char(64) NOT NULL,
                  arguments_json text NOT NULL,
                  prepared_json text NOT NULL,
                  idempotency_digest char(64) NOT NULL,
                  preview_json text NOT NULL,
                  status varchar(24) NOT NULL,
                  expires_at timestamp(3) NOT NULL,
                  change_id varchar(36) NOT NULL,
                  resource_type varchar(32),
                  resource_id varchar(64),
                  change_action varchar(32),
                  failure_code varchar(64),
                  created_at timestamp(3) NOT NULL,
                  updated_at timestamp(3) NOT NULL,
                  CONSTRAINT uq_agent_write_idempotency UNIQUE
                    (subject_id, organization_id, tool_name, idempotency_digest)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE agent_write_audit_event (
                  id varchar(36) NOT NULL PRIMARY KEY,
                  approval_id varchar(36) NOT NULL,
                  change_id varchar(36) NOT NULL,
                  subject_id varchar(32) NOT NULL,
                  organization_id varchar(32) NOT NULL,
                  session_id varchar(36) NOT NULL,
                  request_id varchar(36) NOT NULL,
                  correlation_id varchar(36) NOT NULL,
                  tool_name varchar(32) NOT NULL,
                  arguments_digest char(64) NOT NULL,
                  prepared_digest char(64) NOT NULL,
                  idempotency_digest char(64) NOT NULL,
                  event_type varchar(32) NOT NULL,
                  final_status varchar(24) NOT NULL,
                  resource_type varchar(32),
                  resource_id varchar(64),
                  failure_code varchar(64),
                  created_at timestamp(3) NOT NULL
                )
                """);
    }
}
