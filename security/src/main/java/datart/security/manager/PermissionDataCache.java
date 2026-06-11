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

package datart.security.manager;

import datart.security.base.Permission;
import org.springframework.stereotype.Component;

@Component
public class PermissionDataCache {

    private final RequestScopePermissionDataCache requestScope;

    private final ThreadScopePermissionDataCache threadScope;

    public PermissionDataCache(RequestScopePermissionDataCache permissionDataCache,
                               ThreadScopePermissionDataCache threadScope) {
        this.requestScope = permissionDataCache;
        this.threadScope = threadScope;
    }


    public AuthorizationCache getAuthorizationCache() {
        try {
            return requestScope.getAuthorizationCache();
        } catch (Exception e) {
            return threadScope.getAuthorizationCache();
        }
    }

    public String getCurrentOrg() {
        try {
            return requestScope.getCurrentOrg();
        } catch (Exception e) {
            return threadScope.getCurrentOrg();
        }
    }

    public void setCurrentOrg(String currentOrg) {
        try {
            requestScope.setCurrentOrg(currentOrg);
        } catch (Exception e) {
            threadScope.setCurrentOrg(currentOrg);
        }
    }

    public Boolean getCachedPermission(Permission permission) {
        try {
            return requestScope.getCachedPermission(permission);
        } catch (Exception e) {
            return threadScope.getCachedPermission(permission);
        }
    }

    public void setPermissionCache(Permission permission, Boolean permitted) {
        try {
            requestScope.setPermissionCache(permission, permitted);
        } catch (Exception e) {
            threadScope.setPermissionCache(permission, permitted);
        }
    }


    public void setAuthorizationCache(AuthorizationCache authorizationCache) {
        try {
            requestScope.setAuthorizationCache(authorizationCache);
        } catch (Exception e) {
            threadScope.setAuthorizationCache(authorizationCache);
        }
    }

    public AuthenticationCache getAuthenticationCache() {
        try {
            return requestScope.getAuthenticationCache();
        } catch (Exception e) {
            return threadScope.getAuthenticationCache();
        }
    }

    public void setAuthenticationCache(AuthenticationCache authenticationCache) {
        try {
            requestScope.setAuthenticationCache(authenticationCache);
        } catch (Exception e) {
            threadScope.setAuthenticationCache(authenticationCache);
        }
    }

    public void clear() {
        threadScope.clear();
        try {
            requestScope.clear();
        } catch (Exception e) {
        }
    }

}
