package yubi.agent.domain;

import java.util.List;

import static yubi.agent.domain.StructuredValue.ObjectValue;

public final class AgentModels {

    private AgentModels() {
    }

    public enum SessionStatus { RUNNING, COMPLETED, FAILED, STEP_LIMIT_REACHED }

    public enum StepKind { TOOL_CALL, FINAL_ANSWER, MODEL_FAILURE, REQUEST_REJECTED }

    public enum StepStatus { SUCCEEDED, FAILED }

    public enum FailureCategory {
        VALIDATION, ACCESS_DENIED, DEFINITION, EXECUTION,
        TIMEOUT, CONCURRENCY_LIMIT, UNKNOWN_TOOL, MODEL_FAILURE, STEP_LIMIT, INTERNAL
    }

    public enum AuditEventType { SESSION_STARTED, STEP_COMPLETED, SESSION_COMPLETED }

    public enum AuditStatus { STARTED, SUCCEEDED, FAILED, LIMIT_REACHED }

    public enum ToolMetricStatus { SUCCEEDED, FAILED, TIMED_OUT, CONCURRENCY_REJECTED }

    public record AgentFailure(FailureCategory category,
                               String code,
                               String message,
                               boolean recoverable) {
    }

    public record ArgumentSummary(List<String> recognizedFields,
                                  int rejectedFieldCount,
                                  int scalarCount,
                                  int collectionCount,
                                  int maximumDepth) {
        public ArgumentSummary {
            recognizedFields = recognizedFields == null ? List.of() : List.copyOf(recognizedFields);
        }

        public static ArgumentSummary empty() {
            return new ArgumentSummary(List.of(), 0, 0, 0, 0);
        }
    }

    public record ResultSize(int observedItems,
                             int returnedItems,
                             long observedBytes,
                             long returnedBytes,
                             int maximumItems,
                             long maximumBytes,
                             boolean truncated) {
        public static ResultSize empty() {
            return new ResultSize(0, 0, 0, 0, 0, 0, false);
        }
    }

    public record ToolOutput(ObjectValue value, ResultSize size) {
    }

    public record AgentStep(int index,
                            StepKind kind,
                            String toolName,
                            StepStatus status,
                            ArgumentSummary arguments,
                            ToolOutput output,
                            AgentFailure failure,
                            long durationMillis) {
    }

    public record AgentSession(String sessionId,
                               String requestId,
                               SessionStatus status,
                               List<AgentStep> steps,
                               String finalAnswer,
                               AgentFailure failure) {
        public AgentSession {
            steps = steps == null ? List.of() : List.copyOf(steps);
        }

        public AgentSession append(AgentStep step) {
            java.util.ArrayList<AgentStep> updated = new java.util.ArrayList<>(steps);
            updated.add(step);
            return new AgentSession(sessionId, requestId, status, updated, finalAnswer, failure);
        }

        public AgentSession finish(SessionStatus newStatus, String answer, AgentFailure newFailure) {
            return new AgentSession(sessionId, requestId, newStatus, steps, answer, newFailure);
        }
    }

    public record AgentAuditEvent(AuditEventType eventType,
                                  String sessionId,
                                  String requestId,
                                  String subjectId,
                                  String organizationId,
                                  String correlationId,
                                  int stepIndex,
                                  String toolName,
                                  ArgumentSummary arguments,
                                  long durationMillis,
                                  ResultSize resultSize,
                                  AuditStatus status,
                                  FailureCategory failureCategory) {
    }

    /** 只包含有限枚举和数值，不允许携带身份、请求、参数值或查询内容。 */
    public record ToolMetric(String toolName,
                             ToolMetricStatus status,
                             FailureCategory failureCategory,
                             long durationMillis,
                             int argumentNodes,
                             int resultItems,
                             long resultBytes,
                             int queryRows) {
    }
}
