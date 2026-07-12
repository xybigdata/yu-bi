package yubi.agent.port;

import yubi.agent.domain.AgentModels.ToolOutput;
import yubi.agent.domain.StructuredValue.ObjectValue;
import yubi.query.api.MetadataToolSchema;
import yubi.query.api.QueryExecutionContext;

public interface ReadOnlyTool {
    MetadataToolSchema schema();

    ToolOutput execute(ObjectValue arguments, QueryExecutionContext context);
}
