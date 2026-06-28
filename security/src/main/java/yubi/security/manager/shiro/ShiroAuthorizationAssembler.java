/*
 * YuBi
 * <p>
 * Copyright 2021 (originally Datart by running-elephant)
 * Copyright 2024-2026 YuBi Contributors
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

package yubi.security.manager.shiro;

import yubi.core.entity.RelRoleResource;
import yubi.core.entity.Role;
import yubi.core.mappers.ext.RelRoleResourceMapperExt;
import yubi.core.mappers.ext.RoleMapperExt;
import yubi.security.base.RoleType;
import yubi.security.manager.AuthorizationAssembler;
import yubi.security.manager.AuthorizationCache;
import yubi.security.manager.PermissionStringCodec;
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
