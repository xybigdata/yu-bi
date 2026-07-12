package yubi.agent.application;

import org.junit.jupiter.api.Test;
import yubi.agent.api.AgentExecutionContext;
import yubi.agent.api.AgentRequest;
import yubi.agent.api.AgentRunResult;
import yubi.agent.domain.AgentModels.AgentAuditEvent;
import yubi.agent.domain.AgentModels.AgentSession;
import yubi.agent.domain.AgentModels.FailureCategory;
import yubi.agent.domain.AgentModels.SessionStatus;
import yubi.agent.domain.AgentRuntimePolicy;
import yubi.agent.domain.ModelProtocol.FinalAnswer;
import yubi.agent.domain.ModelProtocol.ModelDecision;
import yubi.agent.domain.ModelProtocol.ModelTurn;
import yubi.agent.domain.ModelProtocol.ToolCall;
import yubi.agent.domain.StructuredValue;
import yubi.agent.domain.StructuredValue.ObjectValue;
import yubi.agent.domain.ToolResultLimits;
import yubi.agent.port.ModelGateway;
import yubi.query.api.DataAssetDetail;
import yubi.query.api.DataAssetSummary;
import yubi.query.api.DescribeDataAssetRequest;
import yubi.query.api.ExecuteQueryCommand;
import yubi.query.api.ExecuteQueryUseCase;
import yubi.query.api.QueryAccessDeniedException;
import yubi.query.api.QueryDefinitionException;
import yubi.query.api.QueryExecutionContext;
import yubi.query.api.QueryMetadataUseCase;
import yubi.query.api.QueryResult;
import yubi.query.api.SearchDataAssetsRequest;
import yubi.query.api.SearchDataAssetsResult;
import yubi.query.domain.QueryModels.Channel;
import yubi.query.domain.QueryModels.ColumnMetadata;
import yubi.query.domain.QueryModels.Page;
import yubi.query.domain.QueryModels.ValueType;
import yubi.query.domain.QueryModels.VariableType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultAgentRuntimeTest {

    @Test
    void shouldCompleteSearchDescribeExecuteChainWithFreshAuthorizationOnEveryTool() {
        ScriptedGateway gateway = new ScriptedGateway(List.of(
                new ToolCall("search_data_assets", object("query", text("orders"), "limit", integer(10))),
                new ToolCall("describe_data_asset", object(
                        "assetId", text("view-1"), "includeScript", bool(false))),
                new ToolCall("execute_view", executeArguments("amount")),
                new FinalAnswer("查询完成")));
        Harness harness = harness(gateway, ToolResultLimits.defaults(), AgentRuntimePolicy.defaults());

        AgentRunResult result = harness.runtime.run(new AgentRequest("分析订单"), harness.context);

        assertEquals(SessionStatus.COMPLETED, result.session().status());
        assertEquals("查询完成", result.session().finalAnswer());
        assertEquals(java.util.Arrays.asList("search_data_assets", "describe_data_asset", "execute_view", null),
                result.session().steps().stream().map(value -> value.toolName()).toList());
        assertFalse(result.truncated());
        assertEquals(1, harness.metadata.searchCalls);
        assertEquals(2, harness.metadata.describeCalls);
        assertEquals(1, harness.execute.calls);
        assertTrue(harness.metadata.contexts.stream().allMatch(value -> value == harness.context.queryContext()));
        assertTrue(harness.execute.contexts.stream().allMatch(value -> value == harness.context.queryContext()));
        assertEquals(6, harness.audits.size());
        assertTrue(harness.audits.stream().allMatch(event -> identitiesMatch(event, harness.context)));
        assertEquals(SessionStatus.COMPLETED, harness.snapshots.getLast().status());
        assertFalse(harness.snapshots.toString().contains("查询完成"));
        assertFalse(harness.snapshots.toString().contains("Order facts"));
        assertTrue(harness.snapshots.stream().flatMap(value -> value.steps().stream())
                .filter(value -> value.output() != null)
                .allMatch(value -> value.output().value().values().isEmpty()));
        assertEquals(DefaultReadOnlyToolRegistry.TOOL_NAMES,
                gateway.turns.getFirst().tools().stream().map(value -> value.name()).toList());
    }

    @Test
    void accessDenialMustTerminateAndNeverDegradeToEmptySuccess() {
        ScriptedGateway gateway = new ScriptedGateway(List.of(
                new ToolCall("search_data_assets", object("query", text("secret"))),
                new FinalAnswer("不应执行")));
        Harness harness = harness(gateway, ToolResultLimits.defaults(), AgentRuntimePolicy.defaults());
        harness.metadata.searchOutcomes.add(new QueryAccessDeniedException("sensitive permission detail"));

        AgentRunResult result = harness.runtime.run(new AgentRequest("搜索机密资产"), harness.context);

        assertEquals(SessionStatus.FAILED, result.session().status());
        assertEquals(FailureCategory.ACCESS_DENIED, result.session().failure().category());
        assertEquals(1, gateway.calls.get());
        assertEquals(1, result.session().steps().size());
        assertFalse(result.session().steps().getFirst().failure().recoverable());
        assertEquals(0, harness.execute.calls);
        assertEquals(3, harness.audits.size());
        assertTrue(harness.audits.stream().allMatch(event -> identitiesMatch(event, harness.context)));
        assertEquals(yubi.agent.domain.AgentModels.AuditStatus.FAILED, harness.audits.getLast().status());
    }

    @Test
    void shouldRecoverOneDefinitionFailureAndTerminateAfterRecoveryBudgetIsExhausted() {
        ScriptedGateway recoveringGateway = new ScriptedGateway(List.of(
                new ToolCall("search_data_assets", object("query", text("orders"))),
                new ToolCall("search_data_assets", object("query", text("orders"))),
                new FinalAnswer("已恢复")));
        Harness recovering = harness(recoveringGateway, ToolResultLimits.defaults(),
                new AgentRuntimePolicy(5, 1, 8_000));
        recovering.metadata.searchOutcomes.add(new QueryDefinitionException("secret source config"));
        recovering.metadata.searchOutcomes.add(
                new SearchDataAssetsResult(List.of(new DataAssetSummary("view-1", "Orders", null))));

        AgentRunResult recovered = recovering.runtime.run(new AgentRequest("搜索订单"), recovering.context);

        assertEquals(SessionStatus.COMPLETED, recovered.session().status());
        assertTrue(recovered.session().steps().getFirst().failure().recoverable());
        assertEquals(3, recoveringGateway.calls.get());

        ScriptedGateway failingGateway = new ScriptedGateway(List.of(
                new ToolCall("search_data_assets", object("query", text("orders"))),
                new ToolCall("search_data_assets", object("query", text("orders"))),
                new FinalAnswer("不应执行")));
        Harness failing = harness(failingGateway, ToolResultLimits.defaults(),
                new AgentRuntimePolicy(5, 1, 8_000));
        failing.metadata.searchOutcomes.add(new QueryDefinitionException("failure-one"));
        failing.metadata.searchOutcomes.add(new QueryDefinitionException("failure-two"));

        AgentRunResult failed = failing.runtime.run(new AgentRequest("搜索订单"), failing.context);

        assertEquals(SessionStatus.FAILED, failed.session().status());
        assertEquals(FailureCategory.DEFINITION, failed.session().failure().category());
        assertEquals(2, failingGateway.calls.get());
        assertFalse(failed.session().steps().getLast().failure().recoverable());
    }

    @Test
    void shouldRejectUnknownAndWriteToolsWithoutInvokingCapabilities() {
        ScriptedGateway gateway = new ScriptedGateway(List.of(
                new ToolCall("delete_dashboard", object("dashboardId", text("dashboard-1")))));
        Harness harness = harness(gateway, ToolResultLimits.defaults(), AgentRuntimePolicy.defaults());

        AgentRunResult result = harness.runtime.run(new AgentRequest("删除仪表盘"), harness.context);

        assertEquals(SessionStatus.FAILED, result.session().status());
        assertEquals(FailureCategory.UNKNOWN_TOOL, result.session().failure().category());
        assertEquals("<unregistered>", result.session().steps().getFirst().toolName());
        assertFalse(result.toString().contains("dashboard-1"));
        assertEquals(0, harness.metadata.totalCalls());
        assertEquals(0, harness.execute.calls);
    }

    @Test
    void shouldStopAtMaximumStepsWithoutUnboundedRetry() {
        AtomicInteger calls = new AtomicInteger();
        ModelGateway gateway = turn -> {
            calls.incrementAndGet();
            return new ToolCall("search_data_assets", object("query", text("orders")));
        };
        Harness harness = harness(gateway, ToolResultLimits.defaults(), new AgentRuntimePolicy(2, 1, 8_000));

        AgentRunResult result = harness.runtime.run(new AgentRequest("持续搜索"), harness.context);

        assertEquals(SessionStatus.STEP_LIMIT_REACHED, result.session().status());
        assertEquals(FailureCategory.STEP_LIMIT, result.session().failure().category());
        assertEquals(2, result.session().steps().size());
        assertEquals(2, calls.get());
        assertEquals(2, harness.metadata.searchCalls);
        assertTrue(harness.metadata.contexts.stream().allMatch(value -> value == harness.context.queryContext()));
        assertEquals(yubi.agent.domain.AgentModels.AuditStatus.LIMIT_REACHED,
                harness.audits.getLast().status());
    }

    @Test
    void truncationMustBeVisibleToNextModelTurnAndFinalResult() {
        List<ModelTurn> turns = new ArrayList<>();
        ModelGateway gateway = turn -> {
            turns.add(turn);
            if (turn.stepIndex() == 1) {
                return new ToolCall("execute_view", executeArguments("amount"));
            }
            assertTrue(turn.history().getFirst().output().size().truncated());
            assertEquals(2, turn.history().getFirst().output().size().returnedItems());
            return new FinalAnswer("结果已截断" );
        };
        Harness harness = harness(gateway, new ToolResultLimits(2, 10_000), AgentRuntimePolicy.defaults());
        harness.execute.result = queryResult(List.of(
                List.of(10), List.of(20), List.of(30)));

        AgentRunResult result = harness.runtime.run(new AgentRequest("查询金额"), harness.context);

        assertEquals(SessionStatus.COMPLETED, result.session().status());
        assertTrue(result.truncated());
        assertEquals(2, result.resultSize().maximumItems());
        assertEquals(10_000, result.resultSize().maximumBytes());
        assertEquals(3, result.session().steps().getFirst().output().size().observedItems());
        assertEquals(2, result.session().steps().getFirst().output().size().returnedItems());
        assertEquals(2, turns.size());
        ExecuteQueryCommand command = harness.execute.commands.getFirst();
        assertSame(harness.context.queryContext(), harness.execute.contexts.getFirst());
        assertEquals(3, command.page().pageSize());
        assertFalse(command.includeScript());
        assertTrue(command.functionColumns().isEmpty());
        assertTrue(command.keywords().isEmpty());
    }

    @Test
    void shouldRejectIdentityOrganizationPermissionSqlPreviewSourceAndWriteOverridesBeforeCapabilities() {
        List<String> forbiddenFields = List.of("userId", "organizationId", "roleId", "permissionOverride",
                "sql", "script", "sourceId", "sourceConfig", "preview", "write", "shareToken");
        for (String forbidden : forbiddenFields) {
            ObjectValue arguments = with(executeArguments("amount"), forbidden, text("super-secret-token"));
            ScriptedGateway gateway = new ScriptedGateway(List.of(new ToolCall("execute_view", arguments)));
            Harness harness = harness(gateway, ToolResultLimits.defaults(), AgentRuntimePolicy.defaults());

            AgentRunResult result = harness.runtime.run(new AgentRequest("尝试覆盖边界"), harness.context);

            assertEquals(SessionStatus.FAILED, result.session().status(), forbidden);
            assertEquals(FailureCategory.VALIDATION, result.session().failure().category(), forbidden);
            assertFalse(result.toString().contains("super-secret-token"), forbidden);
            assertFalse(harness.audits.toString().contains("super-secret-token"), forbidden);
            assertEquals(0, harness.metadata.totalCalls());
            assertEquals(0, harness.execute.calls);
        }
    }

    @Test
    void shouldRejectUnauthorizedFieldsAndEmptyQueriesBeforeExecution() {
        ScriptedGateway unauthorizedGateway = new ScriptedGateway(List.of(
                new ToolCall("execute_view", executeArguments("status"))));
        Harness unauthorized = harness(unauthorizedGateway, ToolResultLimits.defaults(), AgentRuntimePolicy.defaults());

        AgentRunResult denied = unauthorized.runtime.run(new AgentRequest("按状态查询"), unauthorized.context);

        assertEquals(SessionStatus.FAILED, denied.session().status());
        assertEquals("UNAUTHORIZED_FIELD", denied.session().failure().code());
        assertEquals(1, unauthorized.metadata.describeCalls);
        assertSame(unauthorized.context.queryContext(), unauthorized.metadata.contexts.getFirst());
        assertEquals(0, unauthorized.execute.calls);

        ScriptedGateway emptyGateway = new ScriptedGateway(List.of(
                new ToolCall("execute_view", object("viewId", text("view-1")))));
        Harness empty = harness(emptyGateway, ToolResultLimits.defaults(), AgentRuntimePolicy.defaults());

        AgentRunResult emptyResult = empty.runtime.run(new AgentRequest("空查询"), empty.context);

        assertEquals(SessionStatus.FAILED, emptyResult.session().status());
        assertEquals(FailureCategory.VALIDATION, emptyResult.session().failure().category());
        assertEquals(0, empty.metadata.totalCalls());
        assertEquals(0, empty.execute.calls);
    }

    @Test
    void shouldRejectSqlLikeAliasesUnsafeValueTypesAndPermissionVariables() {
        ObjectValue aliasInjection = object(
                "viewId", text("view-1"),
                "columns", array(object(
                        "alias", text("amount; select * from source"),
                        "path", array(text("orders"), text("amount")))));
        Harness alias = harness(new ScriptedGateway(List.of(
                new ToolCall("execute_view", aliasInjection))),
                ToolResultLimits.defaults(), AgentRuntimePolicy.defaults());
        AgentRunResult aliasResult = alias.runtime.run(new AgentRequest("别名注入"), alias.context);
        assertEquals(FailureCategory.VALIDATION, aliasResult.session().failure().category());
        assertEquals(0, alias.metadata.totalCalls());
        assertEquals(0, alias.execute.calls);

        ObjectValue unsafeValueType = object(
                "viewId", text("view-1"),
                "columns", array(object("path", array(text("orders"), text("amount")))),
                "filters", array(object(
                        "operator", text("EQ"),
                        "column", array(text("orders"), text("amount")),
                        "values", array(object("value", text("raw fragment"),
                                "valueType", text("SNIPPET"))))));
        Harness unsafe = harness(new ScriptedGateway(List.of(
                new ToolCall("execute_view", unsafeValueType))),
                ToolResultLimits.defaults(), AgentRuntimePolicy.defaults());
        AgentRunResult unsafeResult = unsafe.runtime.run(new AgentRequest("片段注入"), unsafe.context);
        assertEquals(FailureCategory.VALIDATION, unsafeResult.session().failure().category());
        assertEquals(0, unsafe.metadata.totalCalls());
        assertEquals(0, unsafe.execute.calls);

        ObjectValue permissionParameter = object(
                "viewId", text("view-1"),
                "columns", array(object("path", array(text("orders"), text("amount")))),
                "parameters", array(object("name", text("TENANT"),
                        "values", array(text("other-tenant")))));
        Harness permission = harness(new ScriptedGateway(List.of(
                new ToolCall("execute_view", permissionParameter))),
                ToolResultLimits.defaults(), AgentRuntimePolicy.defaults());
        permission.metadata.describeResult = new DataAssetDetail(
                "view-1", "Orders", null, detail("amount").fields(),
                List.of(new DataAssetDetail.VariableDescription("TENANT", null, VariableType.PERMISSION,
                        ValueType.STRING, false, false, null, DataAssetDetail.VariableScope.VIEW)),
                List.of(), Optional.empty());

        AgentRunResult permissionResult = permission.runtime.run(
                new AgentRequest("覆盖权限变量"), permission.context);

        assertEquals(SessionStatus.FAILED, permissionResult.session().status());
        assertEquals("UNAUTHORIZED_PARAMETER", permissionResult.session().failure().code());
        assertEquals(1, permission.metadata.describeCalls);
        assertEquals(0, permission.execute.calls);
    }

    @Test
    void shouldRejectOverlyDeepModelArgumentsDeterministically() {
        StructuredValue nested = text("value");
        for (int index = 0; index < 6; index++) {
            nested = object("nested", nested);
        }
        Harness harness = harness(new ScriptedGateway(List.of(
                        new ToolCall("search_data_assets", object("query", nested)))),
                ToolResultLimits.defaults(), new AgentRuntimePolicy(3, 0, 8_000, 100, 4));

        AgentRunResult result = harness.runtime.run(new AgentRequest("复杂参数"), harness.context);

        assertEquals(SessionStatus.FAILED, result.session().status());
        assertEquals("TOOL_INPUT_TOO_COMPLEX", result.session().failure().code());
        assertEquals(0, harness.metadata.totalCalls());
    }

    @Test
    void unexpectedToolRuntimeFailureMustTerminateAsInternalWithoutRecovery() {
        ScriptedGateway gateway = new ScriptedGateway(List.of(
                new ToolCall("search_data_assets", object("query", text("orders"))),
                new FinalAnswer("不应执行")));
        Harness harness = harness(gateway, ToolResultLimits.defaults(), AgentRuntimePolicy.defaults());
        harness.metadata.searchOutcomes.add(new NullPointerException("sensitive implementation detail"));

        AgentRunResult result = harness.runtime.run(new AgentRequest("搜索订单"), harness.context);

        assertEquals(SessionStatus.FAILED, result.session().status());
        assertEquals(FailureCategory.INTERNAL, result.session().failure().category());
        assertEquals("INTERNAL_TOOL_FAILURE", result.session().failure().code());
        assertFalse(result.session().failure().recoverable());
        assertEquals(1, gateway.calls.get());
        assertEquals(1, result.session().steps().size());
        assertFalse(result.toString().contains("sensitive implementation detail"));
    }

    @Test
    void fatalModelAndToolErrorsMustPersistAndAuditFailedTerminalStateBeforeRethrow() {
        AssertionError modelError = new AssertionError("model-fatal-secret");
        Harness model = harness(turn -> {
            throw modelError;
        }, ToolResultLimits.defaults(), AgentRuntimePolicy.defaults());

        assertSame(modelError, assertThrows(AssertionError.class,
                () -> model.runtime.run(new AgentRequest("触发模型错误"), model.context)));
        assertEquals(SessionStatus.FAILED, model.snapshots.getLast().status());
        assertEquals(FailureCategory.INTERNAL, model.snapshots.getLast().failure().category());
        assertEquals(yubi.agent.domain.AgentModels.AuditEventType.SESSION_COMPLETED,
                model.audits.getLast().eventType());
        assertEquals(yubi.agent.domain.AgentModels.AuditStatus.FAILED, model.audits.getLast().status());

        AssertionError toolError = new AssertionError("tool-fatal-secret");
        Harness tool = harness(new ScriptedGateway(List.of(
                        new ToolCall("search_data_assets", object("query", text("orders"))))),
                ToolResultLimits.defaults(), AgentRuntimePolicy.defaults());
        tool.metadata.searchOutcomes.add(toolError);

        assertSame(toolError, assertThrows(AssertionError.class,
                () -> tool.runtime.run(new AgentRequest("触发工具错误"), tool.context)));
        assertEquals(SessionStatus.FAILED, tool.snapshots.getLast().status());
        assertEquals(FailureCategory.INTERNAL, tool.snapshots.getLast().failure().category());
        assertEquals(yubi.agent.domain.AgentModels.AuditEventType.SESSION_COMPLETED,
                tool.audits.getLast().eventType());
        assertEquals(yubi.agent.domain.AgentModels.AuditStatus.FAILED, tool.audits.getLast().status());
        assertFalse(tool.audits.toString().contains("tool-fatal-secret"));
    }

    @Test
    void auditMustCoverSuccessAndFailureWithoutRawArgumentsOrResults() {
        String secret = "password=hunter2 token=top-secret";
        ScriptedGateway gateway = new ScriptedGateway(List.of(
                new ToolCall("search_data_assets", object("query", text(secret))),
                new FinalAnswer("完成")));
        Harness harness = harness(gateway, ToolResultLimits.defaults(), AgentRuntimePolicy.defaults());

        AgentRunResult result = harness.runtime.run(new AgentRequest("安全审计"), harness.context);

        assertEquals(SessionStatus.COMPLETED, result.session().status());
        assertEquals(4, harness.audits.size());
        assertEquals(List.of("query"), harness.audits.get(1).arguments().recognizedFields());
        assertFalse(harness.audits.toString().contains(secret));
        assertFalse(harness.audits.toString().contains("Orders"));
        assertTrue(harness.audits.stream().allMatch(event -> identitiesMatch(event, harness.context)));
        assertEquals(yubi.agent.domain.AgentModels.AuditEventType.SESSION_STARTED,
                harness.audits.getFirst().eventType());
        assertEquals(yubi.agent.domain.AgentModels.AuditEventType.SESSION_COMPLETED,
                harness.audits.getLast().eventType());
    }

    private Harness harness(ModelGateway gateway, ToolResultLimits limits, AgentRuntimePolicy policy) {
        MutableMetadataUseCase metadata = new MutableMetadataUseCase();
        MutableExecuteQueryUseCase execute = new MutableExecuteQueryUseCase();
        metadata.searchResult = new SearchDataAssetsResult(
                List.of(new DataAssetSummary("view-1", "Orders", "Order facts")));
        metadata.describeResult = detail("amount");
        execute.result = queryResult(List.of(List.of(42)));

        var registry = new DefaultReadOnlyToolRegistry(List.of(
                new SearchDataAssetsTool(metadata, limits),
                new DescribeDataAssetTool(metadata, limits),
                new ExecuteViewTool(execute, metadata, limits)));
        List<AgentAuditEvent> audits = new ArrayList<>();
        List<AgentSession> snapshots = new ArrayList<>();
        AtomicLong time = new AtomicLong();
        AgentExecutionContext context = new AgentExecutionContext("session-1", "request-1",
                new QueryExecutionContext(Channel.AUTHENTICATED, "user-1", "org-1", "correlation-1"));
        DefaultAgentRuntime runtime = new DefaultAgentRuntime(gateway, registry, audits::add, snapshots::add,
                () -> time.getAndAdd(2_000_000L), policy);
        return new Harness(runtime, metadata, execute, context, audits, snapshots);
    }

    private DataAssetDetail detail(String... fields) {
        return new DataAssetDetail("view-1", "Orders", "Order facts",
                java.util.Arrays.stream(fields).map(name -> new DataAssetDetail.FieldDescription(
                        List.of("orders", name), "orders." + name, ValueType.NUMERIC, null)).toList(),
                List.of(), List.of(), Optional.empty());
    }

    private QueryResult queryResult(List<List<Object>> rows) {
        return new QueryResult("result-1", "Orders", null, null,
                List.of(new ColumnMetadata(List.of("orders", "amount"), ValueType.NUMERIC, null, List.of())),
                rows, new Page(1, rows.size(), rows.size(), true), null);
    }

    private ObjectValue executeArguments(String field) {
        return object(
                "viewId", text("view-1"),
                "columns", array(object("path", array(text("orders"), text(field)))));
    }

    private static boolean identitiesMatch(AgentAuditEvent event, AgentExecutionContext context) {
        return event.sessionId().equals(context.sessionId())
                && event.requestId().equals(context.requestId())
                && event.subjectId().equals(context.queryContext().subjectId())
                && event.organizationId().equals(context.queryContext().organizationId())
                && event.correlationId().equals(context.queryContext().correlationId());
    }

    private ObjectValue object(Object... entries) {
        Map<String, StructuredValue> values = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            values.put((String) entries[index], (StructuredValue) entries[index + 1]);
        }
        return new ObjectValue(values);
    }

    private ObjectValue with(ObjectValue source, String key, StructuredValue value) {
        Map<String, StructuredValue> values = new LinkedHashMap<>(source.values());
        values.put(key, value);
        return new ObjectValue(values);
    }

    private StructuredValue.ArrayValue array(StructuredValue... values) {
        return new StructuredValue.ArrayValue(List.of(values));
    }

    private StructuredValue.TextValue text(String value) {
        return new StructuredValue.TextValue(value);
    }

    private StructuredValue.IntegerValue integer(long value) {
        return new StructuredValue.IntegerValue(value);
    }

    private StructuredValue.BooleanValue bool(boolean value) {
        return new StructuredValue.BooleanValue(value);
    }

    private static final class ScriptedGateway implements ModelGateway {
        private final ArrayDeque<ModelDecision> decisions;
        private final List<ModelTurn> turns = new ArrayList<>();
        private final AtomicInteger calls = new AtomicInteger();

        private ScriptedGateway(List<ModelDecision> decisions) {
            this.decisions = new ArrayDeque<>(decisions);
        }

        @Override
        public ModelDecision next(ModelTurn turn) {
            calls.incrementAndGet();
            turns.add(turn);
            return decisions.removeFirst();
        }
    }

    private record Harness(DefaultAgentRuntime runtime,
                           MutableMetadataUseCase metadata,
                           MutableExecuteQueryUseCase execute,
                           AgentExecutionContext context,
                           List<AgentAuditEvent> audits,
                           List<AgentSession> snapshots) {
    }

    private static final class MutableMetadataUseCase implements QueryMetadataUseCase {
        private final ArrayDeque<Object> searchOutcomes = new ArrayDeque<>();
        private final List<QueryExecutionContext> contexts = new ArrayList<>();
        private SearchDataAssetsResult searchResult;
        private DataAssetDetail describeResult;
        private int searchCalls;
        private int describeCalls;

        @Override
        public SearchDataAssetsResult search(SearchDataAssetsRequest request, QueryExecutionContext context) {
            searchCalls++;
            contexts.add(context);
            Object outcome = searchOutcomes.pollFirst();
            if (outcome instanceof RuntimeException failure) {
                throw failure;
            }
            if (outcome instanceof Error failure) {
                throw failure;
            }
            return outcome instanceof SearchDataAssetsResult result ? result : searchResult;
        }

        @Override
        public DataAssetDetail describe(DescribeDataAssetRequest request, QueryExecutionContext context) {
            describeCalls++;
            contexts.add(context);
            return describeResult;
        }

        private int totalCalls() {
            return searchCalls + describeCalls;
        }
    }

    private static final class MutableExecuteQueryUseCase implements ExecuteQueryUseCase {
        private final List<ExecuteQueryCommand> commands = new ArrayList<>();
        private final List<QueryExecutionContext> contexts = new ArrayList<>();
        private QueryResult result;
        private int calls;

        @Override
        public QueryResult execute(ExecuteQueryCommand command, QueryExecutionContext context) {
            calls++;
            commands.add(command);
            contexts.add(context);
            return result;
        }
    }
}
