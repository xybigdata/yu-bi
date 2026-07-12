package yubi.agent.application;

import yubi.query.api.ExecuteQueryCommand;
import yubi.query.domain.QueryModels.Aggregate;
import yubi.query.domain.QueryModels.ColumnSelection;
import yubi.query.domain.QueryModels.Filter;
import yubi.query.domain.QueryModels.Group;
import yubi.query.domain.QueryModels.Order;
import yubi.query.domain.QueryModels.Page;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

record ExecuteViewInput(String viewId,
                        List<ColumnSelection> columns,
                        List<Aggregate> aggregators,
                        List<Filter> filters,
                        List<Group> groups,
                        List<Order> orders,
                        long pageNo,
                        long pageSize,
                        boolean countTotal,
                        Map<String, Set<String>> parameters) {

    ExecuteViewInput {
        columns = List.copyOf(columns);
        aggregators = List.copyOf(aggregators);
        filters = List.copyOf(filters);
        groups = List.copyOf(groups);
        orders = List.copyOf(orders);
        Map<String, Set<String>> frozen = new LinkedHashMap<>();
        parameters.forEach((key, values) -> frozen.put(key,
                Collections.unmodifiableSet(new java.util.LinkedHashSet<>(values))));
        parameters = Collections.unmodifiableMap(frozen);
    }

    ExecuteViewInput withParameters(Map<String, Set<String>> normalizedParameters) {
        return new ExecuteViewInput(viewId, columns, aggregators, filters, groups, orders,
                pageNo, pageSize, countTotal, normalizedParameters);
    }

    ExecuteQueryCommand command(int maximumRows) {
        long boundedPageSize = Math.min(pageSize, (long) maximumRows + 1L);
        return new ExecuteQueryCommand(viewId, List.of(), columns, List.of(), aggregators, filters, groups, orders,
                Page.request(pageNo, boundedPageSize, countTotal), parameters,
                false, false, 0, false);
    }
}
