package yubi.visualization.write.port;

import yubi.visualization.write.api.VisualizationWriteContext;
import yubi.visualization.write.domain.VisualizationWriteModels.CreateChartDraft;
import yubi.visualization.write.domain.VisualizationWriteModels.CreateChartOutcome;
import yubi.visualization.write.domain.VisualizationWriteModels.ParentTarget;
import yubi.visualization.write.domain.VisualizationWriteModels.ViewTarget;

import java.util.Optional;

public interface ChartWritePort {

    Optional<ViewTarget> findView(String viewId);

    Optional<ParentTarget> findParent(String parentId);

    boolean chartNameExists(String organizationId, String parentId, String name);

    CreateChartOutcome createChart(CreateChartDraft draft, VisualizationWriteContext context);
}
