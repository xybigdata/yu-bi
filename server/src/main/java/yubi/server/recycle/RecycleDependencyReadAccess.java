package yubi.server.recycle;

@FunctionalInterface
interface RecycleDependencyReadAccess {

    boolean canRead(RecycleAccess access,
                    RecycleResourceType resourceType,
                    String resourceId);
}
