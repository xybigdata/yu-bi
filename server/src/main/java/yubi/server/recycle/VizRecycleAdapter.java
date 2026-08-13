package yubi.server.recycle;

import lombok.extern.slf4j.Slf4j;
import yubi.core.base.consts.Const;
import yubi.core.entity.BaseEntity;
import yubi.core.entity.Folder;
import yubi.core.entity.Share;
import yubi.core.mappers.ext.FolderMapperExt;
import yubi.core.mappers.ext.ShareMapperExt;
import yubi.security.base.ResourceType;
import yubi.security.exception.PermissionDeniedException;
import yubi.security.manager.SecurityAuthorizationException;
import yubi.server.service.BaseCRUDService;
import yubi.server.service.FolderService;
import yubi.server.service.VizService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@Slf4j
abstract class VizRecycleAdapter<E extends BaseEntity> implements RecycleResourceAdapter {

    private final RecycleResourceType type;
    private final ResourceType securityType;
    private final BaseCRUDService<E, ?> service;
    private final VizService vizService;
    private final FolderService folderService;
    private final FolderMapperExt folderMapper;
    private final ShareMapperExt shareMapper;
    private final Function<E, String> name;
    private final Function<E, String> organizationId;
    private final Clock clock;
    private final RecycleDependencyResolver dependencyResolver;

    VizRecycleAdapter(RecycleResourceType type,
                      ResourceType securityType,
                      BaseCRUDService<E, ?> service,
                      VizService vizService,
                      FolderService folderService,
                      FolderMapperExt folderMapper,
                      ShareMapperExt shareMapper,
                      Function<E, String> name,
                      Function<E, String> organizationId,
                      RecycleDependencyResolver dependencyResolver) {
        this.type = type;
        this.securityType = securityType;
        this.service = service;
        this.vizService = vizService;
        this.folderService = folderService;
        this.folderMapper = folderMapper;
        this.shareMapper = shareMapper;
        this.name = name;
        this.organizationId = organizationId;
        this.dependencyResolver = dependencyResolver;
        this.clock = Clock.systemDefaultZone();
    }

    @Override
    public RecycleResourceType type() {
        return type;
    }

    @Override
    public RecycleItemPreflight preflight(RecycleAccess access,
                                          String rootId,
                                          Set<String> selectedRootIds) {
        try {
            Folder rootFolder = scopedFolder(rootId);
            if (rootFolder != null) {
                return preflightFolder(access, rootFolder, selectedRootIds);
            }
            E entity = service.retrieve(rootId);
            if (!access.organizationId().equals(organizationId.apply(entity))) {
                return new RecycleItemPreflight(
                        rootId, RecycleItemStatus.FORBIDDEN, "资源不属于当前组织", List.of());
            }
            if (!access.organizationOwner()) {
                service.requirePermission(entity, Const.MANAGE);
            }
            if (hasActiveShare(rootId)) {
                return new RecycleItemPreflight(
                        rootId, RecycleItemStatus.BLOCKED,
                        "存在有效分享链接，请先取消分享", List.of());
            }
            List<RecycleDependency> dependencies = dependencyResolver.find(
                    access, type, rootId);
            if (!dependencies.isEmpty()) {
                return RecycleItemPreflight.blocked(
                        rootId, "存在前置依赖，请先解除依赖", dependencies);
            }
            if (!service.safeDelete(rootId)) {
                return RecycleItemPreflight.blocked(
                        rootId, "存在前置依赖，请先解除依赖",
                        List.of(new RecycleDependency(
                                "restricted", null, type,
                                RecycleDependencyDepth.DIRECT, false)));
            }
            return RecycleItemPreflight.ready(rootId);
        } catch (PermissionDeniedException | SecurityAuthorizationException | SecurityException exception) {
            return new RecycleItemPreflight(
                    rootId, RecycleItemStatus.FORBIDDEN, "没有管理权限", List.of());
        } catch (RuntimeException exception) {
            log.error("回收站预检失败，资源类型={}，资源ID={}", type, rootId, exception);
            return new RecycleItemPreflight(
                    rootId, RecycleItemStatus.FAILED, "预检失败，请稍后重试", List.of());
        }
    }

    private RecycleItemPreflight preflightFolder(RecycleAccess access,
                                                 Folder root,
                                                 Set<String> selectedRootIds) {
        if (!access.organizationId().equals(root.getOrgId())) {
            return new RecycleItemPreflight(
                    root.getId(), RecycleItemStatus.FORBIDDEN,
                    "资源不属于当前组织", List.of());
        }
        List<RecycleDependency> dependencies = new ArrayList<>();
        for (Folder node : navigationSubtree(root)) {
            if (ResourceType.FOLDER.name().equals(node.getRelType())) {
                if (!access.organizationOwner()) {
                    folderService.requirePermission(node, Const.MANAGE);
                }
                continue;
            }
            E entity = service.retrieve(node.getRelId());
            if (!access.organizationId().equals(organizationId.apply(entity))) {
                return new RecycleItemPreflight(
                        root.getId(), RecycleItemStatus.FORBIDDEN,
                        "目录包含其他组织的资源", List.of());
            }
            if (!access.organizationOwner()) {
                service.requirePermission(entity, Const.MANAGE);
            }
            if (hasActiveShare(node.getRelId())) {
                return RecycleItemPreflight.blocked(
                        root.getId(), "目录内存在有效分享链接，请先取消分享", List.of());
            }
            dependencies.addAll(dependencyResolver.find(
                    access, type, node.getRelId()));
            if (!service.safeDelete(node.getRelId())) {
                dependencies.add(new RecycleDependency(
                        "restricted", null, type,
                        RecycleDependencyDepth.DIRECT, false));
            }
        }
        if (!dependencies.isEmpty()) {
            return RecycleItemPreflight.blocked(
                    root.getId(), "目录内存在前置依赖，请先解除依赖",
                    dependencies.stream().distinct().toList());
        }
        return RecycleItemPreflight.ready(root.getId());
    }

    private Folder scopedFolder(String rootId) {
        Folder folder = folderMapper.selectByPrimaryKey(rootId);
        if (folder == null
                || !ResourceType.FOLDER.name().equals(folder.getRelType())
                || !securityType.name().equals(folder.getSubType())) {
            return null;
        }
        return folder;
    }

    private List<Folder> navigationSubtree(Folder root) {
        Map<String, List<Folder>> children = new HashMap<>();
        folderMapper.selectByOrg(root.getOrgId()).stream()
                .filter(this::belongsToCurrentScope)
                .forEach(node -> children
                        .computeIfAbsent(node.getParentId(), ignored -> new ArrayList<>())
                        .add(node));
        List<Folder> result = new ArrayList<>();
        collectNavigation(root, children, result, new HashSet<>());
        return result;
    }

    private void collectNavigation(Folder current,
                                   Map<String, List<Folder>> children,
                                   List<Folder> result,
                                   Set<String> visited) {
        if (!visited.add(current.getId())) {
            throw new IllegalStateException("目录树存在循环引用");
        }
        result.add(current);
        for (Folder child : children.getOrDefault(current.getId(), List.of())) {
            collectNavigation(child, children, result, visited);
        }
    }

    private boolean belongsToCurrentScope(Folder node) {
        return securityType.name().equals(node.getRelType())
                || ResourceType.FOLDER.name().equals(node.getRelType())
                && securityType.name().equals(node.getSubType());
    }

    private RecycleNodeSnapshot navigationSnapshot(Folder node) {
        return new RecycleNodeSnapshot(
                node.getId(), node.getName(), node.getParentId(), safeIndex(node),
                ResourceType.FOLDER.name().equals(node.getRelType()),
                node.getRelId(), node.getSubType(), node.getAvatar());
    }

    private double safeIndex(Folder folder) {
        return folder.getIndex() == null ? 0D : folder.getIndex();
    }

    @Override
    @Transactional
    public RecycleRootSnapshot moveToRecycle(RecycleAccess access, String rootId) {
        Folder rootFolder = scopedFolder(rootId);
        if (rootFolder != null) {
            List<Folder> navigation = navigationSubtree(rootFolder);
            List<RecycleNodeSnapshot> nodes = navigation.stream()
                    .map(this::navigationSnapshot)
                    .toList();
            navigation.stream()
                    .filter(node -> !ResourceType.FOLDER.name().equals(node.getRelType()))
                    .forEach(node -> service.archive(node.getRelId()));
            List<Folder> folders = new ArrayList<>(navigation.stream()
                    .filter(node -> ResourceType.FOLDER.name().equals(node.getRelType()))
                    .toList());
            java.util.Collections.reverse(folders);
            folders.forEach(node -> folderMapper.deleteByPrimaryKey(node.getId()));
            return new RecycleRootSnapshot(
                    rootFolder.getId(), rootFolder.getName(), rootFolder.getParentId(),
                    safeIndex(rootFolder), true, nodes.size(), nodes);
        }
        E entity = service.retrieve(rootId);
        Folder folder = folderMapper.selectByRelTypeAndId(securityType.name(), rootId);
        String originalName = folder == null ? name.apply(entity) : folder.getName();
        String parentId = folder == null ? null : folder.getParentId();
        double index = folder == null || folder.getIndex() == null ? 0D : folder.getIndex();
        service.archive(rootId);
        return new RecycleRootSnapshot(rootId, originalName, parentId, index, false, 1);
    }

    @Override
    public int expandedItemCount(RecycleAccess access, String rootId) {
        Folder rootFolder = scopedFolder(rootId);
        return rootFolder == null ? 1 : navigationSubtree(rootFolder).size();
    }

    @Override
    @Transactional
    public RecycleItemResult restore(RecycleAccess access, RecycleRootSnapshot snapshot) {
        try {
            if (snapshot.folder()) {
                for (RecycleNodeSnapshot node : snapshot.nodes()) {
                    if (node.folder()) {
                        folderService.checkUnique(
                                access.organizationId(), node.parentId(),
                                node.name(), securityType);
                        folderMapper.insert(restoredNavigationNode(
                                access.organizationId(), node, ResourceType.FOLDER, null));
                        continue;
                    }
                    if (node.resourceId() == null) {
                        throw new IllegalArgumentException("目录快照缺少资源ID");
                    }
                    boolean restored = vizService.unarchiveViz(
                            node.resourceId(), securityType, node.name(),
                            node.parentId(), node.index());
                    if (!restored) {
                        throw new IllegalStateException("资源恢复失败");
                    }
                    folderMapper.deleteByRelTypeAndId(
                            securityType.name(), node.resourceId());
                    folderMapper.insert(restoredNavigationNode(
                            access.organizationId(), node, securityType, node.resourceId()));
                }
                return new RecycleItemResult(
                        snapshot.rootId(), RecycleItemStatus.SUCCESS, null, null);
            }
            boolean restored = vizService.unarchiveViz(
                    snapshot.rootId(), securityType, snapshot.originalName(),
                    snapshot.originalParentId(), snapshot.originalIndex());
            return new RecycleItemResult(
                    snapshot.rootId(),
                    restored ? RecycleItemStatus.SUCCESS : RecycleItemStatus.CONFLICT,
                    restored ? null : "原目录不存在或原位置存在同名对象",
                    null);
        } catch (RuntimeException exception) {
            markTransactionRollbackOnly();
            return new RecycleItemResult(
                    snapshot.rootId(), RecycleItemStatus.CONFLICT,
                    "原目录不存在或原位置存在同名对象", null);
        }
    }

    private Folder restoredNavigationNode(String organizationId,
                                          RecycleNodeSnapshot snapshot,
                                          ResourceType relType,
                                          String resourceId) {
        Folder folder = new Folder();
        folder.setId(snapshot.id());
        folder.setName(snapshot.name());
        folder.setOrgId(organizationId);
        folder.setRelType(relType.name());
        folder.setRelId(resourceId);
        folder.setParentId(snapshot.parentId());
        folder.setIndex(snapshot.index());
        folder.setSubType(snapshot.subType());
        folder.setAvatar(snapshot.avatar());
        return folder;
    }

    private void markTransactionRollbackOnly() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
    }

    @Override
    @Transactional
    public RecycleItemResult permanentlyDelete(RecycleAccess access,
                                               RecycleRootSnapshot snapshot) {
        if (snapshot.folder()) {
            List<RecycleNodeSnapshot> resources = snapshot.nodes().stream()
                    .filter(node -> !node.folder())
                    .toList();
            boolean blocked = resources.stream().anyMatch(node ->
                    node.resourceId() == null
                            || hasActiveShare(node.resourceId())
                            || !dependencyResolver.find(
                                    access, type, node.resourceId()).isEmpty()
                            || !service.safeDelete(node.resourceId()));
            if (blocked) {
                return new RecycleItemResult(
                        snapshot.rootId(), RecycleItemStatus.BLOCKED,
                        "目录内仍存在依赖或有效分享链接", null);
            }
            resources.forEach(node ->
                    service.delete(node.resourceId(), false, false));
            snapshot.nodes().stream()
                    .filter(RecycleNodeSnapshot::folder)
                    .forEach(node -> folderService.getRRRMapper()
                            .deleteByResourceId(node.id()));
            return new RecycleItemResult(
                    snapshot.rootId(), RecycleItemStatus.SUCCESS, null, null);
        }
        if (hasActiveShare(snapshot.rootId())
                || !dependencyResolver.find(access, type, snapshot.rootId()).isEmpty()
                || !service.safeDelete(snapshot.rootId())) {
            return new RecycleItemResult(
                    snapshot.rootId(), RecycleItemStatus.BLOCKED,
                    "仍存在依赖或有效分享链接", null);
        }
        service.delete(snapshot.rootId(), false, false);
        return new RecycleItemResult(
                snapshot.rootId(), RecycleItemStatus.SUCCESS, null, null);
    }

    @Override
    public boolean canManageRecycle(RecycleAccess access, RecycleRootSnapshot snapshot) {
        try {
            if (snapshot.folder()) {
                Folder root = restoredNavigationNode(
                        access.organizationId(), snapshot.nodes().getFirst(),
                        ResourceType.FOLDER, null);
                folderService.requirePermission(root, Const.MANAGE);
                return true;
            }
            E entity = service.retrieve(snapshot.rootId());
            service.requirePermission(entity, Const.MANAGE);
            return access.organizationId().equals(organizationId.apply(entity));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean hasActiveShare(String vizId) {
        Date now = Date.from(clock.instant());
        return shareMapper.selectByViz(vizId).stream()
                .map(Share::getExpiryDate)
                .anyMatch(expiry -> expiry == null || expiry.after(now));
    }
}
