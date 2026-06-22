package datart.security.manager.shiro;

import datart.core.base.exception.BaseException;
import datart.core.common.MessageResolver;
import datart.core.mappers.ext.UserMapperExt;
import datart.security.base.Permission;
import datart.security.manager.PermissionDataCache;
import datart.security.manager.SecurityAuthorizationException;
import datart.security.manager.SecuritySubjectFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShiroSecurityManagerTest {

    private PermissionDataCache permissionDataCache;

    private SecuritySubjectFacade securitySubjectFacade;

    private ShiroSecurityManager securityManager;

    @BeforeEach
    void setUp() {
        permissionDataCache = mock(PermissionDataCache.class);
        securitySubjectFacade = mock(SecuritySubjectFacade.class);
        securityManager = new ShiroSecurityManager(
                mock(MessageResolver.class),
                mock(UserMapperExt.class),
                permissionDataCache,
                securitySubjectFacade);
    }

    @Test
    void shouldContinueCheckingRemainingPermissionsWhenAllowedCacheHit() {
        Permission cachedAllowed = permission("org", "role", "view", "view-1", 1);
        Permission unchecked = permission("org", "role", "view", "view-2", 1);

        when(permissionDataCache.getCachedPermission(cachedAllowed)).thenReturn(true);
        when(permissionDataCache.getCachedPermission(unchecked)).thenReturn(null);

        securityManager.requireAllPermissions(cachedAllowed, unchecked);

        verify(securitySubjectFacade).checkPermissions(any(String[].class));
        verify(permissionDataCache).setPermissionCache(unchecked, true);
    }

    @Test
    void shouldRejectImmediatelyWhenDeniedCacheHit() {
        Permission cachedDenied = permission("org", "role", "view", "view-1", 1);

        when(permissionDataCache.getCachedPermission(cachedDenied)).thenReturn(false);

        assertThrows(BaseException.class,
                () -> securityManager.requireAllPermissions(cachedDenied));

        verify(securitySubjectFacade, never()).checkPermissions(any(String[].class));
    }

    @Test
    void shouldCacheDeniedPermissionWhenSubjectCheckFails() {
        Permission unchecked = permission("org", "role", "view", "view-1", 1);

        when(permissionDataCache.getCachedPermission(unchecked)).thenReturn(null);
        org.mockito.Mockito.doThrow(new SecurityAuthorizationException(null))
                .when(securitySubjectFacade)
                .checkPermissions(any(String[].class));

        assertThrows(BaseException.class,
                () -> securityManager.requireAllPermissions(unchecked));

        verify(permissionDataCache).setPermissionCache(unchecked, false);
    }

    private static Permission permission(String orgId,
                                         String roleId,
                                         String resourceType,
                                         String resourceId,
                                         int permission) {
        return Permission.builder()
                .orgId(orgId)
                .roleId(roleId)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .permission(permission)
                .build();
    }
}
