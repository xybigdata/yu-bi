package yubi.server.recycle;

import java.util.Set;

public interface RecycleResourceAdapter {

    RecycleResourceType type();

    RecycleItemPreflight preflight(RecycleAccess access,
                                   String rootId,
                                   Set<String> selectedRootIds);

    RecycleRootSnapshot moveToRecycle(RecycleAccess access, String rootId);

    default int expandedItemCount(RecycleAccess access, String rootId) {
        return 1;
    }

    default RecycleItemResult restore(RecycleAccess access, RecycleRootSnapshot snapshot) {
        return new RecycleItemResult(
                snapshot.rootId(), RecycleItemStatus.FAILED, "该资源暂不支持恢复", null);
    }

    default RecycleItemResult permanentlyDelete(RecycleAccess access,
                                                RecycleRootSnapshot snapshot) {
        return new RecycleItemResult(
                snapshot.rootId(), RecycleItemStatus.FAILED, "该资源暂不支持永久删除", null);
    }

    default boolean canManageRecycle(RecycleAccess access, RecycleRootSnapshot snapshot) {
        return true;
    }
}
