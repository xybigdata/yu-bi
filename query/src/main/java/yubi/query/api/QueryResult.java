package yubi.query.api;

import yubi.query.domain.QueryModels.ColumnMetadata;
import yubi.query.domain.QueryModels.EngineResult;
import yubi.query.domain.QueryModels.Page;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public record QueryResult(String id,
                          String name,
                          String visualizationType,
                          String visualizationId,
                          List<ColumnMetadata> columns,
                          List<List<Object>> rows,
                          Page page,
                          String script) {
    public QueryResult {
        columns = columns == null ? List.of() : List.copyOf(columns);
        rows = rows == null ? List.of() : rows.stream()
                .map(row -> Collections.unmodifiableList(new ArrayList<>(row)))
                .toList();
    }

    public static QueryResult from(EngineResult result, String visibleScript) {
        return new QueryResult(result.id(), result.name(), result.visualizationType(),
                result.visualizationId(), result.columns(), result.rows(), result.page(), visibleScript);
    }

    public static QueryResult empty() {
        return new QueryResult(null, null, null, null, List.of(), List.of(), null, null);
    }
}
