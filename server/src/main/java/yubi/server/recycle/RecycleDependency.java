package yubi.server.recycle;

import java.util.Objects;

public record RecycleDependency(String id,
                                String name,
                                RecycleResourceType type,
                                RecycleDependencyDepth depth,
                                boolean readable,
                                String location,
                                String ownerId,
                                String route) {

    public RecycleDependency(String id,
                             String name,
                             RecycleResourceType type,
                             RecycleDependencyDepth depth,
                             boolean readable) {
        this(id, name, type, depth, readable, null, null, null);
    }

    public RecycleDependency {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(depth, "depth");
        if (!readable) {
            name = null;
            location = null;
            ownerId = null;
            route = null;
        }
    }
}
