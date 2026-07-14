package yubi.server.validation;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import yubi.core.base.consts.DownloadOwnerType;
import yubi.core.common.UUIDGenerator;
import yubi.core.entity.Download;
import yubi.core.mappers.ext.DownloadMapperExt;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoDownloadSchemaCheckTest {

    private static final Path REPOSITORY_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final Path DEMO_DATABASE = REPOSITORY_ROOT.resolve("bin/h2/yubi.demo.mv.db");
    private static final Path H2_MIGRATION = REPOSITORY_ROOT.resolve(
            "bin/migrations/V2026.07.13__2.0.0.download-task-security.h2.sql"
    );

    @TempDir
    Path tempDir;

    @Test
    void shouldVerifyPackagedInputAndExecuteAuthenticatedAndSharedMapperSmoke() throws Exception {
        Path databaseFile = tempDir.resolve("packaged-input/yubi.demo.mv.db");
        Files.createDirectories(databaseFile.getParent());
        Files.copy(DEMO_DATABASE, databaseFile);
        DataSource dataSource = dataSource(fileUrl(databaseFile));
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        try (var connection = dataSource.getConnection()) {
            DemoDownloadSchemaCheck.verify(connection);
        }
        assertStableCreateTime(jdbc);
        assertNoSchemaProbes(jdbc);
        assertEquals(
                jdbc.queryForObject("SELECT COUNT(*) FROM download", Integer.class),
                jdbc.queryForObject("""
                        SELECT COUNT(*) FROM download
                        WHERE owner_type='LEGACY_INACCESSIBLE'
                          AND owner_id IS NULL AND share_id IS NULL
                        """, Integer.class)
        );
        assertConstraintEnforcement(jdbc);
        assertMapperSmoke(dataSource);
    }

    @Test
    void shouldRejectOldPresetThenRepeatablyInvalidateAmbiguousHistory() throws Exception {
        Path databaseFile = tempDir.resolve("repeatable/yubi.demo.mv.db");
        Files.createDirectories(databaseFile.getParent());
        DataSource dataSource = dataSource(fileUrl(databaseFile));
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        createLegacySchema(jdbc);
        seedAmbiguousHistory(jdbc);
        assertLegacyCreateTimeChanges(jdbc);

        try (var connection = dataSource.getConnection()) {
            assertThrows(IllegalStateException.class, () -> DemoDownloadSchemaCheck.verify(connection));
        }

        ResourceDatabasePopulator migration = new ResourceDatabasePopulator(
                new FileSystemResource(H2_MIGRATION)
        );
        migration.execute(dataSource);
        migration.execute(dataSource);
        assertStableCreateTime(jdbc);

        assertEquals(3, jdbc.queryForObject(
                "SELECT COUNT(*) FROM download WHERE owner_type='LEGACY_INACCESSIBLE'",
                Integer.class
        ));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM download WHERE owner_type='AUTHENTICATED'",
                Integer.class
        ));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM download WHERE owner_id IS NOT NULL OR share_id IS NOT NULL",
                Integer.class
        ));
        try (var connection = dataSource.getConnection()) {
            DemoDownloadSchemaCheck.verify(connection);
        }
        assertNoSchemaProbes(jdbc);
        assertConstraintEnforcement(jdbc);
        assertMapperSmoke(dataSource);
    }

    @Test
    void shouldRejectWrongColumnSpecificationWithAllRequiredNamesPresent() throws Exception {
        DataSource dataSource = migratedDataSource();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("ALTER TABLE download ALTER COLUMN owner_id varchar(63)");

        try (var connection = dataSource.getConnection()) {
            assertThrows(IllegalStateException.class, () -> DemoDownloadSchemaCheck.verify(connection));
        }
    }

    @Test
    void shouldRejectNullableOwnerTypeWithAllRequiredNamesPresent() throws Exception {
        DataSource dataSource = migratedDataSource();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("ALTER TABLE download ALTER COLUMN owner_type DROP NOT NULL");

        try (var connection = dataSource.getConnection()) {
            assertThrows(IllegalStateException.class, () -> DemoDownloadSchemaCheck.verify(connection));
        }
    }

    @Test
    void shouldRejectMissingPrimaryKeyAndDuplicateTaskId() throws Exception {
        DataSource dataSource = migratedDataSource();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("""
                INSERT INTO download (id, name, create_time, create_by, status, owner_type, owner_id)
                VALUES ('duplicate-task', 'one', CURRENT_TIMESTAMP, 'demo', 0, 'AUTHENTICATED', 'demo')
                """);
        assertThrows(DataAccessException.class, () -> jdbc.update("""
                INSERT INTO download (id, name, create_time, create_by, status, owner_type, owner_id)
                VALUES ('duplicate-task', 'two', CURRENT_TIMESTAMP, 'demo', 0, 'AUTHENTICATED', 'demo')
                """));

        String primaryKey = jdbc.queryForObject("""
                SELECT constraint_name FROM information_schema.table_constraints
                WHERE table_name = 'download' AND constraint_type = 'PRIMARY KEY'
                """, String.class);
        jdbc.execute("ALTER TABLE download DROP CONSTRAINT " + primaryKey);
        try (var connection = dataSource.getConnection()) {
            assertThrows(IllegalStateException.class, () -> DemoDownloadSchemaCheck.verify(connection));
        }
    }

    @Test
    void shouldRejectMissingRequiredIndex() throws Exception {
        DataSource dataSource = migratedDataSource();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP INDEX idx_download_cleanup");

        try (var connection = dataSource.getConnection()) {
            assertThrows(IllegalStateException.class, () -> DemoDownloadSchemaCheck.verify(connection));
        }
    }

    @Test
    void shouldRejectWrongIndexColumnOrderWithRequiredIndexNamePresent() throws Exception {
        DataSource dataSource = migratedDataSource();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP INDEX idx_download_authenticated_scope");
        jdbc.execute("""
                CREATE INDEX idx_download_authenticated_scope
                ON download (owner_type, deleted_at, owner_id, create_time)
                """);

        try (var connection = dataSource.getConnection()) {
            assertThrows(IllegalStateException.class, () -> DemoDownloadSchemaCheck.verify(connection));
        }
    }

    @Test
    void shouldRejectPermissiveConstraintWithRequiredConstraintNamePresent() throws Exception {
        DataSource dataSource = migratedDataSource();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("ALTER TABLE download DROP CONSTRAINT chk_download_owner_scope");
        jdbc.execute("""
                ALTER TABLE download ADD CONSTRAINT chk_download_owner_scope
                CHECK (TRUE)
                """);

        try (var connection = dataSource.getConnection()) {
            assertThrows(IllegalStateException.class, () -> DemoDownloadSchemaCheck.verify(connection));
        }
    }

    @Test
    void shouldRejectPermissiveFailureConstraintWithRequiredConstraintNamePresent() throws Exception {
        DataSource dataSource = migratedDataSource();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("ALTER TABLE download DROP CONSTRAINT chk_download_failure_code");
        jdbc.execute("""
                ALTER TABLE download ADD CONSTRAINT chk_download_failure_code
                CHECK (TRUE)
                """);

        try (var connection = dataSource.getConnection()) {
            assertThrows(IllegalStateException.class, () -> DemoDownloadSchemaCheck.verify(connection));
        }
    }

    @Test
    void shouldRejectPartialOwnerConstraintWithExpectedNamePresent() throws Exception {
        DataSource dataSource = migratedDataSource();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("ALTER TABLE download DROP CONSTRAINT chk_download_owner_scope");
        jdbc.execute("""
                ALTER TABLE download ADD CONSTRAINT chk_download_owner_scope
                CHECK (owner_type <> 'SHARE' OR share_id IS NOT NULL)
                """);

        try (var connection = dataSource.getConnection()) {
            assertThrows(IllegalStateException.class, () -> DemoDownloadSchemaCheck.verify(connection));
        }
    }

    @Test
    void shouldRejectPartialFailureConstraintWithExpectedNamePresent() throws Exception {
        DataSource dataSource = migratedDataSource();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("ALTER TABLE download DROP CONSTRAINT chk_download_failure_code");
        jdbc.execute("""
                ALTER TABLE download ADD CONSTRAINT chk_download_failure_code
                CHECK (failure_code IS NULL OR failure_code <> 'RAW_EXCEPTION')
                """);

        try (var connection = dataSource.getConnection()) {
            assertThrows(IllegalStateException.class, () -> DemoDownloadSchemaCheck.verify(connection));
        }
    }

    @Test
    void shouldRejectOwnerConstraintThatForbidsSupportedShareRows() throws Exception {
        DataSource dataSource = migratedDataSource();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("ALTER TABLE download DROP CONSTRAINT chk_download_owner_scope");
        jdbc.execute("""
                ALTER TABLE download ADD CONSTRAINT chk_download_owner_scope CHECK (
                  ((owner_type = 'AUTHENTICATED' AND owner_id IS NOT NULL AND share_id IS NULL)
                    OR (owner_type = 'SHARE' AND owner_id IS NOT NULL AND share_id IS NOT NULL)
                    OR (owner_type = 'LEGACY_INACCESSIBLE' AND owner_id IS NULL AND share_id IS NULL))
                  AND owner_type <> 'SHARE'
                )
                """);

        try (var connection = dataSource.getConnection()) {
            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> DemoDownloadSchemaCheck.requireConstraintBehavior(connection)
            );
            assertTrue(exception.getMessage().contains("supported value SHARE"));
            assertThrows(IllegalStateException.class, () -> DemoDownloadSchemaCheck.verify(connection));
        }
        assertNoSchemaProbes(jdbc);
    }

    @Test
    void shouldRejectFailureConstraintThatForbidsSupportedFailureCode() throws Exception {
        DataSource dataSource = migratedDataSource();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("ALTER TABLE download DROP CONSTRAINT chk_download_failure_code");
        jdbc.execute("""
                ALTER TABLE download ADD CONSTRAINT chk_download_failure_code CHECK (
                  (failure_code IS NULL OR failure_code IN (
                    'PERMISSION_DENIED', 'DEFINITION_INVALID', 'QUERY_FAILED',
                    'FILE_GENERATION_FAILED', 'TASK_INTERRUPTED', 'INTERNAL_FAILURE'
                  ))
                  AND failure_code IS NULL
                )
                """);

        try (var connection = dataSource.getConnection()) {
            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> DemoDownloadSchemaCheck.requireConstraintBehavior(connection)
            );
            assertTrue(exception.getMessage().contains("supported value PERMISSION_DENIED"));
            assertThrows(IllegalStateException.class, () -> DemoDownloadSchemaCheck.verify(connection));
        }
        assertNoSchemaProbes(jdbc);
    }

    private void assertMapperSmoke(DataSource dataSource) {
        Configuration configuration = new Configuration(new Environment(
                "demo-download-smoke",
                new JdbcTransactionFactory(),
                dataSource
        ));
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(DownloadMapperExt.class);
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(configuration);

        try (SqlSession session = factory.openSession(false)) {
            DownloadMapperExt mapper = session.getMapper(DownloadMapperExt.class);
            Date createdAfter = new Date(0);
            mapper.selectAuthenticatedTasks("demo-user", createdAfter);
            mapper.selectSharedTasks("share-1", "a".repeat(64), createdAfter);

            Download authenticated = task(
                    UUIDGenerator.generate(),
                    DownloadOwnerType.AUTHENTICATED,
                    "demo-user",
                    null
            );
            assertEquals(1, mapper.insert(authenticated));
            assertNotNull(mapper.selectAuthenticatedTask(authenticated.getId(), "demo-user"));

            Download shared = task(
                    UUIDGenerator.generate(),
                    DownloadOwnerType.SHARE,
                    "a".repeat(64),
                    "share-1"
            );
            assertEquals(1, mapper.insert(shared));
            assertNotNull(mapper.selectSharedTask(
                    shared.getId(),
                    "share-1",
                    "a".repeat(64)
            ));
            session.rollback();
        }
    }

    private void assertConstraintEnforcement(JdbcTemplate jdbc) {
        assertThrows(DataAccessException.class, () -> jdbc.update("""
                INSERT INTO download
                    (id, name, create_time, create_by, status, owner_type, owner_id, share_id)
                VALUES ('invalid-demo-owner', 'bad', CURRENT_TIMESTAMP, 'demo', 0,
                        'SHARE', 'digest', NULL)
                """));
        assertThrows(DataAccessException.class, () -> jdbc.update("""
                INSERT INTO download
                    (id, name, create_time, create_by, status, owner_type, owner_id, failure_code)
                VALUES ('invalid-demo-failure', 'bad', CURRENT_TIMESTAMP, 'demo', 0,
                        'AUTHENTICATED', 'demo', 'RAW_EXCEPTION')
                """));
    }

    private void assertLegacyCreateTimeChanges(JdbcTemplate jdbc) {
        Timestamp fixed = Timestamp.valueOf("2001-02-03 04:05:06");
        jdbc.update("""
                INSERT INTO download (id, name, create_time, create_by, status)
                VALUES ('legacy-time-probe', 'time-probe', ?, 'demo', 0)
                """, fixed);
        jdbc.update("UPDATE download SET status = 1 WHERE id = 'legacy-time-probe'");
        Timestamp updated = jdbc.queryForObject(
                "SELECT create_time FROM download WHERE id = 'legacy-time-probe'",
                Timestamp.class
        );
        assertNotEquals(fixed, updated);
        jdbc.update("DELETE FROM download WHERE id = 'legacy-time-probe'");
    }

    private void assertStableCreateTime(JdbcTemplate jdbc) {
        Timestamp fixed = Timestamp.valueOf("2001-02-03 04:05:06");
        try {
            jdbc.update("""
                    INSERT INTO download
                        (id, name, create_time, create_by, status, owner_type, owner_id)
                    VALUES ('stable-time-probe', 'time-probe', ?, 'demo', 0,
                            'AUTHENTICATED', 'demo')
                    """, fixed);
            jdbc.update("UPDATE download SET status = 1 WHERE id = 'stable-time-probe'");
            assertEquals(fixed, jdbc.queryForObject(
                    "SELECT create_time FROM download WHERE id = 'stable-time-probe'",
                    Timestamp.class
            ));
        } finally {
            jdbc.update("DELETE FROM download WHERE id = 'stable-time-probe'");
        }
    }

    private void assertNoSchemaProbes(JdbcTemplate jdbc) {
        assertEquals(0, jdbc.queryForObject("""
                SELECT COUNT(*) FROM download
                WHERE name = 'schema-probe' OR create_by = 'schema-probe'
                """, Integer.class));
    }

    private Download task(String id,
                          DownloadOwnerType ownerType,
                          String ownerId,
                          String shareId) {
        Download download = new Download();
        download.setId(id);
        download.setName("demo-smoke.xlsx");
        download.setCreateTime(new Date());
        download.setCreateBy("demo");
        download.setStatus((byte) 0);
        download.setOwnerType(ownerType.name());
        download.setOwnerId(ownerId);
        download.setShareId(shareId);
        return download;
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
                  PRIMARY KEY (`id`)
                )
                """);
    }

    private void seedAmbiguousHistory(JdbcTemplate jdbc) {
        jdbc.batchUpdate("INSERT INTO `user` (id) VALUES (?)", List.of(
                new Object[]{"user-1"},
                new Object[]{"user-2"}
        ));
        jdbc.update("""
                INSERT INTO download (id, name, path, create_time, create_by, status)
                VALUES ('auth-candidate', 'same.xlsx', 'download/same.xlsx',
                        CURRENT_TIMESTAMP, 'user-1', 1)
                """);
        jdbc.update("""
                INSERT INTO download (id, name, path, create_time, create_by, status)
                VALUES ('cross-owner-path', 'same.xlsx', 'download/same.xlsx',
                        CURRENT_TIMESTAMP, 'user-2', 1)
                """);
        jdbc.update("""
                INSERT INTO download (id, name, path, create_time, create_by, status)
                VALUES ('forged-client-id', 'forged.xlsx', 'download/forged.xlsx',
                        CURRENT_TIMESTAMP, 'user-1', 1)
                """);
    }

    private DataSource migratedDataSource() {
        DataSource dataSource = dataSource(memoryUrl());
        createLegacySchema(new JdbcTemplate(dataSource));
        new ResourceDatabasePopulator(new FileSystemResource(H2_MIGRATION)).execute(dataSource);
        return dataSource;
    }

    private DataSource dataSource(String url) {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(url);
        dataSource.setUser("");
        dataSource.setPassword("");
        return dataSource;
    }

    private String fileUrl(Path databaseFile) {
        String base = databaseFile.toAbsolutePath().toString().replace(".mv.db", "");
        return "jdbc:h2:file:" + base + h2Options();
    }

    private String memoryUrl() {
        return "jdbc:h2:mem:demo-download-" + UUID.randomUUID() + h2Options()
                + ";DB_CLOSE_DELAY=-1";
    }

    private String h2Options() {
        return ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;IGNORECASE=TRUE"
                + ";CASE_INSENSITIVE_IDENTIFIERS=TRUE;NON_KEYWORDS=USER";
    }
}
