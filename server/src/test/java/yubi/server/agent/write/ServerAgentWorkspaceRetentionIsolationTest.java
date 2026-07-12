package yubi.server.agent.write;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import yubi.core.entity.Organization;
import yubi.core.entity.User;
import yubi.security.manager.YuBiSecurityManager;
import yubi.server.agent.ServerAgentExecutionContextFactory;
import yubi.server.base.dto.OrganizationBaseInfo;
import yubi.server.service.OrgService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServerAgentWorkspaceRetentionIsolationTest {

    @Test
    void shouldNotMaskSessionCreationWhenIsolatedMaintenanceFails() {
        ControlledWriteRetentionService retentionService = mock(ControlledWriteRetentionService.class);
        doThrow(new IllegalStateException("模拟维护失败")).when(retentionService).maintain();
        Fixture fixture = fixture(retentionService);

        AgentWorkspaceSession created = fixture.service().create("org-1");

        assertEquals("user-1", created.subjectId());
        assertEquals(1, fixture.jdbcTemplate().queryForObject(
                "SELECT COUNT(*) FROM agent_workspace_session", Integer.class));
    }

    @Test
    void shouldCommitExpiredSessionMarkerBeforeReturningControlledExpiry() {
        Fixture fixture = fixture(mock(ControlledWriteRetentionService.class));
        String sessionId = UUID.randomUUID().toString();
        Instant now = Instant.parse("2026-07-12T08:00:00Z");
        fixture.jdbcTemplate().update("""
                INSERT INTO agent_workspace_session
                    (id, subject_id, organization_id, status, expires_at, created_at, last_access_at)
                VALUES (?, 'user-1', 'org-1', 'ACTIVE', ?, ?, ?)
                """, sessionId, java.sql.Timestamp.from(now.minusSeconds(1)),
                java.sql.Timestamp.from(now.minus(Duration.ofHours(1))),
                java.sql.Timestamp.from(now.minus(Duration.ofHours(1))));
        DataSourceTransactionManager transactionManager =
                new DataSourceTransactionManager(fixture.dataSource());
        TransactionInterceptor transactions = new TransactionInterceptor(
                (TransactionManager) transactionManager, new AnnotationTransactionAttributeSource());
        ProxyFactory proxyFactory = new ProxyFactory(fixture.service());
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice(transactions);
        ServerAgentWorkspaceSessionService transactional =
                (ServerAgentWorkspaceSessionService) proxyFactory.getProxy();

        AgentWorkspaceSessionException expired = assertThrows(AgentWorkspaceSessionException.class,
                () -> transactional.resume("org-1", sessionId));

        assertEquals("WORKSPACE_SESSION_EXPIRED", expired.code());
        assertEquals("EXPIRED", fixture.jdbcTemplate().queryForObject("""
                SELECT status FROM agent_workspace_session WHERE id = ?
                """, String.class, sessionId));
    }

    private Fixture fixture(ControlledWriteRetentionService retentionService) {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:agent-retention-isolation-" + java.util.UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("CREATE TABLE organization (id varchar(32) PRIMARY KEY)");
        jdbcTemplate.update("INSERT INTO organization (id) VALUES (?)", "org-1");
        jdbcTemplate.execute("""
                CREATE TABLE agent_workspace_session (
                    id varchar(36) PRIMARY KEY,
                    subject_id varchar(32) NOT NULL,
                    organization_id varchar(32) NOT NULL,
                    status varchar(16) NOT NULL,
                    expires_at timestamp(3) NOT NULL,
                    created_at timestamp(3) NOT NULL,
                    last_access_at timestamp(3) NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE agent_write_operation (
                    approval_id varchar(36) PRIMARY KEY,
                    session_id varchar(36) NOT NULL,
                    status varchar(24) NOT NULL,
                    expires_at timestamp(3) NOT NULL
                )
                """);
        YuBiSecurityManager securityManager = mock(YuBiSecurityManager.class);
        OrgService orgService = mock(OrgService.class);
        User user = new User();
        user.setId("user-1");
        Organization organization = new Organization();
        organization.setId("org-1");
        when(securityManager.getCurrentUser()).thenReturn(user);
        when(orgService.listOrganizations()).thenReturn(List.of(new OrganizationBaseInfo(organization)));
        Instant now = Instant.parse("2026-07-12T08:00:00Z");
        ServerAgentWorkspaceSessionService service = new ServerAgentWorkspaceSessionService(
                jdbcTemplate, new ServerAgentExecutionContextFactory(securityManager, orgService),
                Duration.ofHours(1), Clock.fixed(now, ZoneOffset.UTC), retentionService);
        return new Fixture(dataSource, jdbcTemplate, service);
    }

    private record Fixture(JdbcDataSource dataSource,
                           JdbcTemplate jdbcTemplate,
                           ServerAgentWorkspaceSessionService service) {
    }
}
