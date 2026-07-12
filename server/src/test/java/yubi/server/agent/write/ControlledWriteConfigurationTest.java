package yubi.server.agent.write;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import yubi.agent.api.WriteApprovalUseCase;
import yubi.agent.api.WriteProposalUseCase;
import yubi.agent.application.DefaultControlledWriteUseCase;
import yubi.agent.application.DefaultWriteProposalToolRegistry;
import yubi.agent.port.VisualizationWritePort;
import yubi.agent.port.WriteAuditPort;
import yubi.agent.port.WriteOperationStorePort;
import yubi.agent.port.WriteProposalToolRegistry;
import yubi.visualization.write.api.CreateChartUseCase;
import yubi.visualization.write.api.RenameDashboardUseCase;
import yubi.visualization.write.application.DefaultCreateChartService;
import yubi.visualization.write.application.DefaultRenameDashboardService;
import yubi.visualization.write.port.ChartWritePort;
import yubi.visualization.write.port.DashboardWritePort;
import yubi.visualization.write.port.VisualizationWriteAuthorizationPort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ControlledWriteConfigurationTest {

    @Test
    void springCompositionRootMustExposeOnlyTheTwoApprovedWriteUseCases() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(ChartWritePort.class, () -> mock(ChartWritePort.class));
            context.registerBean(DashboardWritePort.class, () -> mock(DashboardWritePort.class));
            context.registerBean(VisualizationWriteAuthorizationPort.class,
                    () -> mock(VisualizationWriteAuthorizationPort.class));
            context.registerBean(WriteOperationStorePort.class, () -> mock(WriteOperationStorePort.class));
            context.registerBean(WriteAuditPort.class, () -> event -> { });
            context.register(VisualizationWriteConfiguration.class, ControlledWriteConfiguration.class);
            context.refresh();

            WriteProposalToolRegistry registry = context.getBean(WriteProposalToolRegistry.class);
            assertInstanceOf(DefaultWriteProposalToolRegistry.class, registry);
            assertEquals(List.of("create_chart", "rename_dashboard"),
                    registry.schemas().stream().map(value -> value.name()).toList());
            assertTrue(registry.schemas().stream().allMatch(schema -> schema.requiresApproval()
                    && schema.effect() == yubi.agent.api.WriteToolSchema.Effect.WRITE));
            for (String forbidden : List.of("delete_chart", "delete_dashboard", "publish_chart",
                    "share_dashboard", "change_permission", "execute_sql")) {
                assertTrue(registry.find(forbidden).isEmpty());
            }

            assertInstanceOf(DefaultCreateChartService.class, context.getBean(CreateChartUseCase.class));
            assertInstanceOf(DefaultRenameDashboardService.class, context.getBean(RenameDashboardUseCase.class));
            assertInstanceOf(ServerAgentVisualizationWriteAdapter.class,
                    context.getBean(VisualizationWritePort.class));
            DefaultControlledWriteUseCase controlled = context.getBean(DefaultControlledWriteUseCase.class);
            assertSame(controlled, context.getBean(WriteProposalUseCase.class));
            assertSame(controlled, context.getBean(WriteApprovalUseCase.class));
        }
    }

    @Test
    void retentionServiceMustUseItsConfiguredProductionConstructor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class));
            context.registerBean(PlatformTransactionManager.class,
                    () -> mock(PlatformTransactionManager.class));
            context.register(ControlledWriteRetentionService.class);

            context.refresh();

            assertInstanceOf(ControlledWriteRetentionService.class,
                    context.getBean(ControlledWriteRetentionService.class));
        }
    }
}
