package yubi.server.agent.write;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ControlledWriteRetentionServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-12T08:00:00Z");

    private DataSource dataSource;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:agent-retention-" + java.util.UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource = source;
        jdbcTemplate = new JdbcTemplate(source);
        new ResourceDatabasePopulator(new ClassPathResource(
                "db/migration/V2026.07.12__2.0.0.agent-controlled-write.sql"))
                .execute(dataSource);
    }

    @Test
    void shouldExpirePendingOperationAndAppendOnlyRedactedAuditInOneTransaction() {
        insertSession("session-expired", "ACTIVE", NOW.minus(Duration.ofHours(1)));
        insertSession("session-active", "ACTIVE", NOW.plus(Duration.ofHours(1)));
        insertOperation("approval-expired", "session-expired", "PENDING",
                NOW.minus(Duration.ofMinutes(1)), NOW.minus(Duration.ofHours(1)), "secret-quarterly-revenue");
        insertOperation("approval-future", "session-active", "PENDING",
                NOW.plus(Duration.ofMinutes(1)), NOW.minus(Duration.ofHours(1)), "future-secret");

        service(20).maintain();
        service(20).maintain();

        assertEquals("EXPIRED", value("""
                SELECT status FROM agent_write_operation WHERE approval_id = 'approval-expired'
                """, String.class));
        assertEquals(Timestamp.from(NOW), value("""
                SELECT updated_at FROM agent_write_operation WHERE approval_id = 'approval-expired'
                """, Timestamp.class));
        assertEquals("PENDING", value("""
                SELECT status FROM agent_write_operation WHERE approval_id = 'approval-future'
                """, String.class));
        assertEquals("EXPIRED", value("""
                SELECT status FROM agent_workspace_session WHERE id = 'session-expired'
                """, String.class));
        assertEquals("ACTIVE", value("""
                SELECT status FROM agent_workspace_session WHERE id = 'session-active'
                """, String.class));

        Map<String, Object> audit = jdbcTemplate.queryForMap("""
                SELECT approval_id, subject_id, organization_id, session_id, request_id,
                       correlation_id, tool_name, arguments_digest, prepared_digest,
                       idempotency_digest, event_type, final_status, resource_type,
                       resource_id, failure_code
                FROM agent_write_audit_event
                WHERE approval_id = 'approval-expired'
                """);
        assertEquals("EXPIRED", audit.get("event_type"));
        assertEquals("EXPIRED", audit.get("final_status"));
        assertEquals("request-approval-expired", audit.get("request_id"));
        assertEquals("correlation-approval-expired", audit.get("correlation_id"));
        assertEquals(digest('a'), audit.get("arguments_digest"));
        assertEquals(1, value("""
                SELECT COUNT(*) FROM agent_write_audit_event
                WHERE approval_id = 'approval-expired' AND event_type = 'EXPIRED'
                """, Integer.class));
        assertFalse(audit.toString().contains("secret-quarterly-revenue"));
    }

    @Test
    void shouldBoundEveryMaintenancePhaseAndDeleteOnlyBeyondRetentionWindows() {
        for (int index = 0; index < 3; index++) {
            insertOperation("approval-pending-" + index, "pending-session-" + index, "PENDING",
                    NOW.minus(Duration.ofMinutes(1)), NOW.minus(Duration.ofHours(1)), "pending-secret-" + index);
            insertOperation("approval-terminal-" + index, "terminal-session-" + index, "SUCCEEDED",
                    NOW.minus(Duration.ofDays(60)), NOW.minus(Duration.ofDays(40)), "terminal-secret-" + index);
            insertSession("old-session-" + index, "EXPIRED", NOW.minus(Duration.ofDays(40)));
            insertAudit("old-audit-" + index, "old-approval-" + index, NOW.minus(Duration.ofDays(40)));
            insertAudit("terminal-audit-" + index, "approval-terminal-" + index,
                    NOW.minus(Duration.ofDays(40)));
        }
        insertPendingAudit("pending-audit", "approval-pending-2", NOW.minus(Duration.ofDays(40)));

        service(2).maintain();

        assertEquals(2, value("""
                SELECT COUNT(*) FROM agent_write_operation
                WHERE approval_id LIKE 'approval-pending-%' AND status = 'EXPIRED'
                """, Integer.class));
        assertEquals(1, value("""
                SELECT COUNT(*) FROM agent_write_operation
                WHERE approval_id LIKE 'approval-pending-%' AND status = 'PENDING'
                """, Integer.class));
        assertEquals(1, value("""
                SELECT COUNT(*) FROM agent_write_operation
                WHERE approval_id LIKE 'approval-terminal-%'
                """, Integer.class));
        assertEquals(1, value("""
                SELECT COUNT(*) FROM agent_workspace_session WHERE id LIKE 'old-session-%'
                """, Integer.class));
        assertEquals(1, value("""
                SELECT COUNT(*) FROM agent_write_audit_event WHERE id LIKE 'old-audit-%'
                """, Integer.class));
        assertEquals(1, value("""
                SELECT COUNT(*) FROM agent_write_audit_event WHERE id = 'pending-audit'
                """, Integer.class));
        assertEquals(1, value("""
                SELECT COUNT(*)
                FROM agent_write_audit_event audit_event
                WHERE audit_event.id = 'terminal-audit-2'
                  AND EXISTS (
                    SELECT 1 FROM agent_write_operation operation_record
                    WHERE operation_record.approval_id = audit_event.approval_id
                  )
                """, Integer.class));
        assertEquals(2, value("""
                SELECT COUNT(*) FROM agent_write_audit_event WHERE event_type = 'EXPIRED'
                """, Integer.class));
    }

    @Test
    void shouldRollbackExpiredStateWhenAuditCannotBeAppended() {
        insertOperation("approval-rollback", "session-rollback", "PENDING",
                NOW.minus(Duration.ofMinutes(1)), NOW.minus(Duration.ofHours(1)), "rollback-secret");
        jdbcTemplate.execute("DROP TABLE agent_write_audit_event");

        assertThrows(DataAccessException.class, () -> service(20).maintain());

        assertEquals("PENDING", value("""
                SELECT status FROM agent_write_operation WHERE approval_id = 'approval-rollback'
                """, String.class));
        assertEquals(Timestamp.from(NOW.minus(Duration.ofHours(1))), value("""
                SELECT updated_at FROM agent_write_operation WHERE approval_id = 'approval-rollback'
                """, Timestamp.class));
    }

    @Test
    void shouldRejectUnboundedOrInconsistentRetentionConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> service(
                Duration.ofDays(366), Duration.ofDays(30), Duration.ofDays(366), Duration.ofMinutes(5), 20));
        assertThrows(IllegalArgumentException.class, () -> service(
                Duration.ofDays(30), Duration.ofDays(366), Duration.ofDays(366), Duration.ofMinutes(5), 20));
        assertThrows(IllegalArgumentException.class, () -> service(
                Duration.ofDays(30), Duration.ofDays(30), Duration.ofDays(3_651), Duration.ofMinutes(5), 20));
        assertThrows(IllegalArgumentException.class, () -> service(
                Duration.ofDays(30), Duration.ofDays(30), Duration.ofDays(20), Duration.ofMinutes(5), 20));
        assertThrows(IllegalArgumentException.class, () -> service(
                Duration.ofDays(30), Duration.ofDays(30), Duration.ofDays(30), Duration.ofHours(2), 20));
        assertThrows(IllegalArgumentException.class, () -> service(
                Duration.ofDays(30), Duration.ofDays(30), Duration.ofDays(30), Duration.ofMinutes(5), 501));
    }

    private ControlledWriteRetentionService service(int batchSize) {
        return service(Duration.ofDays(30), Duration.ofDays(30), Duration.ofDays(30),
                Duration.ofMinutes(5), batchSize);
    }

    private ControlledWriteRetentionService service(Duration operationRetention,
                                                     Duration sessionRetention,
                                                     Duration auditRetention,
                                                     Duration interval,
                                                     int batchSize) {
        return new ControlledWriteRetentionService(jdbcTemplate,
                new DataSourceTransactionManager(dataSource), operationRetention, sessionRetention,
                auditRetention, interval, batchSize, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private void insertSession(String id, String status, Instant expiresAt) {
        jdbcTemplate.update("""
                INSERT INTO agent_workspace_session
                    (id, subject_id, organization_id, status, expires_at, created_at, last_access_at)
                VALUES (?, 'user-1', 'org-1', ?, ?, ?, ?)
                """, id, status, timestamp(expiresAt), timestamp(expiresAt.minus(Duration.ofHours(1))),
                timestamp(expiresAt.minus(Duration.ofHours(1))));
    }

    private void insertOperation(String approvalId,
                                 String sessionId,
                                 String status,
                                 Instant expiresAt,
                                 Instant updatedAt,
                                 String sensitiveValue) {
        String resourceId = "SUCCEEDED".equals(status) ? "resource-" + approvalId : null;
        jdbcTemplate.update("""
                INSERT INTO agent_write_operation (
                  approval_id, subject_id, organization_id, session_id, request_id,
                  correlation_id, tool_name, arguments_digest, prepared_digest,
                  arguments_json, prepared_json, idempotency_digest, preview_json,
                  status, expires_at, change_id, resource_type, resource_id,
                  change_action, failure_code, created_at, updated_at
                ) VALUES (?, 'user-1', 'org-1', ?, ?, ?, 'create_chart', ?, ?, ?, ?, ?, ?, ?, ?, ?,
                          'CHART', ?, 'CREATE', NULL, ?, ?)
                """, approvalId, sessionId, "request-" + approvalId, "correlation-" + approvalId,
                digest('a'), digest('b'), "{\"name\":\"" + sensitiveValue + "\"}",
                "{\"prepared\":\"" + sensitiveValue + "\"}", digestFor(approvalId),
                "{\"preview\":\"" + sensitiveValue + "\"}", status, timestamp(expiresAt),
                "change-" + approvalId, resourceId, timestamp(updatedAt.minus(Duration.ofHours(1))),
                timestamp(updatedAt));
    }

    private void insertAudit(String id, String approvalId, Instant createdAt) {
        jdbcTemplate.update("""
                INSERT INTO agent_write_audit_event (
                  id, approval_id, change_id, subject_id, organization_id, session_id,
                  request_id, correlation_id, tool_name, arguments_digest, prepared_digest,
                  idempotency_digest, event_type, final_status, resource_type, resource_id,
                  failure_code, created_at
                ) VALUES (?, ?, ?, 'user-1', 'org-1', ?, ?, ?, 'create_chart', ?, ?, ?,
                          'SUCCEEDED', 'SUCCEEDED', 'CHART', ?, NULL, ?)
                """, id, approvalId, "change-" + approvalId, "session-" + approvalId,
                "request-" + approvalId, "correlation-" + approvalId, digest('a'), digest('b'),
                digestFor(approvalId), "resource-" + approvalId, timestamp(createdAt));
    }

    private void insertPendingAudit(String id, String approvalId, Instant createdAt) {
        jdbcTemplate.update("""
                INSERT INTO agent_write_audit_event (
                  id, approval_id, change_id, subject_id, organization_id, session_id,
                  request_id, correlation_id, tool_name, arguments_digest, prepared_digest,
                  idempotency_digest, event_type, final_status, resource_type, resource_id,
                  failure_code, created_at
                ) VALUES (?, ?, ?, 'user-1', 'org-1', ?, ?, ?, 'create_chart', ?, ?, ?,
                          'PROPOSED', 'PENDING', 'CHART', NULL, NULL, ?)
                """, id, approvalId, "change-" + approvalId, "session-" + approvalId,
                "request-" + approvalId, "correlation-" + approvalId, digest('a'), digest('b'),
                digestFor(approvalId), timestamp(createdAt));
    }

    private <T> T value(String sql, Class<T> type) {
        return jdbcTemplate.queryForObject(sql, type);
    }

    private String digest(char value) {
        return Character.toString(value).repeat(64);
    }

    private String digestFor(String value) {
        String hex = Integer.toHexString(value.hashCode());
        return (hex + "0".repeat(64)).substring(0, 64);
    }

    private Timestamp timestamp(Instant value) {
        return Timestamp.from(value);
    }
}
