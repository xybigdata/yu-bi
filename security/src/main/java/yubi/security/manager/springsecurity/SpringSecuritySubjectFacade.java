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

import yubi.core.entity.User;
import yubi.security.manager.AuthenticationAssembler;
import yubi.security.manager.AuthenticationCache;
import yubi.security.manager.AuthorizationAssembler;
import yubi.security.manager.AuthorizationCache;
import yubi.security.manager.PermissionDataCache;
import yubi.security.manager.SecurityAuthorizationException;
import yubi.security.manager.SecuritySubjectFacade;
import yubi.security.util.JwtUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security implementation of {@link SecuritySubjectFacade}.
 * <p>
 * Replaces the Shiro-based {@code ShiroSubjectFacade} by delegating all
 * authentication and authorization checks to Spring Security's
 * {@link SecurityContextHolder}.
 */
@Component
public class SpringSecuritySubjectFacade implements SecuritySubjectFacade {

    private static final String REALM_NAME = "yubi";

    private final PermissionDataCache permissionDataCache;

    private final AuthenticationAssembler authenticationAssembler;

    private final AuthorizationAssembler authorizationAssembler;

    public SpringSecuritySubjectFacade(PermissionDataCache permissionDataCache,
                                       AuthenticationAssembler authenticationAssembler,
                                       AuthorizationAssembler authorizationAssembler) {
        this.permissionDataCache = permissionDataCache;
        this.authenticationAssembler = authenticationAssembler;
        this.authorizationAssembler = authorizationAssembler;
    }

    @Override
    public void loginWithPassword(String subject, String password) {
        AuthenticationCache cache = authenticationAssembler.assemble(subject, REALM_NAME);
        if (cache == null) {
            throw new SecurityAuthorizationException(null);
        }
        Authentication auth = new UsernamePasswordAuthenticationToken(
                cache.getPrincipal(),
                cache.getCredentials(),
                Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Override
    public void loginWithBearer(String tokenString) {
        var jwtToken = JwtUtils.toJwtToken(tokenString);
        if (jwtToken == null) {
            throw new SecurityAuthorizationException(null);
        }
        AuthenticationCache cache = authenticationAssembler.assemble(jwtToken.getSubject(), REALM_NAME);
        if (cache == null) {
            throw new SecurityAuthorizationException(null);
        }
        Authentication auth = new UsernamePasswordAuthenticationToken(
                cache.getPrincipal(),
                null,
                Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Override
    public void logoutCurrent() {
        SecurityContextHolder.clearContext();
    }

    @Override
    public boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated();
    }

    @Override
    public void checkPermissions(String... permissions) throws SecurityAuthorizationException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new SecurityAuthorizationException(null);
        }
        for (String permission : permissions) {
            if (!isPermissionAllowed(permission, auth)) {
                throw new SecurityAuthorizationException(null);
            }
        }
    }

    @Override
    public void checkRole(String role) throws SecurityAuthorizationException {
        if (!hasRole(role)) {
            throw new SecurityAuthorizationException(null);
        }
    }

    @Override
    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && getAuthorization(auth).getRoles().contains(role);
    }

    @Override
    public User getPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }

    @Override
    public void clearRunAs() {
        // In Spring Security, clearing the run-as context is equivalent to
        // clearing the security context for the current thread.
        SecurityContextHolder.clearContext();
    }

    @Override
    public void bindSecurityManager() {
        // No-op: Spring Security uses SecurityContextHolder with a thread-local
        // strategy by default. No global security manager binding is needed
        // unlike Shiro's SecurityUtils.setSecurityManager().
    }

    private boolean isPermissionAllowed(String permission, Authentication auth) {
        AuthorizationCache authorization = getAuthorization(auth);
        if (authorization.getStringPermissions().stream().anyMatch(granted -> permissionMatches(granted, permission))) {
            return true;
        }
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(granted -> permissionMatches(granted, permission));
    }

    private AuthorizationCache getAuthorization(Authentication auth) {
        AuthorizationCache authorizationCache = permissionDataCache.getAuthorizationCache();
        if (authorizationCache != null) {
            return authorizationCache;
        }
        User user = auth.getPrincipal() instanceof User principal ? principal : null;
        if (user == null) {
            return new AuthorizationCache();
        }
        authorizationCache = authorizationAssembler.assemble(permissionDataCache.getCurrentOrg(), user.getId());
        permissionDataCache.setAuthorizationCache(authorizationCache);
        return authorizationCache;
    }

    private boolean permissionMatches(String granted, String required) {
        if (granted.equals(required)) {
            return true;
        }
        String[] grantedParts = granted.split(":");
        String[] requiredParts = required.split(":");
        if (grantedParts.length > requiredParts.length) {
            return false;
        }
        for (int i = 0; i < grantedParts.length; i += 1) {
            if (!"*".equals(grantedParts[i]) && !grantedParts[i].equals(requiredParts[i])) {
                return false;
            }
        }
        return true;
    }
}
