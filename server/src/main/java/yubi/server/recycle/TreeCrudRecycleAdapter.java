package yubi.server.recycle;

import yubi.core.base.consts.Const;
import yubi.core.entity.BaseEntity;
import yubi.server.service.BaseCRUDService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

abstract class TreeCrudRecycleAdapter<E extends BaseEntity> implements RecycleResourceAdapter {

    private final RecycleResourceType type;
    private final BaseCRUDService<E, ?> service;
    private final Function<E, String> name;
    private final Function<E, String> organizationId;
    private final Function<E, String> parentId;
    private final Function<E, Double> index;
    private final Function<E, Boolean> folder;
    private final RecycleDependencyResolver dependencyResolver;

    TreeCrudRecycleAdapter(RecycleResourceType type,
                           BaseCRUDService<E, ?> service,
                           Function<E, String> name,
                           Function<E, String> organizationId,
                           Function<E, String> parentId,
                           Function<E, Double> index,
                           Function<E, Boolean> folder,
                           RecycleDependencyResolver dependencyResolver) {
        this.type = type;
        this.service = service;
        this.name = name;
        this.organizationId = organizationId;
        this.parentId = parentId;
        this.index = index;
        this.folder = folder;
        this.dependencyResolver = dependencyResolver;
    }

    @Override
    public final RecycleResourceType type() {
        return type;
    }

    @Override
    public RecycleItemPreflight preflight(RecycleAccess access,
                                          String rootId,
                                          Set<String> selectedRootIds) {
        try {
            E root = service.retrieve(rootId);
            if (!access.organizationId().equals(organizationId.apply(root))) {
                return new RecycleItemPreflight(
                        rootId, RecycleItemStatus.FORBIDDEN, "资源不属于当前组织", List.of());
            }
            List<E> nodes = subtree(root);
            for (E node : nodes) {
                if (!access.organizationOwner()) {
                    service.requirePermission(node, Const.MANAGE);
                }
                RecycleItemPreflight extra = extraPreflight(node);
                if (extra.status() != RecycleItemStatus.SUCCESS) {
                    return extra;
                }
                if (!isFolder(node)) {
                    List<RecycleDependency> dependencies = dependencyResolver.find(
                            access, type, node.getId());
                    if (!dependencies.isEmpty()) {
                        return RecycleItemPreflight.blocked(
                                rootId, "存在前置依赖，请先解除依赖", dependencies);
                    }
                }
                if (!isFolder(node) && !service.safeDelete(node.getId())) {
                    return RecycleItemPreflight.blocked(
                            rootId,
                            "存在前置依赖，请先解除依赖",
                            List.of(new RecycleDependency(
                                    "restricted", null, type,
                                    RecycleDependencyDepth.DIRECT, false)));
                }
            }
            return RecycleItemPreflight.ready(rootId);
        } catch (SecurityException exception) {
            return new RecycleItemPreflight(
                    rootId, RecycleItemStatus.FORBIDDEN, "没有管理权限", List.of());
        }
    }

    @Override
    public RecycleRootSnapshot moveToRecycle(RecycleAccess access, String rootId) {
        E root = service.retrieve(rootId);
        List<E> entities = subtree(root);
        List<RecycleNodeSnapshot> nodes = entities.stream()
                .map(this::snapshot)
                .toList();
        entities.stream()
                .sorted(Comparator.comparingInt((E entity) -> depth(entity, entities)).reversed())
                .forEach(entity -> service.archive(entity.getId()));
        return new RecycleRootSnapshot(
                root.getId(), name.apply(root), parentId.apply(root), safeIndex(root),
                isFolder(root), nodes.size(), nodes);
    }

    @Override
    public int expandedItemCount(RecycleAccess access, String rootId) {
        E root = service.retrieve(rootId);
        if (!access.organizationId().equals(organizationId.apply(root))) {
            throw new SecurityException("资源不属于当前组织");
        }
        return subtree(root).size();
    }

    @Override
    public RecycleItemResult restore(RecycleAccess access, RecycleRootSnapshot snapshot) {
        try {
            snapshot.nodes().stream()
                    .sorted(Comparator.comparingInt(node -> depth(node, snapshot.nodes())))
                    .forEach(this::unarchive);
            return success(snapshot.rootId());
        } catch (RuntimeException exception) {
            return new RecycleItemResult(
                    snapshot.rootId(), RecycleItemStatus.CONFLICT,
                    "原目录不存在或原位置存在同名对象", null);
        }
    }

    @Override
    public RecycleItemResult permanentlyDelete(RecycleAccess access,
                                               RecycleRootSnapshot snapshot) {
        for (RecycleNodeSnapshot node : snapshot.nodes()) {
            List<RecycleDependency> dependencies = dependencyResolver.find(
                    access, type, node.id());
            if (!node.folder() && !dependencies.isEmpty()) {
                return new RecycleItemResult(
                        snapshot.rootId(), RecycleItemStatus.BLOCKED,
                        "仍存在前置依赖，请先解除依赖", null);
            }
            if (!node.folder() && !service.safeDelete(node.id())) {
                return new RecycleItemResult(
                        snapshot.rootId(), RecycleItemStatus.BLOCKED,
                        "存在前置依赖，请先解除依赖", null);
            }
        }
        snapshot.nodes().stream()
                .sorted(Comparator.comparingInt(
                        (RecycleNodeSnapshot node) -> depth(node, snapshot.nodes())).reversed())
                .forEach(node -> service.delete(node.id(), false, false));
        return success(snapshot.rootId());
    }

    @Override
    public boolean canManageRecycle(RecycleAccess access, RecycleRootSnapshot snapshot) {
        try {
            E root = service.retrieve(snapshot.rootId());
            service.requirePermission(root, Const.MANAGE);
            return access.organizationId().equals(organizationId.apply(root));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    protected RecycleItemPreflight extraPreflight(E entity) {
        return RecycleItemPreflight.ready(entity.getId());
    }

    protected abstract List<E> listActive(String organizationId);

    protected abstract void unarchive(RecycleNodeSnapshot node);

    private List<E> subtree(E root) {
        if (!isFolder(root)) {
            return List.of(root);
        }
        List<E> all = listActive(organizationId.apply(root));
        Map<String, List<E>> children = new HashMap<>();
        all.forEach(entity -> children.computeIfAbsent(parentId.apply(entity), ignored -> new ArrayList<>())
                .add(entity));
        List<E> result = new ArrayList<>();
        collect(root, children, result, new HashSet<>());
        return result;
    }

    private void collect(E current,
                         Map<String, List<E>> children,
                         List<E> result,
                         Set<String> visited) {
        if (!visited.add(current.getId())) {
            throw new IllegalStateException("目录树存在循环引用");
        }
        result.add(current);
        for (E child : children.getOrDefault(current.getId(), List.of())) {
            collect(child, children, result, visited);
        }
    }

    private RecycleNodeSnapshot snapshot(E entity) {
        return new RecycleNodeSnapshot(
                entity.getId(), name.apply(entity), parentId.apply(entity),
                safeIndex(entity), isFolder(entity));
    }

    private double safeIndex(E entity) {
        Double value = index.apply(entity);
        return value == null ? 0D : value;
    }

    private boolean isFolder(E entity) {
        return Boolean.TRUE.equals(folder.apply(entity));
    }

    private int depth(E entity, List<E> nodes) {
        return depth(parentId.apply(entity), nodes.stream()
                .collect(java.util.stream.Collectors.toMap(BaseEntity::getId, Function.identity())));
    }

    private int depth(RecycleNodeSnapshot node, List<RecycleNodeSnapshot> nodes) {
        return depth(node.parentId(), nodes.stream()
                .collect(java.util.stream.Collectors.toMap(RecycleNodeSnapshot::id, Function.identity())));
    }

    private int depth(String parent, Map<String, ?> nodes) {
        int depth = 0;
        Set<String> visited = new HashSet<>();
        String current = parent;
        while (current != null && nodes.containsKey(current) && visited.add(current)) {
            depth++;
            Object value = nodes.get(current);
            current = value instanceof RecycleNodeSnapshot snapshot
                    ? snapshot.parentId()
                    : parentId.apply(castEntity(value));
        }
        return depth;
    }

    @SuppressWarnings("unchecked")
    private E castEntity(Object value) {
        return (E) value;
    }

    private RecycleItemResult success(String rootId) {
        return new RecycleItemResult(rootId, RecycleItemStatus.SUCCESS, null, null);
    }
}
