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
import datart.security.manager.SecurityAuthorizationException;
import datart.security.manager.SecuritySubjectFacade;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.BearerToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class ShiroSubjectFacade implements SecuritySubjectFacade {

    private final SecurityManager securityManager;

    public ShiroSubjectFacade(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @Override
    public void loginWithPassword(String subject, String password) {
        currentSubject().login(new UsernamePasswordToken(subject, password));
    }

    @Override
    public void loginWithBearer(String tokenString) {
        currentSubject().login(new BearerToken(tokenString));
    }

    @Override
    public void logoutCurrent() {
        Subject subject = currentSubject();
        if (subject != null) {
            subject.logout();
        }
    }

    @Override
    public boolean isAuthenticated() {
        return currentSubject().isAuthenticated();
    }

    @Override
    public void checkPermissions(String... permissions) throws SecurityAuthorizationException {
        try {
            currentSubject().checkPermissions(permissions);
        } catch (AuthorizationException e) {
            throw new SecurityAuthorizationException(e);
        }
    }

    @Override
    public void checkRole(String role) throws SecurityAuthorizationException {
        try {
            currentSubject().checkRole(role);
        } catch (AuthorizationException e) {
            throw new SecurityAuthorizationException(e);
        }
    }

    @Override
    public boolean hasRole(String role) {
        return currentSubject().hasRole(role);
    }

    @Override
    public User getPrincipal() {
        return (User) currentSubject().getPrincipal();
    }

    @Override
    public void clearRunAs() {
        ThreadContext.unbindSubject();
    }

    @Override
    @PostConstruct
    public void bindSecurityManager() {
        SecurityUtils.setSecurityManager(securityManager);
    }

    private Subject currentSubject() {
        return SecurityUtils.getSubject();
    }

}
