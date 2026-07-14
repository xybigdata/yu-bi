package yubi.agent.application;

import yubi.agent.api.ExecuteViewToolSchema;
import yubi.agent.domain.AgentModels.ToolOutput;
import yubi.agent.domain.StructuredValue.ObjectValue;
import yubi.agent.domain.ToolResultLimits;
import yubi.agent.domain.ToolExecutionPolicy;
import yubi.agent.port.ReadOnlyTool;
import yubi.query.api.ExecuteQueryUseCase;
import yubi.query.api.MetadataToolSchema;
import yubi.query.api.QueryExecutionContext;
import yubi.query.api.QueryMetadataUseCase;

public final class ExecuteViewTool implements ReadOnlyTool {

    private final ExecuteQueryUseCase executeUseCase;
    private final ExecuteViewInputMapper inputMapper;
    private final ExecuteViewAuthorizationGuard authorizationGuard;
    private final ExecuteViewOutputMapper outputMapper;
    private final ToolResultLimits limits;

    public ExecuteViewTool(ExecuteQueryUseCase executeUseCase,
                           QueryMetadataUseCase metadataUseCase,
                           ToolResultLimits limits) {
        this(executeUseCase, metadataUseCase, limits, ToolExecutionPolicy.defaults());
    }

    public ExecuteViewTool(ExecuteQueryUseCase executeUseCase,
                           QueryMetadataUseCase metadataUseCase,
                           ToolResultLimits limits,
                           ToolExecutionPolicy executionPolicy) {
        this.executeUseCase = java.util.Objects.requireNonNull(executeUseCase, "executeUseCase");
        this.limits = java.util.Objects.requireNonNull(limits, "limits");
        this.inputMapper = new ExecuteViewInputMapper(
                java.util.Objects.requireNonNull(executionPolicy, "executionPolicy"));
        this.authorizationGuard = new ExecuteViewAuthorizationGuard(
                java.util.Objects.requireNonNull(metadataUseCase, "metadataUseCase"));
        this.outputMapper = new ExecuteViewOutputMapper(limits);
    }

    @Override
    public MetadataToolSchema schema() {
        return ExecuteViewToolSchema.schema();
    }

    @Override
    public ToolOutput execute(ObjectValue arguments, QueryExecutionContext context) {
        ExecuteViewInput input = inputMapper.map(arguments);
        ExecuteViewInput authorized = authorizationGuard.authorize(input, context);
        return outputMapper.map(executeUseCase.execute(authorized.command(limits.maximumItems()), context), authorized);
    }
}
