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

package yubi.security.manager.springsecurity;

import yubi.core.base.consts.Const;
import yubi.core.base.exception.BaseException;
import yubi.core.base.exception.Exceptions;
import yubi.core.common.MessageResolver;
import yubi.core.entity.User;
import yubi.core.mappers.ext.UserMapperExt;
import yubi.security.base.JwtToken;
import yubi.security.base.PasswordToken;
import yubi.security.base.Permission;
import yubi.security.base.RoleType;
import yubi.security.exception.AuthException;
import yubi.security.exception.PermissionDeniedException;
import yubi.security.manager.YuBiSecurityManager;
import yubi.security.manager.PermissionStringCodec;
import yubi.security.manager.PermissionDataCache;
import yubi.security.manager.SecurityAuthorizationException;
import yubi.security.manager.SecuritySubjectFacade;
import yubi.security.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

/**
 * Spring Security implementation of {@link YuBiSecurityManager}.
 * <p>
 * Replaces the Shiro-based {@code ShiroSecurityManager} by delegating
 * authentication and authorization checks to {@link SpringSecuritySubjectFacade}
 * which uses Spring Security's {@link org.springframework.security.core.context.SecurityContextHolder}.
 */
@Slf4j
@Component(value = "yubiSecurityManager")
public class SpringSecurityManager implements YuBiSecurityManager {

    final MessageResolver messageResolver;

    private final UserMapperExt userMapper;

    private final PermissionDataCache permissionDataCache;

    private final SecuritySubjectFacade securitySubjectFacade;

    public SpringSecurityManager(MessageResolver messageResolver,
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
                    continue;
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
