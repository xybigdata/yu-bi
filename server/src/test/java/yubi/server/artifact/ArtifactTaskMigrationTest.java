package yubi.server.artifact;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ArtifactTaskMigrationTest {

    private static final String FORWARD = "db/migration/V2026.07.24__2.0.0.artifact-task.sql";
    private static final String ROLLBACK = "db/migration/R2026.07.24__2.0.0.artifact-task.sql";
    private static final String METADATA_FORWARD =
            "db/migration/V2026.07.28__2.0.0.artifact-task-metadata.sql";
    private static final String METADATA_ROLLBACK =
            "db/migration/R2026.07.28__2.0.0.artifact-task-metadata.sql";

    @Test
    void migrationMustCreateOnlySafeTaskMetadataAndRollbackCleanly() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:artifact-migration-" + java.util.UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        new ResourceDatabasePopulator(new ClassPathResource(FORWARD)).execute(dataSource);

        assertEquals(Set.of("artifact_task", "artifact_task_owner_guard"), userTables(jdbc));
        assertEquals(List.of(
                        "id", "owner_key", "display_name", "media_type", "file_suffix", "state",
                        "accepted_at", "deadline_at", "completed_at", "blob_key",
                        "failure_code", "failure_hint", "failure_trace_id", "trace_id"),
                columns(jdbc, "artifact_task"));

        new ResourceDatabasePopulator(new ClassPathResource(METADATA_FORWARD)).execute(dataSource);

        assertEquals(List.of(
                        "id", "owner_key", "display_name", "media_type", "file_suffix", "source_module", "state",
                        "accepted_at", "deadline_at", "completed_at", "expires_at", "blob_key",
                        "failure_code", "failure_hint", "failure_trace_id", "trace_id"),
                columns(jdbc, "artifact_task"));
        String schema = String.join(" ", columns(jdbc, "artifact_task")).toLowerCase(Locale.ROOT);
        for (String forbidden : List.of("request", "token", "query", "payload", "parameter", "password")) {
            assertFalse(schema.contains(forbidden), "任务表不得持久化敏感字段：" + forbidden);
        }
        assertEquals(Set.of(
                        "idx_artifact_task_owner_state",
                        "idx_artifact_task_deadline",
                        "idx_artifact_task_expiry"),
                indexes(jdbc, "artifact_task"));

        new ResourceDatabasePopulator(new ClassPathResource(METADATA_ROLLBACK)).execute(dataSource);

        assertEquals(List.of(
                        "id", "owner_key", "display_name", "media_type", "file_suffix", "state",
                        "accepted_at", "deadline_at", "completed_at", "blob_key",
                        "failure_code", "failure_hint", "failure_trace_id", "trace_id"),
                columns(jdbc, "artifact_task"));

        new ResourceDatabasePopulator(new ClassPathResource(ROLLBACK)).execute(dataSource);

        assertEquals(Set.of(), userTables(jdbc));
    }

    @Test
    void metadataMigrationMustTolerateALegacyRetentionIndexWithTheSamePurpose() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:artifact-legacy-index-" + java.util.UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        new ResourceDatabasePopulator(new ClassPathResource(FORWARD)).execute(dataSource);
        jdbc.execute("CREATE INDEX idx_artifact_task_retention ON artifact_task (state, completed_at)");

        new ResourceDatabasePopulator(new ClassPathResource(METADATA_FORWARD)).execute(dataSource);

        assertEquals(Set.of(
                        "idx_artifact_task_owner_state",
                        "idx_artifact_task_deadline",
                        "idx_artifact_task_retention",
                        "idx_artifact_task_expiry"),
                indexes(jdbc, "artifact_task"));
    }

    private Set<String> userTables(JdbcTemplate jdbc) {
        return Set.copyOf(jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
                """, String.class));
    }

    private List<String> columns(JdbcTemplate jdbc, String table) {
        return jdbc.queryForList("""
                SELECT column_name FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = ?
                ORDER BY ordinal_position
                """, String.class, table);
    }

    private Set<String> indexes(JdbcTemplate jdbc, String table) {
        return Set.copyOf(jdbc.queryForList("""
                SELECT DISTINCT index_name FROM information_schema.index_columns
                WHERE table_schema = 'public' AND table_name = ? AND index_name LIKE 'idx_%'
                """, String.class, table));
    }
}
