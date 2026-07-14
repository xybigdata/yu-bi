package yubi.visualization.write.api;

public record CreateChartCommand(String name,
                                 String viewId,
                                 String parentId,
                                 String description) {
}
