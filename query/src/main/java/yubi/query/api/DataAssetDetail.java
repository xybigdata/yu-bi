package yubi.query.api;

import yubi.query.domain.QueryModels.ValueType;
import yubi.query.domain.QueryModels.VariableType;

import java.util.List;
import java.util.Optional;

public record DataAssetDetail(String id,
                              String name,
                              String description,
                              List<FieldDescription> fields,
                              List<VariableDescription> variables,
                              List<FunctionDescription> functions,
                              Optional<String> script) {

    public DataAssetDetail {
        fields = fields == null ? List.of() : List.copyOf(fields);
        variables = variables == null ? List.of() : List.copyOf(variables);
        functions = functions == null ? List.of() : List.copyOf(functions);
        script = script == null ? Optional.empty() : script;
    }

    public record FieldDescription(List<String> path, String name, ValueType valueType, String format) {
        public FieldDescription {
            path = path == null ? List.of() : List.copyOf(path);
        }
    }

    public record VariableDescription(String name,
                                      String label,
                                      VariableType type,
                                      ValueType valueType,
                                      boolean required,
                                      boolean expression,
                                      String format,
                                      VariableScope scope) {
    }

    public record FunctionDescription(String name, String symbol) {
    }

    public enum VariableScope { ORGANIZATION, VIEW }
}
