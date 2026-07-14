package yubi.agent.port;

import yubi.agent.domain.AgentModels.ToolMetric;

@FunctionalInterface
public interface AgentMetricsPort {
    void record(ToolMetric metric);
}
