package yubi.agent.domain;

import yubi.agent.domain.StructuredValue.ObjectValue;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class ControlledWriteModels {

    private ControlledWriteModels() {
    }

    public enum WriteOperationState {
        PENDING,
        SUCCEEDED,
        REJECTED,
        EXPIRED,
        FAILED
    }

    public enum WriteAuditEventType {
        PROPOSED,
        IDEMPOTENT_REPLAYED,
        SUCCEEDED,
        REJECTED,
        EXPIRED,
        FAILED
    }

    public enum WriteFailureCategory {
        ACCESS_DENIED,
        VALIDATION,
        CONFLICT,
        EXECUTION,
        INTERNAL
    }

    public sealed interface WriteAction permits CreateChartAction, RenameDashboardAction {
        String toolName();
    }

    public record CreateChartAction(String name,
                                    String viewId,
                                    String parentId,
                                    String description) implements WriteAction {
        public CreateChartAction {
            requireText(name, "图表名称");
            requireText(viewId, "数据视图标识");
        }

        @Override
        public String toolName() {
            return "create_chart";
        }
    }

    public record RenameDashboardAction(String dashboardId,
                                        String newName) implements WriteAction {
        public RenameDashboardAction {
            requireText(dashboardId, "仪表盘标识");
            requireText(newName, "仪表盘新名称");
        }

        @Override
        public String toolName() {
            return "rename_dashboard";
        }
    }

    public record PreviewField(String label, String value) {
        public PreviewField {
            requireText(label, "预览字段标签");
            requireText(value, "预览字段值");
        }
    }

    public record WritePreview(String title,
                               String resourceType,
                               String targetId,
                               List<PreviewField> fields,
                               List<String> impacts) {
        public WritePreview {
            requireText(title, "预览标题");
            requireText(resourceType, "预览资源类型");
            fields = fields == null ? List.of() : List.copyOf(fields);
            impacts = impacts == null ? List.of() : List.copyOf(impacts);
            if (fields.stream().anyMatch(Objects::isNull)
                    || impacts.stream().anyMatch(value -> value == null || value.isBlank())) {
                throw new IllegalArgumentException("写操作预览不完整");
            }
        }
    }

    /** 由业务 prepare 返回的目标状态摘要必须在 execute 时重新核验。 */
    public record PreparedWrite(WriteAction action,
                                String expectedStateDigest,
                                WritePreview preview) {
        public PreparedWrite {
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(preview, "preview");
            if (expectedStateDigest == null || expectedStateDigest.isBlank()) {
                throw new IllegalArgumentException("业务准备状态摘要不能为空");
            }
        }
    }

    public record TrustedWriteContext(String subjectId,
                                      String organizationId,
                                      String sessionId,
                                      String requestId,
                                      String correlationId) {
        public TrustedWriteContext {
            requireText(subjectId, "可信主体");
            requireText(organizationId, "可信组织");
            requireText(sessionId, "可信会话");
            requireText(requestId, "可信请求");
            requireText(correlationId, "可信关联标识");
            if (subjectId.length() > 256 || organizationId.length() > 256
                    || sessionId.length() > 256 || requestId.length() > 256
                    || correlationId.length() > 256) {
                throw new IllegalArgumentException("可信写入上下文超过长度限制");
            }
        }
    }

    public record NormalizedWriteProposal(WriteAction action,
                                          ObjectValue canonicalParameters,
                                          WritePreview preview) {
        public NormalizedWriteProposal {
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(canonicalParameters, "canonicalParameters");
            Objects.requireNonNull(preview, "preview");
        }
    }

    public record WriteProposalCommand(String toolName,
                                       ObjectValue arguments,
                                       String idempotencyKey) {
    }

    public record WriteIdempotencyScope(String subjectId,
                                        String organizationId,
                                        String toolName,
                                        String idempotencyKeyDigest) {
        public WriteIdempotencyScope {
            requireText(subjectId, "幂等主体");
            requireText(organizationId, "幂等组织");
            requireText(toolName, "幂等工具");
            requireText(idempotencyKeyDigest, "幂等键摘要");
        }
    }

    public record WriteApprovalScope(String subjectId,
                                     String organizationId,
                                     String sessionId) {
        public WriteApprovalScope {
            requireText(subjectId, "审批主体");
            requireText(organizationId, "审批组织");
            requireText(sessionId, "审批会话");
        }
    }

    public record WriteExecutionResult(String resourceId) {
        public WriteExecutionResult {
            requireText(resourceId, "业务资源标识");
        }
    }

    public record ControlledWriteOperation(String approvalId,
                                           String changeId,
                                           TrustedWriteContext context,
                                           String toolName,
                                           ObjectValue canonicalParameters,
                                           String parametersDigest,
                                           String preparedDigest,
                                           String idempotencyKeyDigest,
                                           PreparedWrite preparedWrite,
                                           Instant createdAt,
                                           Instant expiresAt,
                                           WriteOperationState state,
                                           String resourceId,
                                           WriteFailureCategory failure,
                                           Instant completedAt) {
        public ControlledWriteOperation {
            requireText(approvalId, "审批标识");
            requireText(changeId, "变更标识");
            Objects.requireNonNull(context, "context");
            requireText(toolName, "工具名");
            Objects.requireNonNull(canonicalParameters, "canonicalParameters");
            requireText(parametersDigest, "参数摘要");
            requireText(preparedDigest, "业务准备摘要");
            requireText(idempotencyKeyDigest, "幂等键摘要");
            Objects.requireNonNull(preparedWrite, "preparedWrite");
            Objects.requireNonNull(createdAt, "createdAt");
            Objects.requireNonNull(expiresAt, "expiresAt");
            Objects.requireNonNull(state, "state");
            if (!expiresAt.isAfter(createdAt)) {
                throw new IllegalArgumentException("审批有效期必须晚于创建时间");
            }
            if (state == WriteOperationState.PENDING
                    && (resourceId != null || failure != null || completedAt != null)) {
                throw new IllegalArgumentException("待审批操作不能包含终态数据");
            }
            if (state == WriteOperationState.SUCCEEDED
                    && (resourceId == null || resourceId.isBlank() || failure != null || completedAt == null)) {
                throw new IllegalArgumentException("成功写操作终态不完整");
            }
            if (state == WriteOperationState.FAILED
                    && (resourceId != null || failure == null || completedAt == null)) {
                throw new IllegalArgumentException("失败写操作终态不完整");
            }
            if ((state == WriteOperationState.REJECTED || state == WriteOperationState.EXPIRED)
                    && (resourceId != null || failure != null || completedAt == null)) {
                throw new IllegalArgumentException("未执行写操作终态不完整");
            }
        }

        public WriteIdempotencyScope idempotencyScope() {
            return new WriteIdempotencyScope(context.subjectId(), context.organizationId(),
                    toolName, idempotencyKeyDigest);
        }

        public WriteApprovalScope approvalScope() {
            return new WriteApprovalScope(context.subjectId(), context.organizationId(), context.sessionId());
        }

        public ControlledWriteOperation succeeded(String newResourceId, Instant now) {
            requirePending();
            return transition(WriteOperationState.SUCCEEDED, newResourceId, null, now);
        }

        public ControlledWriteOperation rejected(Instant now) {
            requirePending();
            return transition(WriteOperationState.REJECTED, null, null, now);
        }

        public ControlledWriteOperation expired(Instant now) {
            requirePending();
            return transition(WriteOperationState.EXPIRED, null, null, now);
        }

        public ControlledWriteOperation failed(WriteFailureCategory category, Instant now) {
            requirePending();
            return transition(WriteOperationState.FAILED, null,
                    Objects.requireNonNull(category, "category"), now);
        }

        private ControlledWriteOperation transition(WriteOperationState newState,
                                                    String newResourceId,
                                                    WriteFailureCategory newFailure,
                                                    Instant now) {
            Objects.requireNonNull(now, "now");
            return new ControlledWriteOperation(approvalId, changeId, context, toolName,
                    canonicalParameters, parametersDigest, preparedDigest, idempotencyKeyDigest, preparedWrite,
                    createdAt, expiresAt, newState, newResourceId, newFailure, now);
        }

        private void requirePending() {
            if (state != WriteOperationState.PENDING) {
                throw new IllegalStateException("写操作不是待审批状态");
            }
        }

        @Override
        public String toString() {
            return "ControlledWriteOperation[approvalId=" + approvalId
                    + ", changeId=" + changeId + ", toolName=" + toolName
                    + ", state=" + state + ", resourceId=" + resourceId + "]";
        }
    }

    public record WriteOperationView(String approvalId,
                                     String changeId,
                                     String toolName,
                                     WriteOperationState state,
                                     WritePreview preview,
                                     String resourceId,
                                     WriteFailureCategory failure,
                                     Instant createdAt,
                                     Instant expiresAt,
                                     Instant completedAt,
                                     boolean replayed) {
        public static WriteOperationView from(ControlledWriteOperation operation, boolean replayed) {
            return new WriteOperationView(operation.approvalId(), operation.changeId(),
                    operation.toolName(), operation.state(), operation.preparedWrite().preview(),
                    operation.resourceId(), operation.failure(), operation.createdAt(),
                    operation.expiresAt(), operation.completedAt(), replayed);
        }
    }

    /** 不包含参数值、原始幂等键、用户消息或底层异常。 */
    public record WriteAuditEvent(String eventId,
                                  WriteAuditEventType eventType,
                                  String approvalId,
                                  String changeId,
                                  String subjectId,
                                  String organizationId,
                                  String sessionId,
                                  String requestId,
                                  String correlationId,
                                  String toolName,
                                  String parametersDigest,
                                  String preparedDigest,
                                  String idempotencyKeyDigest,
                                  String resourceId,
                                  WriteOperationState state,
                                  WriteFailureCategory failure,
                                  Instant occurredAt) {
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "不能为空");
        }
    }
}
