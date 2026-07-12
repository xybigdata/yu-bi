package yubi.agent.application;

import yubi.agent.domain.StructuredValue;
import yubi.agent.domain.StructuredValue.ObjectValue;
import yubi.agent.domain.ToolResultLimits;
import yubi.query.domain.QueryModels.Aggregate;
import yubi.query.domain.QueryModels.AggregateType;
import yubi.query.domain.QueryModels.ColumnSelection;
import yubi.query.domain.QueryModels.Filter;
import yubi.query.domain.QueryModels.FilterType;
import yubi.query.domain.QueryModels.Group;
import yubi.query.domain.QueryModels.Order;
import yubi.query.domain.QueryModels.OrderType;
import yubi.query.domain.QueryModels.TypedValue;
import yubi.query.domain.QueryModels.ValueType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

final class ExecuteViewInputMapper {

    private static final Set<String> ROOT_FIELDS = Set.of(
            "viewId", "columns", "aggregators", "filters", "groups", "orders", "page", "parameters");
    private static final Set<ValueType> SAFE_VALUE_TYPES = Set.of(
            ValueType.STRING, ValueType.NUMERIC, ValueType.DATE, ValueType.BOOLEAN);
    private static final int MAX_COLLECTION_SIZE = 100;
    private final ToolResultLimits limits;

    ExecuteViewInputMapper(ToolResultLimits limits) {
        this.limits = limits;
    }

    ExecuteViewInput map(ObjectValue arguments) {
        ToolArgumentReader root = new ToolArgumentReader(arguments).exact(ROOT_FIELDS);
        String viewId = root.requiredText("viewId", 200);
        List<ColumnSelection> columns = map(root.array("columns", MAX_COLLECTION_SIZE), this::column);
        List<Aggregate> aggregators = map(root.array("aggregators", MAX_COLLECTION_SIZE), this::aggregate);
        List<Filter> filters = map(root.array("filters", MAX_COLLECTION_SIZE), this::filter);
        List<Group> groups = map(root.array("groups", MAX_COLLECTION_SIZE), this::group);
        List<Order> orders = map(root.array("orders", MAX_COLLECTION_SIZE), this::order);
        if (columns.isEmpty() && aggregators.isEmpty() && groups.isEmpty()) {
            throw ToolArgumentReader.invalid("execute_view 必须包含选择列、聚合或分组");
        }

        ToolArgumentReader page = new ToolArgumentReader(root.optionalObject("page"))
                .exact(Set.of("pageNo", "pageSize", "countTotal"));
        long pageNo = page.optionalInteger("pageNo", 1, 1, Integer.MAX_VALUE);
        long pageSize = page.optionalInteger("pageSize", (long) limits.maximumItems() + 1L,
                1, 10_000);
        boolean countTotal = page.optionalBoolean("countTotal", false);
        Map<String, Set<String>> parameters = parameters(root.array("parameters", MAX_COLLECTION_SIZE));
        return new ExecuteViewInput(viewId, columns, aggregators, filters, groups, orders,
                pageNo, pageSize, countTotal, parameters);
    }

    private ColumnSelection column(StructuredValue value) {
        ToolArgumentReader reader = nested(value, Set.of("alias", "path"));
        return new ColumnSelection(alias(reader), path(reader, "path"));
    }

    private Aggregate aggregate(StructuredValue value) {
        ToolArgumentReader reader = nested(value, Set.of("operator", "alias", "column"));
        return new Aggregate(requiredEnum(reader, "operator", AggregateType.class),
                alias(reader), path(reader, "column"));
    }

    private Filter filter(StructuredValue value) {
        ToolArgumentReader reader = nested(value, Set.of("operator", "column", "values"));
        FilterType operator = requiredEnum(reader, "operator", FilterType.class);
        List<TypedValue> values = map(reader.array("values", MAX_COLLECTION_SIZE), this::typedValue);
        requireValidFilterCardinality(operator, values.size());
        return new Filter(null, operator, path(reader, "column"), values);
    }

    private void requireValidFilterCardinality(FilterType operator, int size) {
        boolean valid = switch (operator) {
            case EQ, NE, GT, LT, GTE, LTE,
                    LIKE, PREFIX_LIKE, SUFFIX_LIKE,
                    NOT_LIKE, PREFIX_NOT_LIKE, SUFFIX_NOT_LIKE -> size == 1;
            case IN, NOT_IN -> size >= 1;
            case BETWEEN, NOT_BETWEEN -> size == 2;
            case IS_NULL, NOT_NULL -> size == 0;
        };
        if (!valid) {
            throw ToolArgumentReader.invalid("过滤值数量与运算符不匹配");
        }
    }

    private TypedValue typedValue(StructuredValue value) {
        ToolArgumentReader reader = nested(value, Set.of("value", "valueType", "format"));
        String raw = reader.requiredText("value", 4_000);
        ValueType valueType = requiredEnum(reader, "valueType", ValueType.class);
        if (!SAFE_VALUE_TYPES.contains(valueType)) {
            throw ToolArgumentReader.invalid("过滤值类型不允许进入 execute_view");
        }
        return new TypedValue(SafeScalarValues.normalize(raw, valueType), valueType, format(reader));
    }

    private Group group(StructuredValue value) {
        ToolArgumentReader reader = nested(value, Set.of("alias", "column"));
        return new Group(alias(reader), path(reader, "column"));
    }

    private Order order(StructuredValue value) {
        ToolArgumentReader reader = nested(value, Set.of("aggregateOperator", "operator", "column"));
        return new Order(optionalEnum(reader, "aggregateOperator", AggregateType.class),
                requiredEnum(reader, "operator", OrderType.class), path(reader, "column"));
    }

    private Map<String, Set<String>> parameters(List<StructuredValue> values) {
        Map<String, Set<String>> parameters = new LinkedHashMap<>();
        for (StructuredValue value : values) {
            ToolArgumentReader reader = nested(value, Set.of("name", "values"));
            String name = reader.requiredText("name", 200);
            LinkedHashSet<String> parameterValues = new LinkedHashSet<>();
            for (StructuredValue item : reader.array("values", MAX_COLLECTION_SIZE)) {
                parameterValues.add(ToolArgumentReader.text(item, 4_000));
            }
            if (parameterValues.isEmpty()) {
                throw ToolArgumentReader.invalid("Query 变量参数值不能为空");
            }
            if (parameters.putIfAbsent(name,
                    java.util.Collections.unmodifiableSet(new LinkedHashSet<>(parameterValues))) != null) {
                throw ToolArgumentReader.invalid("Query 变量参数重复");
            }
        }
        return parameters;
    }

    private List<String> path(ToolArgumentReader reader, String name) {
        List<StructuredValue> values = reader.array(name, 16);
        if (values.isEmpty()) {
            throw ToolArgumentReader.invalid("字段路径不能为空");
        }
        return values.stream().map(value -> ToolArgumentReader.text(value, 200)).toList();
    }

    private ToolArgumentReader nested(StructuredValue value, Set<String> fields) {
        return new ToolArgumentReader(ToolArgumentReader.object(value)).exact(fields);
    }

    private <E extends Enum<E>> E requiredEnum(ToolArgumentReader reader, String name, Class<E> type) {
        String value = reader.requiredText(name, 100);
        return enumValue(value, type);
    }

    private <E extends Enum<E>> E optionalEnum(ToolArgumentReader reader, String name, Class<E> type) {
        String value = reader.optionalText(name, 100);
        return value == null ? null : enumValue(value, type);
    }

    private <E extends Enum<E>> E enumValue(String value, Class<E> type) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            throw ToolArgumentReader.invalid("工具枚举参数无效");
        }
    }

    private String alias(ToolArgumentReader reader) {
        String value = reader.optionalText("alias", 100);
        if (value != null && !value.matches("[A-Za-z_][A-Za-z0-9_]{0,99}")) {
            throw ToolArgumentReader.invalid("结果别名必须是安全标识符");
        }
        return value;
    }

    private String format(ToolArgumentReader reader) {
        String value = reader.optionalText("format", 100);
        if (value != null && !value.matches("[A-Za-z0-9_:/%. -]{0,100}")) {
            throw ToolArgumentReader.invalid("过滤值格式无效");
        }
        return value;
    }

    private <T> List<T> map(List<StructuredValue> values, Function<StructuredValue, T> mapper) {
        List<T> result = new ArrayList<>(values.size());
        values.forEach(value -> result.add(mapper.apply(value)));
        return List.copyOf(result);
    }
}
