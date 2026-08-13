package yubi.server.recycle;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class JdbcRecycleStore implements RecycleStore {

    private static final ObjectMapper JSON = JsonMapper.builder().build();

    private final JdbcTemplate jdbc;

    JdbcRecycleStore(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "JdbcTemplate 不能为空");
    }

    @Override
    public void savePrepared(RecyclePreparedOperation operation) {
        jdbc.update("""
                INSERT INTO recycle_operation_token (
                  token, org_id, actor_id, resource_type, root_ids_json, expires_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                """,
                operation.token(), operation.organizationId(), operation.actorId(),
                operation.resourceType().name(), JSON.writeValueAsString(operation.rootIds()),
                timestamp(operation.expiresAt()));
    }

    @Override
    public Optional<RecyclePreparedOperation> findPrepared(String token) {
        return jdbc.query("""
                        SELECT token, org_id, actor_id, resource_type, root_ids_json, expires_at
                        FROM recycle_operation_token WHERE token = ?
                        """,
                (resultSet, rowNumber) -> new RecyclePreparedOperation(
                        resultSet.getString("token"),
                        resultSet.getString("actor_id"),
                        resultSet.getString("org_id"),
                        RecycleResourceType.valueOf(resultSet.getString("resource_type")),
                        JSON.readerForListOf(String.class).readValue(resultSet.getString("root_ids_json")),
                        instant(resultSet, "expires_at")
                ), token).stream().findFirst();
    }

    @Override
    public Optional<RecycleBatch> findBatchByRequest(String actorId,
                                                     String organizationId,
                                                     String clientRequestId) {
        return jdbc.query("""
                        SELECT id, resource_type, operation, state, undo_token, undo_expires_at,
                               result_json, created_at
                        FROM recycle_batch
                        WHERE org_id = ? AND actor_id = ? AND client_request_id = ?
                        """,
                JdbcRecycleStore::mapBatch,
                organizationId, actorId, clientRequestId).stream().findFirst();
    }

    @Override
    public Optional<RecycleBatch> findBatch(String actorId,
                                            String organizationId,
                                            String batchId) {
        return jdbc.query("""
                        SELECT id, resource_type, operation, state, undo_token, undo_expires_at,
                               result_json, created_at
                        FROM recycle_batch
                        WHERE id = ? AND org_id = ? AND actor_id = ?
                        """,
                JdbcRecycleStore::mapBatch,
                batchId, organizationId, actorId).stream().findFirst();
    }

    @Override
    public RecycleBatch saveBatch(String actorId,
                                  String organizationId,
                                  String clientRequestId,
                                  RecycleBatch batch) {
        try {
            jdbc.update("""
                    INSERT INTO recycle_batch (
                      id, org_id, actor_id, resource_type, operation, state,
                      client_request_id, undo_token, undo_expires_at, result_json,
                      created_at, completed_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    batch.id(), organizationId, actorId, batch.resourceType().name(),
                    batch.operation().name(), batch.state().name(), clientRequestId, batch.undoToken(),
                    timestamp(batch.undoExpiresAt()), JSON.writeValueAsString(batch.items()),
                    timestamp(batch.createdAt()),
                    batch.state() == RecycleBatchState.COMPLETED
                            ? timestamp(batch.createdAt()) : null);
            return batch;
        } catch (DuplicateKeyException duplicate) {
            return findBatchByRequest(actorId, organizationId, clientRequestId).orElseThrow();
        }
    }

    @Override
    public void completeBatch(String actorId,
                              String organizationId,
                              RecycleBatch batch,
                              Instant completedAt) {
        int updated = jdbc.update("""
                UPDATE recycle_batch
                SET state = ?, undo_token = ?, undo_expires_at = ?, result_json = ?, completed_at = ?
                WHERE id = ? AND org_id = ? AND actor_id = ? AND state = ?
                """,
                batch.state().name(), batch.undoToken(), timestamp(batch.undoExpiresAt()),
                JSON.writeValueAsString(batch.items()), timestamp(completedAt),
                batch.id(), organizationId, actorId, RecycleBatchState.PROCESSING.name());
        if (updated != 1) {
            throw new IllegalStateException("异步回收批次不存在或已结束: " + batch.id());
        }
    }

    @Override
    public void saveRecord(RecycleRecord record) {
        RecycleRootSnapshot snapshot = record.snapshot();
        jdbc.update("""
                INSERT INTO recycle_record (
                  id, org_id, resource_type, root_id, original_name, original_parent_id,
                  original_index, root_kind, expanded_item_count, deleted_by, deleted_at, expires_at
                  , subtree_json
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                record.id(), record.organizationId(), record.resourceType().name(), snapshot.rootId(),
                snapshot.originalName(), snapshot.originalParentId(), snapshot.originalIndex(),
                snapshot.folder() ? "FOLDER" : "RESOURCE", snapshot.expandedItemCount(),
                record.deletedBy(), timestamp(record.deletedAt()), timestamp(record.expiresAt()),
                JSON.writeValueAsString(snapshot.nodes()));
    }

    @Override
    public Optional<RecycleRecord> findRecord(String id) {
        return jdbc.query("""
                        SELECT id, org_id, resource_type, root_id, original_name, original_parent_id,
                               original_index, root_kind, expanded_item_count, subtree_json,
                               deleted_by, deleted_at, expires_at
                        FROM recycle_record WHERE id = ?
                        """,
                JdbcRecycleStore::mapRecord, id).stream().findFirst();
    }

    @Override
    public List<RecycleRecord> listRecords(String organizationId,
                                           RecycleResourceType resourceType) {
        return jdbc.query("""
                        SELECT id, org_id, resource_type, root_id, original_name, original_parent_id,
                               original_index, root_kind, expanded_item_count, subtree_json,
                               deleted_by, deleted_at, expires_at
                        FROM recycle_record
                        WHERE org_id = ? AND resource_type = ?
                        ORDER BY deleted_at DESC, id ASC
                        """,
                JdbcRecycleStore::mapRecord, organizationId, resourceType.name());
    }

    @Override
    public void deleteRecord(String id) {
        jdbc.update("DELETE FROM recycle_record WHERE id = ?", id);
    }

    @Override
    public Optional<RecyclePolicy> findPolicy(String organizationId,
                                              RecycleResourceType resourceType) {
        return jdbc.query("""
                        SELECT enabled, retention_days FROM recycle_policy
                        WHERE org_id = ? AND resource_type = ?
                        """,
                (resultSet, rowNumber) -> new RecyclePolicy(
                        resultSet.getBoolean("enabled"),
                        resultSet.getInt("retention_days")),
                organizationId, resourceType.name()).stream().findFirst();
    }

    @Override
    public void savePolicy(String organizationId,
                           RecycleResourceType resourceType,
                           RecyclePolicy policy,
                           String updatedBy,
                           Instant updatedAt) {
        int updated = jdbc.update("""
                UPDATE recycle_policy
                SET enabled = ?, retention_days = ?, updated_by = ?, updated_at = ?
                WHERE org_id = ? AND resource_type = ?
                """,
                policy.enabled(), policy.retentionDays(), updatedBy, timestamp(updatedAt),
                organizationId, resourceType.name());
        if (updated == 0) {
            jdbc.update("""
                    INSERT INTO recycle_policy (
                      org_id, resource_type, enabled, retention_days, updated_by, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    organizationId, resourceType.name(), policy.enabled(), policy.retentionDays(),
                    updatedBy, timestamp(updatedAt));
        }
    }

    @Override
    public void recalculateExpiry(String organizationId,
                                  RecycleResourceType resourceType,
                                  Instant expiresAt) {
        jdbc.update("""
                UPDATE recycle_record SET expires_at = ?
                WHERE org_id = ? AND resource_type = ?
                """, timestamp(expiresAt), organizationId, resourceType.name());
    }

    @Override
    public List<RecycleRecord> findExpired(Instant expiresAtOrBefore, int limit) {
        return jdbc.query("""
                        SELECT id, org_id, resource_type, root_id, original_name, original_parent_id,
                               original_index, root_kind, expanded_item_count, subtree_json,
                               deleted_by, deleted_at, expires_at
                        FROM recycle_record
                        WHERE expires_at IS NOT NULL AND expires_at <= ?
                        ORDER BY expires_at ASC, id ASC
                        LIMIT ?
                        """,
                JdbcRecycleStore::mapRecord, timestamp(expiresAtOrBefore), limit);
    }

    @Override
    public void saveAudit(RecycleAuditEvent event) {
        jdbc.update("""
                INSERT INTO recycle_audit_event (
                  id, batch_id, record_id, org_id, resource_type, root_id,
                  action, result, reason, actor_id, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                event.id(), event.batchId(), event.recordId(), event.organizationId(),
                event.resourceType().name(), event.rootId(), event.action().name(),
                event.result(), event.reason(), event.actorId(), timestamp(event.createdAt()));
    }

    @Override
    public void pruneAudit(Instant createdBefore) {
        jdbc.update("DELETE FROM recycle_audit_event WHERE created_at < ?", timestamp(createdBefore));
    }

    private static RecycleBatch mapBatch(ResultSet resultSet, int rowNumber) throws SQLException {
        return new RecycleBatch(
                resultSet.getString("id"),
                RecycleResourceType.valueOf(resultSet.getString("resource_type")),
                RecycleOperation.valueOf(resultSet.getString("operation")),
                RecycleBatchState.valueOf(resultSet.getString("state")),
                instant(resultSet, "created_at"),
                resultSet.getString("undo_token"),
                instant(resultSet, "undo_expires_at"),
                JSON.readerForListOf(RecycleItemResult.class).readValue(resultSet.getString("result_json"))
        );
    }

    private static RecycleRecord mapRecord(ResultSet resultSet, int rowNumber) throws SQLException {
        RecycleRootSnapshot snapshot = new RecycleRootSnapshot(
                resultSet.getString("root_id"),
                resultSet.getString("original_name"),
                resultSet.getString("original_parent_id"),
                resultSet.getDouble("original_index"),
                "FOLDER".equals(resultSet.getString("root_kind")),
                resultSet.getInt("expanded_item_count"),
                JSON.readerForListOf(RecycleNodeSnapshot.class)
                        .readValue(resultSet.getString("subtree_json")));
        return new RecycleRecord(
                resultSet.getString("id"),
                resultSet.getString("org_id"),
                RecycleResourceType.valueOf(resultSet.getString("resource_type")),
                snapshot,
                resultSet.getString("deleted_by"),
                instant(resultSet, "deleted_at"),
                instant(resultSet, "expires_at"));
    }

    private static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
