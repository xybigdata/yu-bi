package yubi.server.agent.write;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import yubi.agent.domain.ControlledWriteModels.ControlledWriteOperation;
import yubi.agent.domain.ControlledWriteModels.PreparedWrite;
import yubi.agent.domain.ControlledWriteModels.TrustedWriteContext;
import yubi.agent.domain.ControlledWriteModels.WriteApprovalScope;
import yubi.agent.domain.ControlledWriteModels.WriteFailureCategory;
import yubi.agent.domain.ControlledWriteModels.WriteIdempotencyScope;
import yubi.agent.domain.ControlledWriteModels.WriteOperationState;
import yubi.agent.port.WriteOperationStorePort;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public final class JdbcWriteOperationStore implements WriteOperationStorePort {

    private static final int MAXIMUM_LIST_ITEMS = 200;
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
    private static final String COLUMNS = """
            approval_id, subject_id, organization_id, session_id, request_id, correlation_id,
            tool_name, arguments_digest, prepared_digest, arguments_json, prepared_json,
            idempotency_digest, preview_json, status, expires_at, change_id, resource_type,
            resource_id, change_action, failure_code, created_at, updated_at
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ControlledWriteJsonCodec jsonCodec;
    private final RowMapper<ControlledWriteOperation> rowMapper = this::mapOperation;

    public JdbcWriteOperationStore(JdbcTemplate jdbcTemplate,
                                   StructuredValueWebMapper structuredValueMapper) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.jsonCodec = new ControlledWriteJsonCodec(structuredValueMapper);
    }

    @Override
    public Optional<ControlledWriteOperation> findByIdempotency(WriteIdempotencyScope scope) {
        Objects.requireNonNull(scope, "scope");
        return single(jdbcTemplate.query("SELECT " + COLUMNS + """
                        FROM agent_write_operation
                        WHERE subject_id = ? AND organization_id = ?
                          AND tool_name = ? AND idempotency_digest = ?
                        """, rowMapper, scope.subjectId(), scope.organizationId(),
                scope.toolName(), scope.idempotencyKeyDigest()));
    }

    @Override
    public List<ControlledWriteOperation> listByScope(WriteApprovalScope scope, int maximumItems) {
        Objects.requireNonNull(scope, "scope");
        if (maximumItems < 1 || maximumItems > MAXIMUM_LIST_ITEMS) {
            throw new IllegalArgumentException("受控写列表上限必须在 1 到 200 之间");
        }
        return List.copyOf(jdbcTemplate.query("SELECT " + COLUMNS + """
                        FROM agent_write_operation
                        WHERE subject_id = ? AND organization_id = ? AND session_id = ?
                        ORDER BY created_at DESC, approval_id DESC
                        LIMIT ?
                        """, rowMapper, scope.subjectId(), scope.organizationId(),
                scope.sessionId(), maximumItems));
    }

    @Override
    public CreateResult createPending(ControlledWriteOperation operation) {
        Objects.requireNonNull(operation, "operation");
        if (operation.state() != WriteOperationState.PENDING) {
            throw new IllegalArgumentException("只能新建待审批写操作");
        }
        requireCreateIntegrity(operation);
        PreparedWrite prepared = operation.preparedWrite();
        String argumentsJson = jsonCodec.writeArguments(operation.canonicalParameters());
        String preparedJson = jsonCodec.writePrepared(prepared);
        String previewJson = jsonCodec.writePreview(prepared.preview());
        try {
            int inserted = jdbcTemplate.update("""
                            INSERT INTO agent_write_operation (
                              approval_id, subject_id, organization_id, session_id, request_id,
                              correlation_id, tool_name, arguments_digest, prepared_digest,
                              arguments_json, prepared_json, idempotency_digest, preview_json,
                              status, expires_at, change_id, resource_type, resource_id,
                              change_action, failure_code, created_at, updated_at
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    operation.approvalId(), operation.context().subjectId(),
                    operation.context().organizationId(), operation.context().sessionId(),
                    operation.context().requestId(), operation.context().correlationId(),
                    operation.toolName(), operation.parametersDigest(), operation.preparedDigest(),
                    argumentsJson, preparedJson, operation.idempotencyKeyDigest(), previewJson,
                    operation.state().name(), timestamp(operation.expiresAt()), operation.changeId(),
                    jsonCodec.resourceType(prepared.action()), null,
                    jsonCodec.changeAction(prepared.action()), null,
                    timestamp(operation.createdAt()), timestamp(operation.createdAt()));
            if (inserted != 1) {
                throw new IllegalStateException("受控写操作未能持久化");
            }
            return new CreateResult(operation, true);
        } catch (DuplicateKeyException exception) {
            // MySQL 默认 REPEATABLE READ 下，唯一键冲突后的普通一致性读仍可能停留在旧快照。
            // 锁定读使用当前读语义，确保并发提交的幂等记录可见。
            return findByIdempotencyForUpdate(operation.idempotencyScope())
                    .map(existing -> new CreateResult(existing, false))
                    .orElseThrow(() -> new IllegalStateException("受控写审批标识冲突"));
        }
    }

    private Optional<ControlledWriteOperation> findByIdempotencyForUpdate(WriteIdempotencyScope scope) {
        return single(jdbcTemplate.query("SELECT " + COLUMNS + """
                        FROM agent_write_operation
                        WHERE subject_id = ? AND organization_id = ?
                          AND tool_name = ? AND idempotency_digest = ?
                        FOR UPDATE
                        """, rowMapper, scope.subjectId(), scope.organizationId(),
                scope.toolName(), scope.idempotencyKeyDigest()));
    }

    @Override
    public Optional<ControlledWriteOperation> lockByApprovalId(String approvalId) {
        if (approvalId == null || approvalId.isBlank()) {
            return Optional.empty();
        }
        return single(jdbcTemplate.query("SELECT " + COLUMNS + """
                        FROM agent_write_operation
                        WHERE approval_id = ?
                        FOR UPDATE
                        """, rowMapper, approvalId));
    }

    @Override
    public void saveLocked(ControlledWriteOperation previous, ControlledWriteOperation updated) {
        requireValidTransition(previous, updated);
        int changed = jdbcTemplate.update("""
                        UPDATE agent_write_operation
                        SET status = ?, resource_id = ?, failure_code = ?, updated_at = ?
                        WHERE approval_id = ? AND status = ?
                          AND subject_id = ? AND organization_id = ? AND session_id = ?
                          AND arguments_digest = ? AND prepared_digest = ?
                          AND change_id = ? AND request_id = ? AND correlation_id = ?
                          AND tool_name = ? AND idempotency_digest = ?
                        """,
                updated.state().name(), updated.resourceId(), failure(updated.failure()),
                timestamp(updated.completedAt()), previous.approvalId(), previous.state().name(),
                previous.context().subjectId(), previous.context().organizationId(),
                previous.context().sessionId(), previous.parametersDigest(), previous.preparedDigest(),
                previous.changeId(), previous.context().requestId(), previous.context().correlationId(),
                previous.toolName(), previous.idempotencyKeyDigest());
        if (changed != 1) {
            throw new OptimisticLockingFailureException("受控写状态已被并发修改");
        }
    }

    private ControlledWriteOperation mapOperation(ResultSet resultSet, int rowNumber) throws SQLException {
        try {
            String toolName = resultSet.getString("tool_name");
            String parametersDigest = digest(resultSet.getString("arguments_digest"));
            String preparedDigest = digest(resultSet.getString("prepared_digest"));
            String idempotencyDigest = digest(resultSet.getString("idempotency_digest"));
            var arguments = jsonCodec.readArguments(resultSet.getString("arguments_json"));
            PreparedWrite prepared = jsonCodec.readPrepared(resultSet.getString("prepared_json"),
                    resultSet.getString("preview_json"), toolName);
            requireClassification(resultSet, prepared);
            WriteOperationState state = WriteOperationState.valueOf(resultSet.getString("status"));
            Instant createdAt = instant(resultSet, "created_at");
            Instant updatedAt = instant(resultSet, "updated_at");
            return new ControlledWriteOperation(resultSet.getString("approval_id"),
                    resultSet.getString("change_id"),
                    new TrustedWriteContext(resultSet.getString("subject_id"),
                            resultSet.getString("organization_id"), resultSet.getString("session_id"),
                            resultSet.getString("request_id"), resultSet.getString("correlation_id")),
                    toolName, arguments, parametersDigest, preparedDigest, idempotencyDigest, prepared,
                    createdAt, instant(resultSet, "expires_at"), state,
                    resultSet.getString("resource_id"), failure(resultSet.getString("failure_code")),
                    state == WriteOperationState.PENDING ? null : updatedAt);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("受控写持久记录损坏");
        }
    }

    private void requireClassification(ResultSet resultSet, PreparedWrite prepared) throws SQLException {
        if (!jsonCodec.resourceType(prepared.action()).equals(resultSet.getString("resource_type"))
                || !jsonCodec.changeAction(prepared.action()).equals(resultSet.getString("change_action"))) {
            throw new IllegalStateException("受控写持久记录损坏");
        }
    }

    private void requireValidTransition(ControlledWriteOperation previous,
                                        ControlledWriteOperation updated) {
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(updated, "updated");
        boolean immutable = previous.approvalId().equals(updated.approvalId())
                && previous.changeId().equals(updated.changeId())
                && previous.context().equals(updated.context())
                && previous.toolName().equals(updated.toolName())
                && previous.canonicalParameters().equals(updated.canonicalParameters())
                && previous.parametersDigest().equals(updated.parametersDigest())
                && previous.preparedDigest().equals(updated.preparedDigest())
                && previous.idempotencyKeyDigest().equals(updated.idempotencyKeyDigest())
                && previous.preparedWrite().equals(updated.preparedWrite())
                && previous.createdAt().equals(updated.createdAt())
                && previous.expiresAt().equals(updated.expiresAt());
        if (!immutable || previous.state() != WriteOperationState.PENDING
                || updated.state() == WriteOperationState.PENDING || updated.completedAt() == null) {
            throw new IllegalArgumentException("受控写状态转换无效");
        }
    }

    private void requireCreateIntegrity(ControlledWriteOperation operation) {
        if (!operation.toolName().equals(operation.preparedWrite().action().toolName())
                || !SHA_256.matcher(operation.parametersDigest()).matches()
                || !SHA_256.matcher(operation.preparedDigest()).matches()
                || !SHA_256.matcher(operation.idempotencyKeyDigest()).matches()) {
            throw new IllegalArgumentException("受控写待审批记录无效");
        }
    }

    private Optional<ControlledWriteOperation> single(List<ControlledWriteOperation> operations) {
        if (operations.size() > 1) {
            throw new IllegalStateException("受控写唯一记录冲突");
        }
        return operations.stream().findFirst();
    }

    private String digest(String value) {
        if (value == null || !SHA_256.matcher(value).matches()) {
            throw new IllegalStateException("受控写持久记录损坏");
        }
        return value;
    }

    private WriteFailureCategory failure(String value) {
        return value == null ? null : WriteFailureCategory.valueOf(value);
    }

    private String failure(WriteFailureCategory value) {
        return value == null ? null : value.name();
    }

    private Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        if (value == null) {
            throw new IllegalStateException("受控写持久记录损坏");
        }
        return value.toInstant();
    }

    private Timestamp timestamp(Instant value) {
        return Timestamp.from(Objects.requireNonNull(value, "value"));
    }
}
