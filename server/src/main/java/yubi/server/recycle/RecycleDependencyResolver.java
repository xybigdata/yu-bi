package yubi.server.recycle;

import java.util.List;

interface RecycleDependencyResolver {

    List<RecycleDependency> find(RecycleAccess access,
                                 RecycleResourceType resourceType,
                                 String resourceId);
}
