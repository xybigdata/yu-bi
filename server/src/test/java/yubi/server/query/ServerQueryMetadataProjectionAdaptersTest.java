package yubi.server.query;

import org.junit.jupiter.api.Test;
import yubi.core.data.provider.StdSqlOperator;
import yubi.query.api.QueryExecutionContext;
import yubi.query.domain.QueryModels.Channel;
import yubi.query.domain.QueryModels.ColumnMetadata;
import yubi.query.domain.QueryModels.Definition;
import yubi.query.domain.QueryModels.ScriptType;
import yubi.query.domain.QueryModels.ValueType;
import yubi.query.port.QueryDefinitionPort;
import yubi.server.base.dto.VariableValue;
import yubi.server.service.DataProviderService;
import yubi.server.service.VariableService;

import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServerQueryMetadataProjectionAdaptersTest {

    @Test
    void definitionShouldReusePureQueryProjectionWithoutSourceConfiguration() {
        QueryDefinitionPort definitionPort = mock(QueryDefinitionPort.class);
        Definition definition = new Definition("view-1", "org-1", "Orders", null,
                "source-1", "select amount from orders", ScriptType.SQL,
                Map.of("orders.amount", new ColumnMetadata(
                        List.of("orders", "amount"), ValueType.NUMERIC, null, List.of())));
        when(definitionPort.load("view-1")).thenReturn(definition);

        ServerQueryMetadataDefinitionAdapter adapter = new ServerQueryMetadataDefinitionAdapter(definitionPort);
        var result = adapter.load("view-1", true, true);

        assertEquals("source-1", result.sourceId());
        assertEquals(Set.of("orders.amount"), result.schema().keySet());
        assertEquals("select amount from orders", result.script());
        assertEquals(null, adapter.load("view-1", true, false).script());
        assertEquals(null, adapter.load("view-1", false, true).script());
    }

    @Test
    void variablesShouldExposeDescriptionsWithoutDefaultPermissionOrEncryptedValues() {
        VariableService variableService = mock(VariableService.class);
        VariableValue organizationVariable = variable("ORG_FILTER", "PERMISSION", "STRING",
                "[\"secret-default\"]", Set.of("secret-permission"), true);
        VariableValue viewVariable = variable("DATE_FROM", "QUERY", "DATE", null,
                Set.of("2026-07-12"), false);
        when(variableService.listOrgValue("org-1")).thenReturn(List.of(organizationVariable));
        when(variableService.listViewVarValuesByUser("user-1", "view-1"))
                .thenReturn(List.of(viewVariable));
        ServerQueryMetadataVariableAdapter adapter = new ServerQueryMetadataVariableAdapter(variableService);

        var result = adapter.load("org-1", "view-1", context());

        assertEquals(List.of("ORG_FILTER", "DATE_FROM"), result.stream().map(value -> value.name()).toList());
        assertFalse(result.getFirst().required());
        assertTrue(result.get(1).required());
        String projection = result.toString();
        assertFalse(projection.contains("secret-default"));
        assertFalse(projection.contains("secret-permission"));
        assertFalse(projection.contains("2026-07-12"));
    }

    @Test
    void shouldDeriveRequiredFromExistingJsonArrayDefaultSemanticsWithoutExposingValues() {
        VariableService variableService = mock(VariableService.class);
        List<VariableValue> definitions = List.of(
                variable("QUERY_NULL", "QUERY", "STRING", null, Set.of(), false),
                variable("QUERY_BLANK", "QUERY", "STRING", " ", Set.of(), false),
                variable("QUERY_EMPTY", "QUERY", "STRING", "[]", Set.of(), false),
                variable("QUERY_VALUE", "QUERY", "STRING", "[\"secret-default\"]", Set.of(), false),
                variable("QUERY_BROKEN", "QUERY", "STRING", "[\"secret-broken\"", Set.of(), false),
                variable("PERMISSION_NULL", "PERMISSION", "STRING", null, Set.of("secret-row"), false));
        when(variableService.listOrgValue("org-1")).thenReturn(definitions);
        when(variableService.listViewVarValuesByUser("user-1", "view-1")).thenReturn(List.of());

        var result = new ServerQueryMetadataVariableAdapter(variableService)
                .load("org-1", "view-1", context());
        Map<String, Boolean> required = result.stream().collect(java.util.stream.Collectors.toMap(
                value -> value.name(), value -> value.required(), (left, right) -> left, LinkedHashMap::new));

        assertEquals(Map.of(
                "QUERY_NULL", true,
                "QUERY_BLANK", true,
                "QUERY_EMPTY", true,
                "QUERY_VALUE", false,
                "QUERY_BROKEN", true,
                "PERMISSION_NULL", false), required);
        assertFalse(result.toString().contains("secret-default"));
        assertFalse(result.toString().contains("secret-broken"));
        assertFalse(result.toString().contains("secret-row"));
    }

    @Test
    void functionsShouldExposeOnlyStandardNameAndSymbol() {
        DataProviderService dataProviderService = mock(DataProviderService.class);
        LinkedHashSet<StdSqlOperator> supported = new LinkedHashSet<>();
        supported.add(StdSqlOperator.SUM);
        supported.add(StdSqlOperator.ABS);
        when(dataProviderService.supportedStdFunctions("source-1")).thenReturn(supported);

        var result = new ServerQueryMetadataFunctionAdapter(dataProviderService).load("source-1");

        assertEquals(List.of("SUM", "ABS"), result.stream().map(value -> value.name()).toList());
        verify(dataProviderService).supportedStdFunctions("source-1");
    }

    private VariableValue variable(String name,
                                   String type,
                                   String valueType,
                                   String defaultValue,
                                   Set<String> values,
                                   boolean expression) {
        VariableValue variable = new VariableValue();
        variable.setName(name);
        variable.setType(type);
        variable.setValueType(valueType);
        variable.setDefaultValue(defaultValue);
        variable.setValues(values);
        variable.setExpression(expression);
        return variable;
    }

    private QueryExecutionContext context() {
        return new QueryExecutionContext(Channel.AUTHENTICATED,
                "user-1", "org-1", "correlation-1");
    }
}
