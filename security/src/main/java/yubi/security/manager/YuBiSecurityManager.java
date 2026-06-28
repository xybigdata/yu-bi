
package yubi.security.manager;

import yubi.core.entity.*;
import yubi.security.base.Permission;
import yubi.security.exception.AuthException;
import yubi.security.exception.PermissionDeniedException;
import yubi.security.base.PasswordToken;

public interface YuBiSecurityManager {

    void login(PasswordToken token) throws AuthException;

    boolean validateUser(String username,String password) throws AuthException;

    String login(String jwtToken) throws AuthException;

    void logoutCurrent();

    boolean isAuthenticated();

    void requireAllPermissions(Permission... permission) throws PermissionDeniedException;

    void requireAnyPermission(Permission... permissions) throws PermissionDeniedException;

    void requireOrgOwner(String orgId) throws PermissionDeniedException;

    boolean isOrgOwner(String orgId);

    boolean hasPermission(Permission... permission);

    User getCurrentUser();

    void runAs(String userNameOrEmail);

    void releaseRunAs();

}