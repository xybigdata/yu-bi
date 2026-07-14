package yubi.visualization.write.port;

import yubi.visualization.write.api.VisualizationWriteContext;
import yubi.visualization.write.domain.VisualizationWriteModels.DashboardTarget;
import yubi.visualization.write.domain.VisualizationWriteModels.RenameDashboardDraft;
import yubi.visualization.write.domain.VisualizationWriteModels.RenameDashboardOutcome;

import java.util.Optional;

public interface DashboardWritePort {

    Optional<DashboardTarget> findDashboard(String dashboardId);

    boolean dashboardNameExists(String organizationId,
                                String parentId,
                                String name,
                                String excludedDashboardId);

    RenameDashboardOutcome renameDashboard(RenameDashboardDraft draft,
                                           String expectedStateFingerprint,
                                           VisualizationWriteContext context);
}
