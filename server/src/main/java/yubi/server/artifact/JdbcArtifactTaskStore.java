package yubi.server.artifact;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

final class JdbcArtifactTaskStore implements ArtifactTaskStore {

    private static final String SELECT_COLUMNS = """
            id, owner_key, display_name, media_type, file_suffix, source_module, state,
            accepted_at, deadline_at, completed_at, expires_at, blob_key,
            failure_code, failure_hint, failure_trace_id, trace_id
            """;

    private static final RowMapper<StoredArtifactTask> TASK_MAPPER = JdbcArtifactTaskStore::mapTask;

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    JdbcArtifactTaskStore(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "JdbcTemplate 不能为空");
        DataSource dataSource = Objects.requireNonNull(jdbc.getDataSource(), "数据源不能为空");
        this.transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @Override
    public boolean insertIfOwnerBelowLimit(StoredArtifactTask task, int maxActiveTasks) {
        Objects.requireNonNull(task, "产物任务不能为空");
        if (maxActiveTasks < 1) {
            throw new IllegalArgumentException("活动任务上限必须为正数");
        }
        return Boolean.TRUE.equals(transactions.execute(status -> {
            lockOwner(task.ownerKey());
            List<String> activeIds = jdbc.queryForList("""
                    SELECT id FROM artifact_task
                    WHERE owner_key = ? AND state IN ('QUEUED', 'RUNNING')
                      AND deadline_at > ?
                    FOR UPDATE
                    """, String.class, task.ownerKey(), timestamp(task.acceptedAt()));
            if (activeIds.size() >= maxActiveTasks) {
                return false;
            }
            insert(task);
            return true;
        }));
    }

    @Override
    public Optional<StoredArtifactTask> find(String id) {
        List<StoredArtifactTask> tasks = jdbc.query("SELECT " + SELECT_COLUMNS
                + " FROM artifact_task WHERE id = ?", TASK_MAPPER, id);
        return tasks.stream().findFirst();
    }

    @Override
    public List<StoredArtifactTask> findActiveByOwner(String ownerKey) {
        return jdbc.query("SELECT " + SELECT_COLUMNS + """
                 FROM artifact_task
                 WHERE owner_key = ? AND state IN ('QUEUED', 'RUNNING')
                 ORDER BY accepted_at DESC, id ASC
                """, TASK_MAPPER, ownerKey);
    }

    @Override
    public List<StoredArtifactTask> findTerminalByOwner(String ownerKey, int offset, int limit) {
        return jdbc.query("SELECT " + SELECT_COLUMNS + """
                 FROM artifact_task
                 WHERE owner_key = ? AND state IN ('READY', 'FAILED', 'TIMED_OUT')
                 ORDER BY CASE WHEN state IN ('FAILED', 'TIMED_OUT') THEN 0 ELSE 1 END,
                          accepted_at DESC, id ASC
                 LIMIT ? OFFSET ?
                """, TASK_MAPPER, ownerKey, limit, offset);
    }

    @Override
    public boolean updateIfState(String id,
                                 Set<ArtifactTaskState> expected,
                                 UnaryOperator<StoredArtifactTask> update) {
        Objects.requireNonNull(expected, "预期状态不能为空");
        Objects.requireNonNull(update, "任务更新函数不能为空");
        return Boolean.TRUE.equals(transactions.execute(status -> {
            StoredArtifactTask current = lockTask(id).orElse(null);
            if (current == null || !expected.contains(current.state())) {
                return false;
            }
            StoredArtifactTask changed = Objects.requireNonNull(update.apply(current), "更新后的任务不能为空");
            if (!current.id().equals(changed.id()) || !current.ownerKey().equals(changed.ownerKey())) {
                throw new IllegalArgumentException("条件更新不得改变任务 ID 或 owner");
            }
            ArtifactFailure failure = changed.failure();
            int updated = jdbc.update("""
                    UPDATE artifact_task SET
                      state = ?, completed_at = ?, expires_at = ?, blob_key = ?,
                      failure_code = ?, failure_hint = ?, failure_trace_id = ?
                    WHERE id = ? AND state = ?
                    """,
                    changed.state().name(), timestamp(changed.completedAt()), timestamp(changed.expiresAt()),
                    changed.blobKey(),
                    failure == null ? null : failure.code(), failure == null ? null : failure.hint(),
                    failure == null ? null : failure.traceId(), current.id(), current.state().name());
            return updated == 1;
        }));
    }

    @Override
    public Optional<StoredArtifactTask> deleteIfState(String id, Set<ArtifactTaskState> expected) {
        Objects.requireNonNull(expected, "预期状态不能为空");
        StoredArtifactTask observed = find(id).orElse(null);
        if (observed == null) {
            return Optional.empty();
        }
        Optional<StoredArtifactTask> deleted = transactions.execute(status -> {
            lockOwner(observed.ownerKey());
            StoredArtifactTask current = lockTask(id).orElse(null);
            if (current == null || !current.ownerKey().equals(observed.ownerKey())
                    || !expected.contains(current.state())) {
                removeOwnerGuardIfUnused(observed.ownerKey());
                return Optional.empty();
            }
            int affected = jdbc.update("DELETE FROM artifact_task WHERE id = ? AND state = ?",
                    current.id(), current.state().name());
            if (affected != 1) {
                return Optional.empty();
            }
            removeOwnerGuardIfUnused(current.ownerKey());
            return Optional.of(current);
        });
        return deleted == null ? Optional.empty() : deleted;
    }

    @Override
    public List<StoredArtifactTask> findMaintenanceCandidates(Instant deadlineAtOrBefore,
                                                              Instant completedBefore,
                                                              int limit) {
        Objects.requireNonNull(deadlineAtOrBefore, "活动任务截止时间不能为空");
        Objects.requireNonNull(completedBefore, "终态任务完成时间不能为空");
        if (limit < 1) {
            throw new IllegalArgumentException("维护任务批量大小必须为正数");
        }
        return jdbc.query("SELECT " + SELECT_COLUMNS + """
                 FROM artifact_task
                 WHERE (state IN ('QUEUED', 'RUNNING') AND deadline_at <= ?)
                    OR (state IN ('READY', 'FAILED', 'TIMED_OUT')
                        AND expires_at IS NOT NULL AND expires_at < ?)
                 ORDER BY CASE
                            WHEN state IN ('QUEUED', 'RUNNING') THEN deadline_at
                            ELSE completed_at
                          END ASC,
                          id ASC
                 LIMIT ?
                """, TASK_MAPPER,
                timestamp(deadlineAtOrBefore), timestamp(completedBefore), limit);
    }

    private void lockOwner(String ownerKey) {
        try {
            jdbc.update("INSERT INTO artifact_task_owner_guard (owner_key) VALUES (?)", ownerKey);
        } catch (DuplicateKeyException ignored) {
            // 已存在的 guard 正是并发调用需要共同锁定的行。
        }
        List<String> locked = jdbc.queryForList("""
                SELECT owner_key FROM artifact_task_owner_guard
                WHERE owner_key = ? FOR UPDATE
                """, String.class, ownerKey);
        if (locked.isEmpty()) {
            throw new IllegalStateException("无法锁定产物任务 owner");
        }
    }

    private void insert(StoredArtifactTask task) {
        ArtifactFailure failure = task.failure();
        jdbc.update("""
                INSERT INTO artifact_task (
                  id, owner_key, display_name, media_type, file_suffix, source_module, state,
                  accepted_at, deadline_at, completed_at, expires_at, blob_key,
                  failure_code, failure_hint, failure_trace_id, trace_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                task.id(), task.ownerKey(), task.descriptor().displayName(), task.descriptor().mediaType(),
                task.descriptor().suffix(), task.descriptor().source(), task.state().name(),
                timestamp(task.acceptedAt()),
                timestamp(task.deadlineAt()), timestamp(task.completedAt()), timestamp(task.expiresAt()),
                task.blobKey(),
                failure == null ? null : failure.code(), failure == null ? null : failure.hint(),
                failure == null ? null : failure.traceId(), task.traceId());
    }

    private Optional<StoredArtifactTask> lockTask(String id) {
        List<StoredArtifactTask> tasks = jdbc.query("SELECT " + SELECT_COLUMNS
                + " FROM artifact_task WHERE id = ? FOR UPDATE", TASK_MAPPER, id);
        return tasks.stream().findFirst();
    }

    private void removeOwnerGuardIfUnused(String ownerKey) {
        Integer remaining = jdbc.queryForObject(
                "SELECT COUNT(*) FROM artifact_task WHERE owner_key = ?", Integer.class, ownerKey);
        if (remaining != null && remaining == 0) {
            jdbc.update("DELETE FROM artifact_task_owner_guard WHERE owner_key = ?", ownerKey);
        }
    }

    private static StoredArtifactTask mapTask(ResultSet resultSet, int rowNumber) throws SQLException {
        ArtifactFailure failure = resultSet.getString("failure_code") == null ? null : new ArtifactFailure(
                resultSet.getString("failure_code"),
                resultSet.getString("failure_hint"),
                resultSet.getString("failure_trace_id"));
        return new StoredArtifactTask(
                resultSet.getString("id"),
                resultSet.getString("owner_key"),
                new ArtifactDescriptor(resultSet.getString("display_name"),
                        resultSet.getString("media_type"), resultSet.getString("file_suffix"),
                        resultSet.getString("source_module")),
                ArtifactTaskState.valueOf(resultSet.getString("state")),
                instant(resultSet, "accepted_at"),
                instant(resultSet, "deadline_at"),
                instant(resultSet, "completed_at"),
                instant(resultSet, "expires_at"),
                resultSet.getString("blob_key"),
                failure,
                resultSet.getString("trace_id"));
    }

    private static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
