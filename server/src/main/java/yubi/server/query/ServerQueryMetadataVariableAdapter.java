package yubi.server.query;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import yubi.query.api.DataAssetDetail.VariableScope;
import yubi.query.api.QueryExecutionContext;
import yubi.query.domain.QueryMetadataModels.VariableDescriptor;
import yubi.query.domain.QueryModels.ValueType;
import yubi.query.domain.QueryModels.VariableType;
import yubi.query.port.QueryMetadataVariablePort;
import yubi.server.base.dto.VariableValue;
import yubi.server.service.VariableService;

import java.util.ArrayList;
import java.util.List;

@Component
public class ServerQueryMetadataVariableAdapter implements QueryMetadataVariablePort {

    private final VariableService variableService;
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    public ServerQueryMetadataVariableAdapter(VariableService variableService) {
        this.variableService = variableService;
    }

    @Override
    public List<VariableDescriptor> load(String organizationId,
                                         String assetId,
                                         QueryExecutionContext context) {
        List<VariableDescriptor> variables = new ArrayList<>();
        variableService.listOrgValue(organizationId).stream()
                .map(value -> convert(value, VariableScope.ORGANIZATION))
                .forEach(variables::add);
        variableService.listViewVarValuesByUser(context.subjectId(), assetId).stream()
                .map(value -> convert(value, VariableScope.VIEW))
                .forEach(variables::add);
        return variables;
    }

    private VariableDescriptor convert(VariableValue value, VariableScope scope) {
        VariableType type = VariableType.valueOf(value.getType());
        boolean required = type == VariableType.QUERY && !hasDefaultValue(value.getDefaultValue());
        return new VariableDescriptor(value.getName(), value.getLabel(), type,
                ValueType.valueOf(value.getValueType()), required, value.isExpression(),
                value.getFormat(), scope);
    }

    private boolean hasDefaultValue(String defaultValue) {
        if (defaultValue == null || defaultValue.isBlank()) {
            return false;
        }
        try {
            JsonNode values = objectMapper.readTree(defaultValue);
            return values.isArray() && !values.isEmpty();
        } catch (Exception ignored) {
            return false;
        }
    }
}
