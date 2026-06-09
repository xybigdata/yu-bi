package datart.security.util;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import datart.core.base.exception.Exceptions;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.util.CollectionUtils;

import javax.crypto.SecretKey;
import javax.crypto.Mac;
import java.io.File;
import java.io.FileReader;
import java.math.BigInteger;
import java.net.URL;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
public class JwkUtils {

    private static Provider provider = null;

    private static JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter();

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        provider = Security.getProvider("BC");
        keyConverter.setProvider(provider);
    }

    public static Claims parseJwt(String token, String jwkSetFilePath) {
        File file = new File(jwkSetFilePath);
        try {
            JWKSet jwkSet = JWKSet.load(file);
            return parseJwt(token, jwkSet.getKeys());
        } catch (Exception e) {
            throw new JwtException("Failed to load jwkSet from file: " + file.getPath(), e);
        }
    }

    private static Claims parseJwt(String token, List<JWK> jwkList) {
        JWSObject jwsObject = parseJwsObject(token);
        Claims claims = null;
        for (JWK jwk : jwkList) {
            try {
                if (!verify(jwsObject, jwk)) {
                    continue;
                }
                claims = parseClaims(jwsObject.getPayload());
                validateExpiration(claims);
                break;
            } catch (ExpiredJwtException expired) {
                throw expired;
            } catch (Exception e) {
                log.warn(e.getMessage());
            }
        }
        if (claims == null) {
            Exceptions.base("Jwt token parse failed");
        }
        return claims;
    }

    private static JWSObject parseJwsObject(String token) {
        try {
            return JWSObject.parse(token);
        } catch (ParseException e) {
            throw new JwtException("Jwt token parse failed", e);
        }
    }

    private static Claims parseClaims(Payload payload) {
        try {
            Map<String, Object> claimsMap = payload.toJSONObject();
            return Jwts.claims(claimsMap);
        } catch (Exception e) {
            throw new JwtException("Jwt claims parse failed", e);
        }
    }

    private static void validateExpiration(Claims claims) {
        Date expiration = claims.getExpiration();
        if (expiration != null && expiration.before(new Date())) {
            throw new ExpiredJwtException(null, claims, "Jwt token has expired");
        }
    }

    private static boolean verify(JWSObject jwsObject, JWK jwk) throws JOSEException {
        JWSAlgorithm algorithm = jwsObject.getHeader().getAlgorithm();
        if (JWSAlgorithm.HS256.equals(algorithm) || JWSAlgorithm.HS384.equals(algorithm) || JWSAlgorithm.HS512.equals(algorithm)) {
            if (!(jwk instanceof OctetSequenceKey octetSequenceKey)) {
                return false;
            }
            SecretKey secretKey = octetSequenceKey.toSecretKey();
            return verifyHmac(jwsObject, secretKey, toMacAlgorithm(algorithm));
        }
        if (JWSAlgorithm.RS256.equals(algorithm) || JWSAlgorithm.RS384.equals(algorithm) || JWSAlgorithm.RS512.equals(algorithm)) {
            if (!(jwk instanceof RSAKey rsaKey)) {
                return false;
            }
            RSAPublicKey publicKey = rsaKey.toRSAPublicKey();
            return jwsObject.verify(new RSASSAVerifier(publicKey));
        }
        if (JWSAlgorithm.ES256.equals(algorithm) || JWSAlgorithm.ES384.equals(algorithm) || JWSAlgorithm.ES512.equals(algorithm)) {
            if (!(jwk instanceof ECKey ecKey)) {
                return false;
            }
            return verifyEc(jwsObject, ecKey, toEcSignatureAlgorithm(algorithm));
        }
        return false;
    }

    private static boolean verifyHmac(JWSObject jwsObject, SecretKey secretKey, String macAlgorithm) {
        try {
            Mac mac = Mac.getInstance(macAlgorithm);
            mac.init(secretKey);
            byte[] actual = mac.doFinal(jwsObject.getSigningInput());
            byte[] expected = jwsObject.getSignature().decode();
            return MessageDigest.isEqual(actual, expected);
        } catch (Exception e) {
            throw new JwtException("Jwt signature verify failed", e);
        }
    }

    private static boolean verifyEc(JWSObject jwsObject, ECKey ecKey, String signatureAlgorithm) throws JOSEException {
        if (Curve.SECP256K1.equals(ecKey.getCurve())) {
            PublicKey publicKey = toEcPublicKey(ecKey);
            byte[] derSignature = joseToDerSignature(jwsObject.getSignature().decode());
            return verifySignature(signatureAlgorithm, publicKey, jwsObject.getSigningInput(), derSignature);
        }
        ECPublicKey publicKey = ecKey.toECPublicKey();
        byte[] derSignature = joseToDerSignature(jwsObject.getSignature().decode());
        return verifySignature(signatureAlgorithm, publicKey, jwsObject.getSigningInput(), derSignature);
    }

    private static boolean verifySignature(String algorithm, PublicKey publicKey, byte[] signingInput, byte[] signatureBytes) {
        try {
            Signature signature = Signature.getInstance(algorithm, provider);
            signature.initVerify(publicKey);
            signature.update(signingInput);
            return signature.verify(signatureBytes);
        } catch (Exception e) {
            throw new JwtException("Jwt signature verify failed", e);
        }
    }

    private static String toMacAlgorithm(JWSAlgorithm algorithm) {
        if (JWSAlgorithm.HS256.equals(algorithm)) {
            return "HmacSHA256";
        }
        if (JWSAlgorithm.HS384.equals(algorithm)) {
            return "HmacSHA384";
        }
        if (JWSAlgorithm.HS512.equals(algorithm)) {
            return "HmacSHA512";
        }
        throw new JwtException("Unsupported HMAC jwt algorithm: " + algorithm);
    }

    private static String toEcSignatureAlgorithm(JWSAlgorithm algorithm) {
        if (JWSAlgorithm.ES256.equals(algorithm)) {
            return "SHA256withECDSA";
        }
        if (JWSAlgorithm.ES384.equals(algorithm)) {
            return "SHA384withECDSA";
        }
        if (JWSAlgorithm.ES512.equals(algorithm)) {
            return "SHA512withECDSA";
        }
        throw new JwtException("Unsupported EC jwt algorithm: " + algorithm);
    }

    private static PublicKey toEcPublicKey(ECKey ecKey) throws JOSEException {
        if (!Curve.SECP256K1.equals(ecKey.getCurve())) {
            return ecKey.toPublicKey();
        }
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", provider);
            keyPairGenerator.initialize(new ECGenParameterSpec("secp256k1"), new SecureRandom());
            ECPublicKey template = (ECPublicKey) keyPairGenerator.generateKeyPair().getPublic();
            java.security.spec.ECParameterSpec params = template.getParams();
            java.security.spec.ECPoint point = new java.security.spec.ECPoint(
                    new BigInteger(1, ecKey.getX().decode()),
                    new BigInteger(1, ecKey.getY().decode())
            );
            return KeyFactory.getInstance("EC", provider).generatePublic(new java.security.spec.ECPublicKeySpec(point, params));
        } catch (Exception e) {
            throw new JwtException("Jwt ec public key parse failed", e);
        }
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

    public static List<Key> getJwKFromFile(String jwkSetFilePath) {
        List<Key> keys = new ArrayList<>();
        File file = new File(jwkSetFilePath);
        try {
            JWKSet jwkSet = JWKSet.load(file);
            List<JWK> jwkList = jwkSet.getKeys();
            keys = KeyConverter.toJavaKeys(jwkList);
        } catch (Exception e) {
            log.error("Failed to load jwkSet from file: " + file.getPath());
        }
        return keys;
    }

    public static Key getJwKFromFileByKid(String jwkSetFilePath, String kid) {
        Key key = null;
        File file = new File(jwkSetFilePath);
        try {
            JWKSet jwkSet = JWKSet.load(file);
            JWK jwk = jwkSet.getKeyByKeyId(kid);
            List<Key> keys = KeyConverter.toJavaKeys(Collections.singletonList(jwk));
            if (!CollectionUtils.isEmpty(keys)) {
                key = keys.get(0);
            } else {
                log.error("Cannot find jwk from file({}) by kid:({}).", file.getPath(), kid);
            }
        } catch (Exception e) {
            log.error("Failed to load jwkSet from file({}) by kid:({}).", file.getPath(), kid);
        }
        return key;
    }

    public static List<Key> getJwKFromUrl(String jwkSetUrl) {
        List<Key> keys = new ArrayList<>();
        try {
            RemoteJWKSet remoteJWKSet = new RemoteJWKSet(new URL(jwkSetUrl));
            JWKMatcher jwkMatcher = (new JWKMatcher.Builder()).keyUses(KeyUse.SIGNATURE, KeyUse.ENCRYPTION, null).keyTypes(KeyType.OCT, KeyType.RSA, KeyType.EC).build();
            JWKSelector jwsKeySelector = new JWKSelector(jwkMatcher);
            List list = remoteJWKSet.get(jwsKeySelector, null);
            keys = KeyConverter.toJavaKeys(list);
        } catch (Exception e) {
            log.error("Failed to load jwkSet from url: " + jwkSetUrl);
        }
        return keys;
    }

    public static Key getJwKFromUrlByKid(String jwkSetUrl, String kid) {
        Key key = null;
        try {
            RemoteJWKSet set = new RemoteJWKSet(new URL(jwkSetUrl));
            JWKSet cachedJWKSet = set.getCachedJWKSet();
            JWK jwk = cachedJWKSet.getKeyByKeyId(kid);
            List<Key> keys = KeyConverter.toJavaKeys(Collections.singletonList(jwk));
            if (!CollectionUtils.isEmpty(keys)) {
                key = keys.get(0);
            } else {
                log.error("Cannot find jwk from url({}) by kid:({}).", jwkSetUrl, kid);
            }
        } catch (Exception e) {
            log.error("Failed to load jwkSet from url({}) by kid:({}).", jwkSetUrl, kid);
        }
        return key;
    }

    public static Key getPublicKeyFromPem(String path) {
        Key key = null;
        try (PEMParser pemParser = new PEMParser(new FileReader(path))) {
            Object keyObj = pemParser.readObject();
            if (keyObj instanceof PEMKeyPair) {
                PEMKeyPair pemKeyPair = (PEMKeyPair) keyObj;
                KeyPair keyPair = keyConverter.getKeyPair(pemKeyPair);
                key = keyPair.getPublic();
            } else if (keyObj instanceof SubjectPublicKeyInfo) {
                SubjectPublicKeyInfo publicKeyInfo = ((SubjectPublicKeyInfo) keyObj);
                key = keyConverter.getPublicKey(publicKeyInfo);
            } else if (keyObj instanceof PrivateKeyInfo) {
                PrivateKeyInfo privateKeyInfo = (PrivateKeyInfo) keyObj;
                PrivateKey privateKey = keyConverter.getPrivateKey(privateKeyInfo);
                key = privateKey;
                if (privateKey instanceof ECPrivateKey ecPrivateKey) {
                    key = ecPrivateToPublic(ecPrivateKey);
                }
            }
        } catch (Exception e) {
            log.error("The pem file parsed failed: {}", e.getMessage());
        }
        return key;
    }

    public static Key getPrivateKeyFromPem(String path) {
        Key key = null;
        try (PEMParser pemParser = new PEMParser(new FileReader(path))) {
            Object keyObj = pemParser.readObject();
            if (keyObj instanceof PEMKeyPair) {
                PEMKeyPair pemKeyPair = (PEMKeyPair) keyObj;
                KeyPair keyPair = keyConverter.getKeyPair(pemKeyPair);
                key = keyPair.getPrivate();
            } else if (keyObj instanceof PrivateKeyInfo) {
                PrivateKeyInfo privateKeyInfo = (PrivateKeyInfo) keyObj;
                key = keyConverter.getPrivateKey(privateKeyInfo);
            }
        } catch (Exception e) {
            log.error("The pem file parsed failed: {}", e.getMessage());
        }
        return key;
    }

    private static PublicKey ecPrivateToPublic(ECPrivateKey ecPrivateKey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC", provider);
            ECParameterSpec ecSpec = ecPrivateKey.getParameters();
            ECPoint ecPoint = ecSpec.getG().multiply(ecPrivateKey.getD());
            return keyFactory.generatePublic(new ECPublicKeySpec(ecPoint, ecSpec));
        } catch (Exception e) {
            log.error("failed to covert ec privateKey to public.");
        }
        return null;
    }
}
