package yubi.server.agent.write;

import org.springframework.stereotype.Component;
import yubi.core.entity.Dashboard;
import yubi.core.entity.Folder;
import yubi.core.entity.View;
import yubi.core.mappers.ext.DashboardMapperExt;
import yubi.core.mappers.ext.FolderMapperExt;
import yubi.core.mappers.ext.OrganizationMapperExt;
import yubi.core.mappers.ext.RelRoleResourceMapperExt;
import yubi.core.mappers.ext.RoleMapperExt;
import yubi.core.mappers.ext.ViewMapperExt;
import yubi.security.base.ResourceType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** 受控写专用的 MyBatis 当前读边界；所有方法绕过并精确刷新对应二级缓存。 */
@Component
final class ServerVisualizationWriteCurrentReadGateway {

    private final ViewMapperExt viewMapper;
    private final FolderMapperExt folderMapper;
    private final DashboardMapperExt dashboardMapper;
    private final OrganizationMapperExt organizationMapper;
    private final RoleMapperExt roleMapper;
    private final RelRoleResourceMapperExt roleResourceMapper;

    ServerVisualizationWriteCurrentReadGateway(ViewMapperExt viewMapper,
                                               FolderMapperExt folderMapper,
                                               DashboardMapperExt dashboardMapper,
                                               OrganizationMapperExt organizationMapper,
                                               RoleMapperExt roleMapper,
                                               RelRoleResourceMapperExt roleResourceMapper) {
        this.viewMapper = viewMapper;
        this.folderMapper = folderMapper;
        this.dashboardMapper = dashboardMapper;
        this.organizationMapper = organizationMapper;
        this.roleMapper = roleMapper;
        this.roleResourceMapper = roleResourceMapper;
    }

    Optional<LockedCreateTarget> lockCreateTarget(String organizationId,
                                                  String viewId,
                                                  String parentId) {
        View view = viewMapper.selectControlledWriteCurrentForUpdate(viewId);
        if (view == null || !organizationId.equals(view.getOrgId())) {
            return Optional.empty();
        }
        Folder parent = null;
        if (parentId == null) {
            if (!organizationId.equals(folderMapper.selectControlledWriteRootNamespaceForUpdate(organizationId))) {
                return Optional.empty();
            }
        } else {
            parent = folderMapper.selectControlledWriteCurrentForUpdate(parentId);
            if (parent == null || !organizationId.equals(parent.getOrgId())
                    || !ResourceType.FOLDER.name().equals(parent.getRelType())) {
                return Optional.empty();
            }
        }
        return Optional.of(new LockedCreateTarget(view, parent));
    }

    Optional<LockedDashboardTarget> lockDashboardTarget(String dashboardId) {
        Dashboard dashboard = dashboardMapper.selectControlledWriteCurrentForUpdate(dashboardId);
        Folder folder = folderMapper.selectControlledWriteCurrentByRelationForUpdate(
                ResourceType.DASHBOARD.name(), dashboardId);
        if (dashboard == null || folder == null
                || !Objects.equals(dashboard.getId(), folder.getRelId())
                || !Objects.equals(dashboard.getOrgId(), folder.getOrgId())) {
            return Optional.empty();
        }
        if (folder.getParentId() == null) {
            if (!folder.getOrgId().equals(
                    folderMapper.selectControlledWriteRootNamespaceForUpdate(folder.getOrgId()))) {
                return Optional.empty();
            }
        } else {
            Folder namespace = folderMapper.selectControlledWriteCurrentForUpdate(folder.getParentId());
            if (namespace == null || !folder.getOrgId().equals(namespace.getOrgId())
                    || !ResourceType.FOLDER.name().equals(namespace.getRelType())) {
                return Optional.empty();
            }
        }
        return Optional.of(new LockedDashboardTarget(dashboard, folder));
    }

    boolean lockTrustedAuthorization(String organizationId, String subjectId) {
        String membership = organizationMapper.selectControlledWriteCurrentMembershipForUpdate(
                organizationId, subjectId);
        if (!organizationId.equals(membership)) {
            return false;
        }
        List<String> roles = roleMapper.selectControlledWriteCurrentAuthorizationForUpdate(
                organizationId, subjectId);
        if (!withinAuthorizationLockLimit(roles)) {
            return false;
        }
        List<String> resources = roleResourceMapper.selectControlledWriteCurrentAuthorizationForUpdate(
                organizationId, subjectId);
        return withinAuthorizationLockLimit(resources);
    }

    boolean nameExists(String organizationId, String parentId, String name) {
        List<Folder> matches = folderMapper.selectControlledWriteCurrentNamesForUpdate(
                organizationId, parentId, name);
        return matches != null && !matches.isEmpty();
    }

    private boolean withinAuthorizationLockLimit(List<String> values) {
        return values != null && values.size() <= RoleMapperExt.CONTROLLED_WRITE_AUTHORIZATION_LOCK_LIMIT;
    }

    record LockedCreateTarget(View view, Folder parent) {
    }

    record LockedDashboardTarget(Dashboard dashboard, Folder folder) {
    }
}
