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
import yubi.security.manager.SecurityAuthorizationException;
import yubi.security.manager.SecuritySubjectFacade;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Spring Security implementation of {@link SecuritySubjectFacade}.
 * <p>
 * Replaces the Shiro-based {@code ShiroSubjectFacade} by delegating all
 * authentication and authorization checks to Spring Security's
 * {@link SecurityContextHolder}.
 */
@Component
public class SpringSecuritySubjectFacade implements SecuritySubjectFacade {

    @Override
    public void loginWithPassword(String subject, String password) {
        // Create an unauthenticated token and set it in the SecurityContext.
        // Actual credential validation is delegated to the AuthenticationProvider
        // via the filter chain. This programmatic login is used for service-layer
        // login flows (e.g., OAuth2 callback, internal user setup).
        Authentication auth = new UsernamePasswordAuthenticationToken(subject, password);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Override
    public void loginWithBearer(String tokenString) {
        // JWT-based auth: the JwtAuthenticationFilter validates the token and
        // sets the fully authenticated Authentication in the SecurityContext.
        // This method is a hook for programmatic bearer login in service code;
        // the actual token is expected to have been validated upstream.
        // When called directly, we create a pre-authenticated token.
        Authentication auth = new UsernamePasswordAuthenticationToken(tokenString, null);
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
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        for (String permission : permissions) {
            if (!authorities.contains(new SimpleGrantedAuthority(permission))) {
                throw new SecurityAuthorizationException(null);
            }
        }
    }

    @Override
    public void checkRole(String role) throws SecurityAuthorizationException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.getAuthorities().contains(new SimpleGrantedAuthority(role))) {
            throw new SecurityAuthorizationException(null);
        }
    }

    @Override
    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority(role));
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
}
