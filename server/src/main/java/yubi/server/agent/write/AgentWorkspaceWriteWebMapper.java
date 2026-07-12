package yubi.server.agent.write;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import yubi.agent.domain.ControlledWriteModels.WriteFailureCategory;
import yubi.agent.domain.ControlledWriteModels.WriteOperationState;
import yubi.agent.domain.ControlledWriteModels.WriteOperationView;
import yubi.agent.domain.ControlledWriteModels.WriteProposalCommand;
import yubi.server.agent.write.AgentWorkspaceWebModels.ApprovalFailure;
import yubi.server.agent.write.AgentWorkspaceWebModels.ApprovalImpact;
import yubi.server.agent.write.AgentWorkspaceWebModels.ApprovalParameter;
import yubi.server.agent.write.AgentWorkspaceWebModels.ApprovalPreview;
import yubi.server.agent.write.AgentWorkspaceWebModels.ApprovalResponse;
import yubi.server.agent.write.AgentWorkspaceWebModels.BusinessChange;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public final class AgentWorkspaceWriteWebMapper {

    private static final Set<String> PROPOSAL_FIELDS = Set.of("toolName", "arguments");

    private final StructuredValueWebMapper values;

    public AgentWorkspaceWriteWebMapper(StructuredValueWebMapper values) {
        this.values = values;
    }

    public WriteProposalCommand proposal(JsonNode request, String idempotencyKey) {
        if (request == null || !request.isObject() || !exactFields(request, PROPOSAL_FIELDS)) {
            throw invalid();
        }
        JsonNode tool = request.get("toolName");
        JsonNode arguments = request.get("arguments");
        if (tool == null || !tool.isTextual() || tool.textValue() == null
                || tool.textValue().isBlank() || tool.textValue().length() > 64 || arguments == null) {
            throw invalid();
        }
        return new WriteProposalCommand(tool.textValue(), values.toObject(arguments), idempotencyKey);
    }

    public void requireBodylessDecision(JsonNode request) {
        if (request != null) {
            throw invalid();
        }
    }

    public List<ApprovalResponse> responses(List<WriteOperationView> operations, String sessionId) {
        if (operations == null) {
            throw new IllegalStateException("审批列表为空");
        }
        return operations.stream().map(value -> response(value, sessionId)).toList();
    }

    public ApprovalResponse response(WriteOperationView operation, String sessionId) {
        var preview = operation.preview();
        String action = action(operation.toolName());
        List<ApprovalParameter> parameters = new ArrayList<>();
        for (int index = 0; index < preview.fields().size(); index++) {
            var field = preview.fields().get(index);
            parameters.add(new ApprovalParameter("parameter-" + index, field.label(), field.value()));
        }
        List<ApprovalImpact> impacts = preview.impacts().stream()
                .map(value -> new ApprovalImpact(preview.resourceType(), nullToEmpty(preview.targetId()),
                        action, value))
                .toList();
        ApprovalPreview webPreview = new ApprovalPreview(preview.title(), summary(operation.toolName()),
                parameters, impacts);
        WriteOperationState state = operation.state();
        BusinessChange change = state == WriteOperationState.SUCCEEDED
                ? new BusinessChange(operation.changeId(), preview.resourceType(), operation.resourceId(),
                action, WriteOperationState.SUCCEEDED.name())
                : null;
        ApprovalFailure failure = state == WriteOperationState.FAILED
                ? new ApprovalFailure(operation.failure().name(), failureMessage(operation.failure()))
                : null;
        return new ApprovalResponse(operation.approvalId(), sessionId, operation.toolName(),
                state.name(), operation.createdAt(), operation.expiresAt(), operation.replayed(),
                webPreview, change, failure);
    }

    private boolean exactFields(JsonNode value, Set<String> expected) {
        Set<String> actual = new HashSet<>();
        value.properties().forEach(entry -> actual.add(entry.getKey()));
        return actual.equals(expected);
    }

    private String action(String toolName) {
        return "create_chart".equals(toolName) ? "CREATED" : "RENAMED";
    }

    private String summary(String toolName) {
        return "create_chart".equals(toolName)
                ? "创建一个绑定现有数据视图的未发布基础图表"
                : "仅同步修改仪表盘及其导航名称";
    }

    private String failureMessage(WriteFailureCategory failure) {
        return switch (failure) {
            case ACCESS_DENIED -> "批准执行时权限不足";
            case VALIDATION -> "受控写参数或目标状态无效";
            case CONFLICT -> "目标已变化或存在名称冲突";
            case EXECUTION -> "业务写入执行失败";
            case INTERNAL -> "受控写服务执行失败";
        };
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private AgentWriteWebException invalid() {
        return new AgentWriteWebException("INVALID_WRITE_REQUEST", "受控写请求参数无效");
    }
}
