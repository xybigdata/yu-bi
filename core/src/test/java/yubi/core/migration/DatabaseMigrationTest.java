package yubi.core.migration;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DatabaseMigrationTest {

    @Test
    void shouldContinueRollbackCleanupButReportAnySqlError() throws Exception {
        String url = "jdbc:h2:mem:migration-rollback-" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        DatabaseMigration migration = new DatabaseMigration(jdbc);
        setField(migration, "url", url);
        setField(migration, "username", "sa");
        setField(migration, "password", "");

        boolean success = runScript(migration, """
                CREATE TABLE rollback_before_error (id int);
                INVALID ROLLBACK SQL;
                CREATE TABLE rollback_after_error (id int);
                """, false);

        assertFalse(success);
        assertEquals(2, jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_name IN ('rollback_before_error', 'rollback_after_error')
                """, Integer.class));
    }

    private boolean runScript(DatabaseMigration migration, String script, boolean stopOnError)
            throws Exception {
        Method method = DatabaseMigration.class.getDeclaredMethod(
                "runScript",
                org.springframework.core.io.Resource.class,
                boolean.class
        );
        method.setAccessible(true);
        return (boolean) method.invoke(
                migration,
                new ByteArrayResource(script.getBytes(StandardCharsets.UTF_8)),
                stopOnError
        );
    }

    private void setField(DatabaseMigration migration, String name, String value) throws Exception {
        Field field = DatabaseMigration.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(migration, value);
    }
}
