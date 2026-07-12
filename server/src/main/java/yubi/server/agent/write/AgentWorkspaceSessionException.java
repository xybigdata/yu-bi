package yubi.server.agent.write;

public final class AgentWorkspaceSessionException extends RuntimeException {

    private final String code;

    AgentWorkspaceSessionException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
