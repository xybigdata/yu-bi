package yubi.agent.api;

import org.junit.jupiter.api.Test;
import yubi.agent.application.CreateChartWriteProposalTool;
import yubi.agent.application.DefaultReadOnlyToolRegistry;
import yubi.agent.application.DefaultWriteProposalToolRegistry;
import yubi.agent.application.RenameDashboardWriteProposalTool;
import yubi.agent.domain.ControlledWriteModels.NormalizedWriteProposal;
import yubi.agent.domain.StructuredValue.ObjectValue;
import yubi.agent.port.WriteProposalTool;
import yubi.query.api.MetadataToolSchema.Schema;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WriteToolProtocolTest {

    @Test
    void schemasAndRegistryMustExposeExactlyTwoApprovalRequiredWrites() {
        var registry = new DefaultWriteProposalToolRegistry(List.of(
                new CreateChartWriteProposalTool(), new RenameDashboardWriteProposalTool()));

        assertEquals(List.of("create_chart", "rename_dashboard"),
                registry.schemas().stream().map(WriteToolSchema::name).toList());
        assertTrue(registry.schemas().stream()
                .allMatch(schema -> schema.effect() == WriteToolSchema.Effect.WRITE
                        && schema.requiresApproval()));
        assertEquals(List.of("name", "viewId", "parentId", "description"),
                WriteToolSchemas.createChart().inputSchema().properties().keySet().stream().toList());
        assertEquals(List.of("dashboardId", "newName"),
                WriteToolSchemas.renameDashboard().inputSchema().properties().keySet().stream().toList());
        assertSame(WriteToolSchemas.createChart(), new CreateChartWriteProposalTool().schema());
        assertSame(WriteToolSchemas.renameDashboard(), new RenameDashboardWriteProposalTool().schema());
        assertThrows(UnsupportedOperationException.class, registry.schemas()::clear);

        // 新写注册表不能改变目标 F/G 的精确只读集合。
        assertEquals(List.of("search_data_assets", "describe_data_asset", "execute_view"),
                DefaultReadOnlyToolRegistry.TOOL_NAMES);
    }

    @Test
    void schemasMustExcludeIdentitySqlPermissionsAndHighRiskEffects() {
        Set<String> properties = new HashSet<>();
        WriteToolSchemas.all().forEach(schema -> collect(schema.inputSchema(), properties));
        for (String forbidden : List.of("userid", "organizationid", "orgid", "roleid", "permission",
                "permissions", "sql", "script", "sourceid", "sourceconfig", "preview", "status",
                "config", "widgettocreate", "widgettoupdate", "widgettodelete", "delete", "publish",
                "share", "token", "password", "idempotencykey", "approvalid")) {
            assertFalse(properties.contains(forbidden), () -> "写工具 Schema 含越界字段: " + forbidden);
        }
    }

    @Test
    void registryMustRejectHighRiskUnknownReadOrApprovalOptionalTools() {
        WriteProposalTool create = new CreateChartWriteProposalTool();
        WriteProposalTool rename = new RenameDashboardWriteProposalTool();

        for (String highRisk : List.of("delete_chart", "delete_dashboard", "publish_chart",
                "publish_dashboard", "share_dashboard", "change_permission", "execute_sql")) {
            assertThrows(IllegalArgumentException.class,
                    () -> new DefaultWriteProposalToolRegistry(List.of(create, fake(schema(highRisk,
                            WriteToolSchema.Effect.WRITE, true)))));
        }
        assertThrows(IllegalArgumentException.class,
                () -> new DefaultWriteProposalToolRegistry(List.of(create, fake(schema(
                        "rename_dashboard", WriteToolSchema.Effect.READ, true)))));
        assertThrows(IllegalArgumentException.class,
                () -> new DefaultWriteProposalToolRegistry(List.of(create, fake(schema(
                        "rename_dashboard", WriteToolSchema.Effect.WRITE, false)))));
        assertThrows(IllegalArgumentException.class,
                () -> new DefaultWriteProposalToolRegistry(List.of(create)));
        assertThrows(IllegalArgumentException.class,
                () -> new DefaultWriteProposalToolRegistry(List.of(create, rename, rename)));
        assertThrows(IllegalArgumentException.class,
                () -> new DefaultWriteProposalToolRegistry(List.of(
                        fake(WriteToolSchemas.createChart()), rename)));
    }

    private WriteToolSchema schema(String name, WriteToolSchema.Effect effect, boolean approval) {
        Schema empty = Schema.object("empty", Map.of(), List.of());
        return new WriteToolSchema(name, "test", empty, empty, effect, approval);
    }

    private WriteProposalTool fake(WriteToolSchema schema) {
        return new WriteProposalTool() {
            @Override
            public WriteToolSchema schema() {
                return schema;
            }

            @Override
            public NormalizedWriteProposal normalize(ObjectValue arguments) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private void collect(Schema schema, Set<String> names) {
        schema.properties().forEach((name, child) -> {
            names.add(name.toLowerCase(java.util.Locale.ROOT));
            collect(child, names);
        });
        if (schema.items() != null) {
            collect(schema.items(), names);
        }
    }
}
