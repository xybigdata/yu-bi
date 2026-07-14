package yubi.server.agent.write;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yubi.agent.api.AgentExecutionContext;
import yubi.server.agent.ServerAgentExecutionContextFactory;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@Slf4j
public class ServerAgentWorkspaceSessionService {

    private static final String ACTIVE = "ACTIVE";
    private static final String EXPIRED = "EXPIRED";

    private final JdbcTemplate jdbcTemplate;
    private final ServerAgentExecutionContextFactory contextFactory;
    private final Duration lifetime;
    private final Clock clock;
    private final ControlledWriteRetentionService retentionService;

    @Autowired
    public ServerAgentWorkspaceSessionService(
            JdbcTemplate jdbcTemplate,
            ServerAgentExecutionContextFactory contextFactory,
            @Value("${yubi.agent.workspace.session-lifetime-minutes:480}") long lifetimeMinutes,
            ControlledWriteRetentionService retentionService) {
        this(jdbcTemplate, contextFactory, Duration.ofMinutes(lifetimeMinutes), Clock.systemUTC(),
                retentionService);
    }

    ServerAgentWorkspaceSessionService(JdbcTemplate jdbcTemplate,
                                       ServerAgentExecutionContextFactory contextFactory,
                                       Duration lifetime,
                                       Clock clock) {
        this(jdbcTemplate, contextFactory, lifetime, clock, null);
    }

    ServerAgentWorkspaceSessionService(JdbcTemplate jdbcTemplate,
                                       ServerAgentExecutionContextFactory contextFactory,
                                       Duration lifetime,
                                       Clock clock,
                                       ControlledWriteRetentionService retentionService) {
        if (lifetime == null || lifetime.isZero() || lifetime.isNegative() || lifetime.compareTo(Duration.ofDays(7)) > 0) {
            throw new IllegalArgumentException("Agent 工作区会话有效期无效");
        }
        this.jdbcTemplate = jdbcTemplate;
        this.contextFactory = contextFactory;
        this.lifetime = lifetime;
        this.clock = clock;
        this.retentionService = retentionService;
    }

    @Transactional
    public AgentWorkspaceSession create(String organizationId) {
        AgentExecutionContext context = contextFactory.create(organizationId);
        maintainRetention();
        Instant now = clock.instant();
        lockOrganization(organizationId);
        AgentWorkspaceSession reusable = findReusable(
                context.queryContext().subjectId(), organizationId, now);
        if (reusable != null) {
            jdbcTemplate.update("""
                    UPDATE agent_workspace_session
                    SET last_access_at = ?
                    WHERE id = ? AND status = ? AND expires_at > ?
                    """, Timestamp.from(now), reusable.sessionId(), ACTIVE, Timestamp.from(now));
            return reusable;
        }
        Instant expiresAt = now.plus(lifetime);
        jdbcTemplate.update("""
                INSERT INTO agent_workspace_session
                    (id, subject_id, organization_id, status, expires_at, created_at, last_access_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, context.sessionId(), context.queryContext().subjectId(), organizationId, ACTIVE,
                Timestamp.from(expiresAt), Timestamp.from(now), Timestamp.from(now));
        return new AgentWorkspaceSession(context.sessionId(), context.queryContext().subjectId(),
                organizationId, expiresAt);
    }

    private void lockOrganization(String organizationId) {
        String locked = jdbcTemplate.queryForObject(
                "SELECT id FROM organization WHERE id = ? FOR UPDATE", String.class, organizationId);
        if (!organizationId.equals(locked)) {
            throw denied();
        }
    }

    private AgentWorkspaceSession findReusable(String subjectId,
                                                String organizationId,
                                                Instant now) {
        List<AgentWorkspaceSession> sessions = jdbcTemplate.query("""
                SELECT id, subject_id, organization_id, expires_at
                FROM agent_workspace_session
                WHERE subject_id = ? AND organization_id = ?
                  AND status = ? AND expires_at > ?
                ORDER BY CASE WHEN EXISTS (
                    SELECT 1
                    FROM agent_write_operation operation_record
                    WHERE operation_record.session_id = agent_workspace_session.id
                      AND operation_record.status = 'PENDING'
                      AND operation_record.expires_at > ?
                ) THEN 0 ELSE 1 END,
                  last_access_at DESC, created_at DESC, id DESC
                LIMIT 1
                FOR UPDATE
                """, (resultSet, rowNum) -> new AgentWorkspaceSession(
                resultSet.getString("id"),
                resultSet.getString("subject_id"),
                resultSet.getString("organization_id"),
                resultSet.getTimestamp("expires_at").toInstant()),
                subjectId, organizationId, ACTIVE, Timestamp.from(now), Timestamp.from(now));
        return sessions.isEmpty() ? null : sessions.getFirst();
    }

    @Transactional(noRollbackFor = AgentWorkspaceSessionException.class)
    public AgentExecutionContext resume(String organizationId, String sessionId) {
        if (!validIdentifier(sessionId)) {
            throw denied();
        }
        AgentWorkspaceSession stored = find(sessionId);
        AgentExecutionContext current = contextFactory.resume(organizationId, sessionId);
        Instant now = clock.instant();
        if (!stored.subjectId().equals(current.queryContext().subjectId())
                || !stored.organizationId().equals(organizationId)) {
            throw denied();
        }
        if (!now.isBefore(stored.expiresAt())) {
            jdbcTemplate.update("UPDATE agent_workspace_session SET status = ?, last_access_at = ? WHERE id = ?",
                    EXPIRED, Timestamp.from(now), sessionId);
            throw new AgentWorkspaceSessionException("WORKSPACE_SESSION_EXPIRED", "Agent 工作区会话已过期");
        }
        maintainRetention();
        int updated = jdbcTemplate.update("""
                UPDATE agent_workspace_session
                SET last_access_at = ?
                WHERE id = ? AND status = ?
                """, Timestamp.from(now), sessionId, ACTIVE);
        if (updated != 1) {
            throw new AgentWorkspaceSessionException("WORKSPACE_SESSION_EXPIRED", "Agent 工作区会话已过期");
        }
        return current;
    }

    private AgentWorkspaceSession find(String sessionId) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT id, subject_id, organization_id, expires_at
                    FROM agent_workspace_session
                    WHERE id = ? AND status = ?
                    """, (resultSet, rowNum) -> new AgentWorkspaceSession(
                    resultSet.getString("id"),
                    resultSet.getString("subject_id"),
                    resultSet.getString("organization_id"),
                    resultSet.getTimestamp("expires_at").toInstant()), sessionId, ACTIVE);
        } catch (EmptyResultDataAccessException exception) {
            throw denied();
        }
    }

    private boolean validIdentifier(String value) {
        try {
            return value != null && java.util.UUID.fromString(value).toString().equals(value);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private void maintainRetention() {
        if (retentionService == null) {
            return;
        }
        try {
            retentionService.maintain();
        } catch (RuntimeException ignored) {
            // 维护事务与当前请求隔离；账本暂时不可维护时不阻断正常工作区请求。
            log.warn("Agent 受控写保留维护失败，将在后续工作区请求中重试");
        }
    }

    private AgentWorkspaceSessionException denied() {
        return new AgentWorkspaceSessionException("WORKSPACE_SESSION_DENIED", "Agent 工作区会话不可用");
    }
}
