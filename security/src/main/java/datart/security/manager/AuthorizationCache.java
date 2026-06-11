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

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class AuthorizationCache {

    private final Set<String> roles = new HashSet<>();

    private final Set<String> stringPermissions = new HashSet<>();

    public void addRole(String role) {
        roles.add(role);
    }

    public void addPermission(String permission) {
        stringPermissions.add(permission);
    }

    public void addPermissions(Set<String> permissions) {
        stringPermissions.addAll(permissions);
    }

}
