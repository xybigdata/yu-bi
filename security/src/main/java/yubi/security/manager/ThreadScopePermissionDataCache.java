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

package yubi.security.manager;

import yubi.security.base.Permission;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
public class ThreadScopePermissionDataCache {

    private static ThreadLocal<PermissionData> permissionData;

    @PostConstruct
    public void initData() {
        permissionData = new InheritableThreadLocal<>();
        permissionData.set(new PermissionData());
    }

    public String getCurrentOrg() {
        return permissionData.get().currentOrg;
    }

    public void setCurrentOrg(String currentOrg) {
        permissionData.get().currentOrg = currentOrg;
    }

    public Boolean getCachedPermission(Permission permission) {
        return permissionData.get().permissionCache.get(permission);
    }

    public void setPermissionCache(Permission permission, Boolean permitted) {
        permissionData.get().permissionCache.put(permission, permitted);
    }

    public AuthorizationCache getAuthorizationCache() {
        return permissionData.get().authorizationCache;
    }

    public void setAuthorizationCache(AuthorizationCache authorizationCache) {
        permissionData.get().authorizationCache = authorizationCache;
    }

    public AuthenticationCache getAuthenticationCache() {
        return permissionData.get().authenticationCache;
    }

    public void setAuthenticationCache(AuthenticationCache authenticationCache) {
        permissionData.get().authenticationCache = authenticationCache;
    }

    public void clear() {
        permissionData.get().clear();
    }

    private static final class PermissionData {

        private String currentOrg;

        private final Map<Permission, Boolean> permissionCache = new HashMap<>();

        private AuthorizationCache authorizationCache;

        private AuthenticationCache authenticationCache;

        private void clear() {
            currentOrg = null;
            permissionCache.clear();
            authorizationCache = null;
            authenticationCache = null;
        }
    }

}
