package yubi.server.recycle;

import java.util.Objects;

public record RecycleNodeSnapshot(String id,
                                  String name,
                                  String parentId,
                                  double index,
                                  boolean folder,
                                  String resourceId,
                                  String subType,
                                  String avatar) {

    public RecycleNodeSnapshot {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
    }

    public RecycleNodeSnapshot(String id,
                               String name,
                               String parentId,
                               double index,
                               boolean folder) {
        this(id, name, parentId, index, folder, null, null, null);
    }
}
