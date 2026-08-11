package yubi.server.recycle;

import java.util.Objects;

public record RecycleNodeSnapshot(String id,
                                  String name,
                                  String parentId,
                                  double index,
                                  boolean folder) {

    public RecycleNodeSnapshot {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
    }
}
