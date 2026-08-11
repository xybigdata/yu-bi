package yubi.server.recycle;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record RecyclePreflight(String operationToken,
                               Instant expiresAt,
                               List<RecycleItemPreflight> items) {

    public RecyclePreflight {
        Objects.requireNonNull(operationToken, "operationToken");
        Objects.requireNonNull(expiresAt, "expiresAt");
        items = List.copyOf(items);
    }
}
