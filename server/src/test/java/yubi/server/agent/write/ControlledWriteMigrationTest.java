package yubi.server.agent.write;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ControlledWriteMigrationTest {

    private static final String FORWARD =
            "db/migration/V2026.07.12__2.0.0.agent-controlled-write.sql";
    private static final String ROLLBACK =
            "db/migration/R2026.07.12__2.0.0.agent-controlled-write.sql";

    @Test
    void shouldApplyRollbackAndRequireTransactionalStorageEngine() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:agent-write-migration-" + java.util.UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        ClassPathResource forward = new ClassPathResource(FORWARD);
        String sql = forward.getContentAsString(StandardCharsets.UTF_8);
        assertEquals(3, occurrences(sql, "ENGINE = InnoDB"));
        new ResourceDatabasePopulator(forward).execute(dataSource);

        assertEquals(3, jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_name IN ('agent_workspace_session', 'agent_write_operation',
                                     'agent_write_audit_event')
                """, Integer.class));

        TransactionTemplate transaction = new TransactionTemplate(
                new DataSourceTransactionManager(dataSource));
        transaction.executeWithoutResult(status -> {
            jdbc.update("""
                    INSERT INTO agent_workspace_session
                        (id, subject_id, organization_id, status, expires_at, created_at, last_access_at)
                    VALUES ('session-rollback', 'user-1', 'org-1', 'ACTIVE',
                            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """);
            status.setRollbackOnly();
        });
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM agent_workspace_session", Integer.class));

        new ResourceDatabasePopulator(new ClassPathResource(ROLLBACK)).execute(dataSource);
        assertEquals(0, jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_name IN ('agent_workspace_session', 'agent_write_operation',
                                     'agent_write_audit_event')
                """, Integer.class));
    }

    private int occurrences(String value, String search) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(search, offset)) >= 0) {
            count++;
            offset += search.length();
        }
        return count;
    }
}
