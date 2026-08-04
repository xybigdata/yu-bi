package yubi.server.artifact;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import yubi.core.entity.User;
import yubi.security.manager.YuBiSecurityManager;
import yubi.server.config.ArtifactTaskWebExceptionHandler;
import yubi.server.controller.ArtifactTaskController;
import yubi.server.service.OrgService;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ArtifactTaskWebContractTest {

    private ArtifactTasks tasks;
    private YuBiSecurityManager securityManager;
    private OrgService orgService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        tasks = new DefaultArtifactTasks(
                new InMemoryArtifactTaskStore(),
                new InMemoryArtifactBlobStore(),
                Runnable::run,
                Clock.systemUTC(),
                Duration.ofMinutes(15),
                Duration.ofDays(7),
                3
        );
        securityManager = mock(YuBiSecurityManager.class);
        orgService = mock(OrgService.class);
        when(securityManager.getCurrentUser()).thenReturn(user("user-1", "alice"));
        when(orgService.listOrganizations()).thenReturn(java.util.List.of(organization("org-1")));
        mvc = MockMvcBuilders.standaloneSetup(
                        new ArtifactTaskController(tasks, securityManager, orgService, new ArtifactTaskWebMapper()))
                .setControllerAdvice(new ArtifactTaskWebExceptionHandler())
                .build();
    }

    @Test
    void authenticatedUserCanInspectAndOpenOwnedArtifact() throws Exception {
        TaskHandle handle = tasks.submit(
                ArtifactAccess.authenticated("user-1", "org-1", "alice"),
                new ArtifactDescriptor("季度报表", "text/plain", ".txt"),
                context -> context.output().write("已完成".getBytes(StandardCharsets.UTF_8))
        );

        mvc.perform(get("/organizations/{orgId}/artifact-tasks/{id}", "org-1", handle.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(handle.id()))
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.fileName").value("季度报表.txt"))
                .andExpect(jsonPath("$.data.error").doesNotExist());

        mvc.perform(get("/organizations/{orgId}/artifact-tasks/{id}/content", "org-1", handle.id()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/plain"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().bytes("已完成".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void unknownAndCrossUserTasksReturnTheSameSanitizedNotFoundResponse() throws Exception {
        TaskHandle handle = tasks.submit(
                ArtifactAccess.authenticated("user-1", "org-1", "alice"),
                new ArtifactDescriptor("机密报表", "text/plain", ".txt"),
                context -> context.output().write("salary-secret".getBytes(StandardCharsets.UTF_8))
        );
        when(securityManager.getCurrentUser()).thenReturn(user("user-2", "bob"));

        for (String id : java.util.List.of(handle.id(), "unknown-task")) {
            mvc.perform(get("/organizations/{orgId}/artifact-tasks/{id}", "org-1", id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("ARTIFACT_NOT_FOUND"))
                    .andExpect(jsonPath("$.traceId").isNotEmpty())
                    .andExpect(content().string(not(containsString("salary-secret"))));
        }
    }

    @Test
    void 失败任务详情只返回脱敏错误原因() throws Exception {
        TaskHandle failed = tasks.submit(
                ArtifactAccess.authenticated("user-1", "org-1", "alice"),
                new ArtifactDescriptor("失败报表", "text/plain", ".txt"),
                context -> {
                    throw new IllegalStateException("敏感 SQL: select salary from secret_table");
                }
        );

        mvc.perform(get("/organizations/{orgId}/artifact-tasks/{id}", "org-1", failed.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.error.code").value("ARTIFACT_GENERATION_FAILED"))
                .andExpect(jsonPath("$.data.error.message")
                        .value("产物生成失败，请凭追踪 ID 联系管理员"))
                .andExpect(jsonPath("$.data.error.traceId").isNotEmpty())
                .andExpect(content().string(not(containsString("salary"))))
                .andExpect(content().string(not(containsString("secret_table"))));
    }

    @Test
    void currentOrganizationListsOnlyItsTasksAndPaginatesTerminalTasks() throws Exception {
        TaskHandle first = tasks.submit(
                ArtifactAccess.authenticated("user-1", "org-1", "alice"),
                new ArtifactDescriptor("组织一报表", "text/plain", ".txt", "DASHBOARD"),
                context -> context.output().write(1)
        );
        tasks.submit(
                ArtifactAccess.authenticated("user-1", "org-2", "alice"),
                new ArtifactDescriptor("组织二报表", "text/plain", ".txt", "DASHBOARD"),
                context -> context.output().write(2)
        );

        mvc.perform(get("/organizations/{orgId}/artifact-tasks", "org-1")
                        .param("offset", "0")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tasks.length()").value(1))
                .andExpect(jsonPath("$.data.tasks[0].id").value(first.id()))
                .andExpect(jsonPath("$.data.tasks[0].source").value("DASHBOARD"))
                .andExpect(jsonPath("$.data.nextOffset").doesNotExist());
    }

    @Test
    void formerOrganizationMemberCannotListOrDownloadArtifacts() throws Exception {
        TaskHandle handle = tasks.submit(
                ArtifactAccess.authenticated("user-1", "org-1", "alice"),
                new ArtifactDescriptor("离组文件", "text/plain", ".txt"),
                context -> context.output().write(1)
        );
        when(orgService.listOrganizations()).thenReturn(java.util.List.of());

        mvc.perform(get("/organizations/{orgId}/artifact-tasks", "org-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ARTIFACT_NOT_FOUND"));
        mvc.perform(get("/organizations/{orgId}/artifact-tasks/{id}/content", "org-1", handle.id()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ARTIFACT_NOT_FOUND"));
    }

    @Test
    void deletingFinishedTaskIsIdempotent() throws Exception {
        TaskHandle handle = tasks.submit(
                ArtifactAccess.authenticated("user-1", "org-1", "alice"),
                new ArtifactDescriptor("待清除文件", "text/plain", ".txt"),
                context -> context.output().write(1)
        );

        mvc.perform(delete("/organizations/{orgId}/artifact-tasks/{id}", "org-1", handle.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
        mvc.perform(delete("/organizations/{orgId}/artifact-tasks/{id}", "org-1", handle.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void 新浏览器Session可通过失败任务ID创建重试任务() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        TaskHandle failed = tasks.submit(
                ArtifactAccess.authenticated("user-1", "org-1", "alice"),
                new ArtifactDescriptor("重试报表", "text/plain", ".txt"),
                context -> {
                    if (attempts.incrementAndGet() == 1) {
                        throw new IllegalStateException("首次失败");
                    }
                    context.output().write(1);
                }
        );

        mvc.perform(post("/organizations/{orgId}/artifact-tasks/{id}/retry", "org-1", failed.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(not(failed.id())))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    void 不允许或信息已失效的重试返回冲突状态() throws Exception {
        TaskHandle completed = tasks.submit(
                ArtifactAccess.authenticated("user-1", "org-1", "alice"),
                new ArtifactDescriptor("已完成报表", "text/plain", ".txt"),
                context -> context.output().write(1)
        );

        mvc.perform(post("/organizations/{orgId}/artifact-tasks/{id}/retry",
                        "org-1", completed.id()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ARTIFACT_RETRY_NOT_ALLOWED"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());

        TaskHandle failed = tasks.submit(
                ArtifactAccess.authenticated("user-1", "org-1", "alice"),
                new ArtifactDescriptor("失败报表", "text/plain", ".txt"),
                context -> {
                    throw new IllegalStateException("生成失败");
                }
        );
        mvc.perform(post("/organizations/{orgId}/artifact-tasks/{id}/retry",
                        "org-1", failed.id()))
                .andExpect(status().isOk());
        mvc.perform(post("/organizations/{orgId}/artifact-tasks/{id}/retry",
                        "org-1", failed.id()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ARTIFACT_RETRY_UNAVAILABLE"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    private User user(String id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private yubi.server.base.dto.OrganizationBaseInfo organization(String id) {
        yubi.core.entity.Organization organization = new yubi.core.entity.Organization();
        organization.setId(id);
        return new yubi.server.base.dto.OrganizationBaseInfo(organization);
    }
}
