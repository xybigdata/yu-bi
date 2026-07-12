package yubi.server.agent;

public final class AgentWorkspaceRunException extends RuntimeException {

    public enum Code {
        INVALID_AGENT_RUN_REQUEST,
        AGENT_RUNTIME_UNAVAILABLE,
        AGENT_RUNTIME_FAILED,
        INVALID_AGENT_RUN_RESPONSE
    }

    private final Code code;

    public AgentWorkspaceRunException(Code code, String message) {
        super(message);
        this.code = java.util.Objects.requireNonNull(code, "code");
    }

    public Code code() {
        return code;
    }
}
