package yubi.server.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import yubi.agent.api.AgentExecutionContext;
import yubi.agent.api.AgentRequest;
import yubi.agent.api.AgentRunResult;
import yubi.agent.api.AgentUseCase;
import yubi.agent.domain.AgentModels.AgentSession;
import yubi.agent.domain.AgentModels.AgentFailure;
import yubi.agent.domain.AgentModels.AgentStep;
import yubi.agent.domain.AgentModels.ArgumentSummary;
import yubi.agent.domain.AgentModels.ResultSize;
import yubi.agent.domain.AgentModels.FailureCategory;
import yubi.agent.domain.AgentModels.SessionStatus;
import yubi.agent.domain.AgentModels.StepKind;
import yubi.agent.domain.AgentModels.StepStatus;
import yubi.agent.domain.AgentModels.ToolOutput;
import yubi.agent.domain.AgentRuntimePolicy;
import yubi.agent.domain.StructuredValue;
import yubi.agent.domain.ToolResultLimits;
import yubi.query.api.QueryExecutionContext;
import yubi.query.domain.QueryModels.Channel;
import yubi.server.agent.write.ServerAgentWorkspaceSessionService;
import yubi.server.config.AgentWorkspaceRunWebExceptionHandler;
import yubi.server.config.WebExceptionHandler;
import yubi.server.controller.AgentWorkspaceRunController;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgentWorkspaceRunWebContractTest {

    private static final String SESSION_ID = "11111111-1111-1111-1111-111111111111";
    private static final String SESSION_HEADER = AgentWorkspaceRunController.SESSION_HEADER;

    private ServerAgentWorkspaceSessionService sessions;
    private ObjectProvider<AgentUseCase> runtimeProvider;
    private AgentUseCase runtime;
    private AgentExecutionContext context;
    private MockMvc mvc;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        sessions = mock(ServerAgentWorkspaceSessionService.class);
        runtimeProvider = (ObjectProvider<AgentUseCase>) mock(ObjectProvider.class);
        runtime = mock(AgentUseCase.class);
        context = new AgentExecutionContext(SESSION_ID, "request-from-server",
                new QueryExecutionContext(Channel.AUTHENTICATED, "user-1", "org-1", "correlation-1"));
        AgentWorkspaceRunWebMapper mapper = new AgentWorkspaceRunWebMapper(
                AgentRuntimePolicy.defaults(), ToolResultLimits.defaults());
        mvc = MockMvcBuilders.standaloneSetup(
                        new AgentWorkspaceRunController(sessions, runtimeProvider, mapper))
                .setControllerAdvice(new AgentWorkspaceRunWebExceptionHandler(), new WebExceptionHandler())
                .build();
    }

    @Test
    void runMustResumeTrustedSessionAndExposeOnlyFiniteReadOnlyTrace() throws Exception {
        when(sessions.resume("org-1", SESSION_ID)).thenReturn(context);
        when(runtimeProvider.getIfAvailable()).thenReturn(runtime);
        when(runtime.run(any(), same(context))).thenReturn(successfulRun());

        mvc.perform(runRequest("分析 prompt-secret 中的资产"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.runId").value("request-from-server"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.plan.length()").value(2))
                .andExpect(jsonPath("$.data.plan[0].toolName").value("search_data_assets"))
                .andExpect(jsonPath("$.data.steps[0].result.data.assets[0].id").value("view-1"))
                .andExpect(jsonPath("$.data.steps[0].result.data.assets[0].name").value("销售视图"))
                .andExpect(jsonPath("$.data.steps[0].result.data.assets[0].secret").doesNotExist())
                .andExpect(jsonPath("$.data.steps[0].result.data.assets[0].sql").doesNotExist())
                .andExpect(jsonPath("$.data.steps[0].result.data.assets[0].config").doesNotExist())
                .andExpect(jsonPath("$.data.steps[0].result.size.truncated").value(true))
                .andExpect(jsonPath("$.data.resultSize.truncated").value(true))
                .andExpect(jsonPath("$.data.finalAnswer").value("已找到 1 个数据资产"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("prompt-secret"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("create_chart"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("rename_dashboard"))));

        var request = org.mockito.ArgumentCaptor.forClass(AgentRequest.class);
        verify(runtime).run(request.capture(), same(context));
        assertEquals("分析 prompt-secret 中的资产", request.getValue().message());
        verify(sessions).resume("org-1", SESSION_ID);
    }

    @Test
    void describeResultMustNeverExposeScriptOrRawSql() throws Exception {
        when(sessions.resume("org-1", SESSION_ID)).thenReturn(context);
        when(runtimeProvider.getIfAvailable()).thenReturn(runtime);
        when(runtime.run(any(), same(context))).thenReturn(describeRun());

        mvc.perform(runRequest("描述数据资产"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.steps[0].result.data.id").value("view-1"))
                .andExpect(jsonPath("$.data.steps[0].result.data.script").doesNotExist())
                .andExpect(jsonPath("$.data.steps[0].result.size.truncated").value(true))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("select-sensitive-sql"))));
    }

    @Test
    void unavailableRuntimeMustReturnStable503AfterTrustedSessionResume() throws Exception {
        when(sessions.resume("org-1", SESSION_ID)).thenReturn(context);
        when(runtimeProvider.getIfAvailable()).thenReturn(null);

        mvc.perform(runRequest("分析销售数据"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AGENT_RUNTIME_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Agent Runtime 尚未配置"));

        verify(sessions).resume("org-1", SESSION_ID);
        verifyNoInteractions(runtime);
    }

    @Test
    void invalidOrUnknownRequestFieldsMustFailBeforeSessionAndRuntime() throws Exception {
        mvc.perform(post("/agent/workspaces/org-1/runs")
                        .header(SESSION_HEADER, SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"分析\",\"organizationId\":\"org-2\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AGENT_RUN_REQUEST"));
        mvc.perform(post("/agent/workspaces/org-1/runs")
                        .header(SESSION_HEADER, SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AGENT_RUN_REQUEST"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("not-json"))));
        mvc.perform(post("/agent/workspaces/org-1/runs")
                        .header(SESSION_HEADER, SESSION_ID)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("raw-prompt-secret"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("INVALID_AGENT_RUN_REQUEST"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("raw-prompt-secret"))));

        verifyNoInteractions(sessions, runtime);
    }

    @Test
    void missingOrMismatchedSessionMustFailBeforeRuntimeResolution() throws Exception {
        mvc.perform(post("/agent/workspaces/org-1/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"分析\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("WORKSPACE_SESSION_DENIED"));

        when(sessions.resume("org-1", SESSION_ID)).thenThrow(
                new ServerAgentContextAccessDeniedException());
        mvc.perform(runRequest("分析"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("WORKSPACE_ACCESS_DENIED"));

        verifyNoInteractions(runtime);
        verifyNoInteractions(runtimeProvider);
    }

    @Test
    void unexpectedRuntimeFailureMustBeRedacted() throws Exception {
        when(sessions.resume("org-1", SESSION_ID)).thenReturn(context);
        when(runtimeProvider.getIfAvailable()).thenReturn(runtime);
        when(runtime.run(any(), same(context))).thenThrow(
                new IllegalStateException("model-token=raw-sensitive-value"));

        mvc.perform(runRequest("分析"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("AGENT_RUNTIME_FAILED"))
                .andExpect(jsonPath("$.message").value("Agent Runtime 执行失败"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("raw-sensitive-value"))));
    }

    @Test
    void invalidRuntimeResponseMustFailAsFinite502AndNeverExposeWriteTool() throws Exception {
        when(sessions.resume("org-1", SESSION_ID)).thenReturn(context);
        when(runtimeProvider.getIfAvailable()).thenReturn(runtime);
        AgentStep writeStep = new AgentStep(1, StepKind.TOOL_CALL, "create_chart", StepStatus.SUCCEEDED,
                ArgumentSummary.empty(), new ToolOutput(StructuredValue.object(Map.of()),
                new ResultSize(0, 0, 2, 2, 100, 65_536, false)), null, 1);
        when(runtime.run(any(), same(context))).thenReturn(new AgentRunResult(
                new AgentSession(SESSION_ID, "request-from-server", SessionStatus.COMPLETED,
                        List.of(writeStep), "不应返回", null),
                new ResultSize(0, 0, 2, 2, 100, 65_536, false)));

        mvc.perform(runRequest("创建图表"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("INVALID_AGENT_RUN_RESPONSE"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("create_chart"))));

        AgentFailure invalidFailure = new AgentFailure(FailureCategory.INTERNAL, null,
                "raw-sensitive-failure", false);
        when(runtime.run(any(), same(context))).thenReturn(new AgentRunResult(
                new AgentSession(SESSION_ID, "request-from-server", SessionStatus.FAILED,
                        List.of(), null, invalidFailure), ResultSize.empty()));
        mvc.perform(runRequest("分析"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("INVALID_AGENT_RUN_RESPONSE"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("raw-sensitive-failure"))));
    }

    @Test
    void runtimeResponseMustRemainBoundToTheTrustedSessionAndRequest() throws Exception {
        when(sessions.resume("org-1", SESSION_ID)).thenReturn(context);
        when(runtimeProvider.getIfAvailable()).thenReturn(runtime);
        when(runtime.run(any(), same(context))).thenReturn(new AgentRunResult(
                new AgentSession("22222222-2222-2222-2222-222222222222", "request-from-server",
                        SessionStatus.COMPLETED, List.of(), "不应跨会话返回", null),
                ResultSize.empty()));

        mvc.perform(runRequest("分析"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("INVALID_AGENT_RUN_RESPONSE"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("不应跨会话返回"))));

        when(runtime.run(any(), same(context))).thenReturn(new AgentRunResult(
                new AgentSession(SESSION_ID, "other-request", SessionStatus.COMPLETED,
                        List.of(), "不应跨请求返回", null), ResultSize.empty()));
        mvc.perform(runRequest("分析"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("INVALID_AGENT_RUN_RESPONSE"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("不应跨请求返回"))));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder runRequest(String message) {
        return post("/agent/workspaces/org-1/runs")
                .header(SESSION_HEADER, SESSION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"" + message + "\"}");
    }

    private AgentRunResult successfulRun() {
        StructuredValue.ObjectValue asset = StructuredValue.object(Map.of(
                "id", StructuredValue.text("view-1"),
                "name", StructuredValue.text("销售视图"),
                "description", StructuredValue.text("授权后的数据资产"),
                "secret", StructuredValue.text("must-not-cross-web-boundary"),
                "sql", StructuredValue.text("raw-sql-must-not-cross-web-boundary"),
                "config", StructuredValue.text("source-config-must-not-cross-web-boundary")));
        StructuredValue.ObjectValue value = StructuredValue.object(Map.of(
                "assets", StructuredValue.array(List.of(asset))));
        ResultSize size = new ResultSize(1, 1, 180, 160, 100, 65_536, false);
        AgentStep tool = new AgentStep(1, StepKind.TOOL_CALL, "search_data_assets", StepStatus.SUCCEEDED,
                new ArgumentSummary(List.of("query", "limit"), 0, 2, 0, 1),
                new ToolOutput(value, size), null, 12);
        AgentStep answer = new AgentStep(2, StepKind.FINAL_ANSWER, null, StepStatus.SUCCEEDED,
                ArgumentSummary.empty(), null, null, 0);
        AgentSession session = new AgentSession(SESSION_ID, "request-from-server",
                SessionStatus.COMPLETED, List.of(tool, answer), "已找到 1 个数据资产", null);
        return new AgentRunResult(session, size);
    }

    private AgentRunResult describeRun() {
        StructuredValue.ObjectValue value = StructuredValue.object(Map.of(
                "id", StructuredValue.text("view-1"),
                "name", StructuredValue.text("销售视图"),
                "description", StructuredValue.text("说明"),
                "fields", StructuredValue.array(List.of()),
                "variables", StructuredValue.array(List.of()),
                "functions", StructuredValue.array(List.of()),
                "script", StructuredValue.text("select-sensitive-sql")));
        ResultSize size = new ResultSize(0, 0, 160, 150, 100, 65_536, false);
        AgentStep tool = new AgentStep(1, StepKind.TOOL_CALL, "describe_data_asset", StepStatus.SUCCEEDED,
                ArgumentSummary.empty(), new ToolOutput(value, size), null, 8);
        AgentSession session = new AgentSession(SESSION_ID, "request-from-server", SessionStatus.COMPLETED,
                List.of(tool), "描述完成", null);
        return new AgentRunResult(session, size);
    }
}
