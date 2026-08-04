package yubi.server.artifact;

import java.time.Instant;

public record TaskHandle(String id,
                         ArtifactDescriptor descriptor,
                         ArtifactTaskState state,
                         Instant acceptedAt,
                         Instant deadlineAt,
                         String traceId) {
}
