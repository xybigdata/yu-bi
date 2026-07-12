package yubi.server.controller;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import yubi.agent.api.AgentRunResult;
import yubi.agent.api.AgentUseCase;
import yubi.server.agent.AgentWorkspaceRunException;
import yubi.server.agent.AgentWorkspaceRunException.Code;
import yubi.server.agent.AgentWorkspaceRunWebMapper;
import yubi.server.agent.AgentWorkspaceRunWebModels.RunResponse;
import yubi.server.agent.write.ServerAgentWorkspaceSessionService;
import yubi.server.base.dto.ResponseData;

@RestController
@RequestMapping("/agent/workspaces/{organizationId}")
public class AgentWorkspaceRunController extends BaseController {

    public static final String SESSION_HEADER = "X-YuBi-Agent-Session";

    private final ServerAgentWorkspaceSessionService sessions;
    private final ObjectProvider<AgentUseCase> runtime;
    private final AgentWorkspaceRunWebMapper mapper;

    public AgentWorkspaceRunController(ServerAgentWorkspaceSessionService sessions,
                                       ObjectProvider<AgentUseCase> runtime,
                                       AgentWorkspaceRunWebMapper mapper) {
        this.sessions = sessions;
        this.runtime = runtime;
        this.mapper = mapper;
    }

    @PostMapping("/runs")
    public ResponseData<RunResponse> run(
            @PathVariable String organizationId,
            @RequestHeader(SESSION_HEADER) String sessionId,
            @RequestBody(required = false) JsonNode request) {
        var agentRequest = mapper.request(request);
        var context = sessions.resume(organizationId, sessionId);
        AgentUseCase agentRuntime;
        try {
            agentRuntime = runtime.getIfAvailable();
        } catch (RuntimeException exception) {
            throw runtimeUnavailable();
        }
        if (agentRuntime == null) {
            throw runtimeUnavailable();
        }
        AgentRunResult result;
        try {
            result = agentRuntime.run(agentRequest, context);
        } catch (RuntimeException exception) {
            throw new AgentWorkspaceRunException(Code.AGENT_RUNTIME_FAILED, "Agent Runtime 执行失败");
        }
        return ResponseData.success(mapper.response(result, context));
    }

    private AgentWorkspaceRunException runtimeUnavailable() {
        return new AgentWorkspaceRunException(Code.AGENT_RUNTIME_UNAVAILABLE, "Agent Runtime 尚未配置");
    }
}
