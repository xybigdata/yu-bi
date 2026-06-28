/*
 * YuBi
 * <p>
 * Copyright 2021 (originally Datart by running-elephant)
 * Copyright 2024-2026 YuBi Contributors
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

package yubi.security.manager.springsecurity;

import yubi.core.base.consts.Const;
import yubi.security.base.JwtToken;
import yubi.security.manager.AuthenticationTokenAdapter;
import yubi.security.util.JwtUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCrypt;

/**
 * Spring Security implementation of {@link AuthenticationTokenAdapter}.
 * <p>
 * Replaces {@code ShiroAuthenticationTokenAdapter} by converting Spring Security
 * {@link Authentication} objects into username resolution and credential matching
 * operations. Handles two authentication modes:
 * <ul>
 *   <li><b>Password-based:</b> credentials (non-null) contain the raw password;
 *       validated via BCrypt against the stored hash.</li>
 *   <li><b>Bearer/JWT:</b> the principal is a JWT string (prefixed with "Bearer ");
 *       validated by parsing the token and checking expiration.</li>
 * </ul>
 * <p>
 * Not annotated with {@code @Component} — wiring happens in Task 12.4 via
 * {@code SpringSecurityManager} configuration.
 */
public class SpringAuthenticationTokenAdapter implements AuthenticationTokenAdapter<Authentication> {

    @Override
    public String resolveUsername(Authentication token) {
        if (isBearerToken(token)) {
            String tokenString = (String) token.getPrincipal();
            JwtToken jwtToken = JwtUtils.toJwtToken(tokenString);
            return jwtToken == null ? null : jwtToken.getSubject();
        }
        // Password-based: principal is the username
        Object principal = token.getPrincipal();
        return principal == null ? null : principal.toString();
    }

    @Override
    public boolean matches(Authentication token, String credentials) {
        if (isPasswordToken(token)) {
            String password = resolvePassword(token);
            if (password == null) {
                return false;
            }
            return BCrypt.checkpw(password, credentials);
        }
        // Bearer token: validate JWT expiration
        String tokenString = (String) token.getPrincipal();
        JwtToken jwtToken = JwtUtils.toJwtToken(tokenString);
        return jwtToken != null && JwtUtils.validTimeout(jwtToken);
    }

    /**
     * Determines if the authentication represents a bearer/JWT token.
     * A bearer token has null credentials and a principal string starting
     * with the JWT prefix ("Bearer ").
     */
    private boolean isBearerToken(Authentication token) {
        if (token.getCredentials() != null) {
            return false;
        }
        Object principal = token.getPrincipal();
        return principal instanceof String str && str.startsWith(Const.TOKEN_HEADER_PREFIX);
    }

    /**
     * Determines if the authentication represents a password-based login.
     * A password token has non-null credentials.
     */
    private boolean isPasswordToken(Authentication token) {
        return token.getCredentials() != null;
    }

    /**
     * Extracts the raw password from the authentication's credentials.
     * Supports both String and char[] credential types.
     */
    private String resolvePassword(Authentication token) {
        Object creds = token.getCredentials();
        if (creds instanceof String str) {
            return str;
        }
        if (creds instanceof char[] chars) {
            return new String(chars);
        }
        return creds == null ? null : creds.toString();
    }
}
