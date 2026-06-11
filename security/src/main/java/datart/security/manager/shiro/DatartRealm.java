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
import datart.core.entity.User;
import datart.core.mappers.ext.RelRoleResourceMapperExt;
import datart.core.mappers.ext.RoleMapperExt;
import datart.core.mappers.ext.UserMapperExt;
import datart.security.base.RoleType;
import datart.security.manager.AuthenticationCache;
import datart.security.manager.AuthorizationCache;
import datart.security.manager.PermissionDataCache;
import datart.security.manager.PermissionStringCodec;
import datart.security.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.*;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

import java.util.List;

@Slf4j
public class DatartRealm extends AuthorizingRealm {

    private final UserMapperExt userMapper;

    private final RoleMapperExt roleMapper;

    private final RelRoleResourceMapperExt rrrMapper;

    private final PermissionDataCache permissionDataCache;

    private final PasswordCredentialsMatcher passwordCredentialsMatcher;


    public DatartRealm(UserMapperExt userMapper,
                       RoleMapperExt roleMapper,
                       RelRoleResourceMapperExt rrrMapper,
                       PermissionDataCache permissionDataCache, PasswordCredentialsMatcher passwordCredentialsMatcher) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.rrrMapper = rrrMapper;
        this.permissionDataCache = permissionDataCache;
        this.passwordCredentialsMatcher = passwordCredentialsMatcher;
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        return true;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        AuthorizationCache authorizationCache = permissionDataCache.getAuthorizationCache();

        if (authorizationCache != null) {
            return toAuthorizationInfo(authorizationCache);
        }

        String userId = ((User) principals.getPrimaryPrincipal()).getId();

        authorizationCache = new AuthorizationCache();
        List<Role> userRoles = roleMapper.selectByOrgAndUser(permissionDataCache.getCurrentOrg(), userId);
        for (Role role : userRoles) {
            if (role.getType().equals(RoleType.ORG_OWNER.name())) {
                addOrgOwnerRoleAndPermission(authorizationCache, role);
            }
        }
        List<RelRoleResource> relRoleResources = rrrMapper.listByOrgAndUser(permissionDataCache.getCurrentOrg(), userId);
        for (RelRoleResource rrr : relRoleResources) {
            authorizationCache.addPermissions(PermissionStringCodec.toPermissionStrings(
                    rrr.getOrgId(),
                    rrr.getRoleId(),
                    rrr.getResourceType(),
                    rrr.getResourceId(),
                    rrr.getPermission()));
        }
        permissionDataCache.setAuthorizationCache(authorizationCache);

        return toAuthorizationInfo(authorizationCache);
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        AuthenticationCache authenticationCache = permissionDataCache.getAuthenticationCache();

        if (authenticationCache != null) {
            return toAuthenticationInfo(authenticationCache);
        }

        String username = getUsername(token);
        User user = userMapper.selectByNameOrEmail(username);
        if (user == null)
            return null;
        authenticationCache = new AuthenticationCache(user, user.getPassword(), getName());
        permissionDataCache.setAuthenticationCache(authenticationCache);
        return toAuthenticationInfo(authenticationCache);
    }

    @Override
    public CredentialsMatcher getCredentialsMatcher() {
        return passwordCredentialsMatcher;
    }

    /**
     * 为用户添加隐式权限
     */
    private void addOrgOwnerRoleAndPermission(AuthorizationCache authorizationCache, Role role) {
        //添加组织拥有者角色
        authorizationCache.addRole(PermissionStringCodec.toRoleString(role.getType(), role.getOrgId()));
        //添加组织拥有者权限
        String allPermission = PermissionStringCodec.toPermissionString(role.getOrgId(), "*", "*", "*");
        authorizationCache.addPermission(allPermission);
    }

    private SimpleAuthorizationInfo toAuthorizationInfo(AuthorizationCache authorizationCache) {
        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        authorizationInfo.addRoles(authorizationCache.getRoles());
        authorizationInfo.addStringPermissions(authorizationCache.getStringPermissions());
        return authorizationInfo;
    }

    private SimpleAuthenticationInfo toAuthenticationInfo(AuthenticationCache authenticationCache) {
        return new SimpleAuthenticationInfo(
                authenticationCache.getPrincipal(),
                authenticationCache.getCredentials(),
                authenticationCache.getRealmName());
    }

    private String getUsername(AuthenticationToken token) {
        if (token instanceof BearerToken) {
            return JwtUtils.toJwtToken((String) token.getPrincipal()).getSubject();
        } else {
            return (String) token.getPrincipal();
        }
    }

}
