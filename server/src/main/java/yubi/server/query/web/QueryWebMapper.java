package yubi.server.query.web;

import org.springframework.stereotype.Component;
import yubi.query.api.ExecuteQueryCommand;
import yubi.query.api.PreviewQueryCommand;
import yubi.query.api.QueryResult;
import yubi.query.api.QueryValidationException;
import yubi.query.domain.QueryModels;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class QueryWebMapper {

    public ExecuteQueryCommand toCommand(QueryExecuteRequest source) {
        requireRequest(source);
        requireElements(source.keywords());
        requireParameters(source.params());
        return new ExecuteQueryCommand(source.viewId(), source.keywords(),
                map(source.columns(), this::column),
                map(source.functionColumns(), value -> new QueryModels.FunctionColumn(value.alias(), value.snippet())),
                map(source.aggregators(), this::aggregate), map(source.filters(), this::filter),
                map(source.groups(), this::group),
                map(source.orders(), this::order), page(source.pageInfo()), source.params(),
                Boolean.TRUE.equals(source.concurrencyControl()), Boolean.TRUE.equals(source.cache()),
                source.cacheExpires() == null ? 0 : source.cacheExpires(), Boolean.TRUE.equals(source.script()));
    }

    public PreviewQueryCommand toCommand(QueryPreviewRequest source) {
        requireRequest(source);
        return new PreviewQueryCommand(source.sourceId(), source.script(),
                enumValue(QueryModels.ScriptType.class, source.scriptType()),
                map(source.columns(), this::column), map(source.variables(), this::variable),
                source.size() == null ? 100 : source.size());
    }

    public QueryResponse toResponse(QueryResult source) {
        return new QueryResponse(source.id(), source.name(), source.visualizationType(), source.visualizationId(),
                source.columns().stream().map(value -> new QueryResponse.Column(value.name(), value.type().name(),
                        value.format(), value.foreignKeys().stream()
                                .map(key -> new QueryResponse.ForeignKey(key.database(), key.table(), key.column()))
                                .toList())).toList(),
                source.rows(), source.page() == null ? null : new QueryResponse.PageInfo(source.page().pageNo(),
                source.page().pageSize(), source.page().total(), source.page().countTotal()), source.script());
    }

    private QueryModels.ColumnSelection column(QueryExecuteRequest.Column value) {
        requireElements(value.column());
        return new QueryModels.ColumnSelection(value.alias(), value.column());
    }

    private QueryModels.Aggregate aggregate(QueryExecuteRequest.Aggregate value) {
        requireElements(value.column());
        return new QueryModels.Aggregate(enumValue(QueryModels.AggregateType.class, value.sqlOperator()),
                value.alias(), value.column());
    }

    private QueryModels.Filter filter(QueryExecuteRequest.Filter value) {
        requireElements(value.column());
        return new QueryModels.Filter(enumValue(QueryModels.AggregateType.class, value.aggOperator()),
                enumValue(QueryModels.FilterType.class, value.sqlOperator()), value.column(),
                map(value.values(), item -> new QueryModels.TypedValue(item.value(),
                        enumValue(QueryModels.ValueType.class, item.valueType()), item.format())));
    }

    private QueryModels.Group group(QueryExecuteRequest.Group value) {
        requireElements(value.column());
        return new QueryModels.Group(value.alias(), value.column());
    }

    private QueryModels.Order order(QueryExecuteRequest.Order value) {
        requireElements(value.column());
        return new QueryModels.Order(enumValue(QueryModels.AggregateType.class, value.aggOperator()),
                enumValue(QueryModels.OrderType.class, value.operator()), value.column());
    }

    private QueryModels.Variable variable(QueryPreviewRequest.Variable value) {
        requireElements(value.values());
        return new QueryModels.Variable(value.name(), enumValue(QueryModels.VariableType.class, value.type()),
                enumValue(QueryModels.ValueType.class, value.valueType()), value.values(),
                Boolean.TRUE.equals(value.expression()), Boolean.TRUE.equals(value.disabled()), value.format());
    }

    private QueryModels.Page page(QueryExecuteRequest.PageInfo value) {
        return value == null ? null : new QueryModels.Page(number(value.pageNo()), number(value.pageSize()),
                number(value.total()), Boolean.TRUE.equals(value.countTotal()));
    }

    private long number(Long value) {
        return value == null ? 0 : value;
    }

    private <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            throw invalidRequest();
        }
    }

    private <T, R> List<R> map(List<T> values, java.util.function.Function<T, R> mapper) {
        requireElements(values);
        return values == null ? List.of() : values.stream().map(mapper).toList();
    }

    private void requireRequest(Object request) {
        if (request == null) {
            throw invalidRequest();
        }
    }

    private void requireElements(Collection<?> values) {
        if (values != null && values.stream().anyMatch(java.util.Objects::isNull)) {
            throw invalidRequest();
        }
    }

    private void requireParameters(Map<String, ? extends Collection<?>> parameters) {
        if (parameters == null) {
            return;
        }
        for (Map.Entry<String, ? extends Collection<?>> entry : parameters.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                throw invalidRequest();
            }
            requireElements(entry.getValue());
        }
    }

    private QueryValidationException invalidRequest() {
        return new QueryValidationException("查询请求参数无效");
    }
}
