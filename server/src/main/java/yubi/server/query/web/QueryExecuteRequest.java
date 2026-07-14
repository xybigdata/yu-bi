package yubi.server.query.web;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record QueryExecuteRequest(String viewId,
                                  List<String> keywords,
                                  List<Column> columns,
                                  List<FunctionColumn> functionColumns,
                                  List<Aggregate> aggregators,
                                  List<Filter> filters,
                                  List<Group> groups,
                                  List<Order> orders,
                                  PageInfo pageInfo,
                                  Map<String, Set<String>> params,
                                  Boolean concurrencyControl,
                                  String concurrencyControlMode,
                                  Boolean cache,
                                  Integer cacheExpires,
                                  Boolean script) {

    public record Column(String alias, List<String> column) {
    }

    public record FunctionColumn(String alias, String snippet) {
    }

    public record Aggregate(String sqlOperator, String alias, List<String> column) {
    }

    public record Filter(String aggOperator, String sqlOperator, List<String> column, List<TypedValue> values) {
    }

    public record TypedValue(Object value, String valueType, String format) {
    }

    public record Group(String alias, List<String> column) {
    }

    public record Order(String aggOperator, String operator, List<String> column) {
    }

    public record PageInfo(Long pageNo, Long pageSize, Long total, Boolean countTotal) {
    }
}
