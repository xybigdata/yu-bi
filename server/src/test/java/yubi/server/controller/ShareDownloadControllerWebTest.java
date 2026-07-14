package yubi.server.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import yubi.core.base.consts.ShareAuthenticationMode;
import yubi.server.base.dto.DownloadTaskDTO;
import yubi.server.base.params.ShareVizDetail;
import yubi.server.config.DownloadTaskWebExceptionHandler;
import yubi.server.config.WebExceptionHandler;
import yubi.server.service.ShareDownloadSession;
import yubi.server.service.ShareDownloadSessionManager;
import yubi.server.service.ShareService;
import yubi.server.service.ShareVizAccess;
import yubi.server.service.VizService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ShareDownloadControllerWebTest {

    private ShareService shareService;
    private ShareDownloadSessionManager sessionManager;
    private VizService vizService;
    private MockMvc mvc;
    private ShareDownloadSession session;

    @BeforeEach
    void setUp() {
        shareService = mock(ShareService.class);
        sessionManager = mock(ShareDownloadSessionManager.class);
        vizService = mock(VizService.class);
        mvc = MockMvcBuilders.standaloneSetup(
                        new ShareController(shareService, sessionManager),
                        new ShareDownloadController(shareService, sessionManager),
                        new VizController(vizService),
                        new VizDownloadController(vizService)
                )
                .setControllerAdvice(new DownloadTaskWebExceptionHandler(), new WebExceptionHandler())
                .build();
        session = new ShareDownloadSession(
                "share-1", "a".repeat(64), "b".repeat(64), ShareAuthenticationMode.NONE, null
        );
        when(sessionManager.require(eq("share-1"), any())).thenReturn(session);
    }

    @Test
    void shouldReturnSafeDtoFromShareScopedCreateAndListRoutes() throws Exception {
        DownloadTaskDTO task = new DownloadTaskDTO("task-1", "orders.xlsx", (byte) 0);
        when(shareService.createDownload(eq("share-1"), eq(session), any())).thenReturn(task);
        when(shareService.listDownloadTask("share-1", session)).thenReturn(List.of(task));

        mvc.perform(post("/shares/share-1/download")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fileName":"orders.xlsx","downloadParams":[],"executeToken":{}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("task-1"))
                .andExpect(jsonPath("$.data.name").value("orders.xlsx"))
                .andExpect(jsonPath("$.data.status").value(0))
                .andExpect(jsonPath("$.data.path").doesNotExist())
                .andExpect(jsonPath("$.data.createBy").doesNotExist());

        mvc.perform(get("/shares/share-1/download/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("task-1"))
                .andExpect(jsonPath("$.data[0].path").doesNotExist());
    }

    @Test
    void shouldRejectForgedQueryAndBodyOwnerFieldsBeforeUseCase() throws Exception {
        mvc.perform(post("/shares/share-1/download")
                        .queryParam("clientId", "forged")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fileName":"orders.xlsx","downloadParams":[],"executeToken":{}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("分享下载请求包含不允许的字段"));

        mvc.perform(post("/shares/share-1/download")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fileName":"orders.xlsx","downloadParams":[],"executeToken":{},
                                 "subject":"user-2","organization":"org-2",
                                 "password":"raw-secret","path":"/internal/download"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("下载任务请求参数无效"))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("raw-secret"))
                ))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("/internal/download"))
                ));

        verify(shareService, never()).createDownload(any(), any(), any());
    }

    @Test
    void shouldKeepVizExportOnNarrowSafeRequestBoundary() throws Exception {
        DownloadTaskDTO task = new DownloadTaskDTO("task-2", "dashboard.zip", (byte) 0);
        when(vizService.exportResource(any())).thenReturn(task);

        mvc.perform(post("/viz/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("task-2"))
                .andExpect(jsonPath("$.data.name").value("dashboard.zip"))
                .andExpect(jsonPath("$.data.path").doesNotExist());

        mvc.perform(post("/viz/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"raw-canary\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("下载任务请求参数无效"))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("raw-canary"))
                ));
    }

    @Test
    void shouldRemoveUnsafeLegacyShareDownloadRoutes() throws Exception {
        mvc.perform(post("/shares/download")).andExpect(status().is4xxClientError());
        mvc.perform(get("/shares/download/task")).andExpect(status().is4xxClientError());
        mvc.perform(get("/shares/download")).andExpect(status().is4xxClientError());
        verifyNoInteractions(sessionManager, shareService);
    }

    @Test
    void shouldKeepNonDownloadMalformedJsonOnOriginalErrorSemantics() throws Exception {
        mvc.perform(post("/shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"share-canary\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.not("下载任务请求参数无效")
                ));

        mvc.perform(post("/viz/dashboards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"viz-canary\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.not("下载任务请求参数无效")
                ));

        verify(shareService, never()).createShare(any());
        verify(vizService, never()).createDashboard(any());
    }

    @Test
    void shouldIssueSessionOnlyAfterSuccessfulTopShareAuthorization() throws Exception {
        ShareVizDetail detail = new ShareVizDetail();
        ShareVizAccess access = new ShareVizAccess(
                detail,
                "share-1",
                ShareAuthenticationMode.NONE,
                null,
                "b".repeat(64)
        );
        when(shareService.getShareViz(any(), eq(null))).thenReturn(access);

        mvc.perform(post("/shares/share-1/viz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        verify(sessionManager).issue(
                eq("share-1"),
                eq(ShareAuthenticationMode.NONE),
                eq("b".repeat(64)),
                eq(null),
                any(),
                any()
        );
    }
}
