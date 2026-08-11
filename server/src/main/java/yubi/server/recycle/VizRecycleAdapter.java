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
import yubi.server.service.VizService;

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
                      FolderMapperExt folderMapper,
                      ShareMapperExt shareMapper,
                      Function<E, String> name,
                      Function<E, String> organizationId,
                      RecycleDependencyResolver dependencyResolver) {
        this.type = type;
        this.securityType = securityType;
        this.service = service;
        this.vizService = vizService;
        this.folderMapper = folderMapper;
        this.shareMapper = shareMapper;
        this.name = name;
        this.organizationId = organizationId;
        this.dependencyResolver = dependencyResolver;
        this.clock = Clock.systemDefaultZone();
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

    @Override
    public RecycleRootSnapshot moveToRecycle(RecycleAccess access, String rootId) {
        E entity = service.retrieve(rootId);
        Folder folder = folderMapper.selectByRelTypeAndId(securityType.name(), rootId);
        String originalName = folder == null ? name.apply(entity) : folder.getName();
        String parentId = folder == null ? null : folder.getParentId();
        double index = folder == null || folder.getIndex() == null ? 0D : folder.getIndex();
        service.archive(rootId);
        return new RecycleRootSnapshot(rootId, originalName, parentId, index, false, 1);
    }

    @Override
    public RecycleItemResult restore(RecycleAccess access, RecycleRootSnapshot snapshot) {
        try {
            boolean restored = vizService.unarchiveViz(
                    snapshot.rootId(), securityType, snapshot.originalName(),
                    snapshot.originalParentId(), snapshot.originalIndex());
            return new RecycleItemResult(
                    snapshot.rootId(),
                    restored ? RecycleItemStatus.SUCCESS : RecycleItemStatus.CONFLICT,
                    restored ? null : "原目录不存在或原位置存在同名对象",
                    null);
        } catch (RuntimeException exception) {
            return new RecycleItemResult(
                    snapshot.rootId(), RecycleItemStatus.CONFLICT,
                    "原目录不存在或原位置存在同名对象", null);
        }
    }

    @Override
    public RecycleItemResult permanentlyDelete(RecycleAccess access,
                                               RecycleRootSnapshot snapshot) {
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
