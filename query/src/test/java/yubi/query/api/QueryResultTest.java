package yubi.query.api;

import org.junit.jupiter.api.Test;
import yubi.query.domain.QueryModels.ColumnMetadata;
import yubi.query.domain.QueryModels.EngineResult;
import yubi.query.domain.QueryModels.ValueType;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryResultTest {

    @Test
    void shouldFreezeProviderResultOnceAtPublicBoundary() {
        ColumnMetadata amount = new ColumnMetadata(List.of("orders", "amount"),
                ValueType.NUMERIC, null, List.of());
        List<ColumnMetadata> providerColumns = new ArrayList<>(List.of(amount));
        List<Object> providerRow = new ArrayList<>(List.of(42));
        List<List<Object>> providerRows = new ArrayList<>(List.of(providerRow));
        EngineResult engineResult = new EngineResult("result-1", "orders", null, null,
                providerColumns, providerRows, null, null);

        QueryResult result = QueryResult.from(engineResult, null);
        providerColumns.clear();
        providerRow.set(0, 100);
        providerRows.clear();

        assertEquals(List.of(amount), result.columns());
        assertEquals(List.of(List.of(42)), result.rows());
        assertThrows(UnsupportedOperationException.class, () -> result.columns().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.rows().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.rows().getFirst().set(0, 100));
    }
}
