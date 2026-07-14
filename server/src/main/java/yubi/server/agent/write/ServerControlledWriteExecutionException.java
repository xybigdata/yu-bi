package yubi.server.agent.write;

import yubi.agent.domain.ControlledWriteModels.WriteFailureCategory;

public final class ServerControlledWriteExecutionException extends RuntimeException {

    private final String code;
    private final WriteFailureCategory category;

    ServerControlledWriteExecutionException(String code,
                                            String safeMessage,
                                            WriteFailureCategory category) {
        super(safeMessage, null, false, true);
        this.code = code;
        this.category = category;
    }

    public String code() {
        return code;
    }

    public WriteFailureCategory category() {
        return category;
    }
}
