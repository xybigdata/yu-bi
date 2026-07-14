package yubi.agent.api;

import yubi.query.api.MetadataToolSchema;
import yubi.query.domain.QueryModels.AggregateType;
import yubi.query.domain.QueryModels.FilterType;
import yubi.query.domain.QueryModels.OrderType;
import yubi.query.domain.QueryModels.ValueType;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static yubi.query.api.MetadataToolSchema.Schema;

public final class ExecuteViewToolSchema {

    private static final List<String> SAFE_VALUE_TYPES = List.of(
            ValueType.STRING.name(), ValueType.NUMERIC.name(), ValueType.DATE.name(), ValueType.BOOLEAN.name());
    private static final MetadataToolSchema SCHEMA = create();

    private ExecuteViewToolSchema() {
    }

    public static MetadataToolSchema schema() {
        return SCHEMA;
    }

    private static MetadataToolSchema create() {
        Schema path = Schema.array("授权字段的完整路径", Schema.string("路径片段"));
        Schema column = Schema.object("选择列", properties(
                "alias", Schema.string("可选结果别名"),
                "path", path
        ), List.of("path"));
        Schema aggregate = Schema.object("结构化聚合", properties(
                "operator", Schema.enumeration("聚合函数", names(AggregateType.values())),
                "alias", Schema.string("可选结果别名"),
                "column", path
        ), List.of("operator", "column"));
        Schema filterValue = Schema.object("安全标量过滤值", properties(
                "value", Schema.string("标量文本值"),
                "valueType", Schema.enumeration("安全值类型", SAFE_VALUE_TYPES),
                "format", Schema.string("可选格式")
        ), List.of("value", "valueType"));
        Schema filter = Schema.object("结构化过滤条件", properties(
                "operator", Schema.enumeration("过滤运算符", names(FilterType.values())),
                "column", path,
                "values", Schema.array("过滤值：单值比较恰好 1 个，IN 至少 1 个，BETWEEN 恰好 2 个，空值判断不传值",
                        filterValue)
        ), List.of("operator", "column"));
        Schema group = Schema.object("结构化分组", properties(
                "alias", Schema.string("可选结果别名"),
                "column", path
        ), List.of("column"));
        Schema order = Schema.object("结构化排序", properties(
                "aggregateOperator", Schema.enumeration("可选聚合函数", names(AggregateType.values())),
                "operator", Schema.enumeration("排序方向", names(OrderType.values())),
                "column", path
        ), List.of("operator", "column"));
        Schema page = Schema.object("有界分页", properties(
                "pageNo", Schema.integer("页码，从 1 开始"),
                "pageSize", Schema.integer("页大小，由运行时上限约束"),
                "countTotal", Schema.bool("是否计算总行数")
        ), List.of());
        Schema parameter = Schema.object("已有 View 的 Query 变量", properties(
                "name", Schema.string("变量名"),
                "values", Schema.array("安全标量值", Schema.string("变量值"))
        ), List.of("name", "values"));
        Schema resultColumn = Schema.object("结果列", properties(
                "name", path,
                "valueType", Schema.enumeration("Query 值类型", names(ValueType.values())),
                "format", Schema.string("可选格式")
        ), List.of("name", "valueType"));
        Schema cell = Schema.object("稳定文本化单元格", properties(
                "value", Schema.string("非空单元格文本"),
                "isNull", Schema.bool("是否为空值")
        ), List.of("isNull"));
        Schema resultPage = Schema.object("结果分页", properties(
                "pageNo", Schema.integer("页码"),
                "pageSize", Schema.integer("页大小"),
                "total", Schema.integer("总行数"),
                "countTotal", Schema.bool("是否计算总行数")
        ), List.of("pageNo", "pageSize", "total", "countTotal"));

        return new MetadataToolSchema(
                "execute_view",
                "使用结构化只读参数执行当前可信上下文中已有且已授权的 View",
                Schema.object("已有 View 查询输入", properties(
                        "viewId", Schema.string("已有 View 标识"),
                        "columns", Schema.array("选择授权字段", column),
                        "aggregators", Schema.array("聚合", aggregate),
                        "filters", Schema.array("过滤", filter),
                        "groups", Schema.array("分组", group),
                        "orders", Schema.array("排序", order),
                        "page", page,
                        "parameters", Schema.array("Query 变量参数", parameter)
                ), List.of("viewId")),
                Schema.object("有界查询结果", properties(
                        "id", Schema.string("结果标识"),
                        "name", Schema.string("结果名称"),
                        "visualizationType", Schema.string("可选可视化类型"),
                        "visualizationId", Schema.string("可选可视化标识"),
                        "columns", Schema.array("结果列", resultColumn),
                        "rows", Schema.array("有界结果行", Schema.array("结果行", cell)),
                        "page", resultPage
                ), List.of("columns", "rows")),
                true);
    }

    private static List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }

    private static Map<String, Schema> properties(Object... entries) {
        Map<String, Schema> properties = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            properties.put((String) entries[index], (Schema) entries[index + 1]);
        }
        return properties;
    }
}
