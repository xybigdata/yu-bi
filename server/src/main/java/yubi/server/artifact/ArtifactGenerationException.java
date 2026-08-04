package yubi.server.artifact;

public final class ArtifactGenerationException extends RuntimeException {

    private final String code;
    private final String hint;

    public ArtifactGenerationException(String code, String hint, Throwable cause) {
        super(hint, cause);
        this.code = requireText(code, "产物失败代码不能为空");
        this.hint = requireText(hint, "产物失败提示不能为空");
    }

    public String code() {
        return code;
    }

    public String hint() {
        return hint;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
