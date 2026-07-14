package yubi.visualization.write.api;

public interface CreateChartUseCase {

    PreparedCreateChart prepare(CreateChartCommand command, VisualizationWriteContext context);

    BusinessResourceChange execute(PreparedCreateChart prepared, VisualizationWriteContext context);
}
