package yubi.server.agent.write;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import yubi.agent.api.AgentExecutionContext;
import yubi.agent.api.WriteApprovalUseCase;
import yubi.agent.api.WriteProposalUseCase;
import yubi.agent.domain.ControlledWriteModels.PreviewField;
import yubi.agent.domain.ControlledWriteModels.WriteFailureCategory;
import yubi.agent.domain.ControlledWriteModels.WriteOperationState;
import yubi.agent.domain.ControlledWriteModels.WriteOperationView;
import yubi.agent.domain.ControlledWriteModels.WritePreview;
import yubi.agent.domain.ControlledWriteModels.WriteProposalCommand;
import yubi.query.api.QueryExecutionContext;
import yubi.query.domain.QueryModels.Channel;
import yubi.visualization.write.api.VisualizationWriteException;
import yubi.visualization.write.api.VisualizationWriteFailureCode;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServerControlledWriteTransactionServiceTest {

    private JdbcTemplate jdbcTemplate;
    private DataSourceTransactionManager transactionManager;
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        JdbcDataSource embedded = new JdbcDataSource();
        embedded.setURL("jdbc:h2:mem:controlled-write-tx-" + java.util.UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource = embedded;
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("CREATE TABLE business_change (id varchar(32) PRIMARY KEY)");
        jdbcTemplate.execute("CREATE TABLE failure_ledger (id varchar(32) PRIMARY KEY)");
        transactionManager = new DataSourceTransactionManager(dataSource);
    }

    @Test
    void shouldRollbackBusinessWriteBeforePersistingFailureInNewTransaction() {
        AtomicInteger approvalIsolation = new AtomicInteger();
        AtomicInteger failureIsolation = new AtomicInteger();
        WriteApprovalUseCase approvals = new StubApprovals() {
            @Override
            public WriteOperationView approve(String approvalId, AgentExecutionContext context) {
                approvalIsolation.set(currentIsolation());
                jdbcTemplate.update("INSERT INTO business_change (id) VALUES (?)", "partial");
                throw new VisualizationWriteException(
                        VisualizationWriteFailureCode.CREATE_CHART_EXECUTION_FAILED);
            }

            @Override
            public WriteOperationView markFailed(String approvalId,
                                                 WriteFailureCategory failure,
                                                 AgentExecutionContext context) {
                failureIsolation.set(currentIsolation());
                jdbcTemplate.update("INSERT INTO failure_ledger (id) VALUES (?)", approvalId);
                return failed();
            }
        };
        ServerControlledWriteTransactionService service = service(approvals);

        ServerControlledWriteExecutionException failure = assertThrows(
                ServerControlledWriteExecutionException.class,
                () -> service.approve("approval-1", context()));

        assertEquals("CREATE_CHART_EXECUTION_FAILED", failure.code());
        assertEquals(Connection.TRANSACTION_REPEATABLE_READ, approvalIsolation.get());
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, failureIsolation.get());
        assertEquals(0, count("business_change"));
        assertEquals(1, count("failure_ledger"));
    }

    @Test
    void shouldCommitSuccessfulBusinessWriteExactlyOnce() {
        WriteApprovalUseCase approvals = new StubApprovals() {
            @Override
            public WriteOperationView approve(String approvalId, AgentExecutionContext context) {
                jdbcTemplate.update("INSERT INTO business_change (id) VALUES (?)", "resource-1");
                return succeeded();
            }
        };
        ServerControlledWriteTransactionService service = service(approvals);

        WriteOperationView result = service.approve("approval-1", context());

        assertEquals(WriteOperationState.SUCCEEDED, result.state());
        assertEquals(1, count("business_change"));
        assertEquals(0, count("failure_ledger"));
    }

    @Test
    void approvalMustSuspendOuterTransactionAndCommitInDedicatedRepeatableReadTransaction() {
        AtomicInteger approvalIsolation = new AtomicInteger();
        WriteApprovalUseCase approvals = new StubApprovals() {
            @Override
            public WriteOperationView approve(String approvalId, AgentExecutionContext context) {
                approvalIsolation.set(currentIsolation());
                jdbcTemplate.update("INSERT INTO business_change (id) VALUES (?)", "independent-resource");
                return succeeded();
            }
        };
        ServerControlledWriteTransactionService service = service(approvals);
        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        outer.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

        outer.executeWithoutResult(status -> {
            assertEquals(Connection.TRANSACTION_REPEATABLE_READ, currentIsolation());
            service.approve("approval-1", context());
            status.setRollbackOnly();
        });

        assertEquals(Connection.TRANSACTION_REPEATABLE_READ, approvalIsolation.get());
        assertEquals(1, count("business_change"));
    }

    private ServerControlledWriteTransactionService service(WriteApprovalUseCase approvals) {
        WriteProposalUseCase proposals = new WriteProposalUseCase() {
            @Override
            public WriteOperationView propose(WriteProposalCommand command, AgentExecutionContext context) {
                throw new UnsupportedOperationException();
            }
        };
        return new ServerControlledWriteTransactionService(proposals, approvals, transactionManager);
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private int currentIsolation() {
        try {
            return DataSourceUtils.getConnection(dataSource).getTransactionIsolation();
        } catch (SQLException exception) {
            throw new IllegalStateException("无法读取受控写事务隔离级别", exception);
        }
    }

    private AgentExecutionContext context() {
        return new AgentExecutionContext("session-1", "request-1",
                new QueryExecutionContext(Channel.AUTHENTICATED, "user-1", "org-1", "correlation-1"));
    }

    private static WriteOperationView succeeded() {
        return view(WriteOperationState.SUCCEEDED, "resource-1", null);
    }

    private static WriteOperationView failed() {
        return view(WriteOperationState.FAILED, null, WriteFailureCategory.EXECUTION);
    }

    private static WriteOperationView view(WriteOperationState state,
                                           String resourceId,
                                           WriteFailureCategory failure) {
        Instant created = Instant.parse("2026-07-12T02:00:00Z");
        return new WriteOperationView("approval-1", "change-1", "create_chart", state,
                new WritePreview("创建图表", "CHART", "view-1",
                        List.of(new PreviewField("图表名称", "订单")), List.of("创建未发布图表")),
                resourceId, failure, created, created.plusSeconds(900),
                state == WriteOperationState.PENDING ? null : created.plusSeconds(1), false);
    }

    private abstract static class StubApprovals implements WriteApprovalUseCase {
        @Override
        public List<WriteOperationView> listOperations(AgentExecutionContext context) {
            return List.of();
        }

        @Override
        public WriteOperationView reject(String approvalId, AgentExecutionContext context) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WriteOperationView markFailed(String approvalId,
                                             WriteFailureCategory failure,
                                             AgentExecutionContext context) {
            throw new UnsupportedOperationException();
        }
    }
}
