package yubi.server.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import yubi.agent.api.ControlledWriteException;
import yubi.agent.api.ToolInputException;
import yubi.server.agent.ServerAgentContextAccessDeniedException;
import yubi.server.agent.write.AgentWorkspaceSessionException;
import yubi.server.agent.write.AgentWriteWebException;
import yubi.server.agent.write.AgentWorkspaceWebModels.ErrorResponse;
import yubi.server.agent.write.ServerControlledWriteExecutionException;
import yubi.server.controller.AgentWorkspaceSessionController;
import yubi.server.controller.ControlledWriteController;

@RestControllerAdvice(assignableTypes = {AgentWorkspaceSessionController.class, ControlledWriteController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ControlledWriteWebExceptionHandler {

    @ExceptionHandler(ServerAgentContextAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> workspaceAccess(ServerAgentContextAccessDeniedException ignored) {
        return error(HttpStatus.FORBIDDEN, "WORKSPACE_ACCESS_DENIED", "Agent 工作区不可用");
    }

    @ExceptionHandler(AgentWorkspaceSessionException.class)
    public ResponseEntity<ErrorResponse> session(AgentWorkspaceSessionException exception) {
        HttpStatus status = "WORKSPACE_SESSION_EXPIRED".equals(exception.code())
                ? HttpStatus.GONE : HttpStatus.FORBIDDEN;
        return error(status, exception.code(), exception.getMessage());
    }

    @ExceptionHandler(AgentWriteWebException.class)
    public ResponseEntity<ErrorResponse> request(AgentWriteWebException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.code(), exception.getMessage());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> missingHeader(MissingRequestHeaderException exception) {
        if (ControlledWriteController.SESSION_HEADER.equalsIgnoreCase(exception.getHeaderName())) {
            return error(HttpStatus.FORBIDDEN, "WORKSPACE_SESSION_DENIED", "Agent 工作区会话不可用");
        }
        return error(HttpStatus.BAD_REQUEST, "INVALID_WRITE_REQUEST", "受控写请求参数无效");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> unreadableRequest(HttpMessageNotReadableException ignored) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_WRITE_REQUEST", "受控写请求参数无效");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> unsupportedMediaType(HttpMediaTypeNotSupportedException ignored) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "INVALID_WRITE_REQUEST", "受控写请求参数无效");
    }

    @ExceptionHandler(ToolInputException.class)
    public ResponseEntity<ErrorResponse> toolInput(ToolInputException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.code(), exception.getMessage());
    }

    @ExceptionHandler(ControlledWriteException.class)
    public ResponseEntity<ErrorResponse> controlled(ControlledWriteException exception) {
        HttpStatus status = switch (exception.code()) {
            case UNKNOWN_WRITE_TOOL, INVALID_WRITE_REQUEST -> HttpStatus.BAD_REQUEST;
            case APPROVAL_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case APPROVAL_SCOPE_MISMATCH, APPROVAL_TAMPERED -> HttpStatus.FORBIDDEN;
            case IDEMPOTENCY_CONFLICT, APPROVAL_STATE_INVALID -> HttpStatus.CONFLICT;
            case INVALID_BUSINESS_PREPARATION -> HttpStatus.UNPROCESSABLE_CONTENT;
            case INVALID_BUSINESS_RESULT -> HttpStatus.BAD_GATEWAY;
        };
        return error(status, exception.code().name(), exception.getMessage());
    }

    @ExceptionHandler(ServerControlledWriteExecutionException.class)
    public ResponseEntity<ErrorResponse> execution(ServerControlledWriteExecutionException exception) {
        HttpStatus status = switch (exception.category()) {
            case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case VALIDATION -> HttpStatus.UNPROCESSABLE_CONTENT;
            case CONFLICT -> HttpStatus.CONFLICT;
            case EXECUTION, INTERNAL -> HttpStatus.BAD_GATEWAY;
        };
        return error(status, exception.code(), exception.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> internal(RuntimeException ignored) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "CONTROLLED_WRITE_INTERNAL",
                "受控写服务暂时不可用");
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(code, message));
    }
}
