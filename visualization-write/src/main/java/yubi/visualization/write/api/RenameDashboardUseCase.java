package yubi.visualization.write.api;

public interface RenameDashboardUseCase {

    PreparedRenameDashboard prepare(RenameDashboardCommand command, VisualizationWriteContext context);

    BusinessResourceChange execute(PreparedRenameDashboard prepared, VisualizationWriteContext context);
}
