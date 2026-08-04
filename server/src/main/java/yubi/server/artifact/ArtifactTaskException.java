package yubi.server.artifact;

public final class ArtifactTaskException extends RuntimeException {

    private final String code;
    private final String traceId;

    public ArtifactTaskException(String code, String hint, String traceId) {
        super(hint);
        this.code = code;
        this.traceId = traceId;
    }

    public String code() {
        return code;
    }

    public String traceId() {
        return traceId;
    }
}
