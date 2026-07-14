package yubi.agent.api;

import yubi.agent.domain.AgentModels.AgentSession;
import yubi.agent.domain.AgentModels.ResultSize;

public record AgentRunResult(AgentSession session, ResultSize resultSize) {
    public boolean truncated() {
        return resultSize != null && resultSize.truncated();
    }
}
