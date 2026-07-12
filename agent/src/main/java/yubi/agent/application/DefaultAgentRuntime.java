package yubi.agent.application;

import yubi.agent.api.AgentExecutionContext;
import yubi.agent.api.AgentRequest;
import yubi.agent.api.AgentRunResult;
import yubi.agent.api.AgentUseCase;
import yubi.agent.domain.AgentModels.AgentAuditEvent;
import yubi.agent.domain.AgentModels.AgentFailure;
import yubi.agent.domain.AgentModels.AgentSession;
import yubi.agent.domain.AgentModels.AgentStep;
import yubi.agent.domain.AgentModels.ArgumentSummary;
import yubi.agent.domain.AgentModels.AuditEventType;
import yubi.agent.domain.AgentModels.AuditStatus;
import yubi.agent.domain.AgentModels.FailureCategory;
import yubi.agent.domain.AgentModels.ResultSize;
import yubi.agent.domain.AgentModels.SessionStatus;
import yubi.agent.domain.AgentModels.StepKind;
import yubi.agent.domain.AgentModels.StepStatus;
import yubi.agent.domain.AgentRuntimePolicy;
import yubi.agent.domain.ModelProtocol.FinalAnswer;
import yubi.agent.domain.ModelProtocol.ModelDecision;
import yubi.agent.domain.ModelProtocol.ModelTurn;
import yubi.agent.domain.ModelProtocol.ToolCall;
import yubi.agent.port.AgentAuditPort;
import yubi.agent.port.AgentClockPort;
import yubi.agent.port.AgentSessionStorePort;
import yubi.agent.port.ModelGateway;
import yubi.agent.port.ReadOnlyTool;
import yubi.agent.port.ReadOnlyToolRegistry;
import yubi.query.api.MetadataToolSchema;

import java.util.Objects;

public final class DefaultAgentRuntime implements AgentUseCase {

    private static final String UNKNOWN_TOOL = "<unregistered>";

    private final ModelGateway modelGateway;
    private final ReadOnlyToolRegistry toolRegistry;
    private final AgentAuditPort auditPort;
    private final AgentSessionStorePort sessionStore;
    private final AgentClockPort clock;
    private final AgentRuntimePolicy policy;
    private final ArgumentSummarizer argumentSummarizer = new ArgumentSummarizer();
    private final AgentFailureClassifier failureClassifier = new AgentFailureClassifier();

    public DefaultAgentRuntime(ModelGateway modelGateway,
                               ReadOnlyToolRegistry toolRegistry,
                               AgentAuditPort auditPort,
                               AgentSessionStorePort sessionStore,
                               AgentClockPort clock,
                               AgentRuntimePolicy policy) {
        this.modelGateway = Objects.requireNonNull(modelGateway, "modelGateway");
        this.toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry");
        this.auditPort = Objects.requireNonNull(auditPort, "auditPort");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    @Override
    public AgentRunResult run(AgentRequest request, AgentExecutionContext context) {
        Objects.requireNonNull(context, "context");
        AgentSession session = new AgentSession(context.sessionId(), context.requestId(),
                SessionStatus.RUNNING, java.util.List.of(), null, null);
        persist(session);
        audit(context, AuditEventType.SESSION_STARTED, 0, null, ArgumentSummary.empty(),
                0, ResultSize.empty(), AuditStatus.STARTED, null);

        if (request == null || request.message() == null || request.message().isBlank()
                || request.message().length() > policy.maximumPromptLength()) {
            AgentFailure failure = new AgentFailure(FailureCategory.VALIDATION, "INVALID_AGENT_REQUEST",
                    "Agent 请求无效", false);
            AgentStep step = new AgentStep(0, StepKind.REQUEST_REJECTED, null, StepStatus.FAILED,
                    ArgumentSummary.empty(), null, failure, 0);
            session = session.append(step);
            auditStep(context, step);
            return finish(session, context, SessionStatus.FAILED, null, failure);
        }

        int recoveredFailures = 0;
        for (int stepIndex = 1; stepIndex <= policy.maximumSteps(); stepIndex++) {
            ModelDecision decision;
            try {
                decision = modelGateway.next(new ModelTurn(request.message(), stepIndex,
                        toolRegistry.schemas(), session.steps()));
            } catch (RuntimeException exception) {
                AgentFailure failure = modelFailure();
                AgentStep step = new AgentStep(stepIndex, StepKind.MODEL_FAILURE, null,
                        StepStatus.FAILED, ArgumentSummary.empty(), null, failure, 0);
                session = session.append(step);
                persist(session);
                auditStep(context, step);
                return finish(session, context, SessionStatus.FAILED, null, failure);
            } catch (Error error) {
                AgentFailure failure = internalFailure();
                AgentStep step = new AgentStep(stepIndex, StepKind.MODEL_FAILURE, null,
                        StepStatus.FAILED, ArgumentSummary.empty(), null, failure, 0);
                session = session.append(step);
                persist(session);
                auditStep(context, step);
                finishAndRethrow(session, context, failure, error);
                throw error;
            }

            if (decision instanceof FinalAnswer answer) {
                if (answer.answer() == null || answer.answer().isBlank()
                        || answer.answer().length() > policy.maximumPromptLength() * 2L) {
                    AgentFailure failure = modelFailure();
                    AgentStep step = new AgentStep(stepIndex, StepKind.MODEL_FAILURE, null,
                            StepStatus.FAILED, ArgumentSummary.empty(), null, failure, 0);
                    session = session.append(step);
                    persist(session);
                    auditStep(context, step);
                    return finish(session, context, SessionStatus.FAILED, null, failure);
                }
                AgentStep step = new AgentStep(stepIndex, StepKind.FINAL_ANSWER, null,
                        StepStatus.SUCCEEDED, ArgumentSummary.empty(), null, null, 0);
                session = session.append(step);
                persist(session);
                auditStep(context, step);
                return finish(session, context, SessionStatus.COMPLETED, answer.answer(), null);
            }

            if (!(decision instanceof ToolCall call)) {
                AgentFailure failure = modelFailure();
                AgentStep step = new AgentStep(stepIndex, StepKind.MODEL_FAILURE, null,
                        StepStatus.FAILED, ArgumentSummary.empty(), null, failure, 0);
                session = session.append(step);
                persist(session);
                auditStep(context, step);
                return finish(session, context, SessionStatus.FAILED, null, failure);
            }

            ReadOnlyTool tool = toolRegistry.find(call.toolName()).orElse(null);
            MetadataToolSchema schema = tool == null ? null : tool.schema();
            ArgumentSummary summary = argumentSummarizer.summarize(call.arguments(), schema,
                    policy.maximumArgumentNodes(), policy.maximumArgumentDepth());
            if (summary.scalarCount() + summary.collectionCount() > policy.maximumArgumentNodes()
                    || summary.maximumDepth() > policy.maximumArgumentDepth()) {
                AgentFailure failure = new AgentFailure(FailureCategory.VALIDATION, "TOOL_INPUT_TOO_COMPLEX",
                        "工具参数超过确定性复杂度限制", false);
                AgentStep step = new AgentStep(stepIndex, StepKind.TOOL_CALL,
                        tool == null ? UNKNOWN_TOOL : tool.schema().name(), StepStatus.FAILED,
                        summary, null, failure, 0);
                session = session.append(step);
                persist(session);
                auditStep(context, step);
                return finish(session, context, SessionStatus.FAILED, null, failure);
            }
            if (tool == null) {
                AgentFailure failure = new AgentFailure(FailureCategory.UNKNOWN_TOOL, "UNKNOWN_TOOL",
                        "模型请求了未注册工具", false);
                AgentStep step = new AgentStep(stepIndex, StepKind.TOOL_CALL, UNKNOWN_TOOL,
                        StepStatus.FAILED, summary, null, failure, 0);
                session = session.append(step);
                persist(session);
                auditStep(context, step);
                return finish(session, context, SessionStatus.FAILED, null, failure);
            }

            long started = clock.nanoTime();
            try {
                var output = tool.execute(call.arguments(), context.queryContext());
                long duration = durationMillis(started);
                AgentStep step = new AgentStep(stepIndex, StepKind.TOOL_CALL, tool.schema().name(),
                        StepStatus.SUCCEEDED, summary, output, null, duration);
                session = session.append(step);
                persist(session);
                auditStep(context, step);
            } catch (RuntimeException exception) {
                long duration = durationMillis(started);
                boolean failureCanRecover = isPotentiallyRecoverable(exception)
                        && recoveredFailures < policy.maximumRecoverableFailures()
                        && stepIndex < policy.maximumSteps();
                AgentFailure failure = failureClassifier.classify(exception, failureCanRecover);
                AgentStep step = new AgentStep(stepIndex, StepKind.TOOL_CALL, tool.schema().name(),
                        StepStatus.FAILED, summary, null, failure, duration);
                session = session.append(step);
                persist(session);
                auditStep(context, step);
                if (!failure.recoverable()) {
                    return finish(session, context, SessionStatus.FAILED, null, failure);
                }
                recoveredFailures++;
            } catch (Error error) {
                long duration = durationMillis(started);
                AgentFailure failure = internalFailure();
                AgentStep step = new AgentStep(stepIndex, StepKind.TOOL_CALL, tool.schema().name(),
                        StepStatus.FAILED, summary, null, failure, duration);
                session = session.append(step);
                persist(session);
                auditStep(context, step);
                finishAndRethrow(session, context, failure, error);
                throw error;
            }
        }

        AgentFailure failure = new AgentFailure(FailureCategory.STEP_LIMIT, "MAXIMUM_STEPS_REACHED",
                "Agent 已达到最大步骤限制", false);
        return finish(session, context, SessionStatus.STEP_LIMIT_REACHED, null, failure);
    }

    private AgentRunResult finish(AgentSession session,
                                  AgentExecutionContext context,
                                  SessionStatus status,
                                  String answer,
                                  AgentFailure failure) {
        AgentSession finished = session.finish(status, answer, failure);
        persist(finished);
        ResultSize size = aggregateSize(finished);
        audit(context, AuditEventType.SESSION_COMPLETED, finished.steps().size(), null,
                ArgumentSummary.empty(), finished.steps().stream().mapToLong(AgentStep::durationMillis).sum(),
                size, auditStatus(status), failure == null ? null : failure.category());
        return new AgentRunResult(finished, size);
    }

    private ResultSize aggregateSize(AgentSession session) {
        int observed = 0;
        int returned = 0;
        long observedBytes = 0;
        long returnedBytes = 0;
        int maximumItems = 0;
        long maximumBytes = 0;
        boolean truncated = false;
        for (AgentStep step : session.steps()) {
            if (step.output() == null || step.output().size() == null) {
                continue;
            }
            ResultSize size = step.output().size();
            observed += size.observedItems();
            returned += size.returnedItems();
            observedBytes += size.observedBytes();
            returnedBytes += size.returnedBytes();
            maximumItems += size.maximumItems();
            maximumBytes += size.maximumBytes();
            truncated |= size.truncated();
        }
        return new ResultSize(observed, returned, observedBytes, returnedBytes,
                maximumItems, maximumBytes, truncated);
    }

    private boolean isPotentiallyRecoverable(RuntimeException exception) {
        return exception instanceof yubi.query.api.QueryDefinitionException
                || exception instanceof yubi.query.api.QueryExecutionException;
    }

    private AgentFailure modelFailure() {
        return new AgentFailure(FailureCategory.MODEL_FAILURE, "MODEL_GATEWAY_FAILED",
                "模型网关未返回有效结构化决策", false);
    }

    private AgentFailure internalFailure() {
        return new AgentFailure(FailureCategory.INTERNAL, "INTERNAL_AGENT_FAILURE",
                "Agent Runtime 发生内部失败", false);
    }

    private void finishAndRethrow(AgentSession session,
                                  AgentExecutionContext context,
                                  AgentFailure failure,
                                  Error original) {
        try {
            finish(session, context, SessionStatus.FAILED, null, failure);
        } catch (Throwable reportingFailure) {
            if (reportingFailure != original) {
                original.addSuppressed(reportingFailure);
            }
        }
    }

    private long durationMillis(long started) {
        return Math.max(0, clock.nanoTime() - started) / 1_000_000L;
    }

    private void auditStep(AgentExecutionContext context, AgentStep step) {
        audit(context, AuditEventType.STEP_COMPLETED, step.index(), step.toolName(), step.arguments(),
                step.durationMillis(), step.output() == null ? ResultSize.empty() : step.output().size(),
                step.status() == StepStatus.SUCCEEDED ? AuditStatus.SUCCEEDED : AuditStatus.FAILED,
                step.failure() == null ? null : step.failure().category());
    }

    private void audit(AgentExecutionContext context,
                       AuditEventType eventType,
                       int stepIndex,
                       String toolName,
                       ArgumentSummary arguments,
                       long durationMillis,
                       ResultSize resultSize,
                       AuditStatus status,
                       FailureCategory failureCategory) {
        try {
            var query = context.queryContext();
            auditPort.record(new AgentAuditEvent(eventType, context.sessionId(), context.requestId(),
                    query.subjectId(), query.organizationId(), query.correlationId(), stepIndex, toolName,
                    arguments, durationMillis, resultSize, status, failureCategory));
        } catch (RuntimeException ignored) {
            // Trace 失败不能覆盖确定性的 Agent 结果或原始失败。
        }
    }

    private void persist(AgentSession session) {
        try {
            sessionStore.save(storageSnapshot(session));
        } catch (RuntimeException ignored) {
            // V1 仅定义持久化端口，适配器失败不改变本次内存状态机结果。
        }
    }

    private AgentSession storageSnapshot(AgentSession session) {
        var steps = session.steps().stream().map(step -> new AgentStep(
                step.index(), step.kind(), step.toolName(), step.status(), step.arguments(),
                step.output() == null ? null : new yubi.agent.domain.AgentModels.ToolOutput(
                        new yubi.agent.domain.StructuredValue.ObjectValue(java.util.Map.of()),
                        step.output().size()),
                step.failure(), step.durationMillis())).toList();
        return new AgentSession(session.sessionId(), session.requestId(), session.status(),
                steps, null, session.failure());
    }

    private AuditStatus auditStatus(SessionStatus status) {
        return switch (status) {
            case COMPLETED -> AuditStatus.SUCCEEDED;
            case STEP_LIMIT_REACHED -> AuditStatus.LIMIT_REACHED;
            case RUNNING -> AuditStatus.STARTED;
            case FAILED -> AuditStatus.FAILED;
        };
    }
}
