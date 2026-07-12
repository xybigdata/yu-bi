package yubi.server.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import yubi.core.common.Application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QueryCorsConfigurationTest {

    private UrlBasedCorsConfigurationSource source;

    @BeforeEach
    void setUp() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        Environment environment = mock(Environment.class);
        when(applicationContext.getEnvironment()).thenReturn(environment);
        when(environment.getProperty("yubi.server.path-prefix")).thenReturn("/api/v1");
        new Application().setApplicationContext(applicationContext);
        WebSecurityConfig config = new WebSecurityConfig();
        source = config.corsConfigurationSource();
    }

    @Test
    void shouldAllowShareHeaderAndPostPreflightOnlyOnPublicQueryPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/public/queries/execute");
        request.addHeader("Origin", "https://embed.example");
        request.addHeader("Access-Control-Request-Method", "POST");
        request.addHeader("Access-Control-Request-Headers", "Content-Type, X-YuBi-Share-Token");
        CorsConfiguration configuration = source.getCorsConfiguration(request);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertEquals("https://embed.example", configuration.checkOrigin("https://embed.example"));
        assertTrue(configuration.checkHttpMethod(org.springframework.http.HttpMethod.POST)
                .contains(org.springframework.http.HttpMethod.POST));
        assertTrue(configuration.getAllowedHeaders().contains("X-YuBi-Share-Token"));
        assertTrue(configuration.getAllowedMethods().contains("OPTIONS"));
        assertTrue(new DefaultCorsProcessor().processRequest(configuration, request, response));
        assertEquals("https://embed.example", response.getHeader("Access-Control-Allow-Origin"));
        assertTrue(response.getHeader("Access-Control-Allow-Headers").contains("X-YuBi-Share-Token"));
    }

    @Test
    void shouldNotExpandCorsToLegacyOrUnrelatedApiPaths() {
        assertNull(source.getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/api/v1/shares/execute")));
        assertNull(source.getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/api/v1/users")));
    }
}
