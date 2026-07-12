package yubi.query.api;

import yubi.query.domain.QueryModels.Aggregate;
import yubi.query.domain.QueryModels.ColumnSelection;
import yubi.query.domain.QueryModels.Filter;
import yubi.query.domain.QueryModels.FunctionColumn;
import yubi.query.domain.QueryModels.Group;
import yubi.query.domain.QueryModels.Order;
import yubi.query.domain.QueryModels.Page;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record ExecuteQueryCommand(String viewId,
                                  List<String> keywords,
                                  List<ColumnSelection> columns,
                                  List<FunctionColumn> functionColumns,
                                  List<Aggregate> aggregators,
                                  List<Filter> filters,
                                  List<Group> groups,
                                  List<Order> orders,
                                  Page page,
                                  Map<String, Set<String>> parameters,
                                  boolean concurrencyControl,
                                  boolean cache,
                                  int cacheExpires,
                                  boolean includeScript) {
    public ExecuteQueryCommand {
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
        columns = columns == null ? List.of() : List.copyOf(columns);
        functionColumns = functionColumns == null ? List.of() : List.copyOf(functionColumns);
        aggregators = aggregators == null ? List.of() : List.copyOf(aggregators);
        filters = filters == null ? List.of() : List.copyOf(filters);
        groups = groups == null ? List.of() : List.copyOf(groups);
        orders = orders == null ? List.of() : List.copyOf(orders);
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }

    public boolean empty() {
        return columns.isEmpty() && aggregators.isEmpty() && groups.isEmpty();
    }
}
