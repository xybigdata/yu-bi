package yubi.agent.api;

public final class ToolInputException extends RuntimeException {

    private final String code;

    public ToolInputException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
