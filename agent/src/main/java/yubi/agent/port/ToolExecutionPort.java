package yubi.agent.port;

import yubi.agent.domain.AgentModels.ToolOutput;
import yubi.agent.domain.StructuredValue.ObjectValue;
import yubi.query.api.QueryExecutionContext;

@FunctionalInterface
public interface ToolExecutionPort {
    ToolOutput execute(ReadOnlyTool tool, ObjectValue arguments, QueryExecutionContext context);
}
