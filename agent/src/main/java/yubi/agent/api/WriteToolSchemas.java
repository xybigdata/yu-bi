package yubi.agent.api;

import yubi.query.api.MetadataToolSchema.Schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WriteToolSchemas {

    private static final Schema PREVIEW = previewSchema();
    private static final WriteToolSchema CREATE_CHART = createChartSchema();
    private static final WriteToolSchema RENAME_DASHBOARD = renameDashboardSchema();
    private static final List<WriteToolSchema> ALL = List.of(CREATE_CHART, RENAME_DASHBOARD);

    private WriteToolSchemas() {
    }

    public static WriteToolSchema createChart() {
        return CREATE_CHART;
    }

    public static WriteToolSchema renameDashboard() {
        return RENAME_DASHBOARD;
    }

    public static List<WriteToolSchema> all() {
        return ALL;
    }

    private static WriteToolSchema createChartSchema() {
        return new WriteToolSchema(
                "create_chart",
                "在当前可信组织中基于已有可读数据视图创建未发布图表草稿",
                Schema.object("创建图表输入", properties(
                        "name", Schema.string("图表名称"),
                        "viewId", Schema.string("已有数据视图标识"),
                        "parentId", Schema.string("可选目标文件夹标识"),
                        "description", Schema.string("可选图表说明")
                ), List.of("name", "viewId")),
                PREVIEW,
                WriteToolSchema.Effect.WRITE,
                true);
    }

    private static WriteToolSchema renameDashboardSchema() {
        return new WriteToolSchema(
                "rename_dashboard",
                "重命名当前可信组织中具有管理权限的仪表盘",
                Schema.object("重命名仪表盘输入", properties(
                        "dashboardId", Schema.string("已有仪表盘标识"),
                        "newName", Schema.string("仪表盘新名称")
                ), List.of("dashboardId", "newName")),
                PREVIEW,
                WriteToolSchema.Effect.WRITE,
                true);
    }

    private static Schema previewSchema() {
        Schema field = Schema.object("参数预览项", properties(
                "label", Schema.string("稳定显示标签"),
                "value", Schema.string("经过严格映射的参数值")
        ), List.of("label", "value"));
        return Schema.object("待审批写操作预览", properties(
                "title", Schema.string("操作标题"),
                "resourceType", Schema.string("受影响资源类型"),
                "targetId", Schema.string("现有目标资源标识"),
                "fields", Schema.array("规范化参数", field),
                "impacts", Schema.array("有限影响说明", Schema.string("影响"))
        ), List.of("title", "resourceType", "fields", "impacts"));
    }

    private static Map<String, Schema> properties(Object... entries) {
        Map<String, Schema> properties = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            properties.put((String) entries[index], (Schema) entries[index + 1]);
        }
        return properties;
    }
}
