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

import datart.core.entity.User;
import datart.security.manager.AuthenticationCache;
import datart.security.manager.AuthenticationAssembler;
import datart.security.manager.AuthenticationTokenAdapter;
import datart.security.manager.AuthorizationAssembler;
import datart.security.manager.AuthorizationCache;
import datart.security.manager.PermissionDataCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.*;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
@Slf4j
public class DatartRealm extends AuthorizingRealm {

    private final PermissionDataCache permissionDataCache;

    private final PasswordCredentialsMatcher passwordCredentialsMatcher;

    private final AuthenticationTokenAdapter<AuthenticationToken> authenticationTokenAdapter;

    private final AuthenticationAssembler authenticationAssembler;

    private final AuthorizationAssembler authorizationAssembler;

    public DatartRealm(
                       PermissionDataCache permissionDataCache,
                       PasswordCredentialsMatcher passwordCredentialsMatcher,
                       AuthenticationTokenAdapter<AuthenticationToken> authenticationTokenAdapter,
                       AuthenticationAssembler authenticationAssembler,
                       AuthorizationAssembler authorizationAssembler) {
        this.permissionDataCache = permissionDataCache;
        this.passwordCredentialsMatcher = passwordCredentialsMatcher;
        this.authenticationTokenAdapter = authenticationTokenAdapter;
        this.authenticationAssembler = authenticationAssembler;
        this.authorizationAssembler = authorizationAssembler;
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

        authorizationCache = authorizationAssembler.assemble(permissionDataCache.getCurrentOrg(), userId);
        permissionDataCache.setAuthorizationCache(authorizationCache);

        return toAuthorizationInfo(authorizationCache);
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        AuthenticationCache authenticationCache = permissionDataCache.getAuthenticationCache();

        if (authenticationCache != null) {
            return toAuthenticationInfo(authenticationCache);
        }

        String username = authenticationTokenAdapter.resolveUsername(token);
        authenticationCache = authenticationAssembler.assemble(username, getName());
        if (authenticationCache == null) {
            return null;
        }
        permissionDataCache.setAuthenticationCache(authenticationCache);
        return toAuthenticationInfo(authenticationCache);
    }

    @Override
    public CredentialsMatcher getCredentialsMatcher() {
        return passwordCredentialsMatcher;
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

}
