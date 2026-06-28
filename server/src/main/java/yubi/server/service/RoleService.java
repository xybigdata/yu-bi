package yubi.server.service;

import yubi.core.entity.Role;
import yubi.core.entity.User;
import yubi.core.entity.ext.UserBaseInfo;
import yubi.core.mappers.ext.RoleMapperExt;
import yubi.security.base.ResourceType;
import yubi.security.base.SubjectType;
import yubi.security.base.PermissionInfo;
import yubi.server.base.dto.ResourcePermissions;
import yubi.server.base.dto.SubjectPermissions;
import yubi.server.base.dto.ViewPermission;
import yubi.server.base.params.GrantPermissionParam;
import yubi.server.base.params.ViewPermissionParam;

import java.util.List;
import java.util.Set;

public interface RoleService extends BaseCRUDService<Role, RoleMapperExt> {

    boolean updateUsersForRole(String roleId, Set<String> userIds);

    boolean updateRolesForUser(String userId, String orgId, Set<String> roleIds);

    Role getPerUserRole(String orgId, String userId);

    User getPerUserRoleUser(String roleId);

    List<Role> listUserRoles(String orgId, String userId);

    List<UserBaseInfo> listRoleUsers(String roleId);

    boolean grantPermission(List<PermissionInfo> permissionInfo);

    boolean grantOrgOwner(String orgId, String userId, boolean checkPermission);

    boolean revokeOrgOwner(String orgId, String userId);

    List<PermissionInfo> grantPermission(GrantPermissionParam grantPermissionParam);

    SubjectPermissions getSubjectPermissions(String orgId, SubjectType subjectType, String subjectId);

    ResourcePermissions getResourcePermission(String orgId, ResourceType resourceType, String resourceId);

    List<ViewPermission> listRoleViewPermission(String orgId, SubjectType subjectType, String subjectId);

    List<ViewPermission> listViewPermission(String viewId);

    boolean grantViewPermission(ViewPermissionParam viewPermissionParam);

}