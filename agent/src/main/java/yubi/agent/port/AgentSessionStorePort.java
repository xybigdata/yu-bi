package yubi.agent.port;

import yubi.agent.domain.AgentModels.AgentSession;

public interface AgentSessionStorePort {
    /** 接收不含用户消息、最终答案和 Tool payload 的脱敏状态快照。 */
    void save(AgentSession session);
}
