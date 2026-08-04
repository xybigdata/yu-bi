package yubi.server.artifact;

import java.time.Instant;

public record TaskView(String id,
                       ArtifactDescriptor descriptor,
                       ArtifactTaskState state,
                       Instant acceptedAt,
                       Instant deadlineAt,
                       Instant completedAt,
                       ArtifactFailure failure,
                       String traceId) {
}
