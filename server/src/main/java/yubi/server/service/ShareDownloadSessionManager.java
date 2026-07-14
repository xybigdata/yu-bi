package yubi.server.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import yubi.core.base.consts.ShareAuthenticationMode;
import yubi.core.base.exception.NotAllowedException;
import yubi.core.entity.Share;
import yubi.core.entity.User;
import yubi.security.manager.YuBiSecurityManager;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

import static yubi.core.common.Application.getApiPrefix;

@Component
public class ShareDownloadSessionManager {

    private static final String VERSION = "v2";
    private static final String COOKIE_PREFIX = "YUBI_SHARE_DL_";
    private static final String ACCESS_DENIED = "分享下载会话无效";
    private static final String KEY_CONFIGURATION_ERROR = "分享下载会话密钥未安全配置";
    private static final String LEGACY_PUBLIC_SECRET = "d@a$t%a^r&a*t";
    private static final int RANDOM_BYTES = 32;
    private static final int MINIMUM_KEY_BYTES = 32;
    private static final String FINGERPRINT_PATTERN = "[0-9a-f]{64}";

    private final YuBiSecurityManager securityManager;
    private final SecureRandom secureRandom;
    private final byte[] signingKey;

    public ShareDownloadSessionManager(
            YuBiSecurityManager securityManager,
            @Value("${yubi.security.share-download-session.secret:}") String encodedSigningKey
    ) {
        this.securityManager = securityManager;
        this.secureRandom = new SecureRandom();
        this.signingKey = decodeSigningKey(encodedSigningKey);
    }

    public ShareDownloadSession issue(String shareId,
                                      ShareAuthenticationMode authenticationMode,
                                      String securityFingerprint,
                                      String authenticatedSubjectId,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        requireSafeShareId(shareId);
        requireSafeFingerprint(securityFingerprint);
        String subjectId = normalizeSubject(authenticationMode, authenticatedSubjectId);
        String token = findCookie(request, cookieName(shareId));
        if (!isValid(token, shareId, authenticationMode, securityFingerprint, subjectId)) {
            byte[] random = new byte[RANDOM_BYTES];
            secureRandom.nextBytes(random);
            String randomValue = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
            String signature = signature(
                    shareId,
                    authenticationMode,
                    securityFingerprint,
                    subjectId,
                    randomValue
            );
            token = String.join(
                    ".",
                    VERSION,
                    authenticationMode.name(),
                    securityFingerprint,
                    randomValue,
                    signature
            );
        }

        ResponseCookie cookie = ResponseCookie.from(cookieName(shareId), token)
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path(getApiPrefix() + "/shares/" + shareId)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return session(shareId, authenticationMode, securityFingerprint, subjectId, token);
    }

    public ShareDownloadSession require(String shareId, HttpServletRequest request) {
        requireSafeShareId(shareId);
        String token = findCookie(request, cookieName(shareId));
        String[] parts = splitToken(token);
        ShareAuthenticationMode authenticationMode = parseMode(parts[1]);
        String securityFingerprint = parts[2];
        requireSafeFingerprint(securityFingerprint);
        String subjectId = currentSubject(authenticationMode);
        if (!isValid(token, shareId, authenticationMode, securityFingerprint, subjectId)) {
            throw new NotAllowedException(ACCESS_DENIED);
        }
        return session(shareId, authenticationMode, securityFingerprint, subjectId, token);
    }

    public String securityFingerprint(Share share) {
        if (share == null) {
            throw new NotAllowedException(ACCESS_DENIED);
        }
        requireSafeShareId(share.getId());
        ShareAuthenticationMode authenticationMode = parseMode(share.getAuthenticationMode());
        String authenticationCredential = "";
        if (ShareAuthenticationMode.CODE.equals(authenticationMode)) {
            if (StringUtils.isBlank(share.getAuthenticationCode())) {
                throw new NotAllowedException(ACCESS_DENIED);
            }
            authenticationCredential = share.getAuthenticationCode();
        }
        String payload = String.join(
                "\u0000",
                "share-security-v1",
                safeValue(share.getId()),
                safeValue(share.getOrgId()),
                safeValue(share.getVizType()),
                safeValue(share.getVizId()),
                authenticationMode.name(),
                authenticationCredential,
                safeValue(share.getRowPermissionBy()),
                safeValue(share.getRoles()),
                share.getExpiryDate() == null ? "" : Long.toString(share.getExpiryDate().getTime()),
                safeValue(share.getCreateBy()),
                share.getCreateTime() == null ? "" : Long.toString(share.getCreateTime().getTime())
        );
        return HexFormat.of().formatHex(hmac(payload));
    }

    public String cookieName(String shareId) {
        requireSafeShareId(shareId);
        return COOKIE_PREFIX + digest(shareId).substring(0, 20);
    }

    private ShareDownloadSession session(String shareId,
                                         ShareAuthenticationMode authenticationMode,
                                         String securityFingerprint,
                                         String subjectId,
                                         String token) {
        return new ShareDownloadSession(
                shareId,
                digest(token),
                securityFingerprint,
                authenticationMode,
                StringUtils.defaultIfBlank(subjectId, null)
        );
    }

    private boolean isValid(String token,
                            String shareId,
                            ShareAuthenticationMode authenticationMode,
                            String securityFingerprint,
                            String subjectId) {
        try {
            String[] parts = splitToken(token);
            if (!VERSION.equals(parts[0])
                    || !authenticationMode.name().equals(parts[1])
                    || !securityFingerprint.equals(parts[2])) {
                return false;
            }
            byte[] random = Base64.getUrlDecoder().decode(parts[3]);
            if (random.length != RANDOM_BYTES) {
                return false;
            }
            byte[] actual = Base64.getUrlDecoder().decode(parts[4]);
            byte[] expected = Base64.getUrlDecoder().decode(
                    signature(
                            shareId,
                            authenticationMode,
                            securityFingerprint,
                            subjectId,
                            parts[3]
                    )
            );
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private String signature(String shareId,
                             ShareAuthenticationMode authenticationMode,
                             String securityFingerprint,
                             String subjectId,
                             String randomValue) {
        String payload = String.join(
                "\u0000",
                "share-download-session-v2",
                VERSION,
                shareId,
                authenticationMode.name(),
                securityFingerprint,
                subjectId,
                randomValue
        );
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(payload));
    }

    private String normalizeSubject(ShareAuthenticationMode authenticationMode, String authenticatedSubjectId) {
        if (ShareAuthenticationMode.LOGIN.equals(authenticationMode)) {
            if (StringUtils.isBlank(authenticatedSubjectId)) {
                throw new NotAllowedException(ACCESS_DENIED);
            }
            return authenticatedSubjectId;
        }
        return "";
    }

    private String currentSubject(ShareAuthenticationMode authenticationMode) {
        if (!ShareAuthenticationMode.LOGIN.equals(authenticationMode)) {
            return "";
        }
        User currentUser = securityManager.getCurrentUser();
        if (currentUser == null || StringUtils.isBlank(currentUser.getId())) {
            throw new NotAllowedException(ACCESS_DENIED);
        }
        return currentUser.getId();
    }

    private ShareAuthenticationMode parseMode(String value) {
        try {
            return ShareAuthenticationMode.valueOf(value);
        } catch (RuntimeException exception) {
            throw new NotAllowedException(ACCESS_DENIED);
        }
    }

    private String[] splitToken(String token) {
        if (StringUtils.isBlank(token)) {
            throw new NotAllowedException(ACCESS_DENIED);
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 5 || Arrays.stream(parts).anyMatch(StringUtils::isBlank)) {
            throw new NotAllowedException(ACCESS_DENIED);
        }
        return parts;
    }

    private String findCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private String digest(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(messageDigest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("无法建立分享下载会话");
        }
    }

    private byte[] hmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("无法建立分享下载会话");
        }
    }

    private byte[] decodeSigningKey(String encodedSigningKey) {
        if (StringUtils.isBlank(encodedSigningKey)
                || LEGACY_PUBLIC_SECRET.equals(encodedSigningKey)) {
            throw new IllegalStateException(KEY_CONFIGURATION_ERROR);
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(encodedSigningKey.trim());
            if (decoded.length < MINIMUM_KEY_BYTES) {
                throw new IllegalStateException(KEY_CONFIGURATION_ERROR);
            }
            return decoded;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(KEY_CONFIGURATION_ERROR);
        }
    }

    private void requireSafeFingerprint(String securityFingerprint) {
        if (securityFingerprint == null || !securityFingerprint.matches(FINGERPRINT_PATTERN)) {
            throw new NotAllowedException(ACCESS_DENIED);
        }
    }

    private String safeValue(String value) {
        return StringUtils.defaultString(value);
    }

    private void requireSafeShareId(String shareId) {
        if (shareId == null || !shareId.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new NotAllowedException(ACCESS_DENIED);
        }
    }
}
