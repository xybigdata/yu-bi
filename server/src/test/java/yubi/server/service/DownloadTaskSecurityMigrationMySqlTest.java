package yubi.server.service;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "YUBI_TEST_MYSQL_URL", matches = "jdbc:mysql:.*")
class DownloadTaskSecurityMigrationMySqlTest {

    private static final String FORWARD =
            "db/migration/V2026.07.13__2.0.0.download-task-security.sql";
    private static final String ROLLBACK =
            "db/migration/R2026.07.13__2.0.0.download-task-security.sql";
    private static final int[] MINIMUM_MYSQL_VERSION = {8, 4, 10};

    private JdbcTemplate admin;
    private JdbcTemplate jdbc;
    private String databaseName;

    @BeforeEach
    void setUp() {
        String baseUrl = System.getenv("YUBI_TEST_MYSQL_URL");
        String username = environment("YUBI_TEST_MYSQL_USERNAME", "root");
        String password = environment("YUBI_TEST_MYSQL_PASSWORD", "");
        admin = new JdbcTemplate(dataSource(baseUrl, username, password));
        databaseName = "yubi_download_security_" + UUID.randomUUID().toString().replace("-", "");
        admin.execute("CREATE DATABASE `" + databaseName
                + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        jdbc = new JdbcTemplate(dataSource(databaseUrl(baseUrl, databaseName), username, password));
        assertMinimumVersion(jdbc.queryForObject("SELECT VERSION()", String.class));
        assertEquals("InnoDB", jdbc.queryForObject("SELECT @@default_storage_engine", String.class));
        assertNotNull(jdbc.queryForObject("SELECT @@transaction_isolation", String.class));
        assertNotNull(jdbc.queryForObject("SELECT @@sql_mode", String.class));
        createLegacySchema();
        seedAmbiguousLegacyHistory();
    }

    @AfterEach
    void tearDown() {
        if (admin != null && databaseName != null) {
            admin.execute("DROP DATABASE IF EXISTS `" + databaseName + "`");
        }
    }

    @Test
    void shouldEnforceSecurityContractAndRepeatForwardRollbackCycleOnMinimumMySql() {
        execute(FORWARD);
        assertForwardState();
        execute(ROLLBACK);
        assertRollbackState();

        execute(FORWARD);
        assertForwardState();
        execute(ROLLBACK);
        assertRollbackState();
    }

    private void assertForwardState() {
        assertEquals(3, jdbc.queryForObject(
                "SELECT COUNT(*) FROM download WHERE owner_type='LEGACY_INACCESSIBLE'",
                Integer.class));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM download WHERE owner_type='AUTHENTICATED'",
                Integer.class));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM download WHERE owner_id IS NOT NULL OR share_id IS NOT NULL",
                Integer.class));
        assertEquals(2, jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.table_constraints
                WHERE table_schema=DATABASE() AND table_name='download'
                  AND constraint_type='CHECK' AND enforced='YES'
                """, Integer.class));
        assertEquals(3, jdbc.queryForObject("""
                SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics
                WHERE table_schema=DATABASE() AND table_name='download'
                  AND index_name IN ('idx_download_authenticated_scope',
                                     'idx_download_share_scope', 'idx_download_cleanup')
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

        Timestamp original = jdbc.queryForObject(
                "SELECT create_time FROM download WHERE id='auth-candidate'", Timestamp.class);
        jdbc.update("UPDATE download SET status=status + 1 WHERE id='auth-candidate'");
        assertEquals(original, jdbc.queryForObject(
                "SELECT create_time FROM download WHERE id='auth-candidate'", Timestamp.class));
        assertEquals(0, jdbc.update("""
                UPDATE download
                SET owner_type='LEGACY_INACCESSIBLE', owner_id=NULL, share_id=NULL
                WHERE owner_type IS NULL
                """));
    }

    private void assertRollbackState() {
        assertEquals(0, jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema=DATABASE() AND table_name='download'
                  AND column_name IN ('owner_type', 'owner_id', 'share_id',
                                      'failure_code', 'deleted_at')
                """, Integer.class));
        assertEquals(0, jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.table_constraints
                WHERE table_schema=DATABASE() AND table_name='download'
                  AND constraint_name IN ('chk_download_owner_scope',
                                          'chk_download_failure_code')
                """, Integer.class));

        jdbc.update("UPDATE download SET create_time='2000-01-01 00:00:00' WHERE id='auth-candidate'");
        jdbc.update("UPDATE download SET status=status + 1 WHERE id='auth-candidate'");
        Timestamp rolledBack = jdbc.queryForObject(
                "SELECT create_time FROM download WHERE id='auth-candidate'", Timestamp.class);
        assertTrue(rolledBack.after(Timestamp.valueOf("2000-01-01 00:00:00")));
    }

    private void createLegacySchema() {
        jdbc.execute("CREATE TABLE `user` (`id` varchar(32) NOT NULL PRIMARY KEY) ENGINE=InnoDB");
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
                ) ENGINE=InnoDB
                """);
    }

    private void seedAmbiguousLegacyHistory() {
        jdbc.batchUpdate("INSERT INTO `user` (id) VALUES (?)", List.of(
                new Object[]{"user-1"},
                new Object[]{"user-2"}
        ));
        jdbc.update("""
                INSERT INTO download (id, name, path, create_time, create_by, status)
                VALUES ('auth-candidate', 'shared.xlsx', 'download/shared.xlsx',
                        '2024-01-02 03:04:05', 'user-1', 1)
                """);
        jdbc.update("""
                INSERT INTO download (id, name, path, create_time, create_by, status)
                VALUES ('cross-owner-path', 'shared.xlsx', 'download/shared.xlsx',
                        '2024-01-02 03:04:05', 'user-2', 1)
                """);
        jdbc.update("""
                INSERT INTO download (id, name, path, create_time, create_by, status)
                VALUES ('forged-share-collision', 'forged.xlsx', 'download/forged.xlsx',
                        '2024-01-02 03:04:05', 'user-1', 1)
                """);
    }

    private void execute(String resourcePath) {
        new ResourceDatabasePopulator(new ClassPathResource(resourcePath)).execute(jdbc.getDataSource());
    }

    private MysqlDataSource dataSource(String url, String username, String password) {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setURL(url);
        dataSource.setUser(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    private String databaseUrl(String baseUrl, String name) {
        int queryIndex = baseUrl.indexOf('?');
        String query = queryIndex >= 0 ? baseUrl.substring(queryIndex) : "";
        String server = queryIndex >= 0 ? baseUrl.substring(0, queryIndex) : baseUrl;
        return server.replaceAll("/+$", "") + "/" + name + query;
    }

    private String environment(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }

    private void assertMinimumVersion(String version) {
        String[] components = version.split("[-.]");
        int[] actual = {
                Integer.parseInt(components[0]),
                Integer.parseInt(components[1]),
                Integer.parseInt(components[2])
        };
        for (int index = 0; index < MINIMUM_MYSQL_VERSION.length; index++) {
            if (actual[index] > MINIMUM_MYSQL_VERSION[index]) {
                return;
            }
            if (actual[index] < MINIMUM_MYSQL_VERSION[index]) {
                throw new AssertionError("MySQL 版本低于 8.4.10: " + version);
            }
        }
    }
}
