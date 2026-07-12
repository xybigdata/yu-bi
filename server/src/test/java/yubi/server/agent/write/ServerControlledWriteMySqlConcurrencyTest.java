package yubi.server.agent.write;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import yubi.agent.api.AgentExecutionContext;
import yubi.agent.application.CreateChartWriteProposalTool;
import yubi.agent.application.DefaultControlledWriteUseCase;
import yubi.agent.application.DefaultWriteProposalToolRegistry;
import yubi.agent.application.RenameDashboardWriteProposalTool;
import yubi.agent.domain.ControlledWriteModels.WriteFailureCategory;
import yubi.agent.domain.ControlledWriteModels.WriteOperationState;
import yubi.agent.domain.ControlledWriteModels.WriteOperationView;
import yubi.agent.domain.ControlledWriteModels.WriteProposalCommand;
import yubi.agent.domain.StructuredValue;
import yubi.agent.domain.StructuredValue.ObjectValue;
import yubi.core.base.consts.Const;
import yubi.core.base.exception.ParamException;
import yubi.core.entity.Dashboard;
import yubi.core.entity.Folder;
import yubi.core.entity.Organization;
import yubi.core.entity.User;
import yubi.core.entity.View;
import yubi.query.api.QueryExecutionContext;
import yubi.query.domain.QueryModels.Channel;
import yubi.security.base.ResourceType;
import yubi.security.exception.PermissionDeniedException;
import yubi.security.manager.PermissionDataCache;
import yubi.security.manager.RequestScopePermissionDataCache;
import yubi.security.manager.ThreadScopePermissionDataCache;
import yubi.security.manager.YuBiSecurityManager;
import yubi.server.base.dto.OrganizationBaseInfo;
import yubi.server.base.params.DatachartCreateParam;
import yubi.server.base.params.FolderUpdateParam;
import yubi.server.service.DashboardService;
import yubi.server.service.FolderService;
import yubi.server.service.OrgService;
import yubi.server.service.ViewService;
import yubi.server.service.VizService;
import yubi.visualization.write.application.DefaultCreateChartService;
import yubi.visualization.write.application.DefaultRenameDashboardService;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnabledIfEnvironmentVariable(named = "YUBI_TEST_MYSQL_URL", matches = "jdbc:mysql:.*")
class ServerControlledWriteMySqlConcurrencyTest {

    private static final String ORGANIZATION_ID = "org-1";
    private static final String USER_ID = "user-1";
    private static final String VIEW_ID = "view-1";
    private static final String DASHBOARD_ID = "dashboard-1";
    private static final String DASHBOARD_FOLDER_ID = "dashboard-folder-1";
    private static final String DASHBOARD_PARENT_ID = "dashboard-parent-1";

    private String databaseName;
    private JdbcTemplate admin;
    private Fixture fixture;

    @BeforeEach
    void setUp() {
        String baseUrl = System.getenv("YUBI_TEST_MYSQL_URL");
        String username = environment("YUBI_TEST_MYSQL_USERNAME", "root");
        String password = environment("YUBI_TEST_MYSQL_PASSWORD", "");
        MysqlDataSource adminDataSource = dataSource(baseUrl, username, password);
        admin = new JdbcTemplate(adminDataSource);
        databaseName = "yubi_agent_write_" + UUID.randomUUID().toString().replace("-", "");
        admin.execute("CREATE DATABASE `" + databaseName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");

        MysqlDataSource isolated = dataSource(databaseUrl(baseUrl, databaseName), username, password);
        fixture = new Fixture(isolated);
        assertEquals("InnoDB", fixture.jdbc.queryForObject("SELECT @@default_storage_engine", String.class));
        assertEquals("REPEATABLE-READ",
                fixture.jdbc.queryForObject("SELECT @@transaction_isolation", String.class));
    }

    @AfterEach
    void tearDown() {
        if (fixture != null) {
            fixture.close();
        }
        if (admin != null && databaseName != null) {
            admin.execute("DROP DATABASE IF EXISTS `" + databaseName + "`");
        }
    }

    @Test
    void shouldSerializeEquivalentRootChartNamesAndFailTheLoserWithoutDuplicateSideEffects() throws Exception {
        WriteOperationView first = fixture.proposeCreate("同名根图表", "idempotency-create-1", "session-create-1");
        WriteOperationView second = fixture.proposeCreate("同名根图表", "idempotency-create-2", "session-create-2");
        fixture.captureApprovalIsolation.set(true);
        fixture.blockFirstCreate.set(true);

        Future<ApprovalAttempt> firstApproval = fixture.executor.submit(
                () -> fixture.attemptApproval(first, "session-create-1", "approve-create-1"));
        assertTrue(fixture.firstCreateEntered.await(5, TimeUnit.SECONDS));
        Future<ApprovalAttempt> secondApproval = fixture.executor.submit(
                () -> fixture.attemptApproval(second, "session-create-2", "approve-create-2"));
        fixture.awaitMySqlLockWait();
        fixture.releaseFirstCreate.countDown();

        ApprovalAttempt firstResult = firstApproval.get(10, TimeUnit.SECONDS);
        ApprovalAttempt secondResult = secondApproval.get(10, TimeUnit.SECONDS);
        List<ApprovalAttempt> attempts = List.of(firstResult, secondResult);
        assertEquals(1, attempts.stream().filter(ApprovalAttempt::succeeded).count());
        assertEquals(1, attempts.stream().filter(attempt -> !attempt.succeeded()).count());
        ApprovalAttempt loser = attempts.stream().filter(attempt -> !attempt.succeeded()).findFirst().orElseThrow();
        assertEquals("CHART_NAME_CONFLICT", loser.failure().code());
        assertEquals(WriteFailureCategory.CONFLICT, loser.failure().category());

        assertEquals(1, fixture.count("SELECT COUNT(*) FROM datachart WHERE name = ?", "同名根图表"));
        assertEquals(1, fixture.count("""
                SELECT COUNT(*) FROM folder
                WHERE name = ? AND rel_type = ? AND parent_id IS NULL
                """, "同名根图表", ResourceType.DATACHART.name()));
        fixture.assertFailed(loser.approvalId(), WriteFailureCategory.CONFLICT);
        ApprovalAttempt winner = attempts.stream().filter(ApprovalAttempt::succeeded).findFirst().orElseThrow();
        fixture.assertSucceeded(winner.approvalId());
        assertEquals("REPEATABLE-READ", fixture.observedApprovalIsolation.get());
    }

    @Test
    void shouldObservePermissionRevokedWhileApprovalWaitsForViewLockAndLeaveNoChart() throws Exception {
        WriteOperationView proposal = fixture.proposeCreate(
                "权限撤销图表", "idempotency-permission", "session-permission");
        fixture.captureApprovalIsolation.set(true);
        CountDownLatch lockHeld = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);
        Future<?> holder = fixture.holdLock("SELECT id FROM `view` WHERE id = ? FOR UPDATE",
                VIEW_ID, lockHeld, releaseLock);
        assertTrue(lockHeld.await(5, TimeUnit.SECONDS));

        Future<WriteOperationView> approval = fixture.executor.submit(() -> fixture.writes.approve(
                proposal.approvalId(), fixture.context("session-permission", "approve-permission")));
        fixture.awaitMySqlLockWait();
        fixture.jdbc.update("""
                DELETE FROM write_grant
                WHERE subject_id = ? AND resource_kind = 'VIEW_READ' AND resource_id = ?
                """, USER_ID, VIEW_ID);
        releaseLock.countDown();
        holder.get(10, TimeUnit.SECONDS);

        ServerControlledWriteExecutionException failure = executionFailure(approval);
        assertEquals("CREATE_CHART_ACCESS_DENIED", failure.code());
        assertEquals(WriteFailureCategory.ACCESS_DENIED, failure.category());
        assertEquals(0, fixture.count("SELECT COUNT(*) FROM datachart WHERE name = ?", "权限撤销图表"));
        assertEquals(0, fixture.count("""
                SELECT COUNT(*) FROM folder WHERE name = ? AND rel_type = ?
                """, "权限撤销图表", ResourceType.DATACHART.name()));
        fixture.assertFailed(proposal.approvalId(), WriteFailureCategory.ACCESS_DENIED);
        assertEquals("REPEATABLE-READ", fixture.observedApprovalIsolation.get());
    }

    @Test
    void shouldObserveDashboardStateCommittedWhileApprovalWaitsAndLeaveNameUnchanged() throws Exception {
        WriteOperationView proposal = fixture.proposeRename(
                "审批后名称", "idempotency-dashboard-state", "session-dashboard");
        fixture.captureApprovalIsolation.set(true);
        CountDownLatch stateChanged = new CountDownLatch(1);
        CountDownLatch releaseChange = new CountDownLatch(1);
        Future<?> holder = fixture.holdDashboardStateChange(stateChanged, releaseChange);
        assertTrue(stateChanged.await(5, TimeUnit.SECONDS));

        Future<WriteOperationView> approval = fixture.executor.submit(() -> fixture.writes.approve(
                proposal.approvalId(), fixture.context("session-dashboard", "approve-dashboard")));
        fixture.awaitMySqlLockWait();
        releaseChange.countDown();
        holder.get(10, TimeUnit.SECONDS);

        ServerControlledWriteExecutionException failure = executionFailure(approval);
        assertEquals("RENAME_DASHBOARD_ACCESS_DENIED", failure.code());
        assertEquals(WriteFailureCategory.ACCESS_DENIED, failure.category());
        assertEquals("原仪表盘", fixture.jdbc.queryForObject(
                "SELECT name FROM dashboard WHERE id = ?", String.class, DASHBOARD_ID));
        assertEquals("原仪表盘", fixture.jdbc.queryForObject(
                "SELECT name FROM folder WHERE id = ?", String.class, DASHBOARD_FOLDER_ID));
        assertEquals(0, fixture.jdbc.queryForObject(
                "SELECT status FROM dashboard WHERE id = ?", Integer.class, DASHBOARD_ID));
        fixture.assertFailed(proposal.approvalId(), WriteFailureCategory.ACCESS_DENIED);
        assertEquals("REPEATABLE-READ", fixture.observedApprovalIsolation.get());
    }

    private ServerControlledWriteExecutionException executionFailure(Future<WriteOperationView> future)
            throws Exception {
        ExecutionException wrapped = assertThrows(ExecutionException.class,
                () -> future.get(10, TimeUnit.SECONDS));
        assertTrue(wrapped.getCause() instanceof ServerControlledWriteExecutionException);
        return (ServerControlledWriteExecutionException) wrapped.getCause();
    }

    private MysqlDataSource dataSource(String url, String username, String password) {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setURL(url);
        dataSource.setUser(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    private String databaseUrl(String baseUrl, String database) {
        int query = baseUrl.indexOf('?');
        String prefix = query < 0 ? baseUrl : baseUrl.substring(0, query);
        String suffix = query < 0 ? "" : baseUrl.substring(query);
        if (!prefix.endsWith("/")) {
            prefix += "/";
        }
        return prefix + database + suffix;
    }

    private String environment(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }

    private record ApprovalAttempt(String approvalId,
                                   WriteOperationView result,
                                   ServerControlledWriteExecutionException failure) {

        private boolean succeeded() {
            return result != null && failure == null;
        }
    }

    private static final class Fixture {

        private final JdbcTemplate jdbc;
        private final DataSourceTransactionManager transactionManager;
        private final ServerControlledWriteTransactionService writes;
        private final ExecutorService executor = Executors.newFixedThreadPool(4);
        private final AtomicInteger resourceSequence = new AtomicInteger();
        private final AtomicBoolean blockFirstCreate = new AtomicBoolean();
        private final CountDownLatch firstCreateEntered = new CountDownLatch(1);
        private final CountDownLatch releaseFirstCreate = new CountDownLatch(1);
        private final AtomicBoolean captureApprovalIsolation = new AtomicBoolean();
        private final AtomicReference<String> observedApprovalIsolation = new AtomicReference<>();

        private Fixture(DataSource dataSource) {
            this.jdbc = new JdbcTemplate(dataSource);
            this.transactionManager = new DataSourceTransactionManager(dataSource);
            new ResourceDatabasePopulator(new ClassPathResource(
                    "db/migration/V2026.07.12__2.0.0.agent-controlled-write.sql")).execute(dataSource);
            createBusinessSchema();
            seedBusinessData();

            ViewService views = proxy(ViewService.class, this::viewService);
            FolderService folders = proxy(FolderService.class, this::folderService);
            DashboardService dashboards = proxy(DashboardService.class, this::dashboardService);
            VizService visualizations = proxy(VizService.class, this::vizService);
            OrgService organizations = proxy(OrgService.class, this::orgService);
            YuBiSecurityManager security = proxy(YuBiSecurityManager.class, this::securityManager);
            ThreadScopePermissionDataCache threadCache = new ThreadScopePermissionDataCache();
            threadCache.initData();
            PermissionDataCache permissionCache = new PermissionDataCache(
                    new RequestScopePermissionDataCache(), threadCache);
            ServerVisualizationWriteBusinessAdapter business = new ServerVisualizationWriteBusinessAdapter(
                    views, folders, dashboards, visualizations, organizations, security, permissionCache,
                    currentReads());
            var create = new DefaultCreateChartService(business, business);
            var rename = new DefaultRenameDashboardService(business, business);
            var visualizationWrite = new ServerAgentVisualizationWriteAdapter(create, rename);
            var registry = new DefaultWriteProposalToolRegistry(List.of(
                    new CreateChartWriteProposalTool(), new RenameDashboardWriteProposalTool()));
            var controlled = new DefaultControlledWriteUseCase(registry, visualizationWrite,
                    new JdbcWriteOperationStore(jdbc, new StructuredValueWebMapper()),
                    new JdbcWriteAuditAdapter(jdbc), () -> UUID.randomUUID().toString(), Instant::now,
                    Duration.ofMinutes(15));
            this.writes = new ServerControlledWriteTransactionService(controlled, controlled, transactionManager);
        }

        private ServerVisualizationWriteCurrentReadGateway currentReads() {
            ServerVisualizationWriteCurrentReadGateway currentReads =
                    mock(ServerVisualizationWriteCurrentReadGateway.class);
            when(currentReads.lockCreateTarget(anyString(), anyString(), any())).thenAnswer(invocation -> {
                String organizationId = invocation.getArgument(0);
                String viewId = invocation.getArgument(1);
                String parentId = invocation.getArgument(2);
                jdbc.queryForObject("SELECT id FROM `view` WHERE id=? FOR UPDATE", String.class, viewId);
                View view = view(viewId);
                Folder parent = null;
                if (parentId == null) {
                    jdbc.queryForObject("SELECT id FROM organization WHERE id=? FOR UPDATE",
                            String.class, organizationId);
                } else {
                    jdbc.queryForObject("SELECT id FROM folder WHERE id=? FOR UPDATE", String.class, parentId);
                    parent = folder(parentId);
                }
                return java.util.Optional.of(
                        new ServerVisualizationWriteCurrentReadGateway.LockedCreateTarget(view, parent));
            });
            when(currentReads.lockDashboardTarget(anyString())).thenAnswer(invocation -> {
                String dashboardId = invocation.getArgument(0);
                jdbc.queryForObject("SELECT id FROM dashboard WHERE id=? FOR UPDATE", String.class, dashboardId);
                String folderId = jdbc.queryForObject("""
                        SELECT id FROM folder WHERE rel_type=? AND rel_id=? FOR UPDATE
                        """, String.class, ResourceType.DASHBOARD.name(), dashboardId);
                Dashboard dashboard = dashboard(dashboardId);
                Folder folder = folder(folderId);
                if (folder.getParentId() == null) {
                    jdbc.queryForObject("SELECT id FROM organization WHERE id=? FOR UPDATE",
                            String.class, folder.getOrgId());
                } else {
                    jdbc.queryForObject("SELECT id FROM folder WHERE id=? FOR UPDATE",
                            String.class, folder.getParentId());
                }
                return java.util.Optional.of(
                        new ServerVisualizationWriteCurrentReadGateway.LockedDashboardTarget(dashboard, folder));
            });
            when(currentReads.lockTrustedAuthorization(anyString(), anyString())).thenAnswer(invocation -> {
                String organizationId = invocation.getArgument(0);
                String subjectId = invocation.getArgument(1);
                Integer membership = jdbc.queryForObject("""
                        SELECT COUNT(*) FROM membership
                        WHERE organization_id=? AND subject_id=? FOR UPDATE
                        """, Integer.class, organizationId, subjectId);
                jdbc.queryForList("SELECT subject_id FROM write_grant WHERE subject_id=? FOR UPDATE",
                        String.class, subjectId);
                return membership != null && membership == 1;
            });
            when(currentReads.nameExists(anyString(), any(), anyString())).thenAnswer(invocation -> {
                String organizationId = invocation.getArgument(0);
                String parentId = invocation.getArgument(1);
                String name = invocation.getArgument(2);
                if (parentId == null) {
                    return !jdbc.queryForList("""
                            SELECT id FROM folder
                            WHERE org_id=? AND parent_id IS NULL AND name=? FOR UPDATE
                            """, String.class, organizationId, name).isEmpty();
                }
                return !jdbc.queryForList("""
                        SELECT id FROM folder
                        WHERE org_id=? AND parent_id=? AND name=? FOR UPDATE
                        """, String.class, organizationId, parentId, name).isEmpty();
            });
            return currentReads;
        }

        private WriteOperationView proposeCreate(String name, String idempotencyKey, String sessionId) {
            LinkedHashMap<String, StructuredValue> values = new LinkedHashMap<>();
            values.put("name", StructuredValue.text(name));
            values.put("viewId", StructuredValue.text(VIEW_ID));
            return writes.propose(new WriteProposalCommand("create_chart", new ObjectValue(values),
                    idempotencyKey), context(sessionId, "preview-" + sessionId));
        }

        private WriteOperationView proposeRename(String newName, String idempotencyKey, String sessionId) {
            return writes.propose(new WriteProposalCommand("rename_dashboard", new ObjectValue(Map.of(
                    "dashboardId", StructuredValue.text(DASHBOARD_ID),
                    "newName", StructuredValue.text(newName))), idempotencyKey),
                    context(sessionId, "preview-" + sessionId));
        }

        private ApprovalAttempt attemptApproval(WriteOperationView proposal, String sessionId, String requestId) {
            try {
                return new ApprovalAttempt(proposal.approvalId(),
                        writes.approve(proposal.approvalId(), context(sessionId, requestId)), null);
            } catch (ServerControlledWriteExecutionException failure) {
                return new ApprovalAttempt(proposal.approvalId(), null, failure);
            }
        }

        private AgentExecutionContext context(String sessionId, String requestId) {
            return new AgentExecutionContext(sessionId, requestId,
                    new QueryExecutionContext(Channel.AUTHENTICATED, USER_ID, ORGANIZATION_ID,
                            "correlation-" + Integer.toHexString(requestId.hashCode())));
        }

        private Future<?> holdLock(String sql,
                                   String id,
                                   CountDownLatch lockHeld,
                                   CountDownLatch releaseLock) {
            TransactionTemplate holder = repeatableReadTransaction();
            return executor.submit(() -> holder.executeWithoutResult(status -> {
                jdbc.queryForObject(sql, String.class, id);
                lockHeld.countDown();
                await(releaseLock);
            }));
        }

        private Future<?> holdDashboardStateChange(CountDownLatch stateChanged,
                                                   CountDownLatch releaseChange) {
            TransactionTemplate holder = repeatableReadTransaction();
            return executor.submit(() -> holder.executeWithoutResult(status -> {
                jdbc.update("UPDATE dashboard SET status = 0, update_time = CURRENT_TIMESTAMP(3) WHERE id = ?",
                        DASHBOARD_ID);
                stateChanged.countDown();
                await(releaseChange);
            }));
        }

        private TransactionTemplate repeatableReadTransaction() {
            TransactionTemplate transaction = new TransactionTemplate(transactionManager);
            transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
            return transaction;
        }

        private void awaitMySqlLockWait() {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (System.nanoTime() < deadline) {
                Integer waits = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM performance_schema.data_lock_waits", Integer.class);
                if (waits != null && waits > 0) {
                    return;
                }
                try {
                    Thread.sleep(20);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("等待 MySQL 锁竞争时被中断", exception);
                }
            }
            throw new AssertionError("审批事务未进入预期的 MySQL 行锁等待");
        }

        private void assertFailed(String approvalId, WriteFailureCategory failure) {
            Map<String, Object> operation = jdbc.queryForMap("""
                    SELECT status, failure_code, resource_id
                    FROM agent_write_operation WHERE approval_id = ?
                    """, approvalId);
            assertEquals(WriteOperationState.FAILED.name(), operation.get("status"));
            assertEquals(failure.name(), operation.get("failure_code"));
            assertEquals(null, operation.get("resource_id"));
            assertEquals(1, count("""
                    SELECT COUNT(*) FROM agent_write_audit_event
                    WHERE approval_id = ? AND event_type = 'FAILED'
                      AND final_status = 'FAILED' AND failure_code = ?
                    """, approvalId, failure.name()));
        }

        private void assertSucceeded(String approvalId) {
            Map<String, Object> operation = jdbc.queryForMap("""
                    SELECT status, failure_code, resource_id
                    FROM agent_write_operation WHERE approval_id = ?
                    """, approvalId);
            assertEquals(WriteOperationState.SUCCEEDED.name(), operation.get("status"));
            assertEquals(null, operation.get("failure_code"));
            assertNotNull(operation.get("resource_id"));
            assertEquals(1, count("""
                    SELECT COUNT(*) FROM agent_write_audit_event
                    WHERE approval_id = ? AND event_type = 'SUCCEEDED' AND final_status = 'SUCCEEDED'
                    """, approvalId));
        }

        private int count(String sql, Object... parameters) {
            Integer value = jdbc.queryForObject(sql, Integer.class, parameters);
            return value == null ? 0 : value;
        }

        private Object viewService(Object proxy, Method method, Object[] arguments) {
            return switch (method.getName()) {
                case "retrieve" -> view((String) arguments[0]);
                case "requirePermission" -> {
                    View view = (View) arguments[0];
                    requireGrant("VIEW_READ", view.getId());
                    yield null;
                }
                default -> objectMethod(proxy, method, arguments);
            };
        }

        private Object folderService(Object proxy, Method method, Object[] arguments) {
            return switch (method.getName()) {
                case "retrieve" -> folder((String) arguments[0]);
                case "getVizFolder" -> visualizationFolder((String) arguments[0], (String) arguments[1]);
                case "requirePermission" -> {
                    Folder folder = (Folder) arguments[0];
                    String kind = folder.getId() == null ? "ROOT_CREATE"
                            : ResourceType.DASHBOARD.name().equals(folder.getRelType())
                            ? "DASHBOARD_MANAGE" : "FOLDER_CREATE";
                    requireGrant(kind, folder.getId() == null ? folder.getOrgId() : folder.getId());
                    yield null;
                }
                case "checkUnique" -> {
                    String organizationId = (String) arguments[0];
                    String parentId = (String) arguments[1];
                    String name = (String) arguments[2];
                    int existing = parentId == null
                            ? count("SELECT COUNT(*) FROM folder WHERE org_id = ? AND parent_id IS NULL AND name = ?",
                            organizationId, name)
                            : count("SELECT COUNT(*) FROM folder WHERE org_id = ? AND parent_id = ? AND name = ?",
                            organizationId, parentId, name);
                    if (existing > 0) {
                        throw new ParamException("名称已存在");
                    }
                    yield true;
                }
                case "update" -> updateFolder((FolderUpdateParam) arguments[0]);
                default -> objectMethod(proxy, method, arguments);
            };
        }

        private Object dashboardService(Object proxy, Method method, Object[] arguments) {
            return switch (method.getName()) {
                case "retrieve" -> dashboard((String) arguments[0]);
                default -> objectMethod(proxy, method, arguments);
            };
        }

        private Object vizService(Object proxy, Method method, Object[] arguments) {
            if (!"createDatachart".equals(method.getName())) {
                return objectMethod(proxy, method, arguments);
            }
            if (blockFirstCreate.compareAndSet(true, false)) {
                firstCreateEntered.countDown();
                await(releaseFirstCreate);
            }
            DatachartCreateParam parameter = (DatachartCreateParam) arguments[0];
            String suffix = String.valueOf(resourceSequence.incrementAndGet());
            String chartId = "chart-" + suffix;
            String folderId = "chart-folder-" + suffix;
            jdbc.update("""
                    INSERT INTO datachart (id, name, description, view_id, org_id, config, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, chartId, parameter.getName(), parameter.getDescription(), parameter.getViewId(),
                    parameter.getOrgId(), parameter.getConfig(), parameter.getStatus());
            jdbc.update("""
                    INSERT INTO folder (id, name, org_id, rel_type, rel_id, parent_id, `index`)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, folderId, parameter.getName(), parameter.getOrgId(), ResourceType.DATACHART.name(),
                    chartId, parameter.getParentId(), parameter.getIndex());
            return folder(folderId);
        }

        private Object orgService(Object proxy, Method method, Object[] arguments) {
            if (!"listOrganizations".equals(method.getName())) {
                return objectMethod(proxy, method, arguments);
            }
            if (count("SELECT COUNT(*) FROM membership WHERE subject_id = ? AND organization_id = ?",
                    USER_ID, ORGANIZATION_ID) == 0) {
                return List.of();
            }
            Organization organization = new Organization();
            organization.setId(ORGANIZATION_ID);
            organization.setName("并发测试组织");
            return List.of(new OrganizationBaseInfo(organization));
        }

        private Object securityManager(Object proxy, Method method, Object[] arguments) {
            if (!"getCurrentUser".equals(method.getName())) {
                return objectMethod(proxy, method, arguments);
            }
            User user = new User();
            user.setId(USER_ID);
            user.setUsername("mysql-concurrency-user");
            return user;
        }

        private void requireGrant(String kind, String resourceId) {
            captureIsolation();
            if (count("""
                    SELECT COUNT(*) FROM write_grant
                    WHERE subject_id = ? AND resource_kind = ? AND resource_id = ? AND allowed = 1
                    """, USER_ID, kind, resourceId) == 0) {
                throw new PermissionDeniedException("当前授权已撤销");
            }
        }

        private void captureIsolation() {
            if (captureApprovalIsolation.get()) {
                observedApprovalIsolation.set(jdbc.queryForObject(
                        "SELECT @@transaction_isolation", String.class));
            }
        }

        private View view(String id) {
            captureIsolation();
            return jdbc.queryForObject("SELECT id, name, org_id, status FROM `view` WHERE id = ?",
                    (result, row) -> {
                        View view = new View();
                        view.setId(result.getString("id"));
                        view.setName(result.getString("name"));
                        view.setOrgId(result.getString("org_id"));
                        view.setStatus(result.getByte("status"));
                        return view;
                    }, id);
        }

        private Folder folder(String id) {
            return jdbc.queryForObject("""
                    SELECT id, name, org_id, rel_type, rel_id, parent_id, `index`, sub_type, avatar,
                           update_by, update_time
                    FROM folder WHERE id = ?
                    """, (result, row) -> {
                        Folder folder = new Folder();
                        folder.setId(result.getString("id"));
                        folder.setName(result.getString("name"));
                        folder.setOrgId(result.getString("org_id"));
                        folder.setRelType(result.getString("rel_type"));
                        folder.setRelId(result.getString("rel_id"));
                        folder.setParentId(result.getString("parent_id"));
                        folder.setIndex(result.getObject("index", Double.class));
                        folder.setSubType(result.getString("sub_type"));
                        folder.setAvatar(result.getString("avatar"));
                        folder.setUpdateBy(result.getString("update_by"));
                        Timestamp updated = result.getTimestamp("update_time");
                        folder.setUpdateTime(updated == null ? null : new Date(updated.getTime()));
                        return folder;
                    }, id);
        }

        private Folder visualizationFolder(String visualizationId, String relType) {
            return jdbc.queryForObject("SELECT id FROM folder WHERE rel_id = ? AND rel_type = ?",
                    (result, row) -> folder(result.getString("id")), visualizationId, relType);
        }

        private Dashboard dashboard(String id) {
            captureIsolation();
            return jdbc.queryForObject("""
                    SELECT id, name, org_id, config, status, update_by, update_time
                    FROM dashboard WHERE id = ?
                    """, (result, row) -> {
                        Dashboard dashboard = new Dashboard();
                        dashboard.setId(result.getString("id"));
                        dashboard.setName(result.getString("name"));
                        dashboard.setOrgId(result.getString("org_id"));
                        dashboard.setConfig(result.getString("config"));
                        dashboard.setStatus(result.getByte("status"));
                        dashboard.setUpdateBy(result.getString("update_by"));
                        Timestamp updated = result.getTimestamp("update_time");
                        dashboard.setUpdateTime(updated == null ? null : new Date(updated.getTime()));
                        return dashboard;
                    }, id);
        }

        private boolean updateFolder(FolderUpdateParam parameter) {
            int folder = jdbc.update("""
                    UPDATE folder SET name = ?, parent_id = ?, `index` = ?, update_by = ?,
                                      update_time = CURRENT_TIMESTAMP(3)
                    WHERE id = ?
                    """, parameter.getName(), parameter.getParentId(), parameter.getIndex(), USER_ID,
                    parameter.getId());
            int dashboard = jdbc.update("""
                    UPDATE dashboard SET name = ?, update_by = ?, update_time = CURRENT_TIMESTAMP(3)
                    WHERE id = (SELECT rel_id FROM folder WHERE id = ? AND rel_type = ?)
                    """, parameter.getName(), USER_ID, parameter.getId(), ResourceType.DASHBOARD.name());
            return folder == 1 && dashboard == 1;
        }

        private void createBusinessSchema() {
            jdbc.execute("""
                    CREATE TABLE organization (id varchar(32) NOT NULL PRIMARY KEY, name varchar(255) NOT NULL)
                    ENGINE=InnoDB
                    """);
            jdbc.execute("""
                    CREATE TABLE `view` (
                      id varchar(32) NOT NULL PRIMARY KEY, name varchar(255) NOT NULL,
                      org_id varchar(32) NOT NULL, status tinyint NOT NULL
                    ) ENGINE=InnoDB
                    """);
            jdbc.execute("""
                    CREATE TABLE dashboard (
                      id varchar(32) NOT NULL PRIMARY KEY, name varchar(255) NOT NULL,
                      org_id varchar(32) NOT NULL, config text, status tinyint NOT NULL,
                      update_by varchar(32), update_time timestamp(3) NULL
                    ) ENGINE=InnoDB
                    """);
            jdbc.execute("""
                    CREATE TABLE folder (
                      id varchar(32) NOT NULL PRIMARY KEY, name varchar(255) NOT NULL,
                      org_id varchar(32) NOT NULL, rel_type varchar(64) NOT NULL,
                      rel_id varchar(32), parent_id varchar(32), `index` double,
                      sub_type varchar(64), avatar varchar(255), update_by varchar(32),
                      update_time timestamp(3) NULL,
                      UNIQUE KEY uq_folder_name (name, org_id, parent_id),
                      KEY idx_folder_rel (rel_type, rel_id)
                    ) ENGINE=InnoDB
                    """);
            jdbc.execute("""
                    CREATE TABLE datachart (
                      id varchar(32) NOT NULL PRIMARY KEY, name varchar(255) NOT NULL,
                      description varchar(255), view_id varchar(32), org_id varchar(32) NOT NULL,
                      config text, status tinyint
                    ) ENGINE=InnoDB
                    """);
            jdbc.execute("""
                    CREATE TABLE membership (
                      subject_id varchar(32) NOT NULL, organization_id varchar(32) NOT NULL,
                      PRIMARY KEY (subject_id, organization_id)
                    ) ENGINE=InnoDB
                    """);
            jdbc.execute("""
                    CREATE TABLE write_grant (
                      subject_id varchar(32) NOT NULL, resource_kind varchar(32) NOT NULL,
                      resource_id varchar(32) NOT NULL, allowed tinyint NOT NULL,
                      PRIMARY KEY (subject_id, resource_kind, resource_id)
                    ) ENGINE=InnoDB
                    """);
        }

        private void seedBusinessData() {
            jdbc.update("INSERT INTO organization (id, name) VALUES (?, ?)", ORGANIZATION_ID, "并发测试组织");
            jdbc.update("INSERT INTO `view` (id, name, org_id, status) VALUES (?, ?, ?, ?)",
                    VIEW_ID, "并发测试视图", ORGANIZATION_ID, Const.DATA_STATUS_ACTIVE);
            jdbc.update("""
                    INSERT INTO folder (id, name, org_id, rel_type, rel_id, parent_id, `index`,
                                        sub_type, avatar, update_by, update_time)
                    VALUES (?, ?, ?, ?, NULL, NULL, 0, NULL, NULL, ?, CURRENT_TIMESTAMP(3)),
                           (?, ?, ?, ?, ?, ?, 3, 'free', 'dashboard', ?, CURRENT_TIMESTAMP(3))
                    """, DASHBOARD_PARENT_ID, "仪表盘目录", ORGANIZATION_ID, ResourceType.FOLDER.name(), USER_ID,
                    DASHBOARD_FOLDER_ID, "原仪表盘", ORGANIZATION_ID, ResourceType.DASHBOARD.name(),
                    DASHBOARD_ID, DASHBOARD_PARENT_ID, USER_ID);
            jdbc.update("""
                    INSERT INTO dashboard (id, name, org_id, config, status, update_by, update_time)
                    VALUES (?, ?, ?, '{}', ?, ?, CURRENT_TIMESTAMP(3))
                    """, DASHBOARD_ID, "原仪表盘", ORGANIZATION_ID, Const.DATA_STATUS_ACTIVE, USER_ID);
            jdbc.update("INSERT INTO membership (subject_id, organization_id) VALUES (?, ?)",
                    USER_ID, ORGANIZATION_ID);
            jdbc.update("""
                    INSERT INTO write_grant (subject_id, resource_kind, resource_id, allowed)
                    VALUES (?, 'VIEW_READ', ?, 1), (?, 'ROOT_CREATE', ?, 1),
                           (?, 'DASHBOARD_MANAGE', ?, 1), (?, 'FOLDER_CREATE', ?, 1)
                    """, USER_ID, VIEW_ID, USER_ID, ORGANIZATION_ID,
                    USER_ID, DASHBOARD_FOLDER_ID, USER_ID, DASHBOARD_PARENT_ID);
        }

        private void close() {
            releaseFirstCreate.countDown();
            executor.shutdownNow();
            try {
                assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }

        @SuppressWarnings("unchecked")
        private static <T> T proxy(Class<T> type, InvocationHandler handler) {
            return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
        }

        private static Object objectMethod(Object proxy, Method method, Object[] arguments) {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> proxy.getClass().getInterfaces()[0].getSimpleName() + "Proxy";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == arguments[0];
                    default -> throw new UnsupportedOperationException(method.toString());
                };
            }
            throw new UnsupportedOperationException(method.toString());
        }

        private static void await(CountDownLatch latch) {
            try {
                if (!latch.await(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("等待并发测试闩锁超时");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("并发测试被中断", exception);
            }
        }
    }
}
