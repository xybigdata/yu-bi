package yubi.server.artifact;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

public final class ArtifactTaskWebMapper {

    public ArtifactTaskResponse response(TaskHandle task) {
        return new ArtifactTaskResponse(
                task.id(),
                status(task.state()),
                task.descriptor().fileName(),
                task.descriptor().mediaType(),
                task.acceptedAt(),
                task.deadlineAt(),
                null,
                null,
                task.traceId(),
                task.descriptor().source()
        );
    }

    public ArtifactTaskResponse response(TaskView task) {
        return new ArtifactTaskResponse(
                task.id(),
                status(task.state()),
                task.descriptor().fileName(),
                task.descriptor().mediaType(),
                task.acceptedAt(),
                task.deadlineAt(),
                task.completedAt(),
                failure(task.failure()),
                task.traceId(),
                task.descriptor().source()
        );
    }

    private String status(ArtifactTaskState state) {
        return switch (state) {
            case QUEUED -> "ACCEPTED";
            case RUNNING -> "RUNNING";
            case READY -> "SUCCEEDED";
            case FAILED -> "FAILED";
            case TIMED_OUT -> "TIMED_OUT";
        };
    }

    private ArtifactTaskError failure(ArtifactFailure failure) {
        return failure == null
                ? null
                : new ArtifactTaskError(failure.code(), failure.hint(), failure.traceId());
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ArtifactTaskResponse(String id,
                                       String status,
                                       String fileName,
                                       String mediaType,
                                       Instant acceptedAt,
                                       Instant deadlineAt,
                                       Instant completedAt,
                                       ArtifactTaskError error,
                                       String traceId,
                                       String source) {
    }

    public record ArtifactTaskError(String code, String message, String traceId) {
    }
}
