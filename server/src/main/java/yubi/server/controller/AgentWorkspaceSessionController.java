package yubi.server.controller;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yubi.agent.api.AgentUseCase;
import yubi.agent.port.WriteProposalToolRegistry;
import yubi.server.agent.write.AgentWorkspaceSession;
import yubi.server.agent.write.ServerAgentWorkspaceSessionService;
import yubi.server.agent.write.AgentWorkspaceWebModels.SessionResponse;
import yubi.server.base.dto.ResponseData;

@RestController
@RequestMapping("/agent/workspaces/{organizationId}")
public class AgentWorkspaceSessionController extends BaseController {

    private final ServerAgentWorkspaceSessionService sessions;
    private final ObjectProvider<AgentUseCase> runtime;
    private final WriteProposalToolRegistry writeTools;

    public AgentWorkspaceSessionController(ServerAgentWorkspaceSessionService sessions,
                                           ObjectProvider<AgentUseCase> runtime,
                                           WriteProposalToolRegistry writeTools) {
        this.sessions = sessions;
        this.runtime = runtime;
        this.writeTools = writeTools;
    }

    @PostMapping("/sessions")
    public ResponseData<SessionResponse> create(@PathVariable String organizationId) {
        AgentWorkspaceSession session = sessions.create(organizationId);
        return ResponseData.success(new SessionResponse(session.sessionId(), session.expiresAt(),
                runtime.getIfAvailable() != null,
                writeTools.schemas().stream().map(value -> value.name()).toList()));
    }
}
