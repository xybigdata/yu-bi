package yubi.server.service;

public record SharedDownloadContext(
        String shareId,
        String sessionDigest,
        String executionUsername,
        String auditCreatorId,
        String querySubjectId,
        String organizationId
) {
}
