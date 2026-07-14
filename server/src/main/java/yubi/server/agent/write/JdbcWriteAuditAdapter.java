package yubi.server.agent.write;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import yubi.agent.domain.ControlledWriteModels.WriteAuditEvent;
import yubi.agent.domain.ControlledWriteModels.WriteAuditEventType;
import yubi.agent.domain.ControlledWriteModels.WriteFailureCategory;
import yubi.agent.domain.ControlledWriteModels.WriteOperationState;
import yubi.agent.port.WriteAuditPort;

import java.sql.Timestamp;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** 仅追加固定审计字段；参数值、原始幂等键及异常内容没有写入路径。 */
@Component
public final class JdbcWriteAuditAdapter implements WriteAuditPort {

    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
    private static final Set<String> ALLOWED_TOOLS = Set.of("create_chart", "rename_dashboard");

    private final JdbcTemplate jdbcTemplate;

    public JdbcWriteAuditAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    }

    @Override
    public void record(WriteAuditEvent event) {
        validate(event);
        int inserted = jdbcTemplate.update("""
                        INSERT INTO agent_write_audit_event (
                          id, approval_id, change_id, subject_id, organization_id, session_id,
                          request_id, correlation_id, tool_name, arguments_digest, prepared_digest,
                          idempotency_digest, event_type, final_status, resource_type, resource_id,
                          failure_code, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                event.eventId(), event.approvalId(), event.changeId(), event.subjectId(),
                event.organizationId(), event.sessionId(), event.requestId(), event.correlationId(),
                event.toolName(), event.parametersDigest(), event.preparedDigest(),
                event.idempotencyKeyDigest(), event.eventType().name(), event.state().name(),
                resourceType(event.toolName()), event.resourceId(), failure(event.failure()),
                Timestamp.from(event.occurredAt()));
        if (inserted != 1) {
            throw new IllegalStateException("受控写审计事件未能持久化");
        }
    }

    private void validate(WriteAuditEvent event) {
        Objects.requireNonNull(event, "event");
        requireText(event.eventId(), 36);
        requireText(event.approvalId(), 36);
        requireText(event.changeId(), 36);
        requireText(event.subjectId(), 32);
        requireText(event.organizationId(), 32);
        requireText(event.sessionId(), 36);
        requireText(event.requestId(), 36);
        requireText(event.correlationId(), 36);
        if (!ALLOWED_TOOLS.contains(event.toolName())
                || !digest(event.parametersDigest()) || !digest(event.preparedDigest())
                || !digest(event.idempotencyKeyDigest()) || event.eventType() == null
                || event.state() == null || event.occurredAt() == null) {
            throw new IllegalArgumentException("受控写审计事件无效");
        }
        if (!consistent(event.eventType(), event.state())
                || (event.state() == WriteOperationState.SUCCEEDED) != (event.resourceId() != null)
                || (event.state() == WriteOperationState.FAILED) != (event.failure() != null)) {
            throw new IllegalArgumentException("受控写审计终态无效");
        }
        if (event.resourceId() != null) {
            requireText(event.resourceId(), 64);
        }
    }

    private boolean consistent(WriteAuditEventType eventType, WriteOperationState state) {
        return switch (eventType) {
            case PROPOSED -> state == WriteOperationState.PENDING;
            case SUCCEEDED -> state == WriteOperationState.SUCCEEDED;
            case REJECTED -> state == WriteOperationState.REJECTED;
            case EXPIRED -> state == WriteOperationState.EXPIRED;
            case FAILED -> state == WriteOperationState.FAILED;
            case IDEMPOTENT_REPLAYED -> true;
        };
    }

    private String resourceType(String toolName) {
        return "create_chart".equals(toolName) ? "CHART" : "DASHBOARD";
    }

    private String failure(WriteFailureCategory value) {
        return value == null ? null : value.name();
    }

    private boolean digest(String value) {
        return value != null && SHA_256.matcher(value).matches();
    }

    private void requireText(String value, int maximumLength) {
        if (value == null || value.isBlank() || value.length() > maximumLength) {
            throw new IllegalArgumentException("受控写审计事件无效");
        }
    }
}
