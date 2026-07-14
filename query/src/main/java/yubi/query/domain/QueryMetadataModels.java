package yubi.query.domain;

import yubi.query.api.DataAssetDetail.VariableScope;
import yubi.query.domain.QueryModels.ColumnMetadata;
import yubi.query.domain.QueryModels.ColumnSelection;
import yubi.query.domain.QueryModels.ValueType;
import yubi.query.domain.QueryModels.VariableType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class QueryMetadataModels {

    private QueryMetadataModels() {
    }

    public record AssetReference(String id,
                                 String organizationId,
                                 String name,
                                 String description,
                                 String parentId,
                                 String sourceId) {
    }

    public record MetadataDefinition(String viewId,
                                     String organizationId,
                                     String sourceId,
                                     String script,
                                     Map<String, ColumnMetadata> schema) {
        public MetadataDefinition {
            schema = schema == null
                    ? Map.of()
                    : Collections.unmodifiableMap(new LinkedHashMap<>(schema));
        }
    }

    public record MetadataAccessDecision(boolean organizationOwner,
                                         Set<ColumnSelection> allowedColumns,
                                         boolean scriptVisible) {
        public MetadataAccessDecision {
            allowedColumns = allowedColumns == null ? Set.of() : Set.copyOf(allowedColumns);
        }
    }

    public record VariableDescriptor(String name,
                                     String label,
                                     VariableType type,
                                     ValueType valueType,
                                     boolean required,
                                     boolean expression,
                                     String format,
                                     VariableScope scope) {
    }

    public record FunctionDescriptor(String name, String symbol) {
    }
}
