package yubi.server.agent.write;

public final class AgentWriteWebException extends RuntimeException {

    private final String code;

    AgentWriteWebException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
