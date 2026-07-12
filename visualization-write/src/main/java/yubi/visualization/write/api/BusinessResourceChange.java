package yubi.visualization.write.api;

import yubi.visualization.write.domain.VisualizationWriteModels.ChangeType;
import yubi.visualization.write.domain.VisualizationWriteModels.ResourceType;

public record BusinessResourceChange(ResourceType resourceType,
                                     String resourceId,
                                     String resourceName,
                                     ChangeType changeType,
                                     String beforeStateFingerprint,
                                     String afterStateFingerprint) {
}
