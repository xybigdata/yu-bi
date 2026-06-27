package datart.server.config.filter;

import datart.core.base.consts.Const;
import datart.core.entity.User;
import datart.security.base.JwtToken;
import datart.security.manager.AuthenticationAssembler;
import datart.security.manager.AuthenticationCache;
import datart.security.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT authentication filter that validates Bearer tokens from the Authorization
 * header and populates the Spring Security {@link SecurityContextHolder} with
 * an authenticated {@link UsernamePasswordAuthenticationToken}.
 * <p>
 * Replaces the previous Shiro {@code Subject.login()} delegation with direct
 * JWT validation via {@link JwtUtils} and user lookup via
 * {@link AuthenticationAssembler}.
 * <p>
 * Behavior:
 * <ul>
 *   <li>No token present → continues filter chain (anonymous request)</li>
 *   <li>Valid token → sets SecurityContext, refreshes token in response header</li>
 *   <li>Invalid/expired token → logs warning, does not set SecurityContext,
 *       continues filter chain (authorization rules will reject if endpoint requires auth)</li>
 * </ul>
 */
@Component
public class JwtRequestAuthenticationFilter extends OncePerRequestFilter {

    private static final String REALM_NAME = "datart";

    private final AuthenticationAssembler authenticationAssembler;

    public JwtRequestAuthenticationFilter(AuthenticationAssembler authenticationAssembler) {
        this.authenticationAssembler = authenticationAssembler;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader(Const.TOKEN);
        if (token != null && !token.isBlank()) {
            authenticateWithToken(token, response);
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Attempts to authenticate the request using the provided JWT token string.
     * On success, sets the SecurityContext and refreshes the token in the response header.
     * On failure, logs a debug message but does not throw — the request continues
     * as unauthenticated and Spring Security's authorization rules will handle rejection.
     */
    private void authenticateWithToken(String tokenString, HttpServletResponse response) {
        try {
            JwtToken jwtToken = JwtUtils.toJwtToken(tokenString);
            if (jwtToken == null) {
                logger.debug("JWT authentication skipped: unable to parse token");
                return;
            }

            if (!JwtUtils.validTimeout(jwtToken)) {
                logger.debug("JWT authentication skipped: token expired");
                return;
            }

            String username = jwtToken.getSubject();
            AuthenticationCache cache = authenticationAssembler.assemble(username, REALM_NAME);
            if (cache == null) {
                logger.debug("JWT authentication skipped: user not found for subject: " + username);
                return;
            }

            User user = cache.getPrincipal();

            // Validate password hash to detect password changes after token was issued
            if (user.getPassword() != null && user.getPassword().hashCode() != jwtToken.getPwdHash()) {
                logger.debug("JWT authentication skipped: password hash mismatch (password changed)");
                return;
            }

            // Set authenticated principal in SecurityContext
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Refresh token with new expiration and set in response header
            JwtToken refreshedToken = JwtUtils.createJwtToken(user);
            response.setHeader(Const.TOKEN, JwtUtils.toJwtString(refreshedToken));

        } catch (Exception e) {
            // Do not propagate exceptions — let authorization rules handle unauthenticated access
            logger.debug("JWT authentication failed: " + e.getMessage());
        }
    }
}
