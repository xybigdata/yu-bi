package yubi.query.api;

import yubi.query.domain.QueryModels.ColumnSelection;
import yubi.query.domain.QueryModels.ScriptType;
import yubi.query.domain.QueryModels.Variable;

import java.util.List;

public record PreviewQueryCommand(String sourceId,
                                  String script,
                                  ScriptType scriptType,
                                  List<ColumnSelection> columns,
                                  List<Variable> variables,
                                  int size) {
    public PreviewQueryCommand {
        columns = columns == null ? List.of() : List.copyOf(columns);
        variables = variables == null ? List.of() : List.copyOf(variables);
    }
}
