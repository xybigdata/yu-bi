package datart.security.manager.springsecurity;

import datart.core.common.Application;
import datart.security.base.JwtToken;
import datart.security.util.JwtUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringAuthenticationTokenAdapterTest {

    private final SpringAuthenticationTokenAdapter adapter = new SpringAuthenticationTokenAdapter();

    @BeforeAll
    static void setUpApplicationContext() {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("datart.security.token.secret", "d@a$t%a^r&a*t"))
                .thenReturn("d@a$t%a^r&a*t");
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getEnvironment()).thenReturn(environment);
        new Application().setApplicationContext(context);
    }

    @Test
    void shouldResolveUsernameFromPasswordToken() {
        Authentication token = new UsernamePasswordAuthenticationToken("demo", "secret");
        assertEquals("demo", adapter.resolveUsername(token));
    }

    @Test
    void shouldMatchBcryptPasswordToken() {
        String encodedPassword = BCrypt.hashpw("secret", BCrypt.gensalt());

        Authentication correctToken = new UsernamePasswordAuthenticationToken("demo", "secret");
        Authentication wrongToken = new UsernamePasswordAuthenticationToken("demo", "wrong");

        assertTrue(adapter.matches(correctToken, encodedPassword));
        assertFalse(adapter.matches(wrongToken, encodedPassword));
    }

    @Test
    void shouldResolveUsernameFromBearerToken() {
        // JwtUtils.toJwtString() already returns "Bearer " + jwt
        String jwtString = JwtUtils.toJwtString(jwtToken("demo", "password"));
        // Bearer token: principal is the full JWT string (with "Bearer " prefix), credentials is null
        Authentication token = new UsernamePasswordAuthenticationToken(jwtString, null);

        assertEquals("demo", adapter.resolveUsername(token));
        assertTrue(adapter.matches(token, "ignored"));
    }

    @Test
    void shouldTreatTokenWithoutBearerPrefixAsPlainUsername() {
        // Without "Bearer " prefix, the token is NOT recognized as a bearer token.
        // The adapter falls through to the username path and returns principal as-is.
        Authentication token = new UsernamePasswordAuthenticationToken("invalid", null);

        assertEquals("invalid", adapter.resolveUsername(token));
        // matches returns false because JwtUtils.toJwtToken("invalid") returns null (no "Bearer " prefix)
        assertFalse(adapter.matches(token, "ignored"));
    }

    @Test
    void shouldReturnNullWhenPrincipalIsNull() {
        Authentication token = new UsernamePasswordAuthenticationToken(null, null,
                Collections.emptyList());

        assertNull(adapter.resolveUsername(token));
    }

    private static JwtToken jwtToken(String username, String password) {
        JwtToken jwtToken = new JwtToken();
        jwtToken.setSubject(username);
        jwtToken.setPwdHash(password.hashCode());
        jwtToken.setExp(new Date(System.currentTimeMillis() + 10 * 60 * 1000));
        return jwtToken;
    }
}
