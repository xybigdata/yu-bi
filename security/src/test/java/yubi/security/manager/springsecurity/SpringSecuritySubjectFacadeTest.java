package yubi.security.manager.springsecurity;

import yubi.core.entity.User;
import yubi.security.manager.AuthenticationAssembler;
import yubi.security.manager.AuthorizationAssembler;
import yubi.security.manager.AuthorizationCache;
import yubi.security.manager.PermissionDataCache;
import yubi.security.manager.PermissionStringCodec;
import yubi.security.manager.SecurityAuthorizationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpringSecuritySubjectFacadeTest {

    private PermissionDataCache permissionDataCache;

    private AuthorizationAssembler authorizationAssembler;

    private SpringSecuritySubjectFacade subjectFacade;

    @BeforeEach
    void setUp() {
        permissionDataCache = mock(PermissionDataCache.class);
        authorizationAssembler = mock(AuthorizationAssembler.class);
        subjectFacade = new SpringSecuritySubjectFacade(
                permissionDataCache,
                mock(AuthenticationAssembler.class),
                authorizationAssembler);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldExposeUserPrincipalFromSecurityContext() {
        User user = user("user-1", "demo");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));

        assertEquals(user, subjectFacade.getPrincipal());
    }

    @Test
    void shouldAllowSpecificPermissionByOwnerWildcardPermission() {
        User user = user("user-1", "demo");
        AuthorizationCache authorizationCache = new AuthorizationCache();
        authorizationCache.addPermission(PermissionStringCodec.toPermissionString("org-1", "*", "*", "*"));
        when(permissionDataCache.getCurrentOrg()).thenReturn("org-1");
        when(permissionDataCache.getAuthorizationCache()).thenReturn(null);
        when(authorizationAssembler.assemble("org-1", "user-1")).thenReturn(authorizationCache);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));

        assertDoesNotThrow(() -> subjectFacade.checkPermissions("org-1:*:USER:READ:*"));

        verify(permissionDataCache).setAuthorizationCache(authorizationCache);
    }

    @Test
    void shouldRejectPermissionWhenNoAuthorityMatches() {
        User user = user("user-1", "demo");
        when(permissionDataCache.getCurrentOrg()).thenReturn("org-1");
        when(permissionDataCache.getAuthorizationCache()).thenReturn(null);
        when(authorizationAssembler.assemble("org-1", "user-1")).thenReturn(new AuthorizationCache());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));

        assertThrows(SecurityAuthorizationException.class,
                () -> subjectFacade.checkPermissions("org-1:*:USER:READ:*"));
    }

    private static User user(String id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }
}
