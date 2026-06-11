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

import datart.core.base.consts.Const;

import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

public final class PermissionStringCodec {

    private PermissionStringCodec() {
    }

    public static String toRoleString(String roleType, String orgId) {
        return roleType + "." + orgId;
    }

    public static Set<String> toPermissionStrings(String orgId, String roleId, String resourceType, String resourceId, int permission) {
        Set<String> permissionStrings = new HashSet<>();
        Set<String> permissions = expandPermissions(permission);
        for (String permissionName : permissions) {
            StringJoiner stringJoiner = new StringJoiner(":");
            stringJoiner.add(orgId)
                    .add(roleId != null ? roleId : "*")
                    .add(resourceType)
                    .add(permissionName)
                    .add(resourceId);
            permissionStrings.add(stringJoiner.toString());
        }
        return permissionStrings;
    }

    public static String toPermissionString(String orgId, String resourceType, String resourceId, String permission) {
        StringJoiner stringJoiner = new StringJoiner(":");
        stringJoiner.add(orgId)
                .add(resourceType)
                .add(permission)
                .add(resourceId);
        return stringJoiner.toString();
    }

    public static Set<String> expandPermissions(int permission) {
        Set<String> permissions = new HashSet<>();
        if (permission == Const.DISABLE) {
            permissions.add("DISABLE");
            return permissions;
        }
        if ((Const.ENABLE & permission) == Const.ENABLE) {
            permissions.add("ENABLE");
        }
        if ((Const.READ & permission) == Const.READ) {
            permissions.add("READ");
        }
        if ((Const.MANAGE & permission) == Const.MANAGE) {
            permissions.add("MANAGE");
        }
        if ((Const.GRANT & permission) == Const.GRANT) {
            permissions.add("GRANT");
        }
        if ((Const.DOWNLOAD & permission) == Const.DOWNLOAD) {
            permissions.add("DOWNLOAD");
        }
        if ((Const.SHARE & permission) == Const.SHARE) {
            permissions.add("SHARE");
        }
        return permissions;
    }

}
