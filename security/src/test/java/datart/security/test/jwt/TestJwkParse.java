package datart.security.test.jwt;

import datart.core.base.exception.Exceptions;
import datart.security.util.JwkUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.Mac;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

@Slf4j
public class TestJwkParse {

    private final static String PEM_PATH = JwkSetCreator.PEM_PATH;

    private final static String JWK_PATH = JwkSetCreator.JWK_PATH;

    @Test
    public void testHMACJwk() {
        String tokenSecret = "d@a$t%a^r&a*t";
        String token = createLegacyHmacToken(tokenSecret, "demo");

        Claims claims = JwkUtils.parseJwt(token, JWK_PATH + "oct.json");
        assert "demo".equals(claims.getSubject());
    }

    @Test
    public void testRsaJwk() {
        String privateFile = PEM_PATH + "rsa_private_key.pem";
        String token = createTokenFromPem(Jwts.SIG.RS256, privateFile, "demo");

        Claims claims = JwkUtils.parseJwt(token, JWK_PATH + "rsa.json");
        assert "demo".equals(claims.getSubject());
    }

    @Test
    public void testEcJwk() {
        String privateFile = PEM_PATH + "ec_private_key.pem";
        String token = createTokenFromPem(Jwts.SIG.ES256, privateFile, "demo");

        Claims claims = JwkUtils.parseJwt(token, JWK_PATH + "ec.json");
        assert "demo".equals(claims.getSubject());
    }

    @Test
    public void testRsaPemJwt() {
        List<String> tokens = new ArrayList<>();
        String privateFile = PEM_PATH + "rsa_private_key.pem";
        tokens.add(createTokenFromPem(Jwts.SIG.RS256, privateFile, "demo"));
        String privateFile_pkcs1 = PEM_PATH + "rsa_private_key_pkcs1.pem";
        tokens.add(createTokenFromPem(Jwts.SIG.RS256, privateFile_pkcs1, "demo"));
        for (String token : tokens) {
            String keyFile = PEM_PATH + "rsa_public_key.pem";
            validTokenWithPem(keyFile, token, "demo");
            keyFile = PEM_PATH + "rsa_public_key_pkcs1.pem";
            validTokenWithPem(keyFile, token, "demo");
        }
    }

    @Test
    public void testEcPemJwt() {
        List<String> tokens = new ArrayList<>();
        String privateFile = PEM_PATH + "ec_private_key.pem";
        tokens.add(createTokenFromPem(Jwts.SIG.ES256, privateFile, "demo"));
        String privateFile_pkcs1 = PEM_PATH + "ec_private_key_pkcs1.pem";
        tokens.add(createTokenFromPem(Jwts.SIG.ES256, privateFile_pkcs1, "demo"));

        for (String token : tokens) {
            //String keyFile = PEM_PATH + "ec_public_key.pem";
            String keyFile = PEM_PATH + "ec_private_key.pem";
            validTokenWithPem(keyFile, token, "demo");
        }
    }

    private static String createTokenFromPem(io.jsonwebtoken.security.SignatureAlgorithm algorithm, String pemFile, String subject) {
        Key privateKey = JwkUtils.getPrivateKeyFromPem(pemFile);
        JwtBuilder jwtBuilder = Jwts.builder()
                .expiration(new Date(System.currentTimeMillis() + 10 * 60 * 1000))
                .signWith((PrivateKey) privateKey, algorithm);
        return jwtBuilder.claim(Claims.SUBJECT, subject).compact();
    }

    private static void validTokenWithPem(String pemFile, String token, String subject) {
        try {
            Key publicKey = JwkUtils.getPublicKeyFromPem(pemFile);
            if (!verifyPemToken(token, publicKey, subject)) {
                Exceptions.msg("jwt claim subject valid failed.");
            }
            return ;
        } catch (Exception e) {
            Exceptions.msg("jwt valid failed: "+e.getMessage());
            return ;
        }
    }

    private static boolean verifyPemToken(String token, Key publicKey, String subject) throws Exception {
        String[] parts = token.split("\\.");
        String headerJson = new String(io.jsonwebtoken.io.Decoders.BASE64URL.decode(parts[0]), StandardCharsets.UTF_8);
        String signingInput = parts[0] + "." + parts[1];
        Signature signature;
        byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[2]);
        if (headerJson.contains("\"alg\":\"RS256\"")) {
            signature = Signature.getInstance("SHA256withRSA");
        } else {
            signature = Signature.getInstance("SHA256withECDSA", "BC");
            signatureBytes = joseToDerSignature(signatureBytes);
        }
        signature.initVerify((PublicKey) publicKey);
        signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
        if (!signature.verify(signatureBytes)) {
            return false;
        }
        String payloadJson = new String(io.jsonwebtoken.io.Decoders.BASE64URL.decode(parts[1]), StandardCharsets.UTF_8);
        return payloadJson.contains("\"sub\":\"" + subject + "\"");
    }

    private static byte[] joseToDerSignature(byte[] joseSignature) {
        int rawLen = joseSignature.length / 2;
        byte[] r = new byte[rawLen];
        byte[] s = new byte[rawLen];
        System.arraycopy(joseSignature, 0, r, 0, rawLen);
        System.arraycopy(joseSignature, rawLen, s, 0, rawLen);
        byte[] derR = toDerInteger(r);
        byte[] derS = toDerInteger(s);
        byte[] der = new byte[2 + derR.length + derS.length];
        der[0] = 0x30;
        der[1] = (byte) (derR.length + derS.length);
        System.arraycopy(derR, 0, der, 2, derR.length);
        System.arraycopy(derS, 0, der, 2 + derR.length, derS.length);
        return der;
    }

    private static byte[] toDerInteger(byte[] value) {
        int offset = 0;
        while (offset < value.length - 1 && value[offset] == 0) {
            offset++;
        }
        int len = value.length - offset;
        boolean prependZero = (value[offset] & 0x80) != 0;
        byte[] der = new byte[2 + len + (prependZero ? 1 : 0)];
        der[0] = 0x02;
        der[1] = (byte) (len + (prependZero ? 1 : 0));
        int cursor = 2;
        if (prependZero) {
            der[cursor++] = 0;
        }
        System.arraycopy(value, offset, der, cursor, len);
        return der;
    }

    private static String createLegacyHmacToken(String secret, String subject) {
        try {
            String header = JwtUtilsTestSupport.base64Url("{\"alg\":\"HS256\"}");
            String payload = JwtUtilsTestSupport.base64Url(
                    "{\"sub\":\"" + subject + "\",\"exp\":" + ((System.currentTimeMillis() + 10 * 60 * 1000) / 1000) + "}"
            );
            String signingInput = header + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(JwtUtilsTestSupport.legacyHmacKey(secret));
            byte[] signature = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
            return signingInput + "." + JwtUtilsTestSupport.base64Url(signature);
        } catch (Exception e) {
            throw new IllegalStateException("failed to create legacy hmac token", e);
        }
    }


}
