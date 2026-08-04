package yubi.server.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import yubi.server.artifact.ArtifactTaskException;
import yubi.server.controller.ArtifactTaskController;

@RestControllerAdvice(assignableTypes = ArtifactTaskController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ArtifactTaskWebExceptionHandler {

    @ExceptionHandler(ArtifactTaskException.class)
    public ResponseEntity<ArtifactTaskErrorResponse> artifactTask(ArtifactTaskException exception) {
        HttpStatus status = switch (exception.code()) {
            case "ARTIFACT_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "ARTIFACT_NOT_READY", "ARTIFACT_TASK_ACTIVE",
                    "ARTIFACT_RETRY_NOT_ALLOWED", "ARTIFACT_RETRY_UNAVAILABLE" -> HttpStatus.CONFLICT;
            case "ARTIFACT_CONCURRENCY_LIMIT" -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return ResponseEntity.status(status).body(new ArtifactTaskErrorResponse(
                false,
                exception.code(),
                exception.getMessage(),
                exception.traceId()
        ));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ArtifactTaskErrorResponse(boolean success,
                                            String code,
                                            String message,
                                            String traceId) {
    }
}
