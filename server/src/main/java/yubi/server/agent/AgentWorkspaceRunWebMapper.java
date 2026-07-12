package yubi.server.agent;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import yubi.agent.api.AgentRequest;
import yubi.agent.api.AgentRunResult;
import yubi.agent.api.AgentExecutionContext;
import yubi.agent.domain.AgentModels.AgentFailure;
import yubi.agent.domain.AgentModels.AgentStep;
import yubi.agent.domain.AgentModels.ResultSize;
import yubi.agent.domain.AgentModels.SessionStatus;
import yubi.agent.domain.AgentModels.StepKind;
import yubi.agent.domain.AgentModels.StepStatus;
import yubi.agent.domain.AgentRuntimePolicy;
import yubi.agent.domain.StructuredValue;
import yubi.agent.domain.StructuredValue.ArrayValue;
import yubi.agent.domain.StructuredValue.BooleanValue;
import yubi.agent.domain.StructuredValue.IntegerValue;
import yubi.agent.domain.StructuredValue.NullValue;
import yubi.agent.domain.StructuredValue.ObjectValue;
import yubi.agent.domain.StructuredValue.TextValue;
import yubi.agent.domain.ToolResultLimits;
import yubi.server.agent.AgentWorkspaceRunException.Code;
import yubi.server.agent.AgentWorkspaceRunWebModels.RunDataResult;
import yubi.server.agent.AgentWorkspaceRunWebModels.RunFailure;
import yubi.server.agent.AgentWorkspaceRunWebModels.RunPlanStep;
import yubi.server.agent.AgentWorkspaceRunWebModels.RunResponse;
import yubi.server.agent.AgentWorkspaceRunWebModels.RunResultSize;
import yubi.server.agent.AgentWorkspaceRunWebModels.RunStep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public final class AgentWorkspaceRunWebMapper {

    private static final String UNREGISTERED_TOOL = "<unregistered>";
    private static final int MAXIMUM_DATA_DEPTH = 24;
    private static final int MAXIMUM_DATA_NODES = 20_000;
    private static final int MAXIMUM_DATA_TEXT_LENGTH = 16_384;
    private static final Set<String> REQUEST_FIELDS = Set.of("message");
    private static final Set<String> READ_ONLY_TOOLS = Set.of(
            "search_data_assets", "describe_data_asset", "execute_view");
    private static final Map<String, Set<String>> TOOL_OUTPUT_FIELDS = Map.of(
            "search_data_assets", Set.of("assets"),
            "describe_data_asset", Set.of("id", "name", "description", "fields", "variables",
                    "functions", "script"),
            "execute_view", Set.of("id", "name", "visualizationType", "visualizationId", "columns",
                    "rows", "page"));
    private static final Map<String, Set<String>> TOOL_REQUIRED_OUTPUT_FIELDS = Map.of(
            "search_data_assets", Set.of("assets"),
            "describe_data_asset", Set.of("id", "name", "fields", "variables", "functions"),
            "execute_view", Set.of("columns", "rows"));
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "script", "sql", "rawsql", "password", "token", "secret", "credential", "credentials",
            "source", "sourceconfig", "connection", "jdbcurl", "config");

    private final AgentRuntimePolicy policy;
    private final ToolResultLimits resultLimits;

    public AgentWorkspaceRunWebMapper(AgentRuntimePolicy policy, ToolResultLimits resultLimits) {
        this.policy = java.util.Objects.requireNonNull(policy, "policy");
        this.resultLimits = java.util.Objects.requireNonNull(resultLimits, "resultLimits");
    }

    public AgentRequest request(JsonNode request) {
        if (request == null || !request.isObject() || !exactFields(request, REQUEST_FIELDS)) {
            throw invalidRequest();
        }
        JsonNode message = request.get("message");
        if (message == null || !message.isString() || message.stringValue() == null
                || message.stringValue().isBlank() || message.stringValue().length() > policy.maximumPromptLength()) {
            throw invalidRequest();
        }
        return new AgentRequest(message.stringValue());
    }

    public RunResponse response(AgentRunResult result, AgentExecutionContext trustedContext) {
        if (trustedContext == null) {
            throw invalidResponse();
        }
        if (result == null || result.session() == null || result.session().requestId() == null
                || result.session().requestId().isBlank() || result.session().requestId().length() > 128
                || !trustedContext.sessionId().equals(result.session().sessionId())
                || !trustedContext.requestId().equals(result.session().requestId())
                || result.session().status() == null || result.session().status() == SessionStatus.RUNNING) {
            throw invalidResponse();
        }
        List<RunPlanStep> plan = new ArrayList<>();
        List<RunStep> steps = new ArrayList<>();
        int previousIndex = -1;
        for (AgentStep step : result.session().steps()) {
            if (step == null || step.index() < 0 || step.index() <= previousIndex
                    || step.kind() == null || step.status() == null || step.durationMillis() < 0) {
                throw invalidResponse();
            }
            previousIndex = step.index();
            String toolName = safeToolName(step);
            RunFailure failure = failure(step.failure());
            if ((step.status() == StepStatus.SUCCEEDED && failure != null)
                    || (step.status() == StepStatus.FAILED && failure == null)) {
                throw invalidResponse();
            }
            RunDataResult dataResult = dataResult(step, toolName);
            plan.add(new RunPlanStep(step.index(), step.kind().name(), toolName, step.status().name()));
            steps.add(new RunStep(step.index(), step.kind().name(), toolName, step.status().name(),
                    step.durationMillis(), dataResult, failure));
        }
        String finalAnswer = result.session().finalAnswer();
        if (finalAnswer != null && (finalAnswer.isBlank()
                || finalAnswer.length() > policy.maximumPromptLength() * 2L)) {
            throw invalidResponse();
        }
        if ((result.session().status() == SessionStatus.COMPLETED && finalAnswer == null)
                || (result.session().status() != SessionStatus.COMPLETED && finalAnswer != null)
                || (result.session().status() == SessionStatus.COMPLETED && result.session().failure() != null)
                || (result.session().status() != SessionStatus.COMPLETED && result.session().failure() == null)) {
            throw invalidResponse();
        }
        boolean webTruncated = steps.stream()
                .anyMatch(step -> step.result() != null && step.result().size().truncated());
        return new RunResponse(result.session().requestId(), result.session().status().name(),
                List.copyOf(plan), List.copyOf(steps), finalAnswer, failure(result.session().failure()),
                size(result.resultSize(), false, webTruncated));
    }

    private RunDataResult dataResult(AgentStep step, String toolName) {
        if (step.output() == null) {
            return null;
        }
        if (step.kind() != StepKind.TOOL_CALL || step.status() != StepStatus.SUCCEEDED
                || !READ_ONLY_TOOLS.contains(toolName)
                || step.output().value() == null) {
            throw invalidResponse();
        }
        Set<String> allowedFields = TOOL_OUTPUT_FIELDS.get(toolName);
        Set<String> actualFields = step.output().value().values().keySet();
        if (!allowedFields.containsAll(actualFields)
                || !actualFields.containsAll(TOOL_REQUIRED_OUTPUT_FIELDS.get(toolName))) {
            throw invalidResponse();
        }
        Counter counter = new Counter();
        Map<String, Object> data = new LinkedHashMap<>();
        step.output().value().values().forEach((key, value) -> {
            if (!sensitive(key)) {
                data.put(key, plain(value, 0, counter));
            } else {
                counter.redacted = true;
            }
        });
        return new RunDataResult(Collections.unmodifiableMap(data),
                size(step.output().size(), true, counter.redacted));
    }

    private Object plain(StructuredValue value, int depth, Counter counter) {
        if (value == null || depth > MAXIMUM_DATA_DEPTH || ++counter.nodes > MAXIMUM_DATA_NODES) {
            throw invalidResponse();
        }
        if (value == NullValue.INSTANCE) {
            return null;
        }
        if (value instanceof TextValue text) {
            if (text.value().length() > MAXIMUM_DATA_TEXT_LENGTH) {
                throw invalidResponse();
            }
            return text.value();
        }
        if (value instanceof IntegerValue integer) {
            return integer.value();
        }
        if (value instanceof BooleanValue bool) {
            return bool.value();
        }
        if (value instanceof ArrayValue array) {
            List<Object> values = new ArrayList<>();
            for (StructuredValue item : array.values()) {
                values.add(plain(item, depth + 1, counter));
            }
            return Collections.unmodifiableList(values);
        }
        if (value instanceof ObjectValue object) {
            Map<String, Object> values = new LinkedHashMap<>();
            object.values().forEach((key, item) -> {
                if (key == null || key.isBlank() || key.length() > 128) {
                    throw invalidResponse();
                }
                if (!sensitive(key)) {
                    values.put(key, plain(item, depth + 1, counter));
                } else {
                    counter.redacted = true;
                }
            });
            return Collections.unmodifiableMap(values);
        }
        throw invalidResponse();
    }

    private RunResultSize size(ResultSize size, boolean enforceToolLimit, boolean forceTruncated) {
        if (size == null || size.observedItems() < 0 || size.returnedItems() < 0
                || size.returnedItems() > size.observedItems() || size.observedBytes() < 0
                || size.returnedBytes() < 0 || size.returnedBytes() > size.observedBytes()
                || size.maximumItems() < 0 || size.maximumBytes() < 0
                || size.returnedItems() > size.maximumItems() || size.returnedBytes() > size.maximumBytes()
                || (enforceToolLimit && (size.maximumItems() > resultLimits.maximumItems()
                || size.maximumBytes() > resultLimits.maximumBytes()))) {
            throw invalidResponse();
        }
        return new RunResultSize(size.observedItems(), size.returnedItems(), size.observedBytes(),
                size.returnedBytes(), size.maximumItems(), size.maximumBytes(),
                size.truncated() || forceTruncated);
    }

    private RunFailure failure(AgentFailure failure) {
        if (failure == null) {
            return null;
        }
        if (failure.code() == null) {
            throw invalidResponse();
        }
        FailureSpec spec = switch (failure.code()) {
            case "INVALID_AGENT_REQUEST" -> new FailureSpec("VALIDATION", "Agent 请求无效", false);
            case "TOOL_INPUT_TOO_COMPLEX" -> new FailureSpec("VALIDATION", "工具参数超过复杂度限制", false);
            case "UNKNOWN_TOOL" -> new FailureSpec("UNKNOWN_TOOL", "模型请求了未注册工具", false);
            case "MAXIMUM_STEPS_REACHED" -> new FailureSpec("STEP_LIMIT", "Agent 已达到最大步骤限制", false);
            case "MODEL_GATEWAY_FAILED" -> new FailureSpec("MODEL_FAILURE", "模型网关未返回有效决策", false);
            case "INTERNAL_AGENT_FAILURE" -> new FailureSpec("INTERNAL", "Agent Runtime 发生内部失败", false);
            case "TOOL_EXECUTION_TIMEOUT" -> new FailureSpec("TIMEOUT", "只读工具执行超时", false);
            case "TOOL_CONCURRENCY_LIMIT" -> new FailureSpec("CONCURRENCY_LIMIT", "只读工具并发已达到上限", false);
            case "UNSUPPORTED_VIEW_VARIABLE", "UNAUTHORIZED_FIELD", "UNAUTHORIZED_PARAMETER",
                    "INVALID_TOOL_INPUT" -> new FailureSpec("VALIDATION", "工具参数不符合只读契约", false);
            case "QUERY_ACCESS_DENIED" -> new FailureSpec("ACCESS_DENIED", "查询或元数据权限被拒绝", false);
            case "QUERY_VALIDATION_FAILED" -> new FailureSpec("VALIDATION", "查询参数未通过校验", false);
            case "QUERY_DEFINITION_FAILED" -> new FailureSpec("DEFINITION", "数据资产定义暂时不可用", true);
            case "QUERY_EXECUTION_FAILED" -> new FailureSpec("EXECUTION", "只读查询执行失败", true);
            case "INTERNAL_TOOL_FAILURE" -> new FailureSpec("INTERNAL", "只读工具发生内部失败", false);
            default -> new FailureSpec("INTERNAL", "Agent Runtime 发生内部失败", false);
        };
        String code = knownFailureCode(failure.code()) ? failure.code() : "INTERNAL_AGENT_FAILURE";
        return new RunFailure(spec.category(), code, spec.message(), spec.recoverable() && failure.recoverable());
    }

    private boolean knownFailureCode(String code) {
        return code != null && switch (code) {
            case "INVALID_AGENT_REQUEST", "TOOL_INPUT_TOO_COMPLEX", "UNKNOWN_TOOL",
                    "MAXIMUM_STEPS_REACHED", "MODEL_GATEWAY_FAILED", "INTERNAL_AGENT_FAILURE",
                    "TOOL_EXECUTION_TIMEOUT", "TOOL_CONCURRENCY_LIMIT", "UNSUPPORTED_VIEW_VARIABLE",
                    "UNAUTHORIZED_FIELD", "UNAUTHORIZED_PARAMETER", "INVALID_TOOL_INPUT",
                    "QUERY_ACCESS_DENIED", "QUERY_VALIDATION_FAILED", "QUERY_DEFINITION_FAILED",
                    "QUERY_EXECUTION_FAILED", "INTERNAL_TOOL_FAILURE" -> true;
            default -> false;
        };
    }

    private String safeToolName(AgentStep step) {
        if (step.kind() != StepKind.TOOL_CALL) {
            if (step.toolName() != null) {
                throw invalidResponse();
            }
            return null;
        }
        if (READ_ONLY_TOOLS.contains(step.toolName())) {
            return step.toolName();
        }
        return UNREGISTERED_TOOL;
    }

    private boolean exactFields(JsonNode value, Set<String> expected) {
        Set<String> actual = new HashSet<>(value.propertyNames());
        return actual.equals(expected);
    }

    private boolean sensitive(String key) {
        return SENSITIVE_KEYS.contains(key.toLowerCase(Locale.ROOT).replace("_", ""));
    }

    private AgentWorkspaceRunException invalidRequest() {
        return new AgentWorkspaceRunException(Code.INVALID_AGENT_RUN_REQUEST, "Agent 运行请求参数无效");
    }

    private AgentWorkspaceRunException invalidResponse() {
        return new AgentWorkspaceRunException(Code.INVALID_AGENT_RUN_RESPONSE, "Agent 运行结果不可用");
    }

    private record FailureSpec(String category, String message, boolean recoverable) {
    }

    private static final class Counter {
        private int nodes;
        private boolean redacted;
    }
}
