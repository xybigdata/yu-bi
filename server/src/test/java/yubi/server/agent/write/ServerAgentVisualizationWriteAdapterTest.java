package yubi.server.agent.write;

import org.junit.jupiter.api.Test;
import yubi.agent.domain.ControlledWriteModels.CreateChartAction;
import yubi.agent.domain.ControlledWriteModels.PreparedWrite;
import yubi.agent.domain.ControlledWriteModels.PreviewField;
import yubi.agent.domain.ControlledWriteModels.TrustedWriteContext;
import yubi.agent.domain.ControlledWriteModels.WriteExecutionResult;
import yubi.agent.domain.ControlledWriteModels.WritePreview;
import yubi.visualization.write.api.BusinessResourceChange;
import yubi.visualization.write.api.CreateChartCommand;
import yubi.visualization.write.api.CreateChartUseCase;
import yubi.visualization.write.api.PreparedCreateChart;
import yubi.visualization.write.api.PreparedRenameDashboard;
import yubi.visualization.write.api.RenameDashboardCommand;
import yubi.visualization.write.api.RenameDashboardUseCase;
import yubi.visualization.write.api.VisualizationWriteContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static yubi.visualization.write.domain.VisualizationWriteModels.ChangeType.CREATED;
import static yubi.visualization.write.domain.VisualizationWriteModels.ResourceType.CHART;

class ServerAgentVisualizationWriteAdapterTest {

    @Test
    void createApprovalExecutionMustUsePersistedPreparationWithoutRunningPreflightReadsAgain() {
        RecordingCreateUseCase create = new RecordingCreateUseCase();
        ServerAgentVisualizationWriteAdapter adapter = new ServerAgentVisualizationWriteAdapter(
                create, new UnusedRenameUseCase());
        CreateChartAction action = new CreateChartAction("订单图表", "view-1", null, null);
        PreparedWrite persisted = new PreparedWrite(action, "a".repeat(64),
                new WritePreview("创建图表", "CHART", "view-1",
                        List.of(new PreviewField("图表名称", "订单图表"),
                                new PreviewField("数据视图", "view-1")),
                        List.of("创建一个未发布图表草稿")));
        TrustedWriteContext trusted = new TrustedWriteContext(
                "user-1", "org-1", "session-1", "request-approve", "correlation-approve");

        WriteExecutionResult result = adapter.execute(persisted, trusted);

        assertEquals("chart-1", result.resourceId());
        assertEquals(0, create.prepareCalls);
        assertEquals(1, create.executeCalls);
        assertEquals(new CreateChartCommand("订单图表", "view-1", null, null),
                create.executed.command());
        assertEquals("request-approve", create.executedContext.requestId());
    }

    private static final class RecordingCreateUseCase implements CreateChartUseCase {

        private int prepareCalls;
        private int executeCalls;
        private PreparedCreateChart executed;
        private VisualizationWriteContext executedContext;

        @Override
        public PreparedCreateChart prepare(CreateChartCommand command, VisualizationWriteContext context) {
            prepareCalls++;
            throw new AssertionError("审批执行不得重新运行 prepare");
        }

        @Override
        public BusinessResourceChange execute(PreparedCreateChart prepared, VisualizationWriteContext context) {
            executeCalls++;
            executed = prepared;
            executedContext = context;
            return new BusinessResourceChange(CHART, "chart-1", prepared.command().name(), CREATED,
                    null, "chart-state-1");
        }
    }

    private static final class UnusedRenameUseCase implements RenameDashboardUseCase {

        @Override
        public PreparedRenameDashboard prepare(RenameDashboardCommand command,
                                                VisualizationWriteContext context) {
            throw new AssertionError("未预期的重命名 prepare");
        }

        @Override
        public BusinessResourceChange execute(PreparedRenameDashboard prepared,
                                              VisualizationWriteContext context) {
            throw new AssertionError("未预期的重命名 execute");
        }
    }
}
