package yubi.server.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import yubi.server.agent.AgentWorkspaceRunException;
import yubi.server.agent.ServerAgentContextAccessDeniedException;
import yubi.server.agent.write.AgentWorkspaceSessionException;
import yubi.server.agent.write.AgentWorkspaceWebModels.ErrorResponse;
import yubi.server.controller.AgentWorkspaceRunController;

@RestControllerAdvice(assignableTypes = AgentWorkspaceRunController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AgentWorkspaceRunWebExceptionHandler {

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

    @ExceptionHandler(AgentWorkspaceRunException.class)
    public ResponseEntity<ErrorResponse> run(AgentWorkspaceRunException exception) {
        HttpStatus status = switch (exception.code()) {
            case INVALID_AGENT_RUN_REQUEST -> HttpStatus.BAD_REQUEST;
            case AGENT_RUNTIME_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case AGENT_RUNTIME_FAILED, INVALID_AGENT_RUN_RESPONSE -> HttpStatus.BAD_GATEWAY;
        };
        return error(status, exception.code().name(), exception.getMessage());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> missingHeader(MissingRequestHeaderException ignored) {
        return error(HttpStatus.FORBIDDEN, "WORKSPACE_SESSION_DENIED", "Agent 工作区会话不可用");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> unreadableRequest(HttpMessageNotReadableException ignored) {
        return invalidRequest(HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> unsupportedMediaType(HttpMediaTypeNotSupportedException ignored) {
        return invalidRequest(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> internal(RuntimeException ignored) {
        return error(HttpStatus.BAD_GATEWAY, "AGENT_RUNTIME_FAILED", "Agent Runtime 执行失败");
    }

    private ResponseEntity<ErrorResponse> invalidRequest(HttpStatus status) {
        return error(status, "INVALID_AGENT_RUN_REQUEST", "Agent 运行请求参数无效");
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(code, message));
    }
}
