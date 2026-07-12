package yubi.server.agent.write;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import yubi.agent.api.AgentExecutionContext;
import yubi.agent.api.AgentUseCase;
import yubi.agent.api.ControlledWriteException;
import yubi.agent.domain.ControlledWriteModels.PreviewField;
import yubi.agent.domain.ControlledWriteModels.WriteFailureCategory;
import yubi.agent.domain.ControlledWriteModels.WriteOperationState;
import yubi.agent.domain.ControlledWriteModels.WriteOperationView;
import yubi.agent.domain.ControlledWriteModels.WritePreview;
import yubi.agent.domain.ControlledWriteModels.WriteProposalCommand;
import yubi.agent.domain.StructuredValue;
import yubi.agent.port.WriteProposalToolRegistry;
import yubi.query.api.QueryExecutionContext;
import yubi.query.domain.QueryModels.Channel;
import yubi.server.agent.ServerAgentContextAccessDeniedException;
import yubi.server.config.ControlledWriteWebExceptionHandler;
import yubi.server.config.WebExceptionHandler;
import yubi.server.controller.AgentWorkspaceSessionController;
import yubi.server.controller.ControlledWriteController;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ControlledWriteWebContractTest {

    private static final String SESSION_ID = "11111111-1111-1111-1111-111111111111";
    private static final String SESSION_HEADER = ControlledWriteController.SESSION_HEADER;
    private static final String IDEMPOTENCY_HEADER = ControlledWriteController.IDEMPOTENCY_HEADER;

    private ServerAgentWorkspaceSessionService sessions;
    private ServerControlledWriteTransactionService writes;
    private ObjectProvider<AgentUseCase> runtime;
    private WriteProposalToolRegistry registry;
    private MockMvc mvc;
    private AgentExecutionContext context;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        sessions = mock(ServerAgentWorkspaceSessionService.class);
        writes = mock(ServerControlledWriteTransactionService.class);
        runtime = (ObjectProvider<AgentUseCase>) mock(ObjectProvider.class);
        registry = new ControlledWriteConfiguration().writeProposalToolRegistry();
        AgentWorkspaceWriteWebMapper mapper = new AgentWorkspaceWriteWebMapper(new StructuredValueWebMapper());
        mvc = MockMvcBuilders.standaloneSetup(
                        new AgentWorkspaceSessionController(sessions, runtime, registry),
                        new ControlledWriteController(sessions, writes, mapper))
                .setControllerAdvice(new ControlledWriteWebExceptionHandler(), new WebExceptionHandler())
                .build();
        context = context("user-1", "org-1", SESSION_ID);
    }

    @Test
    void sessionEndpointMustExposeOnlyTheExactServerWriteAllowlist() throws Exception {
        Instant expiresAt = Instant.parse("2099-07-12T10:00:00Z");
        when(sessions.create("org-1")).thenReturn(
                new AgentWorkspaceSession(SESSION_ID, "user-1", "org-1", expiresAt));
        when(runtime.getIfAvailable()).thenReturn(null);

        mvc.perform(post("/agent/workspaces/org-1/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionId").value(SESSION_ID))
                .andExpect(jsonPath("$.data.modelRuntimeAvailable").value(false))
                .andExpect(jsonPath("$.data.writableTools.length()").value(2))
                .andExpect(jsonPath("$.data.writableTools[0]").value("create_chart"))
                .andExpect(jsonPath("$.data.writableTools[1]").value("rename_dashboard"));

        verify(sessions).create("org-1");
        assertEquals(List.of("create_chart", "rename_dashboard"),
                registry.schemas().stream().map(value -> value.name()).toList());
    }

    @Test
    void previewMustMapOnlyBodyArgumentsAndDedicatedHeaders() throws Exception {
        when(sessions.resume("org-1", SESSION_ID)).thenReturn(context);
        when(writes.propose(any(), any())).thenReturn(pending("create_chart"));

        mvc.perform(post("/agent/workspaces/org-1/writes/previews")
                        .header(SESSION_HEADER, SESSION_ID)
                        .header(IDEMPOTENCY_HEADER, "idempotency-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toolName":"create_chart","arguments":{"name":"销售图表","viewId":"view-1"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionId").value(SESSION_ID))
                .andExpect(jsonPath("$.data.toolName").value("create_chart"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.duplicate").value(false));

        var command = org.mockito.ArgumentCaptor.forClass(WriteProposalCommand.class);
        verify(writes).propose(command.capture(), org.mockito.ArgumentMatchers.same(context));
        assertEquals("create_chart", command.getValue().toolName());
        assertEquals("idempotency-secret", command.getValue().idempotencyKey());
        assertEquals(StructuredValue.text("销售图表"), command.getValue().arguments().values().get("name"));
        assertEquals(StructuredValue.text("view-1"), command.getValue().arguments().values().get("viewId"));
    }

    @Test
    void everyControlledEndpointMustRequireItsDedicatedHeaders() throws Exception {
        String body = "{\"toolName\":\"create_chart\",\"arguments\":{\"name\":\"图表\",\"viewId\":\"view-1\"}}";
        mvc.perform(post("/agent/workspaces/org-1/writes/previews")
                        .header(IDEMPOTENCY_HEADER, "key-1")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("WORKSPACE_SESSION_DENIED"));
        mvc.perform(post("/agent/workspaces/org-1/writes/previews")
                        .header(SESSION_HEADER, SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WRITE_REQUEST"));
        mvc.perform(get("/agent/workspaces/org-1/approvals"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("WORKSPACE_SESSION_DENIED"));
        mvc.perform(post("/agent/workspaces/org-1/approvals/approval-1/approve"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("WORKSPACE_SESSION_DENIED"));
        mvc.perform(post("/agent/workspaces/org-1/approvals/approval-1/reject"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("WORKSPACE_SESSION_DENIED"));

        verifyNoInteractions(sessions, writes);
    }

    @Test
    void unknownMalformedOrUnsupportedInputsMustFailBeforeSessionOrWriteCalls() throws Exception {
        mvc.perform(post("/agent/workspaces/org-1/writes/previews")
                        .header(SESSION_HEADER, SESSION_ID)
                        .header(IDEMPOTENCY_HEADER, "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toolName":"create_chart","arguments":{},"organizationId":"org-2"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WRITE_REQUEST"))
                .andExpect(jsonPath("$.message").value("受控写请求参数无效"));
        mvc.perform(post("/agent/workspaces/org-1/writes/previews")
                        .header(SESSION_HEADER, SESSION_ID)
                        .header(IDEMPOTENCY_HEADER, "key-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WRITE_REQUEST"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("not-json"))));
        mvc.perform(post("/agent/workspaces/org-1/writes/previews")
                        .header(SESSION_HEADER, SESSION_ID)
                        .header(IDEMPOTENCY_HEADER, "key-3")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("payload-secret"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("INVALID_WRITE_REQUEST"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("payload-secret"))));

        verifyNoInteractions(sessions, writes);
    }

    @Test
    void approvalDecisionsMustRejectEveryRequestBodyBeforeAnySideEffect() throws Exception {
        for (String decision : List.of("approve", "reject")) {
            mvc.perform(post("/agent/workspaces/org-1/approvals/approval-1/" + decision)
                            .header(SESSION_HEADER, SESSION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newName\":\"tampered-value\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_WRITE_REQUEST"))
                    .andExpect(content().string(org.hamcrest.Matchers.not(
                            org.hamcrest.Matchers.containsString("tampered-value"))));
        }

        verifyNoInteractions(sessions, writes);
    }

    @Test
    void currentOrganizationAndSessionScopeFailuresMustRemainForbiddenAndFinite() throws Exception {
        when(sessions.create("org-2")).thenThrow(new ServerAgentContextAccessDeniedException());
        mvc.perform(post("/agent/workspaces/org-2/sessions"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("WORKSPACE_ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Agent 工作区不可用"));

        when(sessions.resume("org-2", SESSION_ID)).thenThrow(new ServerAgentContextAccessDeniedException());
        mvc.perform(get("/agent/workspaces/org-2/approvals").header(SESSION_HEADER, SESSION_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("WORKSPACE_ACCESS_DENIED"));

        String otherSession = "22222222-2222-2222-2222-222222222222";
        when(sessions.resume("org-1", otherSession)).thenThrow(
                new AgentWorkspaceSessionException("WORKSPACE_SESSION_DENIED", "Agent 工作区会话不可用"));
        mvc.perform(get("/agent/workspaces/org-1/approvals").header(SESSION_HEADER, otherSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("WORKSPACE_SESSION_DENIED"));

        String expiredSession = "33333333-3333-3333-3333-333333333333";
        when(sessions.resume("org-1", expiredSession)).thenThrow(
                new AgentWorkspaceSessionException("WORKSPACE_SESSION_EXPIRED", "Agent 工作区会话已过期"));
        mvc.perform(get("/agent/workspaces/org-1/approvals").header(SESSION_HEADER, expiredSession))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("WORKSPACE_SESSION_EXPIRED"));

        verifyNoInteractions(writes);
    }

    @ParameterizedTest
    @MethodSource("controlledFailures")
    void controlledFailureCodesMustMapToStableStatuses(ControlledWriteException.Code code,
                                                       int expectedStatus) throws Exception {
        when(sessions.resume("org-1", SESSION_ID)).thenReturn(context);
        when(writes.approve("approval-1", context)).thenThrow(new ControlledWriteException(code, "有限错误"));

        mvc.perform(decisionRequest("approve"))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(code.name()))
                .andExpect(jsonPath("$.message").value("有限错误"));
    }

    @ParameterizedTest
    @MethodSource("executionFailures")
    void executionFailureCategoriesMustMapToStableStatuses(WriteFailureCategory category,
                                                            int expectedStatus) throws Exception {
        when(sessions.resume("org-1", SESSION_ID)).thenReturn(context);
        when(writes.approve("approval-1", context)).thenThrow(
                new ServerControlledWriteExecutionException("SAFE_EXECUTION_CODE", "有限错误", category));

        mvc.perform(decisionRequest("approve"))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.code").value("SAFE_EXECUTION_CODE"))
                .andExpect(jsonPath("$.message").value("有限错误"));
    }

    @Test
    void unexpectedFailuresMustBeRedactedByTheDedicatedAdvice() throws Exception {
        when(sessions.resume("org-1", SESSION_ID)).thenReturn(context);
        when(writes.listOperations(context)).thenThrow(
                new IllegalStateException("jdbc:password=raw-sensitive-value"));

        mvc.perform(get("/agent/workspaces/org-1/approvals").header(SESSION_HEADER, SESSION_ID))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("CONTROLLED_WRITE_INTERNAL"))
                .andExpect(jsonPath("$.message").value("受控写服务暂时不可用"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("raw-sensitive-value"))));
    }

    private MockHttpServletRequestBuilder decisionRequest(String decision) {
        return post("/agent/workspaces/org-1/approvals/approval-1/" + decision)
                .header(SESSION_HEADER, SESSION_ID);
    }

    private WriteOperationView pending(String toolName) {
        WritePreview preview = new WritePreview("创建图表", "CHART", "view-1",
                List.of(new PreviewField("图表名称", "销售图表")), List.of("创建未发布图表"));
        return new WriteOperationView("approval-1", "change-1", toolName,
                WriteOperationState.PENDING, preview, null, null,
                Instant.parse("2099-07-12T09:00:00Z"),
                Instant.parse("2099-07-12T09:15:00Z"), null, false);
    }

    private AgentExecutionContext context(String subject, String organization, String session) {
        return new AgentExecutionContext(session, "request-1",
                new QueryExecutionContext(Channel.AUTHENTICATED, subject, organization, "correlation-1"));
    }

    private static Stream<Arguments> controlledFailures() {
        return Stream.of(
                Arguments.of(ControlledWriteException.Code.UNKNOWN_WRITE_TOOL, 400),
                Arguments.of(ControlledWriteException.Code.INVALID_WRITE_REQUEST, 400),
                Arguments.of(ControlledWriteException.Code.APPROVAL_NOT_FOUND, 404),
                Arguments.of(ControlledWriteException.Code.APPROVAL_SCOPE_MISMATCH, 403),
                Arguments.of(ControlledWriteException.Code.APPROVAL_TAMPERED, 403),
                Arguments.of(ControlledWriteException.Code.IDEMPOTENCY_CONFLICT, 409),
                Arguments.of(ControlledWriteException.Code.APPROVAL_STATE_INVALID, 409),
                Arguments.of(ControlledWriteException.Code.INVALID_BUSINESS_PREPARATION, 422),
                Arguments.of(ControlledWriteException.Code.INVALID_BUSINESS_RESULT, 502));
    }

    private static Stream<Arguments> executionFailures() {
        return Stream.of(
                Arguments.of(WriteFailureCategory.ACCESS_DENIED, 403),
                Arguments.of(WriteFailureCategory.VALIDATION, 422),
                Arguments.of(WriteFailureCategory.CONFLICT, 409),
                Arguments.of(WriteFailureCategory.EXECUTION, 502),
                Arguments.of(WriteFailureCategory.INTERNAL, 502));
    }
}
