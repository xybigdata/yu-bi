package yubi.visualization.write.api;

public enum VisualizationWriteFailureCode {
    INVALID_TRUSTED_CONTEXT(VisualizationWriteFailureCategory.VALIDATION, "可信写入上下文无效"),
    INVALID_CREATE_CHART_COMMAND(VisualizationWriteFailureCategory.VALIDATION, "创建图表命令无效"),
    INVALID_PREPARED_CREATE_CHART(VisualizationWriteFailureCategory.VALIDATION, "已准备的创建图表命令无效"),
    INVALID_RENAME_DASHBOARD_COMMAND(VisualizationWriteFailureCategory.VALIDATION, "重命名仪表盘命令无效"),
    INVALID_PREPARED_RENAME_DASHBOARD(VisualizationWriteFailureCategory.VALIDATION, "已准备的重命名仪表盘命令无效"),
    CREATE_CHART_ACCESS_DENIED(VisualizationWriteFailureCategory.ACCESS_DENIED, "当前用户无权创建该图表"),
    RENAME_DASHBOARD_ACCESS_DENIED(VisualizationWriteFailureCategory.ACCESS_DENIED, "当前用户无权重命名该仪表盘"),
    CHART_NAME_CONFLICT(VisualizationWriteFailureCategory.CONFLICT, "目标位置已存在同名图表"),
    DASHBOARD_NAME_CONFLICT(VisualizationWriteFailureCategory.CONFLICT, "目标位置已存在同名仪表盘"),
    DASHBOARD_STATE_STALE(VisualizationWriteFailureCategory.STALE, "仪表盘已在预览后发生变化"),
    CREATE_CHART_EXECUTION_FAILED(VisualizationWriteFailureCategory.EXECUTION, "创建图表执行失败"),
    RENAME_DASHBOARD_EXECUTION_FAILED(VisualizationWriteFailureCategory.EXECUTION, "重命名仪表盘执行失败");

    private final VisualizationWriteFailureCategory category;
    private final String safeMessage;

    VisualizationWriteFailureCode(VisualizationWriteFailureCategory category, String safeMessage) {
        this.category = category;
        this.safeMessage = safeMessage;
    }

    public VisualizationWriteFailureCategory category() {
        return category;
    }

    public String safeMessage() {
        return safeMessage;
    }
}
