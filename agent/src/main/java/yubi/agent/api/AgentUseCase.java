package yubi.agent.api;

public interface AgentUseCase {
    AgentRunResult run(AgentRequest request, AgentExecutionContext context);
}
