package yubi.security.manager.springsecurity;

import yubi.core.base.exception.BaseException;
import yubi.core.common.MessageResolver;
import yubi.core.entity.User;
import yubi.core.mappers.ext.UserMapperExt;
import yubi.security.base.PasswordToken;
import yubi.security.base.Permission;
import yubi.security.manager.PermissionDataCache;
import yubi.security.manager.SecurityAuthorizationException;
import yubi.security.manager.SecuritySubjectFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCrypt;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpringSecurityManagerTest {

    private PermissionDataCache permissionDataCache;

    private SecuritySubjectFacade securitySubjectFacade;

    private UserMapperExt userMapper;

    private SpringSecurityManager securityManager;

    @BeforeEach
    void setUp() {
        permissionDataCache = mock(PermissionDataCache.class);
        securitySubjectFacade = mock(SecuritySubjectFacade.class);
        userMapper = mock(UserMapperExt.class);
        securityManager = new SpringSecurityManager(
                mock(MessageResolver.class),
                userMapper,
                permissionDataCache,
                securitySubjectFacade);
    }

    @Test
    void shouldRejectLoginWhenPasswordDoesNotMatch() {
        User user = activeUser("demo", BCrypt.hashpw("secret", BCrypt.gensalt()));
        when(userMapper.selectByNameOrEmail("demo")).thenReturn(user);

        assertThrows(BaseException.class,
                () -> securityManager.login(new PasswordToken("demo", "wrong", System.currentTimeMillis())));

        verify(securitySubjectFacade, never()).loginWithPassword(any(), any());
    }

    @Test
    void shouldAllowLoginWhenPasswordMatches() {
        User user = activeUser("demo", BCrypt.hashpw("secret", BCrypt.gensalt()));
        when(userMapper.selectByNameOrEmail("demo")).thenReturn(user);

        assertDoesNotThrow(
                () -> securityManager.login(new PasswordToken("demo", "secret", System.currentTimeMillis())));

        verify(securitySubjectFacade).loginWithPassword("demo", "secret");
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

    private static User activeUser(String username, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setActive(true);
        return user;
    }
}
