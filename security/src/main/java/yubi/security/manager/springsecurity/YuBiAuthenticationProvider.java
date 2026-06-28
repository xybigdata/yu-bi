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

import yubi.core.entity.User;
import yubi.security.base.JwtToken;
import yubi.security.manager.AuthenticationAssembler;
import yubi.security.manager.AuthenticationCache;
import yubi.security.util.JwtUtils;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.Collections;

/**
 * Spring Security {@link AuthenticationProvider} that bridges existing
 * {@link AuthenticationAssembler} credential lookup with Spring Security's
 * authentication model.
 * <p>
 * Supports two authentication flows via {@link UsernamePasswordAuthenticationToken}:
 * <ul>
 *   <li><b>Password authentication:</b> username + password credentials validated with BCrypt</li>
 *   <li><b>Bearer token authentication:</b> JWT token string as principal, validated for
 *       integrity and timeout</li>
 * </ul>
 * <p>
 * Not annotated with {@code @Component} — wired explicitly in a later SecurityFilterChain
 * configuration task.
 */
public class YuBiAuthenticationProvider implements AuthenticationProvider {

    private static final String REALM_NAME = "yubi";

    private final AuthenticationAssembler authenticationAssembler;

    public YuBiAuthenticationProvider(AuthenticationAssembler authenticationAssembler) {
        this.authenticationAssembler = authenticationAssembler;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String principal = authentication.getName();
        Object credentials = authentication.getCredentials();

        if (credentials != null && !credentials.toString().isEmpty()) {
            return authenticateWithPassword(principal, credentials.toString());
        } else {
            return authenticateWithBearer(principal);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    /**
     * Password-based authentication: looks up user by username/email, then
     * validates the presented password against the stored BCrypt hash.
     */
    private Authentication authenticateWithPassword(String usernameOrEmail, String password) {
        AuthenticationCache cache = authenticationAssembler.assemble(usernameOrEmail, REALM_NAME);
        if (cache == null) {
            throw new BadCredentialsException("User not found");
        }

        String storedHash = cache.getCredentials();
        if (storedHash == null || !BCrypt.checkpw(password, storedHash)) {
            throw new BadCredentialsException("Invalid credentials");
        }

        User user = cache.getPrincipal();
        return new UsernamePasswordAuthenticationToken(
                user,
                cache.getCredentials(),
                Collections.emptyList() // Authorities loaded lazily per-request via AuthorizationAssembler
        );
    }

    /**
     * Bearer token authentication: parses the JWT token string to extract
     * the subject, looks up the user, and validates token timeout.
     */
    private Authentication authenticateWithBearer(String tokenString) {
        JwtToken jwtToken = JwtUtils.toJwtToken(tokenString);
        if (jwtToken == null) {
            throw new BadCredentialsException("Invalid bearer token");
        }

        if (!JwtUtils.validTimeout(jwtToken)) {
            throw new BadCredentialsException("Bearer token expired");
        }

        String username = jwtToken.getSubject();
        AuthenticationCache cache = authenticationAssembler.assemble(username, REALM_NAME);
        if (cache == null) {
            throw new BadCredentialsException("User not found for bearer token");
        }

        User user = cache.getPrincipal();
        return new UsernamePasswordAuthenticationToken(
                user,
                null,
                Collections.emptyList()
        );
    }
}
