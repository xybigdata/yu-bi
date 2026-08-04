package yubi.server.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import yubi.server.artifact.ArtifactDescriptor;
import yubi.server.artifact.ArtifactTaskState;
import yubi.server.artifact.ArtifactTaskWebMapper;
import yubi.server.artifact.TaskHandle;
import yubi.server.service.DownloadService;
import yubi.server.service.VizService;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ArtifactExportCreationWebTest {

    private DownloadService downloadService;
    private VizService vizService;
    private MockMvc downloadMvc;
    private MockMvc vizMvc;

    @BeforeEach
    void setUp() {
        downloadService = mock(DownloadService.class);
        vizService = mock(VizService.class);
        ArtifactTaskWebMapper mapper = new ArtifactTaskWebMapper();
        downloadMvc = MockMvcBuilders.standaloneSetup(new DownloadController(downloadService, mapper)).build();
        vizMvc = MockMvcBuilders.standaloneSetup(new VizController(vizService, mapper)).build();
    }

    @Test
    void 下载创建返回统一产物任务响应且不回显原始请求() throws Exception {
        when(downloadService.submitDownloadTask(any())).thenReturn(handle("销售报表", ".xlsx"));

        downloadMvc.perform(post("/download/submit/task")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orgId": "org-1",
                                  "fileName": "销售报表",
                                  "downloadType": "EXCEL",
                                  "downloadParams": [{"viewId": "view-secret"}]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("task-1"))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.fileName").value("销售报表.xlsx"))
                .andExpect(jsonPath("$.data.acceptedAt").value("2026-07-24T08:00:00Z"))
                .andExpect(jsonPath("$.data.downloadParams").doesNotExist());
    }

    @Test
    void Viz模板创建返回同一产物任务响应() throws Exception {
        when(vizService.exportDatachartTemplate(any())).thenReturn(handle("销售图表", ".ybt"));

        vizMvc.perform(post("/viz/export/datachart/template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orgId":"org-1","datachart":{"id":"chart-1","name":"销售图表"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("task-1"))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.fileName").value("销售图表.ybt"));
    }

    private TaskHandle handle(String displayName, String suffix) {
        return new TaskHandle(
                "task-1",
                new ArtifactDescriptor(displayName, "application/octet-stream", suffix),
                ArtifactTaskState.QUEUED,
                Instant.parse("2026-07-24T08:00:00Z"),
                Instant.parse("2026-07-24T08:15:00Z"),
                "trace-1"
        );
    }
}
