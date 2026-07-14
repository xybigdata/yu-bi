package yubi.agent.application;

import yubi.agent.domain.AgentModels.ResultSize;
import yubi.agent.domain.AgentModels.ToolOutput;
import yubi.agent.domain.StructuredValue;
import yubi.agent.domain.StructuredValue.ObjectValue;
import yubi.agent.domain.ToolResultLimits;
import yubi.agent.port.ReadOnlyTool;
import yubi.query.api.DataAssetDetail;
import yubi.query.api.DescribeDataAssetRequest;
import yubi.query.api.MetadataToolSchema;
import yubi.query.api.QueryExecutionContext;
import yubi.query.api.QueryMetadataToolSchemas;
import yubi.query.api.QueryMetadataUseCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class DescribeDataAssetTool implements ReadOnlyTool {

    private static final long MAXIMUM_METADATA_TEXT_BYTES = 1_024;
    private static final long MAXIMUM_SCRIPT_BYTES = 4_096;

    private final QueryMetadataUseCase useCase;
    private final ToolResultLimits limits;

    public DescribeDataAssetTool(QueryMetadataUseCase useCase, ToolResultLimits limits) {
        this.useCase = java.util.Objects.requireNonNull(useCase, "useCase");
        this.limits = java.util.Objects.requireNonNull(limits, "limits");
    }

    @Override
    public MetadataToolSchema schema() {
        return QueryMetadataToolSchemas.describeDataAsset();
    }

    @Override
    public ToolOutput execute(ObjectValue arguments, QueryExecutionContext context) {
        ToolArgumentReader reader = new ToolArgumentReader(arguments)
                .exact(Set.of("assetId", "includeScript"));
        String assetId = reader.requiredText("assetId", 200);
        boolean includeScript = reader.optionalBoolean("includeScript", false);
        DataAssetDetail detail = useCase.describe(new DescribeDataAssetRequest(assetId, includeScript), context);

        List<StructuredValue> fields = detail.fields().stream().map(value -> StructuredValues.object(
                "path", StructuredValues.array(value.path().stream().map(StructuredValues::text).toList()),
                "name", StructuredValues.text(value.name()),
                "valueType", StructuredValues.text(value.valueType().name()),
                "format", StructuredValues.text(value.format()))).map(StructuredValue.class::cast).toList();
        List<StructuredValue> variables = detail.variables().stream().map(value -> StructuredValues.object(
                "name", StructuredValues.text(value.name()),
                "label", StructuredValues.text(value.label()),
                "type", StructuredValues.text(value.type().name()),
                "valueType", StructuredValues.text(value.valueType().name()),
                "required", StructuredValues.bool(value.required()),
                "expression", StructuredValues.bool(value.expression()),
                "format", StructuredValues.text(value.format()),
                "scope", StructuredValues.text(value.scope().name()))).map(StructuredValue.class::cast).toList();
        List<StructuredValue> functions = detail.functions().stream().map(value -> StructuredValues.object(
                "name", StructuredValues.text(value.name()),
                "symbol", StructuredValues.text(value.symbol()))).map(StructuredValue.class::cast).toList();

        String requiredId = detail.id() == null ? "" : detail.id();
        String requiredName = detail.name() == null ? "" : detail.name();
        ObjectValue minimumOutput = output(StructuredValues.text(""), StructuredValues.text(""), null,
                List.of(), List.of(), List.of(), null);
        long requiredTextBytes = limits.maximumBytes() - StructuredValues.utf8Bytes(minimumOutput);
        if (requiredTextBytes < 0) {
            throw new IllegalStateException("describe_data_asset 最小必填输出超过字节上限");
        }
        long idAdditionalBytes = Math.min(MAXIMUM_METADATA_TEXT_BYTES - 2, requiredTextBytes / 2);
        StructuredValues.BoundedText id = StructuredValues.boundedText(requiredId, 2 + idAdditionalBytes);
        long remainingRequiredTextBytes = requiredTextBytes - Math.max(0, id.returnedBytes() - 2);
        long nameAdditionalBytes = Math.min(MAXIMUM_METADATA_TEXT_BYTES - 2, remainingRequiredTextBytes);
        StructuredValues.BoundedText name = StructuredValues.boundedText(requiredName, 2 + nameAdditionalBytes);
        StructuredValues.BoundedText description = bounded(detail.description(), MAXIMUM_METADATA_TEXT_BYTES);
        StructuredValues.BoundedText script = bounded(detail.script().orElse(null), MAXIMUM_SCRIPT_BYTES);

        List<StructuredValue> returnedFields = new ArrayList<>();
        List<StructuredValue> returnedVariables = new ArrayList<>();
        List<StructuredValue> returnedFunctions = new ArrayList<>();
        boolean truncated = detail.id() == null || detail.name() == null
                || id.truncated() || name.truncated() || description.truncated() || script.truncated();

        StructuredValue returnedId = id.value();
        StructuredValue returnedName = name.value();
        StructuredValue returnedDescription = description.value() != null
                && fits(returnedId, returnedName, description.value(), returnedFields,
                returnedVariables, returnedFunctions, null) ? description.value() : null;
        truncated |= description.value() != null && returnedDescription == null;
        StructuredValue returnedScript = script.value() != null
                && fits(returnedId, returnedName, returnedDescription, returnedFields,
                returnedVariables, returnedFunctions, script.value()) ? script.value() : null;
        truncated |= script.value() != null && returnedScript == null;

        int returnedItems = 0;
        boolean accepting = true;
        List<List<StructuredValue>> sourceGroups = List.of(fields, variables, functions);
        List<List<StructuredValue>> targetGroups = List.of(returnedFields, returnedVariables, returnedFunctions);
        for (int groupIndex = 0; groupIndex < sourceGroups.size(); groupIndex++) {
            for (StructuredValue value : sourceGroups.get(groupIndex)) {
                if (!accepting || returnedItems >= limits.maximumItems()) {
                    accepting = false;
                    truncated = true;
                    continue;
                }
                targetGroups.get(groupIndex).add(value);
                if (StructuredValues.utf8Bytes(output(returnedId, returnedName, returnedDescription,
                        returnedFields, returnedVariables, returnedFunctions, returnedScript))
                        > limits.maximumBytes()) {
                    targetGroups.get(groupIndex).removeLast();
                    accepting = false;
                    truncated = true;
                } else {
                    returnedItems++;
                }
            }
        }

        ObjectValue output = output(returnedId, returnedName, returnedDescription,
                returnedFields, returnedVariables, returnedFunctions, returnedScript);
        ObjectValue observedOutput = output(
                StructuredValues.text(requiredId),
                StructuredValues.text(requiredName),
                StructuredValues.text(detail.description()),
                fields, variables, functions,
                StructuredValues.text(detail.script().orElse(null)));
        long observedBytes = StructuredValues.utf8Bytes(observedOutput);
        long returnedBytes = StructuredValues.utf8Bytes(output);
        int observedItems = fields.size() + variables.size() + functions.size();
        truncated |= returnedItems < observedItems || returnedBytes < observedBytes;
        if (returnedBytes > limits.maximumBytes()) {
            throw new IllegalStateException("describe_data_asset 结果超过确定性字节上限");
        }
        return new ToolOutput(output, new ResultSize(observedItems, returnedItems, observedBytes,
                returnedBytes, limits.maximumItems(), limits.maximumBytes(), truncated));
    }

    private StructuredValues.BoundedText bounded(String value, long perFieldMaximum) {
        return StructuredValues.boundedText(value,
                Math.max(2, Math.min(perFieldMaximum, limits.maximumBytes() / 2)));
    }

    private boolean fits(StructuredValue id,
                         StructuredValue name,
                         StructuredValue description,
                         List<StructuredValue> fields,
                         List<StructuredValue> variables,
                         List<StructuredValue> functions,
                         StructuredValue script) {
        return StructuredValues.utf8Bytes(output(id, name, description,
                fields, variables, functions, script)) <= limits.maximumBytes();
    }

    private ObjectValue output(StructuredValue id,
                               StructuredValue name,
                               StructuredValue description,
                               List<StructuredValue> fields,
                               List<StructuredValue> variables,
                               List<StructuredValue> functions,
                               StructuredValue script) {
        return StructuredValues.object(
                "id", id,
                "name", name,
                "description", description,
                "fields", StructuredValues.array(fields),
                "variables", StructuredValues.array(variables),
                "functions", StructuredValues.array(functions),
                "script", script);
    }
}
