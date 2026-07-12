package yubi.query.api;

import yubi.query.domain.QueryModels.ValueType;
import yubi.query.domain.QueryModels.VariableType;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static yubi.query.api.MetadataToolSchema.Schema;

public final class QueryMetadataToolSchemas {

    private static final MetadataToolSchema SEARCH = searchSchema();
    private static final MetadataToolSchema DESCRIBE = describeSchema();
    private static final List<MetadataToolSchema> ALL = List.of(SEARCH, DESCRIBE);

    private QueryMetadataToolSchemas() {
    }

    public static List<MetadataToolSchema> all() {
        return ALL;
    }

    public static MetadataToolSchema searchDataAssets() {
        return SEARCH;
    }

    public static MetadataToolSchema describeDataAsset() {
        return DESCRIBE;
    }

    private static MetadataToolSchema searchSchema() {
        Schema summary = Schema.object("可读取的数据资产摘要", properties(
                "id", Schema.string("数据资产标识"),
                "name", Schema.string("数据资产名称"),
                "description", Schema.string("数据资产说明")
        ), List.of("id", "name"));
        return new MetadataToolSchema(
                "search_data_assets",
                "按名称或说明搜索当前可信上下文中可读取的数据资产",
                Schema.object("资产搜索输入", properties(
                        "query", Schema.string("非空搜索词"),
                        "limit", Schema.integer("返回数量，范围 1 到 100")
                ), List.of("query")),
                Schema.object("资产搜索输出", properties(
                        "assets", Schema.array("按名称和标识稳定排序的资产", summary)
                ), List.of("assets")),
                true);
    }

    private static MetadataToolSchema describeSchema() {
        Schema field = Schema.object("授权字段", properties(
                "path", Schema.array("可用于查询的完整字段路径", Schema.string("路径片段")),
                "name", Schema.string("点分隔字段名"),
                "valueType", Schema.enumeration("Query 值类型", enumNames(ValueType.values())),
                "format", Schema.string("可选格式")
        ), List.of("path", "name", "valueType"));
        Schema variable = Schema.object("不含值的变量描述", properties(
                "name", Schema.string("变量名"),
                "label", Schema.string("可选显示名称"),
                "type", Schema.enumeration("变量类型", enumNames(VariableType.values())),
                "valueType", Schema.enumeration("Query 值类型", enumNames(ValueType.values())),
                "required", Schema.bool("调用方是否需要提供值"),
                "expression", Schema.bool("变量是否按表达式处理"),
                "format", Schema.string("可选格式"),
                "scope", Schema.enumeration("变量作用域", List.of("ORGANIZATION", "VIEW"))
        ), List.of("name", "type", "valueType", "required", "expression", "scope"));
        Schema function = Schema.object("数据源支持的标准函数", properties(
                "name", Schema.string("稳定函数名"),
                "symbol", Schema.string("查询表达式使用的符号")
        ), List.of("name", "symbol"));
        return new MetadataToolSchema(
                "describe_data_asset",
                "描述当前可信上下文中可读取的数据资产及其授权字段、变量和标准函数",
                Schema.object("资产详情输入", properties(
                        "assetId", Schema.string("数据资产标识"),
                        "includeScript", Schema.bool("仅在具有 MANAGE 权限时请求原始查询脚本")
                ), List.of("assetId")),
                Schema.object("资产详情输出", properties(
                        "id", Schema.string("数据资产标识"),
                        "name", Schema.string("数据资产名称"),
                        "description", Schema.string("数据资产说明"),
                        "fields", Schema.array("按完整路径稳定排序的授权字段", field),
                        "variables", Schema.array("不含默认值或权限值的变量", variable),
                        "functions", Schema.array("按名称稳定排序的标准函数", function),
                        "script", Schema.string("仅在请求且具有 MANAGE 权限时出现的查询脚本")
                ), List.of("id", "name", "fields", "variables", "functions")),
                true);
    }

    private static List<String> enumNames(Enum<?>[] values) {
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
