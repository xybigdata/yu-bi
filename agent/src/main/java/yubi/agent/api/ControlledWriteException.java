package yubi.agent.api;

/** 受控写协议只向调用方暴露有限错误码，不携带参数或底层异常详情。 */
public final class ControlledWriteException extends RuntimeException {

    private final Code code;

    public ControlledWriteException(Code code, String message) {
        super(message);
        this.code = java.util.Objects.requireNonNull(code, "code");
    }

    public Code code() {
        return code;
    }

    public enum Code {
        UNKNOWN_WRITE_TOOL,
        INVALID_WRITE_REQUEST,
        IDEMPOTENCY_CONFLICT,
        APPROVAL_NOT_FOUND,
        APPROVAL_SCOPE_MISMATCH,
        APPROVAL_STATE_INVALID,
        APPROVAL_TAMPERED,
        INVALID_BUSINESS_PREPARATION,
        INVALID_BUSINESS_RESULT
    }
}
