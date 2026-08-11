package yubi.server.recycle;

import java.util.Objects;
import java.util.List;

public record RecycleRootSnapshot(String rootId,
                                  String originalName,
                                  String originalParentId,
                                  double originalIndex,
                                  boolean folder,
                                  int expandedItemCount,
                                  List<RecycleNodeSnapshot> nodes) {

    public RecycleRootSnapshot {
        Objects.requireNonNull(rootId, "rootId");
        Objects.requireNonNull(originalName, "originalName");
        if (expandedItemCount < 1) {
            throw new IllegalArgumentException("展开项数量至少为 1");
        }
        nodes = nodes == null || nodes.isEmpty()
                ? List.of(new RecycleNodeSnapshot(
                        rootId, originalName, originalParentId, originalIndex, folder))
                : List.copyOf(nodes);
        if (nodes.size() != expandedItemCount) {
            throw new IllegalArgumentException("展开项数量与子树快照不一致");
        }
    }

    public RecycleRootSnapshot(String rootId,
                               String originalName,
                               String originalParentId,
                               double originalIndex,
                               boolean folder,
                               int expandedItemCount) {
        this(rootId, originalName, originalParentId, originalIndex,
                folder, expandedItemCount,
                expandedItemCount == 1
                        ? List.of(new RecycleNodeSnapshot(
                                rootId, originalName, originalParentId, originalIndex, folder))
                        : null);
    }
}
