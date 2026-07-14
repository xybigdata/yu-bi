package yubi.visualization.write.port;

import yubi.visualization.write.api.VisualizationWriteContext;
import yubi.visualization.write.domain.VisualizationWriteModels.DashboardTarget;
import yubi.visualization.write.domain.VisualizationWriteModels.ParentTarget;
import yubi.visualization.write.domain.VisualizationWriteModels.ViewTarget;

public interface VisualizationWriteAuthorizationPort {

    boolean canCreateChart(VisualizationWriteContext context, ViewTarget view, ParentTarget parent);

    boolean canRenameDashboard(VisualizationWriteContext context, DashboardTarget dashboard);
}
