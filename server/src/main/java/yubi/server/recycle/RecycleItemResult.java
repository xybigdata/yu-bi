package yubi.server.recycle;

import java.util.Objects;

public record RecycleItemResult(String rootId,
                                RecycleItemStatus status,
                                String message,
                                String recordId) {

    public RecycleItemResult {
        Objects.requireNonNull(rootId, "rootId");
        Objects.requireNonNull(status, "status");
    }
}
