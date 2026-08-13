package yubi.server.recycle;

import java.util.List;
import java.util.Objects;

public record RecycleItemPreflight(String rootId,
                                   RecycleItemStatus status,
                                   String message,
                                   List<RecycleDependency> dependencies) {

    public RecycleItemPreflight {
        Objects.requireNonNull(rootId, "rootId");
        Objects.requireNonNull(status, "status");
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
    }

    public static RecycleItemPreflight ready(String rootId) {
        return new RecycleItemPreflight(rootId, RecycleItemStatus.SUCCESS, null, List.of());
    }

    public static RecycleItemPreflight blocked(String rootId,
                                               String message,
                                               List<RecycleDependency> dependencies) {
        return new RecycleItemPreflight(
                rootId,
                RecycleItemStatus.BLOCKED,
                message,
                dependencies
        );
    }
}
