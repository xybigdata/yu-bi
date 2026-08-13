package yubi.server.recycle;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record RecycleBatch(String id,
                           RecycleResourceType resourceType,
                           RecycleOperation operation,
                           RecycleBatchState state,
                           Instant createdAt,
                           String undoToken,
                           Instant undoExpiresAt,
                           List<RecycleItemResult> items) {

    public RecycleBatch {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(resourceType, "resourceType");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(createdAt, "createdAt");
        items = List.copyOf(items);
    }
}
