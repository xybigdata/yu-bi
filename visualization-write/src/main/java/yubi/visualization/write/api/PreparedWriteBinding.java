package yubi.visualization.write.api;

public record PreparedWriteBinding(String subjectId,
                                   String organizationId,
                                   String sessionId,
                                   String originatingRequestId,
                                   String originatingCorrelationId) {
}
