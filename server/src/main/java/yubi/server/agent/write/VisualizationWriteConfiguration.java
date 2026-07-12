package yubi.server.agent.write;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import yubi.agent.port.VisualizationWritePort;
import yubi.visualization.write.api.CreateChartUseCase;
import yubi.visualization.write.api.RenameDashboardUseCase;
import yubi.visualization.write.application.DefaultCreateChartService;
import yubi.visualization.write.application.DefaultRenameDashboardService;
import yubi.visualization.write.port.ChartWritePort;
import yubi.visualization.write.port.DashboardWritePort;
import yubi.visualization.write.port.VisualizationWriteAuthorizationPort;

@Configuration
public class VisualizationWriteConfiguration {

    @Bean
    public CreateChartUseCase createChartUseCase(ChartWritePort chartWritePort,
                                                 VisualizationWriteAuthorizationPort authorizationPort) {
        return new DefaultCreateChartService(chartWritePort, authorizationPort);
    }

    @Bean
    public RenameDashboardUseCase renameDashboardUseCase(DashboardWritePort dashboardWritePort,
                                                         VisualizationWriteAuthorizationPort authorizationPort) {
        return new DefaultRenameDashboardService(dashboardWritePort, authorizationPort);
    }

    @Bean
    public VisualizationWritePort agentVisualizationWritePort(CreateChartUseCase createChartUseCase,
                                                               RenameDashboardUseCase renameDashboardUseCase) {
        return new ServerAgentVisualizationWriteAdapter(createChartUseCase, renameDashboardUseCase);
    }
}
