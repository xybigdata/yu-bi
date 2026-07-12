package yubi.server.query.web;

import org.junit.jupiter.api.Test;
import yubi.query.api.ExecuteQueryCommand;
import yubi.query.api.PreviewQueryCommand;
import yubi.query.api.QueryResult;
import yubi.query.api.QueryValidationException;
import yubi.query.domain.QueryModels;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QueryWebMapperTest {

    private final QueryWebMapper mapper = new QueryWebMapper();

    @Test
    void shouldMapExecuteDtoToQueryCommand() {
        QueryExecuteRequest request = new QueryExecuteRequest("view-1", List.of("DISTINCT"),
                List.of(new QueryExecuteRequest.Column("amount", List.of("orders", "amount"))),
                List.of(new QueryExecuteRequest.FunctionColumn("tax", "amount * 0.1")),
                List.of(new QueryExecuteRequest.Aggregate("SUM", "sum_amount", List.of("orders", "amount"))),
                List.of(new QueryExecuteRequest.Filter(null, "EQ", List.of("orders", "status"),
                        List.of(new QueryExecuteRequest.TypedValue("paid", "STRING", null)))),
                List.of(new QueryExecuteRequest.Group("status", List.of("orders", "status"))),
                List.of(new QueryExecuteRequest.Order("SUM", "DESC", List.of("orders", "amount"))),
                new QueryExecuteRequest.PageInfo(2L, 50L, 0L, true), Map.of("region", Set.of("east")),
                true, "DIRTYREAD", false, 60, true);

        ExecuteQueryCommand command = mapper.toCommand(request);

        assertEquals("view-1", command.viewId());
        assertEquals(QueryModels.AggregateType.SUM, command.aggregators().getFirst().operator());
        assertEquals(QueryModels.FilterType.EQ, command.filters().getFirst().operator());
        assertEquals(QueryModels.OrderType.DESC, command.orders().getFirst().operator());
        assertEquals(2, command.page().pageNo());
        assertEquals(Set.of("east"), command.parameters().get("region"));
        assertTrue(command.concurrencyControl());
        assertFalse(command.cache());
        assertTrue(command.includeScript());
    }

    @Test
    void shouldMapPreviewDtoAndQueryResponse() {
        QueryPreviewRequest request = new QueryPreviewRequest("source-1", "select 1", "SQL", List.of(),
                List.of(new QueryPreviewRequest.Variable("region", "QUERY", "STRING", Set.of("east"),
                        false, false, null)), 25);
        PreviewQueryCommand command = mapper.toCommand(request);
        QueryResult result = new QueryResult("df-1", "result", "DATACHART", "chart-1",
                List.of(new QueryModels.ColumnMetadata(List.of("orders", "amount"),
                        QueryModels.ValueType.NUMERIC, "#,##0", List.of())), List.of(List.of(12)),
                new QueryModels.Page(1, 25, 1, true), "select 1");

        QueryResponse response = mapper.toResponse(result);

        assertEquals(QueryModels.ScriptType.SQL, command.scriptType());
        assertEquals(QueryModels.VariableType.QUERY, command.variables().getFirst().type());
        assertEquals("DATACHART", response.vizType());
        assertEquals(List.of("orders", "amount"), response.columns().getFirst().name());
        assertEquals("#,##0", response.columns().getFirst().fmt());
        assertEquals(25, response.pageInfo().pageSize());
    }

    @Test
    void shouldConvertInvalidExecuteStructuresAndEnumsToValidationFailure() {
        assertThrows(QueryValidationException.class, () -> mapper.toCommand((QueryExecuteRequest) null));
        assertThrows(QueryValidationException.class, () -> mapper.toCommand(executeRequest(
                java.util.Arrays.asList((QueryExecuteRequest.Column) null), null, null, null)));
        assertThrows(QueryValidationException.class, () -> mapper.toCommand(executeRequest(
                List.of(new QueryExecuteRequest.Column("amount", java.util.Arrays.asList("amount", null))),
                null, null, null)));
        assertThrows(QueryValidationException.class, () -> mapper.toCommand(executeRequest(null,
                List.of(new QueryExecuteRequest.Aggregate("INVALID", "amount", List.of("amount"))), null, null)));
        assertThrows(QueryValidationException.class, () -> mapper.toCommand(executeRequest(null, null,
                List.of(new QueryExecuteRequest.Filter(null, "INVALID", List.of("amount"), List.of())), null)));
        assertThrows(QueryValidationException.class, () -> mapper.toCommand(executeRequest(null, null,
                List.of(new QueryExecuteRequest.Filter(null, "EQ", List.of("amount"),
                        List.of(new QueryExecuteRequest.TypedValue("1", "INVALID", null)))), null)));
        assertThrows(QueryValidationException.class, () -> mapper.toCommand(executeRequest(null, null, null,
                List.of(new QueryExecuteRequest.Order(null, "INVALID", List.of("amount"))))));
        assertThrows(QueryValidationException.class, () -> mapper.toCommand(executeRequestWithParameters(
                Collections.singletonMap(null, Set.of("east")))));
        assertThrows(QueryValidationException.class, () -> mapper.toCommand(executeRequestWithParameters(
                Collections.singletonMap("region", null))));
        assertThrows(QueryValidationException.class, () -> mapper.toCommand(executeRequestWithParameters(
                Map.of("region", Collections.singleton(null)))));
    }

    @Test
    void shouldConvertInvalidPreviewStructuresAndEnumsToValidationFailure() {
        assertThrows(QueryValidationException.class, () -> mapper.toCommand((QueryPreviewRequest) null));
        assertThrows(QueryValidationException.class, () -> mapper.toCommand(
                new QueryPreviewRequest("source-1", "select 1", "INVALID", List.of(), List.of(), 10)));
        assertThrows(QueryValidationException.class, () -> mapper.toCommand(
                new QueryPreviewRequest("source-1", "select 1", "SQL", List.of(),
                        List.of(new QueryPreviewRequest.Variable("v", "INVALID", "STRING", Set.of(),
                                false, false, null)), 10)));
        assertThrows(QueryValidationException.class, () -> mapper.toCommand(
                new QueryPreviewRequest("source-1", "select 1", "SQL", List.of(),
                        List.of(new QueryPreviewRequest.Variable("v", "QUERY", "INVALID", Set.of(),
                                false, false, null)), 10)));
        assertThrows(QueryValidationException.class, () -> mapper.toCommand(
                new QueryPreviewRequest("source-1", "select 1", "SQL", List.of(),
                        List.of(new QueryPreviewRequest.Variable("v", "QUERY", "STRING",
                                Collections.singleton(null), false, false, null)), 10)));
    }

    @Test
    void shouldNotConvertInternalMapperFailuresToValidationFailure() {
        QueryExecuteRequest.Column npeColumn = mock(QueryExecuteRequest.Column.class);
        when(npeColumn.column()).thenReturn(List.of("amount"));
        NullPointerException npe = new NullPointerException("mapper npe");
        when(npeColumn.alias()).thenThrow(npe);

        NullPointerException thrownNpe = assertThrows(NullPointerException.class,
                () -> mapper.toCommand(executeRequest(List.of(npeColumn), null, null, null)));

        assertSame(npe, thrownNpe);

        QueryExecuteRequest.Column illegalArgumentColumn = mock(QueryExecuteRequest.Column.class);
        when(illegalArgumentColumn.column()).thenReturn(List.of("amount"));
        IllegalArgumentException illegalArgument = new IllegalArgumentException("mapper illegal argument");
        when(illegalArgumentColumn.alias()).thenThrow(illegalArgument);

        IllegalArgumentException thrownIllegalArgument = assertThrows(IllegalArgumentException.class,
                () -> mapper.toCommand(executeRequest(List.of(illegalArgumentColumn), null, null, null)));

        assertSame(illegalArgument, thrownIllegalArgument);
    }

    private QueryExecuteRequest executeRequest(List<QueryExecuteRequest.Column> columns,
                                               List<QueryExecuteRequest.Aggregate> aggregates,
                                               List<QueryExecuteRequest.Filter> filters,
                                               List<QueryExecuteRequest.Order> orders) {
        return new QueryExecuteRequest("view-1", null, columns, null, aggregates, filters, null, orders,
                null, null, null, null, null, null, null);
    }

    private QueryExecuteRequest executeRequestWithParameters(Map<String, Set<String>> parameters) {
        return new QueryExecuteRequest("view-1", null, null, null, null, null, null, null,
                null, parameters, null, null, null, null, null);
    }
}
