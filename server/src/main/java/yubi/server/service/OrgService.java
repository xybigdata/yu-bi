package yubi.server.service;

import yubi.core.entity.Organization;
import yubi.core.entity.User;
import yubi.core.entity.ext.RoleBaseInfo;
import yubi.core.entity.ext.UserBaseInfo;
import yubi.core.mappers.ext.OrganizationMapperExt;
import yubi.security.base.RoleType;
import yubi.server.base.dto.InviteMemberResponse;
import yubi.server.base.dto.OrganizationBaseInfo;
import yubi.server.base.params.OrgCreateParam;
import yubi.server.base.params.OrgUpdateParam;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface OrgService extends BaseCRUDService<Organization, OrganizationMapperExt> {

    List<OrganizationBaseInfo> listOrganizations();

    OrganizationBaseInfo getOrgDetail(String orgId);

    boolean updateOrganization(OrgUpdateParam updateParam);

    Organization createOrganization(OrgCreateParam createParam);

    void initOrganization(Organization organization, User creator);

    void createDefaultRole(RoleType roleType, User creator, Organization org);

    boolean deleteOrganization(String orgId);

    boolean updateAvatar(String orgId, String path) throws IOException;

    List<UserBaseInfo> listOrgMembers(String orgId);

    List<RoleBaseInfo> listOrgRoles(String orgId);

    InviteMemberResponse addMembers(String orgId, Set<String> emails, boolean sendMail);

    boolean confirmInvite(String token);

    boolean removeUser(String orgId, String userId);

    List<RoleBaseInfo> listUserRoles(String orgId, String userId);

    void addUserToOrg(String userId, String orgId);

    Organization checkTeamOrg();

}
