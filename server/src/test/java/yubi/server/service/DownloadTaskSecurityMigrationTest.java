package yubi.server.service;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownloadTaskSecurityMigrationTest {

    private static final String FORWARD =
            "db/migration/V2026.07.13__2.0.0.download-task-security.sql";
    private static final String ROLLBACK =
            "db/migration/R2026.07.13__2.0.0.download-task-security.sql";

    @Test
    void shouldInvalidateAllHistoryEnforceScopesFixCreateTimeAndRollback() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:download-security-" + java.util.UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        createLegacySchema(jdbc);

        jdbc.update("INSERT INTO `user` (id) VALUES ('client-id-collision')");
        jdbc.update("INSERT INTO `user` (id) VALUES ('user-1')");
        jdbc.update("INSERT INTO `user` (id) VALUES ('user-2')");
        jdbc.update("""
                INSERT INTO download (id, name, path, create_time, create_by, status)
                VALUES ('collision-task', 'collision.xlsx', 'download/collision.xlsx',
                        TIMESTAMP '2024-01-02 03:04:05', 'client-id-collision', 1)
                """);
        jdbc.update("""
                INSERT INTO download (id, name, path, create_time, create_by, status)
                VALUES ('same-path-user-1', 'same.xlsx', 'download/same.xlsx',
                        TIMESTAMP '2024-01-02 03:04:05', 'user-1', 1)
                """);
        jdbc.update("""
                INSERT INTO download (id, name, path, create_time, create_by, status)
                VALUES ('same-path-user-2', 'same.xlsx', 'download/same.xlsx',
                        TIMESTAMP '2024-01-02 03:04:05', 'user-2', 1)
                """);
        jdbc.update("""
                INSERT INTO download (id, name, path, create_time, create_by, status)
                VALUES ('unmatched-client-task', 'legacy.xlsx', 'download/legacy.xlsx',
                        TIMESTAMP '2024-01-02 03:04:05', 'browser-client-id', 1)
                """);

        ClassPathResource forward = new ClassPathResource(FORWARD);
        String forwardSql = forward.getContentAsString(StandardCharsets.UTF_8);
        assertTrue(forwardSql.contains("WHERE `owner_type` IS NULL"));
        assertFalse(forwardSql.contains("FROM `user`"));
        assertFalse(forwardSql.contains("d.`create_by`"));
        assertFalse(forwardSql.toLowerCase().contains("password"));
        assertEquals(2, forwardSql.split(" ENFORCED", -1).length - 1);
        String h2ForwardSql = forwardSql.replace(" ENFORCED", "");
        new ResourceDatabasePopulator(new ByteArrayResource(
                h2ForwardSql.getBytes(StandardCharsets.UTF_8)
        )).execute(dataSource);

        assertEquals("LEGACY_INACCESSIBLE", jdbc.queryForObject(
                "SELECT owner_type FROM download WHERE id='collision-task'", String.class));
        assertEquals(4, jdbc.queryForObject(
                "SELECT COUNT(*) FROM download WHERE owner_type='LEGACY_INACCESSIBLE'", Integer.class));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM download WHERE owner_type='AUTHENTICATED'", Integer.class));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM download WHERE owner_id IS NOT NULL OR share_id IS NOT NULL",
                Integer.class));
        assertEquals(2, jdbc.queryForObject("""
                SELECT COUNT(*) FROM download
                WHERE path='download/same.xlsx' AND owner_type='LEGACY_INACCESSIBLE'
                """, Integer.class));
        assertEquals(2, jdbc.queryForObject("""
                SELECT COUNT(DISTINCT create_by) FROM download
                WHERE path='download/same.xlsx'
                """, Integer.class));

        assertEquals(3, jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.indexes
                WHERE table_name='download'
                  AND index_name IN ('idx_download_authenticated_scope',
                                     'idx_download_share_scope', 'idx_download_cleanup')
                """, Integer.class));
        assertEquals(2, jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.table_constraints
                WHERE table_name='download' AND constraint_type='CHECK'
                """, Integer.class));

        assertThrows(DataAccessException.class, () -> jdbc.update("""
                INSERT INTO download
                    (id, name, create_time, create_by, status, owner_type, owner_id, share_id)
                VALUES ('invalid-owner', 'bad', CURRENT_TIMESTAMP, 'user-1', 0,
                        'SHARE', 'digest', NULL)
                """));
        assertThrows(DataAccessException.class, () -> jdbc.update("""
                INSERT INTO download
                    (id, name, create_time, create_by, status, owner_type, owner_id, failure_code)
                VALUES ('invalid-failure', 'bad', CURRENT_TIMESTAMP, 'user-1', 0,
                        'AUTHENTICATED', 'user-1', 'RAW_EXCEPTION')
                """));

        Timestamp originalCreateTime = jdbc.queryForObject(
                "SELECT create_time FROM download WHERE id='collision-task'", Timestamp.class);
        jdbc.update("UPDATE download SET status=2 WHERE id='collision-task'");
        assertEquals(originalCreateTime, jdbc.queryForObject(
                "SELECT create_time FROM download WHERE id='collision-task'", Timestamp.class));

        assertEquals(0, jdbc.update("""
                UPDATE download SET owner_type='LEGACY_INACCESSIBLE', owner_id=NULL, share_id=NULL
                WHERE owner_type IS NULL
                """));

        String rollbackSql = new ClassPathResource(ROLLBACK)
                .getContentAsString(StandardCharsets.UTF_8);
        assertTrue(rollbackSql.contains("DROP CHECK `chk_download_failure_code`"));
        assertTrue(rollbackSql.contains("DROP CHECK `chk_download_owner_scope`"));
        String h2RollbackSql = rollbackSql.replace("DROP CHECK", "DROP CONSTRAINT");
        new ResourceDatabasePopulator(new ByteArrayResource(
                h2RollbackSql.getBytes(StandardCharsets.UTF_8)
        )).execute(dataSource);
        assertEquals(0, jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_name='download'
                  AND column_name IN ('owner_type', 'owner_id', 'share_id',
                                      'failure_code', 'deleted_at')
                """, Integer.class));

        jdbc.update("""
                UPDATE download SET create_time=TIMESTAMP '2000-01-01 00:00:00'
                WHERE id='collision-task'
                """);
        jdbc.update("UPDATE download SET status=1 WHERE id='collision-task'");
        Timestamp rolledBackCreateTime = jdbc.queryForObject(
                "SELECT create_time FROM download WHERE id='collision-task'", Timestamp.class);
        assertTrue(rolledBackCreateTime.after(Timestamp.valueOf("2000-01-01 00:00:00")));
    }

    private void createLegacySchema(JdbcTemplate jdbc) {
        jdbc.execute("CREATE TABLE `user` (`id` varchar(32) NOT NULL PRIMARY KEY)");
        jdbc.execute("""
                CREATE TABLE `download` (
                  `id` varchar(32) NOT NULL,
                  `name` varchar(255) NOT NULL,
                  `path` varchar(512) NULL,
                  `last_download_time` timestamp(0) NULL DEFAULT NULL,
                  `create_time` timestamp(0) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(0),
                  `create_by` varchar(128) NOT NULL,
                  `status` tinyint NOT NULL,
                  PRIMARY KEY (`id`),
                  INDEX `create_by` (`create_by`)
                )
                """);
    }
}
