package yubi.server.recycle;

import java.util.List;
import java.util.Objects;

public record RecyclePreflightCommand(RecycleResourceType resourceType,
                                      List<String> rootIds) {

    public RecyclePreflightCommand {
        Objects.requireNonNull(resourceType, "resourceType");
        rootIds = rootIds == null ? List.of() : List.copyOf(rootIds);
    }
}
