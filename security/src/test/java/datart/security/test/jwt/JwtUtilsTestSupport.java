package datart.security.test.jwt;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class JwtUtilsTestSupport {

    private JwtUtilsTestSupport() {
    }

    static SecretKey legacyHmacKey(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    static String base64Url(String value) {
        return base64Url(value.getBytes(StandardCharsets.UTF_8));
    }

    static String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

}
