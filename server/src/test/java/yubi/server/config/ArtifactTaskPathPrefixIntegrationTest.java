package yubi.server.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
import yubi.server.artifact.ArtifactDescriptor;
import yubi.server.artifact.ArtifactTaskState;
import yubi.server.artifact.ArtifactTaskWebMapper;
import yubi.server.artifact.ArtifactTasks;
import yubi.server.artifact.TaskBatch;
import yubi.server.artifact.TaskView;
import yubi.server.controller.ArtifactTaskController;
import yubi.server.controller.ShareArtifactTaskController;
import yubi.server.service.ShareService;
import yubi.server.service.OrgService;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(ArtifactTaskPathPrefixIntegrationTest.TestConfig.class)
@ContextConfiguration(initializers = ArtifactTaskPathPrefixIntegrationTest.ApplicationInitializer.class)
@WebAppConfiguration
@TestPropertySource(properties = "yubi.server.path-prefix=/api/v1")
class ArtifactTaskPathPrefixIntegrationTest {

    static class ApplicationInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            new Application().setApplicationContext(applicationContext);
        }
    }

    private final WebApplicationContext context;
    private MockMvc mvc;

    ArtifactTaskPathPrefixIntegrationTest(WebApplicationContext context) {
        this.context = context;
    }

    @BeforeEach
    void setUp() {
        ArtifactTasks tasks = context.getBean(ArtifactTasks.class);
        YuBiSecurityManager securityManager = context.getBean(YuBiSecurityManager.class);
        ShareService shareService = context.getBean(ShareService.class);
        OrgService orgService = context.getBean(OrgService.class);
        reset(tasks, securityManager, shareService, orgService);

        User user = new User();
        user.setId("user-1");
        user.setUsername("alice");
        when(securityManager.getCurrentUser()).thenReturn(user);
        yubi.core.entity.Organization organization = new yubi.core.entity.Organization();
        organization.setId("org-1");
        when(orgService.listOrganizations()).thenReturn(List.of(
                new yubi.server.base.dto.OrganizationBaseInfo(organization)));

        ArtifactDescriptor descriptor = new ArtifactDescriptor("orders", "text/plain", ".txt");
        TaskView task = new TaskView(
                "task-1", descriptor, ArtifactTaskState.READY,
                Instant.EPOCH, Instant.EPOCH.plusSeconds(60), Instant.EPOCH.plusSeconds(1),
                null, "trace-1"
        );
        when(tasks.inspect(any(), any())).thenReturn(new TaskBatch(List.of(task), Set.of()));
        when(shareService.getArtifactTask("share-1", "client-1", null, "task-1"))
                .thenReturn(task);

        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void 已登录用户可通过统一Api前缀查询产物任务() throws Exception {
        mvc.perform(get("/api/v1/organizations/org-1/artifact-tasks/task-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("task-1"));
    }

    @Test
    void 分享用户可通过统一Api前缀查询产物任务() throws Exception {
        mvc.perform(get("/api/v1/shares/share-1/artifact-tasks/task-1")
                        .queryParam("clientId", "client-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("task-1"));
    }

    @Configuration
    @EnableWebMvc
    @Import({WebMvcConfig.class, ArtifactTaskController.class, ShareArtifactTaskController.class})
    static class TestConfig {

        @Bean
        ArtifactTasks artifactTasks() {
            return mock(ArtifactTasks.class);
        }

        @Bean
        YuBiSecurityManager securityManager() {
            return mock(YuBiSecurityManager.class);
        }

        @Bean
        ArtifactTaskWebMapper artifactTaskWebMapper() {
            return new ArtifactTaskWebMapper();
        }

        @Bean
        ShareService shareService() {
            return mock(ShareService.class);
        }

        @Bean
        OrgService orgService() {
            return mock(OrgService.class);
        }
    }
}
