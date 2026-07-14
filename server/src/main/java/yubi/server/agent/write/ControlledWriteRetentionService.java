package yubi.server.agent.write;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 在工作区请求流量中机会式维护受控写账本，避免引入额外调度器。
 * 每轮、每类记录均受同一个批量上限约束。
 * 过期转换与审计写入在独立事务中原子完成。
 */
@Service
public final class ControlledWriteRetentionService {

    private static final Duration MAXIMUM_OPERATION_RETENTION = Duration.ofDays(365);
    private static final Duration MAXIMUM_SESSION_RETENTION = Duration.ofDays(365);
    private static final Duration MAXIMUM_AUDIT_RETENTION = Duration.ofDays(3_650);
    private static final Duration MAXIMUM_MAINTENANCE_INTERVAL = Duration.ofHours(1);
    private static final int MAXIMUM_BATCH_SIZE = 500;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate requiresNew;
    private final RetentionPolicy policy;
    private final Clock clock;
    private final AtomicReference<Instant> nextMaintenanceAt = new AtomicReference<>(Instant.EPOCH);

    @Autowired
    public ControlledWriteRetentionService(
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager,
            @Value("${yubi.agent.workspace.retention.terminal-operation-days:90}") long operationDays,
            @Value("${yubi.agent.workspace.retention.expired-session-days:30}") long sessionDays,
            @Value("${yubi.agent.workspace.retention.audit-event-days:365}") long auditDays,
            @Value("${yubi.agent.workspace.retention.cleanup-interval-seconds:300}") long intervalSeconds,
            @Value("${yubi.agent.workspace.retention.batch-size:100}") int batchSize) {
        this(jdbcTemplate, transactionManager,
                policy(operationDays, sessionDays, auditDays, intervalSeconds, batchSize),
                Clock.systemUTC());
    }

    ControlledWriteRetentionService(JdbcTemplate jdbcTemplate,
                                    PlatformTransactionManager transactionManager,
                                    Duration operationRetention,
                                    Duration sessionRetention,
                                    Duration auditRetention,
                                    Duration maintenanceInterval,
                                    int batchSize,
                                    Clock clock) {
        this(jdbcTemplate, transactionManager,
                new RetentionPolicy(operationRetention, sessionRetention, auditRetention,
                        maintenanceInterval, batchSize), clock);
    }

    private ControlledWriteRetentionService(JdbcTemplate jdbcTemplate,
                                            PlatformTransactionManager transactionManager,
                                            RetentionPolicy policy,
                                            Clock clock) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.requiresNew = new TransactionTemplate(
                Objects.requireNonNull(transactionManager, "transactionManager"));
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.policy = Objects.requireNonNull(policy, "policy");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public void maintain() {
        Instant now = clock.instant();
        if (!claimOpportunity(now)) {
            return;
        }
        requiresNew.executeWithoutResult(status -> maintainInTransaction(now));
    }

    private boolean claimOpportunity(Instant now) {
        while (true) {
            Instant current = nextMaintenanceAt.get();
            if (now.isBefore(current)) {
                return false;
            }
            Instant next = now.plus(policy.maintenanceInterval());
            if (nextMaintenanceAt.compareAndSet(current, next)) {
                return true;
            }
        }
    }

    private void maintainInTransaction(Instant now) {
        expirePendingOperations(now);
        expireSessions(now);
        deleteTerminalOperations(now.minus(policy.operationRetention()));
        deleteExpiredSessions(now.minus(policy.sessionRetention()));
        deleteAuditEvents(now.minus(policy.auditRetention()));
    }

    private void expirePendingOperations(Instant now) {
        List<String> approvals = jdbcTemplate.queryForList("""
                SELECT approval_id
                FROM agent_write_operation
                WHERE status = 'PENDING' AND expires_at <= ?
                ORDER BY expires_at, approval_id
                LIMIT ?
                FOR UPDATE
                """, String.class, timestamp(now), policy.batchSize());
        for (String approvalId : approvals) {
            int changed = jdbcTemplate.update("""
                    UPDATE agent_write_operation
                    SET status = 'EXPIRED', resource_id = NULL, failure_code = NULL, updated_at = ?
                    WHERE approval_id = ? AND status = 'PENDING' AND expires_at <= ?
                    """, timestamp(now), approvalId, timestamp(now));
            if (changed != 1) {
                throw new IllegalStateException("过期受控写状态未能原子转换");
            }
            int audited = jdbcTemplate.update("""
                    INSERT INTO agent_write_audit_event (
                      id, approval_id, change_id, subject_id, organization_id, session_id,
                      request_id, correlation_id, tool_name, arguments_digest, prepared_digest,
                      idempotency_digest, event_type, final_status, resource_type, resource_id,
                      failure_code, created_at
                    )
                    SELECT ?, approval_id, change_id, subject_id, organization_id, session_id,
                           request_id, correlation_id, tool_name, arguments_digest, prepared_digest,
                           idempotency_digest, 'EXPIRED', 'EXPIRED', resource_type, NULL, NULL, ?
                    FROM agent_write_operation
                    WHERE approval_id = ? AND status = 'EXPIRED'
                    """, UUID.randomUUID().toString(), timestamp(now), approvalId);
            if (audited != 1) {
                throw new IllegalStateException("过期受控写审计未能原子追加");
            }
        }
    }

    private void expireSessions(Instant now) {
        List<String> sessions = jdbcTemplate.queryForList("""
                SELECT id
                FROM agent_workspace_session
                WHERE status = 'ACTIVE' AND expires_at <= ?
                ORDER BY expires_at, id
                LIMIT ?
                FOR UPDATE
                """, String.class, timestamp(now), policy.batchSize());
        for (String sessionId : sessions) {
            int changed = jdbcTemplate.update("""
                    UPDATE agent_workspace_session
                    SET status = 'EXPIRED'
                    WHERE id = ? AND status = 'ACTIVE' AND expires_at <= ?
                    """, sessionId, timestamp(now));
            if (changed != 1) {
                throw new IllegalStateException("过期工作区会话未能原子转换");
            }
        }
    }

    private void deleteTerminalOperations(Instant cutoff) {
        List<String> approvals = jdbcTemplate.queryForList("""
                SELECT approval_id
                FROM agent_write_operation
                WHERE status IN ('SUCCEEDED', 'REJECTED', 'EXPIRED', 'FAILED')
                  AND updated_at <= ?
                ORDER BY updated_at, approval_id
                LIMIT ?
                FOR UPDATE
                """, String.class, timestamp(cutoff), policy.batchSize());
        for (String approvalId : approvals) {
            jdbcTemplate.update("""
                    DELETE FROM agent_write_operation
                    WHERE approval_id = ?
                      AND status IN ('SUCCEEDED', 'REJECTED', 'EXPIRED', 'FAILED')
                      AND updated_at <= ?
                    """, approvalId, timestamp(cutoff));
        }
    }

    private void deleteExpiredSessions(Instant cutoff) {
        List<String> sessions = jdbcTemplate.queryForList("""
                SELECT session_record.id
                FROM agent_workspace_session session_record
                WHERE session_record.status = 'EXPIRED' AND session_record.expires_at <= ?
                  AND NOT EXISTS (
                    SELECT 1 FROM agent_write_operation operation_record
                    WHERE operation_record.session_id = session_record.id
                  )
                ORDER BY session_record.expires_at, session_record.id
                LIMIT ?
                FOR UPDATE
                """, String.class, timestamp(cutoff), policy.batchSize());
        for (String sessionId : sessions) {
            jdbcTemplate.update("""
                    DELETE FROM agent_workspace_session
                    WHERE id = ? AND status = 'EXPIRED' AND expires_at <= ?
                      AND NOT EXISTS (
                        SELECT 1 FROM agent_write_operation WHERE session_id = ?
                      )
                    """, sessionId, timestamp(cutoff), sessionId);
        }
    }

    private void deleteAuditEvents(Instant cutoff) {
        List<String> events = jdbcTemplate.queryForList("""
                SELECT audit_event.id
                FROM agent_write_audit_event audit_event
                WHERE audit_event.created_at <= ?
                  AND NOT EXISTS (
                    SELECT 1 FROM agent_write_operation operation_record
                    WHERE operation_record.approval_id = audit_event.approval_id
                  )
                ORDER BY audit_event.created_at, audit_event.id
                LIMIT ?
                FOR UPDATE
                """, String.class, timestamp(cutoff), policy.batchSize());
        for (String eventId : events) {
            jdbcTemplate.update("""
                    DELETE FROM agent_write_audit_event
                    WHERE id = ? AND created_at <= ?
                      AND NOT EXISTS (
                        SELECT 1 FROM agent_write_operation operation_record
                        WHERE operation_record.approval_id = agent_write_audit_event.approval_id
                      )
                    """, eventId, timestamp(cutoff));
        }
    }

    private Timestamp timestamp(Instant value) {
        return Timestamp.from(value);
    }

    private static RetentionPolicy policy(long operationDays,
                                          long sessionDays,
                                          long auditDays,
                                          long intervalSeconds,
                                          int batchSize) {
        requireRange(operationDays, 1, 365, "终态写操作保留天数");
        requireRange(sessionDays, 1, 365, "过期工作区会话保留天数");
        requireRange(auditDays, 1, 3_650, "受控写审计保留天数");
        requireRange(intervalSeconds, 1, 3_600, "保留维护间隔秒数");
        return new RetentionPolicy(Duration.ofDays(operationDays), Duration.ofDays(sessionDays),
                Duration.ofDays(auditDays), Duration.ofSeconds(intervalSeconds), batchSize);
    }

    private static void requireRange(long value, long minimum, long maximum, String name) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + "无效");
        }
    }

    private record RetentionPolicy(Duration operationRetention,
                                   Duration sessionRetention,
                                   Duration auditRetention,
                                   Duration maintenanceInterval,
                                   int batchSize) {
        private RetentionPolicy {
            requireDuration(operationRetention, MAXIMUM_OPERATION_RETENTION, "终态写操作保留期");
            requireDuration(sessionRetention, MAXIMUM_SESSION_RETENTION, "过期工作区会话保留期");
            requireDuration(auditRetention, MAXIMUM_AUDIT_RETENTION, "受控写审计保留期");
            requireDuration(maintenanceInterval, MAXIMUM_MAINTENANCE_INTERVAL, "保留维护间隔");
            if (auditRetention.compareTo(operationRetention) < 0) {
                throw new IllegalArgumentException("受控写审计保留期不能短于操作保留期");
            }
            if (batchSize < 1 || batchSize > MAXIMUM_BATCH_SIZE) {
                throw new IllegalArgumentException("保留维护批量必须在 1 到 500 之间");
            }
        }

        private static void requireDuration(Duration value, Duration maximum, String name) {
            if (value == null || value.isZero() || value.isNegative() || value.compareTo(maximum) > 0) {
                throw new IllegalArgumentException(name + "无效");
            }
        }
    }
}
