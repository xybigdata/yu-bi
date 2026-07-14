package yubi.server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import yubi.server.agent.write.AgentWorkspaceWriteWebMapper;
import yubi.server.agent.write.ServerAgentWorkspaceSessionService;
import yubi.server.agent.write.ServerControlledWriteTransactionService;
import yubi.server.agent.write.AgentWorkspaceWebModels.ApprovalResponse;
import yubi.server.base.dto.ResponseData;

import java.util.List;

@RestController
@RequestMapping("/agent/workspaces/{organizationId}")
public class ControlledWriteController extends BaseController {

    public static final String SESSION_HEADER = "X-YuBi-Agent-Session";
    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final ServerAgentWorkspaceSessionService sessions;
    private final ServerControlledWriteTransactionService writes;
    private final AgentWorkspaceWriteWebMapper mapper;

    public ControlledWriteController(ServerAgentWorkspaceSessionService sessions,
                                     ServerControlledWriteTransactionService writes,
                                     AgentWorkspaceWriteWebMapper mapper) {
        this.sessions = sessions;
        this.writes = writes;
        this.mapper = mapper;
    }

    @PostMapping("/writes/previews")
    public ResponseData<ApprovalResponse> preview(
            @PathVariable String organizationId,
            @RequestHeader(SESSION_HEADER) String sessionId,
            @RequestHeader(IDEMPOTENCY_HEADER) String idempotencyKey,
            @RequestBody(required = false) JsonNode request) {
        var proposal = mapper.proposal(request, idempotencyKey);
        var context = sessions.resume(organizationId, sessionId);
        var operation = writes.propose(proposal, context);
        return ResponseData.success(mapper.response(operation, sessionId));
    }

    @GetMapping("/approvals")
    public ResponseData<List<ApprovalResponse>> list(
            @PathVariable String organizationId,
            @RequestHeader(SESSION_HEADER) String sessionId) {
        var context = sessions.resume(organizationId, sessionId);
        return ResponseData.success(mapper.responses(writes.listOperations(context), sessionId));
    }

    @PostMapping("/approvals/{approvalId}/approve")
    public ResponseData<ApprovalResponse> approve(
            @PathVariable String organizationId,
            @PathVariable String approvalId,
            @RequestHeader(SESSION_HEADER) String sessionId,
            @RequestBody(required = false) JsonNode request) {
        mapper.requireBodylessDecision(request);
        var context = sessions.resume(organizationId, sessionId);
        return ResponseData.success(mapper.response(writes.approve(approvalId, context), sessionId));
    }

    @PostMapping("/approvals/{approvalId}/reject")
    public ResponseData<ApprovalResponse> reject(
            @PathVariable String organizationId,
            @PathVariable String approvalId,
            @RequestHeader(SESSION_HEADER) String sessionId,
            @RequestBody(required = false) JsonNode request) {
        mapper.requireBodylessDecision(request);
        var context = sessions.resume(organizationId, sessionId);
        return ResponseData.success(mapper.response(writes.reject(approvalId, context), sessionId));
    }
}
