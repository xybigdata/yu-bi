package yubi.server.agent.write;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServerAgentWorkspaceSessionServiceTest {

    private JdbcDataSource dataSource;
    private JdbcTemplate jdbcTemplate;
    private YuBiSecurityManager securityManager;
    private OrgService orgService;

    @BeforeEach
    void setUp() {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:agent-workspace-" + java.util.UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        jdbcTemplate = new JdbcTemplate(dataSource);
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
        securityManager = mock(YuBiSecurityManager.class);
        orgService = mock(OrgService.class);
        when(orgService.listOrganizations()).thenReturn(List.of(organization("org-1")));
        when(securityManager.getCurrentUser()).thenReturn(user("user-1"));
    }

    @Test
    void shouldPersistServerGeneratedSessionAndResumeOnlyInBoundScope() {
        Instant now = Instant.parse("2026-07-12T02:00:00Z");
        ServerAgentWorkspaceSessionService service = service(now, Duration.ofHours(1));

        AgentWorkspaceSession session = service.create("org-1");
        var resumed = service.resume("org-1", session.sessionId());

        assertEquals("user-1", resumed.queryContext().subjectId());
        assertEquals("org-1", resumed.queryContext().organizationId());
        assertEquals(session.sessionId(), resumed.sessionId());
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM agent_workspace_session WHERE id = ?", Integer.class, session.sessionId()));

        when(securityManager.getCurrentUser()).thenReturn(user("user-2"));
        AgentWorkspaceSessionException denied = assertThrows(AgentWorkspaceSessionException.class,
                () -> service.resume("org-1", session.sessionId()));
        assertEquals("WORKSPACE_SESSION_DENIED", denied.code());
    }

    @Test
    void shouldRecoverActiveSessionOnlyWithinCurrentTrustedScope() {
        Instant now = Instant.parse("2026-07-12T02:00:00Z");
        ServerAgentWorkspaceSessionService service = service(now, Duration.ofHours(1));

        AgentWorkspaceSession original = service.create("org-1");
        AgentWorkspaceSession recovered = service.create("org-1");

        assertEquals(original, recovered);
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM agent_workspace_session", Integer.class));

        when(securityManager.getCurrentUser()).thenReturn(user("user-2"));
        AgentWorkspaceSession otherUser = service.create("org-1");
        org.junit.jupiter.api.Assertions.assertNotEquals(original.sessionId(), otherUser.sessionId());
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM agent_workspace_session", Integer.class));
    }

    @Test
    void shouldPreferTheActiveSessionThatStillOwnsPendingApproval() {
        Instant now = Instant.parse("2026-07-12T02:00:00Z");
        jdbcTemplate.update("""
                INSERT INTO agent_workspace_session
                    (id, subject_id, organization_id, status, expires_at, created_at, last_access_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, "11111111-1111-1111-1111-111111111111", "user-1", "org-1", "ACTIVE",
                java.sql.Timestamp.from(now.plus(Duration.ofHours(1))), java.sql.Timestamp.from(now),
                java.sql.Timestamp.from(now));
        jdbcTemplate.update("""
                INSERT INTO agent_workspace_session
                    (id, subject_id, organization_id, status, expires_at, created_at, last_access_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, "22222222-2222-2222-2222-222222222222", "user-1", "org-1", "ACTIVE",
                java.sql.Timestamp.from(now.plus(Duration.ofHours(1))),
                java.sql.Timestamp.from(now.plusSeconds(10)), java.sql.Timestamp.from(now.plusSeconds(10)));
        jdbcTemplate.update("""
                INSERT INTO agent_write_operation (approval_id, session_id, status, expires_at)
                VALUES (?, ?, ?, ?)
                """, "33333333-3333-3333-3333-333333333333",
                "11111111-1111-1111-1111-111111111111", "PENDING",
                java.sql.Timestamp.from(now.plus(Duration.ofMinutes(15))));

        AgentWorkspaceSession recovered = service(now, Duration.ofHours(1)).create("org-1");

        assertEquals("11111111-1111-1111-1111-111111111111", recovered.sessionId());
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM agent_workspace_session", Integer.class));
    }

    @Test
    void concurrentCreateMustReturnOneSessionForTheSameTrustedScope() throws Exception {
        Instant now = Instant.parse("2026-07-12T02:00:00Z");
        ServerAgentWorkspaceSessionService service = service(now, Duration.ofHours(1));
        TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> createConcurrently(service, transaction, ready, start));
            var second = executor.submit(() -> createConcurrently(service, transaction, ready, start));
            org.junit.jupiter.api.Assertions.assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            assertEquals(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
        }
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM agent_workspace_session", Integer.class));
    }

    @Test
    void shouldRejectExpiredOrTamperedSessionWithoutCreatingTrustedContext() {
        Instant now = Instant.parse("2026-07-12T02:00:00Z");
        AgentWorkspaceSession session = service(now, Duration.ofMinutes(1)).create("org-1");

        AgentWorkspaceSessionException expired = assertThrows(AgentWorkspaceSessionException.class,
                () -> service(now.plus(Duration.ofMinutes(2)), Duration.ofMinutes(1))
                        .resume("org-1", session.sessionId()));
        assertEquals("WORKSPACE_SESSION_EXPIRED", expired.code());
        assertEquals("EXPIRED", jdbcTemplate.queryForObject(
                "SELECT status FROM agent_workspace_session WHERE id = ?", String.class, session.sessionId()));

        AgentWorkspaceSessionException tampered = assertThrows(AgentWorkspaceSessionException.class,
                () -> service(now, Duration.ofMinutes(1)).resume("org-1", "not-a-session"));
        assertEquals("WORKSPACE_SESSION_DENIED", tampered.code());
    }

    private ServerAgentWorkspaceSessionService service(Instant now, Duration lifetime) {
        return new ServerAgentWorkspaceSessionService(jdbcTemplate,
                new ServerAgentExecutionContextFactory(securityManager, orgService), lifetime,
                Clock.fixed(now, ZoneOffset.UTC));
    }

    private String createConcurrently(ServerAgentWorkspaceSessionService service,
                                      TransactionTemplate transaction,
                                      CountDownLatch ready,
                                      CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("并发会话测试未能同步启动");
        }
        return transaction.execute(status -> service.create("org-1").sessionId());
    }

    private User user(String id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private OrganizationBaseInfo organization(String id) {
        Organization organization = new Organization();
        organization.setId(id);
        return new OrganizationBaseInfo(organization);
    }
}
