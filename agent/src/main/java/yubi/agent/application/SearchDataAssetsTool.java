package yubi.agent.application;

import yubi.agent.domain.AgentModels.ResultSize;
import yubi.agent.domain.AgentModels.ToolOutput;
import yubi.agent.domain.StructuredValue;
import yubi.agent.domain.StructuredValue.ObjectValue;
import yubi.agent.domain.ToolResultLimits;
import yubi.agent.port.ReadOnlyTool;
import yubi.query.api.DataAssetSummary;
import yubi.query.api.MetadataToolSchema;
import yubi.query.api.QueryExecutionContext;
import yubi.query.api.QueryMetadataToolSchemas;
import yubi.query.api.QueryMetadataUseCase;
import yubi.query.api.SearchDataAssetsRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class SearchDataAssetsTool implements ReadOnlyTool {

    private final QueryMetadataUseCase useCase;
    private final ToolResultLimits limits;

    public SearchDataAssetsTool(QueryMetadataUseCase useCase, ToolResultLimits limits) {
        this.useCase = java.util.Objects.requireNonNull(useCase, "useCase");
        this.limits = java.util.Objects.requireNonNull(limits, "limits");
    }

    @Override
    public MetadataToolSchema schema() {
        return QueryMetadataToolSchemas.searchDataAssets();
    }

    @Override
    public ToolOutput execute(ObjectValue arguments, QueryExecutionContext context) {
        ToolArgumentReader reader = new ToolArgumentReader(arguments).exact(Set.of("query", "limit"));
        String query = reader.requiredText("query", 200);
        int limit = Math.toIntExact(reader.optionalInteger("limit", 20, 1, 100));
        List<DataAssetSummary> assets = useCase.search(new SearchDataAssetsRequest(query, limit), context).assets();

        List<StructuredValue> returned = new ArrayList<>();
        ObjectValue emptyOutput = StructuredValues.object("assets", StructuredValues.array(List.of()));
        long observedBytes = StructuredValues.utf8Bytes(emptyOutput);
        long returnedBytes = observedBytes;
        boolean accepting = true;
        for (int index = 0; index < assets.size(); index++) {
            DataAssetSummary asset = assets.get(index);
            ObjectValue value = StructuredValues.object(
                    "id", StructuredValues.text(asset.id()),
                    "name", StructuredValues.text(asset.name()),
                    "description", StructuredValues.text(asset.description()));
            long bytes = StructuredValues.utf8Bytes(value);
            observedBytes += bytes + (index == 0 ? 0 : 1);
            long separator = returned.isEmpty() ? 0 : 1;
            if (accepting && returned.size() < limits.maximumItems()
                    && returnedBytes + separator + bytes <= limits.maximumBytes()) {
                returned.add(value);
                returnedBytes += separator + bytes;
            } else {
                accepting = false;
            }
        }
        boolean truncated = returned.size() < assets.size() || returnedBytes < observedBytes;
        ObjectValue output = StructuredValues.object("assets", StructuredValues.array(returned));
        returnedBytes = StructuredValues.utf8Bytes(output);
        return new ToolOutput(output, new ResultSize(assets.size(), returned.size(), observedBytes,
                returnedBytes, limits.maximumItems(), limits.maximumBytes(), truncated));
    }
}
