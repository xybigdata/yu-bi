package yubi.visualization.write.api;

import java.util.Objects;

public final class VisualizationWriteException extends RuntimeException {

    private final VisualizationWriteFailureCode code;

    public VisualizationWriteException(VisualizationWriteFailureCode code) {
        super(Objects.requireNonNull(code, "code").safeMessage(), null, false, true);
        this.code = code;
    }

    public VisualizationWriteFailureCategory category() {
        return code.category();
    }

    public VisualizationWriteFailureCode code() {
        return code;
    }
}
