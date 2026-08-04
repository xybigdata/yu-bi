package yubi.server.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import yubi.server.artifact.ArtifactContent;
import yubi.server.artifact.ArtifactDescriptor;
import yubi.server.artifact.ArtifactTaskException;
import yubi.server.artifact.ArtifactTaskState;
import yubi.server.artifact.ArtifactTaskWebMapper;
import yubi.server.artifact.TaskHandle;
import yubi.server.artifact.TaskView;
import yubi.server.base.params.ShareDownloadParam;
import yubi.server.config.ShareArtifactTaskWebExceptionHandler;
import yubi.server.service.ShareService;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ShareArtifactTaskWebContractTest {

    private ShareService shareService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        shareService = mock(ShareService.class);
        ArtifactTaskWebMapper mapper = new ArtifactTaskWebMapper();
        mvc = MockMvcBuilders.standaloneSetup(
                        new ShareController(shareService, mapper),
                        new ShareArtifactTaskController(shareService, mapper))
                .setControllerAdvice(new ShareArtifactTaskWebExceptionHandler())
                .build();
    }

    @Test
    void 合法凭据可创建查询并下载分享产物() throws Exception {
        ArtifactDescriptor descriptor = new ArtifactDescriptor("orders", "text/plain", ".txt");
        TaskHandle handle = new TaskHandle("task-1", descriptor, ArtifactTaskState.QUEUED,
                Instant.EPOCH, Instant.EPOCH.plusSeconds(60), "trace-1");
        TaskView view = new TaskView("task-1", descriptor, ArtifactTaskState.READY,
                Instant.EPOCH, Instant.EPOCH.plusSeconds(60), Instant.EPOCH.plusSeconds(1),
                null, "trace-1");
        byte[] bytes = "artifact-content".getBytes(StandardCharsets.UTF_8);
        when(shareService.createDownload(eq("client-1"), eq("correct-password"), any()))
                .thenReturn(handle);
        when(shareService.getArtifactTask("share-1", "client-1", "correct-password", "task-1"))
                .thenReturn(view);
        when(shareService.openArtifact("share-1", "client-1", "correct-password", "task-1"))
                .thenReturn(new ArtifactContent("orders.txt", "text/plain", bytes.length,
                        new ByteArrayInputStream(bytes)));

        mvc.perform(post("/shares/download")
                        .queryParam("clientId", "client-1")
                        .queryParam("password", "correct-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shareToken": "share-1",
                                  "fileName": "orders",
                                  "downloadParams": [{"viewId": "view-1"}],
                                  "executeToken": {"view-1": {"authorizedToken": "execute-token"}}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("task-1"))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.fileName").value("orders.txt"));

        ArgumentCaptor<ShareDownloadParam> createParam = ArgumentCaptor.forClass(ShareDownloadParam.class);
        verify(shareService).createDownload(eq("client-1"), eq("correct-password"), createParam.capture());
        assertEquals("share-1", createParam.getValue().getShareToken());

        mvc.perform(get("/shares/{shareId}/artifact-tasks/{id}", "share-1", "task-1")
                        .queryParam("clientId", "client-1")
                        .queryParam("password", "correct-password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));

        mvc.perform(get("/shares/{shareId}/artifact-tasks/{id}/content", "share-1", "task-1")
                        .queryParam("clientId", "client-1")
                        .queryParam("password", "correct-password"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().bytes(bytes));
    }

    @Test
    void 越权和未知任务返回同形的未找到响应() throws Exception {
        ArtifactTaskException notFound = notFound();
        when(shareService.getArtifactTask(any(), any(), any(), any())).thenThrow(notFound);
        when(shareService.openArtifact(any(), any(), any(), any())).thenThrow(notFound());
        when(shareService.createDownload(eq("client-1"), eq("correct-password"), any()))
                .thenThrow(notFound());

        List<org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder> requests = List.of(
                get("/shares/share-1/artifact-tasks/task-1")
                        .queryParam("clientId", "client-2").queryParam("password", "correct-password"),
                get("/shares/share-2/artifact-tasks/task-1")
                        .queryParam("clientId", "client-1").queryParam("password", "correct-password"),
                get("/shares/share-1/artifact-tasks/task-1")
                        .queryParam("clientId", "client-1").queryParam("password", "wrong-password"),
                get("/shares/share-1/artifact-tasks/unknown-task")
                        .queryParam("clientId", "client-1").queryParam("password", "correct-password"),
                get("/shares/share-1/artifact-tasks/task-1/content")
                        .queryParam("clientId", "client-2").queryParam("password", "correct-password"),
                get("/shares/share-1/artifact-tasks/unknown-task/content")
                        .queryParam("clientId", "client-1").queryParam("password", "correct-password"),
                post("/shares/download")
                        .queryParam("clientId", "client-1")
                        .queryParam("password", "correct-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shareToken": "valid-execute-token",
                                  "fileName": "orders",
                                  "downloadParams": [{"viewId": "view-1"}],
                                  "executeToken": {"view-1": {"authorizedToken": "valid-execute-token"}}
                                }
                                """)
        );

        for (var request : requests) {
            mvc.perform(request)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("ARTIFACT_NOT_FOUND"))
                    .andExpect(jsonPath("$.traceId").isNotEmpty())
                    .andExpect(content().string(not(containsString("artifact-content"))));
        }
    }

    @Test
    void 未完成产物下载返回冲突状态与任务追踪信息() throws Exception {
        when(shareService.openArtifact("share-1", "client-1", "correct-password", "task-1"))
                .thenThrow(new ArtifactTaskException(
                        "ARTIFACT_NOT_READY", "产物任务尚未完成", "trace-task-1"));

        mvc.perform(get("/shares/share-1/artifact-tasks/task-1/content")
                        .queryParam("clientId", "client-1")
                        .queryParam("password", "correct-password"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ARTIFACT_NOT_READY"))
                .andExpect(jsonPath("$.traceId").value("trace-task-1"));
    }

    @Test
    void 分享重试不可用与活动任务清理均返回冲突状态() throws Exception {
        when(shareService.retryArtifactTask(
                "share-1", "client-1", "correct-password", "task-1"))
                .thenThrow(new ArtifactTaskException(
                        "ARTIFACT_RETRY_UNAVAILABLE",
                        "原任务重试信息已失效，请重新发起导出",
                        "trace-retry"));
        org.mockito.Mockito.doThrow(new ArtifactTaskException(
                        "ARTIFACT_TASK_ACTIVE", "正在生成的任务不能清除", "trace-active"))
                .when(shareService)
                .deleteArtifactTask("share-1", "client-1", "correct-password", "task-1");

        mvc.perform(post("/shares/{shareId}/artifact-tasks/{id}/retry", "share-1", "task-1")
                        .queryParam("clientId", "client-1")
                        .queryParam("password", "correct-password"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ARTIFACT_RETRY_UNAVAILABLE"))
                .andExpect(jsonPath("$.traceId").value("trace-retry"));
        mvc.perform(delete("/shares/{shareId}/artifact-tasks/{id}", "share-1", "task-1")
                        .queryParam("clientId", "client-1")
                        .queryParam("password", "correct-password"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ARTIFACT_TASK_ACTIVE"))
                .andExpect(jsonPath("$.traceId").value("trace-active"));
    }

    private ArtifactTaskException notFound() {
        return new ArtifactTaskException(
                "ARTIFACT_NOT_FOUND", "产物任务不存在或已过期", "trace-not-found");
    }
}
