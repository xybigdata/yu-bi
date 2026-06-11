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

import datart.core.base.consts.Const;
import datart.core.base.exception.BaseException;
import datart.core.base.exception.Exceptions;
import datart.core.common.MessageResolver;
import datart.core.entity.User;
import datart.core.mappers.ext.UserMapperExt;
import datart.security.base.JwtToken;
import datart.security.base.PasswordToken;
import datart.security.base.Permission;
import datart.security.base.RoleType;
import datart.security.exception.AuthException;
import datart.security.exception.PermissionDeniedException;
import datart.security.manager.DatartSecurityManager;
import datart.security.manager.PermissionStringCodec;
import datart.security.manager.PermissionDataCache;
import datart.security.manager.SecurityAuthorizationException;
import datart.security.manager.SecuritySubjectFacade;
import datart.security.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.util.ThreadContext;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;


@Slf4j
@Component(value = "datartSecurityManager")
public class ShiroSecurityManager implements DatartSecurityManager {

    final MessageResolver messageResolver;

    private final UserMapperExt userMapper;

    private final PermissionDataCache permissionDataCache;

    private final SecuritySubjectFacade securitySubjectFacade;

    public ShiroSecurityManager(MessageResolver messageResolver,
                                UserMapperExt userMapper,
                                PermissionDataCache permissionDataCache,
                                SecuritySubjectFacade securitySubjectFacade) {
        this.messageResolver = messageResolver;
        this.userMapper = userMapper;
        this.permissionDataCache = permissionDataCache;
        this.securitySubjectFacade = securitySubjectFacade;
    }

    @Override
    public void login(PasswordToken token) throws RuntimeException {
        logoutCurrent();
        User user = userMapper.selectByNameOrEmail(token.getSubject());
        if (user == null) {
            Exceptions.tr(BaseException.class, "login.fail");
        }
        if (!user.getActive()) {
            Exceptions.tr(BaseException.class, "message.user.not.active");
        }
        try {
            securitySubjectFacade.loginWithPassword(token.getSubject(), token.getPassword());
        } catch (Exception e) {
            log.error("Login error ({})", token.getSubject());
            Exceptions.msg("login.fail");
        }
    }

    @Override
    public boolean validateUser(String username, String password) throws AuthException {
        User user = userMapper.selectByNameOrEmail(username);
        if (user == null) {
            return false;
        }
        return BCrypt.checkpw(password, user.getPassword()) || Objects.equals(password, user.getPassword());
    }

    @Override
    public String login(String tokenString) throws AuthException {
        logoutCurrent();
        JwtToken jwtToken = JwtUtils.toJwtToken(tokenString);
        if (!JwtUtils.validTimeout(jwtToken)) {
            Exceptions.tr(AuthException.class, "login.session.timeout");
        }
        User user = userMapper.selectByNameOrEmail(jwtToken.getSubject());
        if (user == null) {
            Exceptions.tr(AuthException.class, "login.session.timeout");
        }
        if (!user.getActive()) {
            Exceptions.tr(BaseException.class, "message.user.not.active");
        }

        if (jwtToken.getPwdHash() != user.getPassword().hashCode()) {
            Exceptions.tr(AuthException.class, "login.fail.pwd.hash");
        }

        try {
            securitySubjectFacade.loginWithBearer(tokenString);
        } catch (Exception e) {
            log.error("Login error ({})", user.getUsername());
            Exceptions.msg("login.fail");
        }
        return JwtUtils.toJwtString(JwtUtils.createJwtToken(user));
    }

    @Override
    public void logoutCurrent() {
        permissionDataCache.clear();
        securitySubjectFacade.logoutCurrent();
    }

    @Override
    public boolean isAuthenticated() {
        return securitySubjectFacade.isAuthenticated();
    }

    @Override
    public void requireAllPermissions(Permission... permissions) throws PermissionDeniedException {
        for (Permission permission : permissions) {
            Boolean permitted = permissionDataCache.getCachedPermission(permission);
            if (permitted != null) {
                if (!permitted) {
                    Exceptions.e(new SecurityAuthorizationException(null));
                } else {
                    return;
                }
            }
            Set<String> permissionString = PermissionStringCodec.toPermissionStrings(permission.getOrgId()
                    , permission.getRoleId()
                    , permission.getResourceType()
                    , permission.getResourceId()
                    , permission.getPermission());
            try {
                permissionDataCache.setCurrentOrg(permission.getOrgId());
                securitySubjectFacade.checkPermissions(permissionString.toArray(new String[0]));
                permissionDataCache.setPermissionCache(permission, true);
            } catch (SecurityAuthorizationException e) {
                log.warn("User permission denied. User-{} Permission-{}"
                        , getCurrentUser() != null ? getCurrentUser().getUsername() : "none"
                        , permission);
                permissionDataCache.setPermissionCache(permission, false);
                Exceptions.tr(PermissionDeniedException.class, "message.security.permission-denied");
            }
        }
    }

    @Override
    public void requireAnyPermission(Permission... permissions) throws PermissionDeniedException {
        boolean anyMatch = Arrays.stream(permissions).anyMatch(permission -> {
            if (permission == null) {
                return false;
            }
            Boolean permitted = permissionDataCache.getCachedPermission(permission);
            if (permitted != null) {
                if (!permitted) {
                    Exceptions.e(new SecurityAuthorizationException(null));
                } else {
                    return true;
                }
            }
            Set<String> permissionString = PermissionStringCodec.toPermissionStrings(permission.getOrgId()
                    , permission.getRoleId()
                    , permission.getResourceType()
                    , permission.getResourceId()
                    , permission.getPermission());
            try {
                permissionDataCache.setCurrentOrg(permission.getOrgId());
                securitySubjectFacade.checkPermissions(permissionString.toArray(new String[0]));
                permissionDataCache.setPermissionCache(permission, true);
                return true;
            } catch (SecurityAuthorizationException e) {
                log.warn("User permission denied. User-{} Permission-{}"
                        , getCurrentUser() != null ? getCurrentUser().getUsername() : "none"
                        , permission);
                permissionDataCache.setPermissionCache(permission, false);
                return false;
            }
        });
        if (!anyMatch) {
            Exceptions.tr(PermissionDeniedException.class, "message.security.permission-denied");
        }
    }

    @Override
    public void requireOrgOwner(String orgId) throws PermissionDeniedException {
        try {
            permissionDataCache.setCurrentOrg(orgId);
            securitySubjectFacade.checkRole(PermissionStringCodec.toRoleString(RoleType.ORG_OWNER.name(), orgId));
        } catch (SecurityAuthorizationException e) {
            log.warn("User permission denied. User-{} Role-{}"
                    , getCurrentUser() != null ? getCurrentUser().getUsername() : "none"
                    , RoleType.ORG_OWNER.name());
            Exceptions.tr(PermissionDeniedException.class, "message.security.permission-denied");
        }
    }

    @Override
    public boolean isOrgOwner(String orgId) {
        permissionDataCache.setCurrentOrg(orgId);
        return securitySubjectFacade.hasRole(PermissionStringCodec.toRoleString(RoleType.ORG_OWNER.name(), orgId));
    }

    @Override
    public boolean hasPermission(Permission... permissions) {

        for (Permission permission : permissions) {

            Boolean permitted = permissionDataCache.getCachedPermission(permission);
            if (permitted != null) {
                return permitted;
            }

            Set<String> strings = PermissionStringCodec.toPermissionStrings(permission.getOrgId()
                    , permission.getRoleId()
                    , permission.getResourceType()
                    , permission.getResourceId()
                    , permission.getPermission());
            try {
                permissionDataCache.setCurrentOrg(permission.getOrgId());
                securitySubjectFacade.checkPermissions(strings.toArray(new String[0]));
                permissionDataCache.setPermissionCache(permission, true);
            } catch (SecurityAuthorizationException e) {
                log.debug("User permission denied. User-{} Permission-{}"
                        , getCurrentUser() != null ? getCurrentUser().getUsername() : "none"
                        , permission);
                permissionDataCache.setPermissionCache(permission, false);
                return false;
            }
        }
        return true;
    }

    @Override
    public User getCurrentUser() {
        return securitySubjectFacade.getPrincipal();
    }

    @Override
    public void runAs(String userNameOrEmail) {
        securitySubjectFacade.clearRunAs();
        User user = userMapper.selectByNameOrEmail(userNameOrEmail);
        login(JwtUtils.toJwtString(JwtUtils.createJwtToken(user)));
    }

    @Override
    public void releaseRunAs() {
        logoutCurrent();
    }

}
