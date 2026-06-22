package datart.security.manager.shiro;

import datart.core.common.Application;
import datart.security.base.JwtToken;
import datart.security.util.JwtUtils;
import org.apache.shiro.authc.BearerToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShiroAuthenticationTokenAdapterTest {

    private final ShiroAuthenticationTokenAdapter adapter = new ShiroAuthenticationTokenAdapter();

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
        assertEquals("demo", adapter.resolveUsername(new UsernamePasswordToken("demo", "secret")));
    }

    @Test
    void shouldMatchBcryptPasswordToken() {
        String encodedPassword = BCrypt.hashpw("secret", BCrypt.gensalt());

        assertTrue(adapter.matches(new UsernamePasswordToken("demo", "secret"), encodedPassword));
        assertFalse(adapter.matches(new UsernamePasswordToken("demo", "wrong"), encodedPassword));
    }

    @Test
    void shouldResolveUsernameFromBearerToken() {
        String token = JwtUtils.toJwtString(jwtToken("demo", "password"));

        assertEquals("demo", adapter.resolveUsername(new BearerToken(token)));
        assertTrue(adapter.matches(new BearerToken(token), "ignored"));
    }

    @Test
    void shouldRejectBearerTokenWithoutExpectedPrefix() {
        BearerToken token = new BearerToken("invalid");

        assertNull(adapter.resolveUsername(token));
        assertFalse(adapter.matches(token, "ignored"));
    }

    private static JwtToken jwtToken(String username, String password) {
        JwtToken jwtToken = new JwtToken();
        jwtToken.setSubject(username);
        jwtToken.setPwdHash(password.hashCode());
        jwtToken.setExp(new Date(System.currentTimeMillis() + 10 * 60 * 1000));
        return jwtToken;
    }
}
