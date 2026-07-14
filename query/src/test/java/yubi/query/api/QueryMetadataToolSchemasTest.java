package yubi.query.api;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryMetadataToolSchemasTest {

    @Test
    void shouldExposeStableReadOnlySchemasWithoutIdentitySqlOrWriteInputs() {
        List<MetadataToolSchema> schemas = QueryMetadataToolSchemas.all();

        assertEquals(List.of("search_data_assets", "describe_data_asset"),
                schemas.stream().map(MetadataToolSchema::name).toList());
        assertTrue(schemas.stream().allMatch(MetadataToolSchema::readOnly));
        assertEquals(List.of("query", "limit"),
                schemas.getFirst().inputSchema().properties().keySet().stream().toList());
        assertEquals(List.of("assetId", "includeScript"),
                schemas.get(1).inputSchema().properties().keySet().stream().toList());
        assertEquals(List.of("id", "name", "description", "fields", "variables", "functions", "script"),
                schemas.get(1).outputSchema().properties().keySet().stream().toList());

        String structure = schemas.toString().toLowerCase(Locale.ROOT);
        for (String forbidden : List.of("userid", "organizationid", "orgid", "roleid", "permissionoverride",
                "sql", "statement", "sourceconfig", "password", "connectionstring", "write", "delete", "update")) {
            assertFalse(structure.contains(forbidden), () -> "Tool Schema 包含越界字段: " + forbidden);
        }
        assertThrows(UnsupportedOperationException.class, schemas::clear);
        assertThrows(UnsupportedOperationException.class,
                () -> schemas.getFirst().inputSchema().properties().clear());
        assertSame(schemas, QueryMetadataToolSchemas.all());
    }

    @Test
    void requestsMustNotAcceptIdentityOrPermissionOverrides() {
        assertEquals(Set.of("query", "limit"), componentNames(SearchDataAssetsRequest.class));
        assertEquals(Set.of("assetId", "includeScript"), componentNames(DescribeDataAssetRequest.class));
    }

    private Set<String> componentNames(Class<?> type) {
        return java.util.Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toUnmodifiableSet());
    }
}
