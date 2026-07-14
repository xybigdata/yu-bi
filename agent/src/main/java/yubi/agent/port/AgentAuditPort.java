package yubi.agent.port;

import yubi.agent.domain.AgentModels.AgentAuditEvent;

public interface AgentAuditPort {
    void record(AgentAuditEvent event);
}
