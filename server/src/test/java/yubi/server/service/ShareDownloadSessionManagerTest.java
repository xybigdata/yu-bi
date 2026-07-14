package yubi.server.service;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import yubi.core.base.consts.ShareAuthenticationMode;
import yubi.core.base.exception.NotAllowedException;
import yubi.core.common.Application;
import yubi.core.entity.User;
import yubi.security.manager.YuBiSecurityManager;
import yubi.server.controller.ShareDownloadController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ShareDownloadSessionManagerTest {

    private static final String FINGERPRINT = "b".repeat(64);
    private static final String LEGACY_PUBLIC_SECRET = "d@a$t%a^r&a*t";
    private static final String STRONG_SECRET = encodedKey("0123456789abcdef0123456789abcdef");
    private static final String ROTATED_SECRET = encodedKey("abcdef0123456789abcdef0123456789");

    private YuBiSecurityManager securityManager;
    private ShareDownloadSessionManager manager;

    @BeforeEach
    void setUp() {
        ApplicationContext context = mock(ApplicationContext.class);
        Environment environment = mock(Environment.class);
        when(context.getEnvironment()).thenReturn(environment);
        when(environment.getProperty("yubi.server.path-prefix")).thenReturn("/api/v1");
        new Application().setApplicationContext(context);

        securityManager = mock(YuBiSecurityManager.class);
        manager = new ShareDownloadSessionManager(securityManager, STRONG_SECRET);
    }

    @Test
    void shouldIssueHighEntropyHttpOnlyPathScopedCookieAndPersistOnlyDigest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        ShareDownloadSession session = manager.issue(
                "share-1",
                ShareAuthenticationMode.NONE,
                FINGERPRINT,
                null,
                request,
                response
        );

        String setCookie = response.getHeader("Set-Cookie");
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("Secure"));
        assertTrue(setCookie.contains("SameSite=Lax"));
        assertTrue(setCookie.contains("Path=/api/v1/shares/share-1"));
        assertEquals(0, response.getContentAsByteArray().length);

        String rawToken = cookieValue(setCookie);
        String[] tokenParts = rawToken.split("\\.");
        assertEquals("v2", tokenParts[0]);
        assertEquals(FINGERPRINT, tokenParts[2]);
        assertEquals(32, Base64.getUrlDecoder().decode(tokenParts[3]).length);
        assertEquals(64, session.sessionDigest().length());
        assertNotEquals(rawToken, session.sessionDigest());
        assertFalse(session.sessionDigest().contains(rawToken));

        MockHttpServletRequest followUp = requestWithCookie(manager, "share-1", rawToken);
        ShareDownloadSession restored = manager.require("share-1", followUp);
        assertEquals(session, restored);

        ShareDownloadSessionManager sameKeyInstance = new ShareDownloadSessionManager(
                securityManager,
                STRONG_SECRET
        );
        assertEquals(session, sameKeyInstance.require("share-1", followUp));
    }

    @Test
    void shouldGenerateUnpredictableValuesAndRejectForgedOrCrossShareCookies() {
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        manager.issue(
                "share-1",
                ShareAuthenticationMode.NONE,
                FINGERPRINT,
                null,
                new MockHttpServletRequest(),
                firstResponse
        );
        manager.issue(
                "share-1",
                ShareAuthenticationMode.NONE,
                FINGERPRINT,
                null,
                new MockHttpServletRequest(),
                secondResponse
        );

        String first = cookieValue(firstResponse.getHeader("Set-Cookie"));
        String second = cookieValue(secondResponse.getHeader("Set-Cookie"));
        assertNotEquals(first, second);
        assertFalse(firstResponse.getHeader("Set-Cookie").contains("; Secure"));

        MockHttpServletRequest forged = requestWithCookie(manager, "share-1", first + "forged");
        assertThrows(NotAllowedException.class, () -> manager.require("share-1", forged));

        MockHttpServletRequest crossShare = requestWithCookie(manager, "share-2", first);
        assertThrows(NotAllowedException.class, () -> manager.require("share-2", crossShare));
    }

    @Test
    void shouldBindLoginCookieToCurrentAuthenticatedUser() {
        User owner = user("user-1");
        when(securityManager.getCurrentUser()).thenReturn(owner);
        MockHttpServletResponse response = new MockHttpServletResponse();
        ShareDownloadSession issued = manager.issue(
                "share-1",
                ShareAuthenticationMode.LOGIN,
                FINGERPRINT,
                "user-1",
                new MockHttpServletRequest(),
                response
        );
        String rawToken = cookieValue(response.getHeader("Set-Cookie"));

        MockHttpServletRequest request = requestWithCookie(manager, "share-1", rawToken);
        assertEquals("user-1", manager.require("share-1", request).authenticatedSubjectId());

        when(securityManager.getCurrentUser()).thenReturn(user("user-2"));
        assertThrows(NotAllowedException.class, () -> manager.require("share-1", request));
        assertEquals(64, issued.sessionDigest().length());
    }

    @Test
    void shouldFailFastForMissingWeakOrLegacyPublicSigningKeys() {
        assertThrows(
                IllegalStateException.class,
                () -> new ShareDownloadSessionManager(securityManager, "")
        );
        assertThrows(
                IllegalStateException.class,
                () -> new ShareDownloadSessionManager(
                        securityManager,
                        Base64.getEncoder().encodeToString(new byte[31])
                )
        );
        assertThrows(
                IllegalStateException.class,
                () -> new ShareDownloadSessionManager(securityManager, LEGACY_PUBLIC_SECRET)
        );
        assertThrows(
                IllegalStateException.class,
                () -> new ShareDownloadSessionManager(
                        securityManager,
                        Base64.getEncoder().encodeToString(
                                LEGACY_PUBLIC_SECRET.getBytes(StandardCharsets.UTF_8)
                        )
                )
        );
    }

    @Test
    void shouldRejectCookiesAfterKeyRotationAndLegacyPublicKeyForgery() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        manager.issue(
                "share-1",
                ShareAuthenticationMode.NONE,
                FINGERPRINT,
                null,
                new MockHttpServletRequest(),
                response
        );
        String oldCookie = cookieValue(response.getHeader("Set-Cookie"));
        ShareDownloadSessionManager rotated = new ShareDownloadSessionManager(
                securityManager,
                ROTATED_SECRET
        );
        assertThrows(
                NotAllowedException.class,
                () -> rotated.require(
                        "share-1",
                        requestWithCookie(rotated, "share-1", oldCookie)
                )
        );

        String randomValue = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);
        String forged = String.join(
                ".",
                "v2",
                ShareAuthenticationMode.NONE.name(),
                FINGERPRINT,
                randomValue,
                legacySignature("share-1", randomValue)
        );
        assertThrows(
                NotAllowedException.class,
                () -> manager.require(
                        "share-1",
                        requestWithCookie(manager, "share-1", forged)
                )
        );

        ShareService shareService = mock(ShareService.class);
        ShareDownloadController controller = new ShareDownloadController(shareService, manager);
        assertThrows(
                NotAllowedException.class,
                () -> controller.downloadList(
                        "share-1",
                        requestWithCookie(manager, "share-1", forged)
                )
        );
        verifyNoInteractions(shareService);
    }

    @Test
    void shouldBeConstructibleBySpringContainerWithStrongConfiguredKey() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "share-download-session-test",
                    Map.of("yubi.security.share-download-session.secret", STRONG_SECRET)
            ));
            context.registerBean(YuBiSecurityManager.class, () -> securityManager);
            context.register(ShareDownloadSessionManager.class);
            context.refresh();

            assertNotNull(context.getBean(ShareDownloadSessionManager.class));
        }
    }

    private String legacySignature(String shareId, String randomValue) throws Exception {
        String payload = String.join(
                "\u0000",
                "share-download-session-v2",
                "v2",
                shareId,
                ShareAuthenticationMode.NONE.name(),
                FINGERPRINT,
                "",
                randomValue
        );
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                LEGACY_PUBLIC_SECRET.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        ));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))
        );
    }

    private MockHttpServletRequest requestWithCookie(ShareDownloadSessionManager cookieManager,
                                                     String shareId,
                                                     String value) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(cookieManager.cookieName(shareId), value));
        return request;
    }

    private User user(String id) {
        User user = new User();
        user.setId(id);
        user.setUsername(id);
        return user;
    }

    private String cookieValue(String setCookie) {
        return setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
    }

    private static String encodedKey(String raw) {
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
