package yubi.server.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import yubi.core.common.Application;
import yubi.core.entity.User;
import yubi.security.manager.YuBiSecurityManager;
import yubi.server.controller.RecycleController;
import yubi.server.recycle.RecycleItemPreflight;
import yubi.server.recycle.RecyclePreflight;
import yubi.server.recycle.RecycleService;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(RecyclePathPrefixIntegrationTest.TestConfig.class)
@ContextConfiguration(initializers = RecyclePathPrefixIntegrationTest.ApplicationInitializer.class)
@WebAppConfiguration
@TestPropertySource(properties = "yubi.server.path-prefix=/api/v1")
class RecyclePathPrefixIntegrationTest {

    static class ApplicationInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            new Application().setApplicationContext(applicationContext);
        }
    }

    private final WebApplicationContext context;
    private MockMvc mvc;

    RecyclePathPrefixIntegrationTest(WebApplicationContext context) {
        this.context = context;
    }

    @BeforeEach
    void setUp() {
        RecycleService service = context.getBean(RecycleService.class);
        YuBiSecurityManager securityManager = context.getBean(YuBiSecurityManager.class);
        reset(service, securityManager);

        User user = new User();
        user.setId("user-1");
        when(securityManager.getCurrentUser()).thenReturn(user);
        when(service.preflight(any(), any())).thenReturn(new RecyclePreflight(
                "token-1",
                Instant.parse("2026-08-07T06:05:00Z"),
                List.of(RecycleItemPreflight.ready("dashboard-1"))));

        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void 已登录用户可通过统一Api前缀执行回收站预检() throws Exception {
        mvc.perform(post("/api/v1/organizations/org-1/recycle/DASHBOARD/preflight")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rootIds\":[\"dashboard-1\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.operationToken").value("token-1"));
    }

    @Configuration
    @EnableWebMvc
    @Import({WebMvcConfig.class, RecycleController.class})
    static class TestConfig {

        @Bean
        RecycleService recycleService() {
            return mock(RecycleService.class);
        }

        @Bean
        YuBiSecurityManager securityManager() {
            return mock(YuBiSecurityManager.class);
        }
    }
}
