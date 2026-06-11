/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.security.manager.shiro;

import datart.core.entity.RelRoleResource;
import datart.core.entity.Role;
import datart.core.mappers.ext.RelRoleResourceMapperExt;
import datart.core.mappers.ext.RoleMapperExt;
import datart.security.base.RoleType;
import datart.security.manager.AuthorizationAssembler;
import datart.security.manager.AuthorizationCache;
import datart.security.manager.PermissionStringCodec;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ShiroAuthorizationAssembler implements AuthorizationAssembler {

    private final RoleMapperExt roleMapper;

    private final RelRoleResourceMapperExt rrrMapper;

    public ShiroAuthorizationAssembler(RoleMapperExt roleMapper, RelRoleResourceMapperExt rrrMapper) {
        this.roleMapper = roleMapper;
        this.rrrMapper = rrrMapper;
    }

    @Override
    public AuthorizationCache assemble(String orgId, String userId) {
        AuthorizationCache authorizationCache = new AuthorizationCache();
        List<Role> userRoles = roleMapper.selectByOrgAndUser(orgId, userId);
        for (Role role : userRoles) {
            if (role.getType().equals(RoleType.ORG_OWNER.name())) {
                authorizationCache.addRole(PermissionStringCodec.toRoleString(role.getType(), role.getOrgId()));
                authorizationCache.addPermission(PermissionStringCodec.toPermissionString(role.getOrgId(), "*", "*", "*"));
            }
        }
        List<RelRoleResource> relRoleResources = rrrMapper.listByOrgAndUser(orgId, userId);
        for (RelRoleResource rrr : relRoleResources) {
            authorizationCache.addPermissions(PermissionStringCodec.toPermissionStrings(
                    rrr.getOrgId(),
                    rrr.getRoleId(),
                    rrr.getResourceType(),
                    rrr.getResourceId(),
                    rrr.getPermission()));
        }
        return authorizationCache;
    }

}
