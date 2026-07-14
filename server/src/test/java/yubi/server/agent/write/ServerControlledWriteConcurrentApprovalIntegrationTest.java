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
import yubi.agent.api.AgentExecutionContext;
import yubi.agent.application.CreateChartWriteProposalTool;
import yubi.agent.application.DefaultControlledWriteUseCase;
import yubi.agent.application.DefaultWriteProposalToolRegistry;
import yubi.agent.application.RenameDashboardWriteProposalTool;
import yubi.agent.domain.ControlledWriteModels.PreparedWrite;
import yubi.agent.domain.ControlledWriteModels.TrustedWriteContext;
import yubi.agent.domain.ControlledWriteModels.WriteAction;
import yubi.agent.domain.ControlledWriteModels.WriteExecutionResult;
import yubi.agent.domain.ControlledWriteModels.WriteOperationState;
import yubi.agent.domain.ControlledWriteModels.WriteOperationView;
import yubi.agent.domain.ControlledWriteModels.WritePreview;
import yubi.agent.domain.ControlledWriteModels.WriteProposalCommand;
import yubi.agent.domain.StructuredValue;
import yubi.agent.domain.StructuredValue.ObjectValue;
import yubi.agent.port.VisualizationWritePort;
import yubi.query.api.QueryExecutionContext;
import yubi.query.domain.QueryModels.Channel;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "YUBI_TEST_MYSQL_URL", matches = "jdbc:mysql:.*")
class ServerControlledWriteConcurrentApprovalIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-12T04:00:00Z");

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final AtomicInteger generatedIds = new AtomicInteger();

    private String databaseName;
    private JdbcTemplate admin;
    private JdbcTemplate jdbc;
    private BlockingChartWritePort business;
    private ServerControlledWriteTransactionService service;
    private AgentExecutionContext context;

    @BeforeEach
    void setUp() {
        String baseUrl = System.getenv("YUBI_TEST_MYSQL_URL");
        String username = environment("YUBI_TEST_MYSQL_USERNAME", "root");
        String password = environment("YUBI_TEST_MYSQL_PASSWORD", "");
        admin = new JdbcTemplate(dataSource(baseUrl, username, password));
        databaseName = "yubi_concurrent_approval_"
                + java.util.UUID.randomUUID().toString().replace("-", "");
        admin.execute("CREATE DATABASE `" + databaseName
                + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        DataSource dataSource = dataSource(databaseUrl(baseUrl, databaseName), username, password);
        new ResourceDatabasePopulator(new ClassPathResource(
                "db/migration/V2026.07.12__2.0.0.agent-controlled-write.sql"))
                .execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        assertEquals("REPEATABLE-READ", jdbc.queryForObject(
                "SELECT @@transaction_isolation", String.class));
        jdbc.execute("""
                CREATE TABLE test_chart_business_write (
                  resource_id varchar(64) NOT NULL PRIMARY KEY,
                  chart_name varchar(255) NOT NULL
                )
                """);

        JdbcWriteOperationStore store = new JdbcWriteOperationStore(
                jdbc, new StructuredValueWebMapper());
        JdbcWriteAuditAdapter audit = new JdbcWriteAuditAdapter(jdbc);
        business = new BlockingChartWritePort(jdbc);
        DefaultControlledWriteUseCase useCase = new DefaultControlledWriteUseCase(
                new DefaultWriteProposalToolRegistry(List.of(
                        new CreateChartWriteProposalTool(), new RenameDashboardWriteProposalTool())),
                business, store, audit, () -> "server-id-" + generatedIds.incrementAndGet(),
                () -> NOW, Duration.ofMinutes(5));
        service = new ServerControlledWriteTransactionService(
                useCase, useCase, new DataSourceTransactionManager(dataSource));
        context = context("proposal-request", "proposal-correlation");
    }

    @AfterEach
    void tearDown() {
        business.release();
        executor.shutdownNow();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        if (admin != null && databaseName != null) {
            admin.execute("DROP DATABASE IF EXISTS `" + databaseName + "`");
        }
    }

    @Test
    void concurrentApprovalMustHoldLockUntilCommitAndReplayTerminalResult() throws Exception {
        WriteOperationView proposal = service.propose(new WriteProposalCommand(
                "create_chart", createChartArguments(), "concurrent-approval-key"), context);
        assertEquals(WriteOperationState.PENDING, proposal.state());
        assertEquals(0, business.executeCalls());

        AgentExecutionContext firstApproval = context("approval-request-1", "approval-correlation-1");
        AgentExecutionContext secondApproval = context("approval-request-2", "approval-correlation-2");
        Future<WriteOperationView> first = executor.submit(
                () -> service.approve(proposal.approvalId(), firstApproval));
        assertTrue(business.awaitFirstExecution(), "第一次批准未进入业务执行");

        CountDownLatch secondStarted = new CountDownLatch(1);
        Future<WriteOperationView> second = executor.submit(() -> {
            secondStarted.countDown();
            return service.approve(proposal.approvalId(), secondApproval);
        });
        assertTrue(secondStarted.await(5, TimeUnit.SECONDS), "第二次批准未启动");

        // 第一事务已在 execute 中，审批行的 SELECT FOR UPDATE 锁必须持有到业务与审计一并提交。
        assertThrows(TimeoutException.class, () -> second.get(300, TimeUnit.MILLISECONDS));
        assertEquals(1, business.executeCalls());
        assertFalse(second.isDone());

        business.release();
        WriteOperationView firstResult = first.get(5, TimeUnit.SECONDS);
        WriteOperationView secondResult = second.get(5, TimeUnit.SECONDS);

        assertEquals(WriteOperationState.SUCCEEDED, firstResult.state());
        assertFalse(firstResult.replayed());
        assertEquals(WriteOperationState.SUCCEEDED, secondResult.state());
        assertTrue(secondResult.replayed());
        assertEquals(firstResult.resourceId(), secondResult.resourceId());

        assertEquals(1, business.executeCalls());
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM test_chart_business_write", Integer.class));
        assertEquals(1, jdbc.queryForObject("""
                SELECT COUNT(*) FROM agent_write_operation
                WHERE approval_id = ? AND status = 'SUCCEEDED' AND resource_id = ?
                """, Integer.class, proposal.approvalId(), firstResult.resourceId()));
        assertEquals(1, jdbc.queryForObject("""
                SELECT COUNT(*) FROM agent_write_audit_event
                WHERE approval_id = ? AND event_type = 'SUCCEEDED'
                  AND request_id = 'approval-request-1'
                  AND correlation_id = 'approval-correlation-1'
                """, Integer.class, proposal.approvalId()));
        assertEquals(1, jdbc.queryForObject("""
                SELECT COUNT(*) FROM agent_write_audit_event
                WHERE approval_id = ? AND event_type = 'IDEMPOTENT_REPLAYED'
                  AND request_id = 'approval-request-2'
                  AND correlation_id = 'approval-correlation-2'
                """, Integer.class, proposal.approvalId()));
    }

    private AgentExecutionContext context(String requestId, String correlationId) {
        return new AgentExecutionContext("session-1", requestId,
                new QueryExecutionContext(Channel.AUTHENTICATED,
                        "user-1", "org-1", correlationId));
    }

    private ObjectValue createChartArguments() {
        Map<String, StructuredValue> values = new LinkedHashMap<>();
        values.put("name", StructuredValue.text("并发批准图表"));
        values.put("viewId", StructuredValue.text("view-1"));
        return new ObjectValue(values);
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
        return (prefix.endsWith("/") ? prefix : prefix + "/") + database + suffix;
    }

    private String environment(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }

    private static final class BlockingChartWritePort implements VisualizationWritePort {

        private final JdbcTemplate jdbc;
        private final AtomicInteger executeCalls = new AtomicInteger();
        private final CountDownLatch firstExecution = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        private BlockingChartWritePort(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        @Override
        public PreparedWrite prepare(WriteAction action,
                                     WritePreview safePreview,
                                     TrustedWriteContext context) {
            return new PreparedWrite(action, "view-state-v1", safePreview);
        }

        @Override
        public WriteExecutionResult execute(PreparedWrite operation,
                                            TrustedWriteContext context) {
            int call = executeCalls.incrementAndGet();
            if (call == 1) {
                firstExecution.countDown();
                await(release, "等待释放第一次业务写入超时");
            }
            jdbc.update("""
                    INSERT INTO test_chart_business_write (resource_id, chart_name)
                    VALUES (?, ?)
                    """, "chart-created", "并发批准图表");
            return new WriteExecutionResult("chart-created");
        }

        private boolean awaitFirstExecution() throws InterruptedException {
            return firstExecution.await(5, TimeUnit.SECONDS);
        }

        private int executeCalls() {
            return executeCalls.get();
        }

        private void release() {
            release.countDown();
        }

        private static void await(CountDownLatch latch, String timeoutMessage) {
            try {
                if (!latch.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(timeoutMessage);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("业务写入等待被中断");
            }
        }
    }
}
