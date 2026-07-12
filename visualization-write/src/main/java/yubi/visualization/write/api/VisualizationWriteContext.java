package yubi.visualization.write.api;

/** 只能由可信服务端边界构造，命令和模型参数不得覆盖其中任何字段。 */
public record VisualizationWriteContext(String subjectId,
                                        String organizationId,
                                        String sessionId,
                                        String requestId,
                                        String correlationId) {
}
