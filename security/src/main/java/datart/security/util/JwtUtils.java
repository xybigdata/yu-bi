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

package datart.security.util;

import datart.core.base.consts.Const;
import datart.core.common.Application;
import datart.core.entity.User;
import datart.security.base.InviteToken;
import datart.security.base.JwtToken;
import datart.security.base.PasswordToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtils {

    private static final String TOKEN_KEY_ORG_ID = "org_id";

    private static final String TOKEN_KEY_USER_ID = "user_id";

    private static final String TOKEN_KEY_INVITER = "inviter";

    private static final String PASSWORD_HASH = "password";

    private static final String LEGACY_SECRET_COMPAT_SALT = "datart-jjwt-hs256-compat";

    public static final int VERIFY_CODE_TIMEOUT_MIN = 10 * 60 * 1000;

    public static JwtToken createJwtToken(User user) {
        JwtToken jwtToken = new JwtToken();
        jwtToken.setSubject(user.getUsername());
        jwtToken.setExp(new Date(System.currentTimeMillis() + getSessionTimeout()));
        jwtToken.setPwdHash(user.getPassword().hashCode());
        return jwtToken;
    }

    public static String toJwtString(JwtToken token) {
        HashMap<String, Object> claims = new HashMap<>();
        if (token instanceof PasswordToken) {
            claims.put(PASSWORD_HASH, ((PasswordToken) token).getPassword().hashCode());
        } else {
            claims.put(PASSWORD_HASH, token.getPwdHash());
        }
        String jwt = Jwts.builder()
                .claims(claims)
                .subject(token.getSubject())
                .expiration(token.getExp() != null ? token.getExp() : new Date(token.getCreateTime() + getSessionTimeout()))
                .signWith(getSigningKey())
                .compact();
        return Const.TOKEN_HEADER_PREFIX + jwt;
    }

    public static String toJwtString(InviteToken token) {
        HashMap<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_KEY_INVITER, token.getInviter());
        claims.put(TOKEN_KEY_USER_ID, token.getUserId());
        claims.put(TOKEN_KEY_ORG_ID, token.getOrgId());
        String jwt = Jwts.builder()
                .claims(claims)
                .subject(token.getSubject())
                .expiration(token.getExp() != null ? token.getExp() : new Date(token.getCreateTime() + VERIFY_CODE_TIMEOUT_MIN))
                .signWith(getSigningKey())
                .compact();
        return Const.TOKEN_HEADER_PREFIX + jwt;
    }

    public static JwtToken toJwtToken(String jwtString) {
        if (jwtString == null || !jwtString.startsWith(Const.TOKEN_HEADER_PREFIX)) {
            return null;
        }
        jwtString = StringUtils.removeStart(jwtString, Const.TOKEN_HEADER_PREFIX);
        Claims claims = getClaims(jwtString);
        JwtToken jwtToken = new JwtToken();
        jwtToken.setSubject(claims.getSubject());
        jwtToken.setPwdHash(claims.get(PASSWORD_HASH, Integer.class));
        jwtToken.setExp(claims.getExpiration());
        return jwtToken;
    }

    public static InviteToken toInviteToken(String token) {
        token = StringUtils.removeStart(token, Const.TOKEN_HEADER_PREFIX);
        Claims claims = getClaims(token);
        InviteToken inviteToken = new InviteToken();
        inviteToken.setInviter(claims.get(TOKEN_KEY_INVITER, String.class));
        inviteToken.setUserId(claims.get(TOKEN_KEY_USER_ID, String.class));
        inviteToken.setOrgId(claims.get(TOKEN_KEY_ORG_ID, String.class));
        return inviteToken;
    }

    public static boolean validTimeout(String jwtToken) {
        return validTimeout(toJwtToken(jwtToken));
    }


    public static boolean validTimeout(JwtToken token) {
        return token.getExp() == null || token.getExp().after(Calendar.getInstance().getTime());
    }

    private static Claims getClaims(String token) {
        String compactToken = token.trim();
        try {
            return parseClaims(compactToken, getSigningKey()).getPayload();
        } catch (ExpiredJwtException expired) {
            throw expired;
        } catch (JwtException ignored) {
            SecretKey legacyKey = getLegacyVerificationKey();
            if (legacyKey == null) {
                throw ignored;
            }
            try {
                return parseClaims(compactToken, legacyKey).getPayload();
            } catch (ExpiredJwtException expired) {
                throw expired;
            } catch (JwtException secondary) {
                throw ignored;
            }
        }
    }

    private static Jws<Claims> parseClaims(String token, SecretKey key) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }

    private static SecretKey getSigningKey() {
        byte[] secretBytes = Application.getTokenSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length >= 32) {
            return Keys.hmacShaKeyFor(secretBytes);
        }
        return new SecretKeySpec(sha256(secretBytes), "HmacSHA256");
    }

    private static SecretKey getLegacyVerificationKey() {
        byte[] secretBytes = Application.getTokenSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length >= 32) {
            return null;
        }
        return new SecretKeySpec(secretBytes, "HmacSHA256");
    }

    private static byte[] sha256(byte[] input) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(LEGACY_SECRET_COMPAT_SALT.getBytes(StandardCharsets.UTF_8));
            return messageDigest.digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    private static long getSessionTimeout() {
        String timeout = Application.getProperty("datart.security.token.timeout-min", "30");
        return Long.parseLong(timeout) * 60 * 1000;
    }

}
