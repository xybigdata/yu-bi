package yubi.agent.api;

import org.junit.jupiter.api.Test;
import yubi.agent.application.DefaultReadOnlyToolRegistry;
import yubi.agent.application.DescribeDataAssetTool;
import yubi.agent.application.SearchDataAssetsTool;
import yubi.agent.domain.ToolResultLimits;
import yubi.agent.port.ReadOnlyTool;
import yubi.query.api.MetadataToolSchema;
import yubi.query.api.QueryMetadataToolSchemas;
import yubi.query.api.QueryMetadataUseCase;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentToolSchemaTest {

    @Test
    void executeViewSchemaMustBeStableReadOnlyAndExcludeUnsafeCapabilities() {
        MetadataToolSchema schema = ExecuteViewToolSchema.schema();

        assertSame(schema, ExecuteViewToolSchema.schema());
        assertEquals("execute_view", schema.name());
        assertTrue(schema.readOnly());
        assertEquals(List.of("viewId", "columns", "aggregators", "filters", "groups", "orders",
                "page", "parameters"), schema.inputSchema().properties().keySet().stream().toList());
        MetadataToolSchema.Schema filterSchema = schema.inputSchema().properties().get("filters").items();
        assertFalse(filterSchema.properties().containsKey("aggregateOperator"));
        assertEquals(List.of("operator", "column", "values"),
                filterSchema.properties().keySet().stream().toList());

        Set<String> inputFields = propertyNames(schema.inputSchema());
        for (String forbidden : List.of("userid", "organizationid", "orgid", "roleid", "permission",
                "sql", "script", "snippet", "sourceid", "sourceconfig", "preview", "write",
                "create", "update", "delete", "publish", "share", "token", "password")) {
            assertFalse(inputFields.contains(forbidden), () -> "execute_view Schema 包含越界字段: " + forbidden);
        }
        assertThrows(UnsupportedOperationException.class, schema.inputSchema().properties()::clear);
    }

    @Test
    void registryMustContainExactlyThreeReadOnlyTools() {
        List<ReadOnlyTool> tools = List.of(
                tool("search_data_assets"), tool("describe_data_asset"), tool("execute_view"));
        var registry = new DefaultReadOnlyToolRegistry(tools);

        assertEquals(DefaultReadOnlyToolRegistry.TOOL_NAMES,
                registry.schemas().stream().map(MetadataToolSchema::name).toList());
        assertTrue(registry.schemas().stream().allMatch(MetadataToolSchema::readOnly));
        assertThrows(UnsupportedOperationException.class, registry.schemas()::clear);
        assertThrows(IllegalArgumentException.class,
                () -> new DefaultReadOnlyToolRegistry(List.of(tools.get(0), tools.get(1), tool("write_view"))));
        assertThrows(IllegalArgumentException.class,
                () -> new DefaultReadOnlyToolRegistry(List.of(tools.get(0), tools.get(1))));
    }

    @Test
    void metadataToolsMustReuseQuerySchemasWithoutDuplication() {
        QueryMetadataUseCase useCase = new QueryMetadataUseCase() {
            @Override
            public yubi.query.api.SearchDataAssetsResult search(
                    yubi.query.api.SearchDataAssetsRequest request,
                    yubi.query.api.QueryExecutionContext context) {
                throw new UnsupportedOperationException();
            }

            @Override
            public yubi.query.api.DataAssetDetail describe(
                    yubi.query.api.DescribeDataAssetRequest request,
                    yubi.query.api.QueryExecutionContext context) {
                throw new UnsupportedOperationException();
            }
        };

        assertSame(QueryMetadataToolSchemas.searchDataAssets(),
                new SearchDataAssetsTool(useCase, ToolResultLimits.defaults()).schema());
        assertSame(QueryMetadataToolSchemas.describeDataAsset(),
                new DescribeDataAssetTool(useCase, ToolResultLimits.defaults()).schema());
    }

    private ReadOnlyTool tool(String name) {
        return new ReadOnlyTool() {
            private final MetadataToolSchema schema = new MetadataToolSchema(name, name,
                    MetadataToolSchema.Schema.object("input", java.util.Map.of(), List.of()),
                    MetadataToolSchema.Schema.object("output", java.util.Map.of(), List.of()), true);

            @Override
            public MetadataToolSchema schema() {
                return schema;
            }

            @Override
            public yubi.agent.domain.AgentModels.ToolOutput execute(
                    yubi.agent.domain.StructuredValue.ObjectValue arguments,
                    yubi.query.api.QueryExecutionContext context) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private Set<String> propertyNames(MetadataToolSchema.Schema schema) {
        Set<String> names = new HashSet<>();
        schema.properties().forEach((name, child) -> {
            names.add(name.toLowerCase(java.util.Locale.ROOT));
            names.addAll(propertyNames(child));
        });
        if (schema.items() != null) {
            names.addAll(propertyNames(schema.items()));
        }
        return names;
    }
}
