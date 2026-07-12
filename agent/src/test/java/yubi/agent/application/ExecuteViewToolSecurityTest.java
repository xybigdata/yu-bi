package yubi.agent.application;

import org.junit.jupiter.api.Test;
import yubi.agent.api.ToolInputException;
import yubi.agent.domain.AgentModels.ToolOutput;
import yubi.agent.domain.StructuredValue;
import yubi.agent.domain.StructuredValue.ArrayValue;
import yubi.agent.domain.StructuredValue.ObjectValue;
import yubi.agent.domain.StructuredValue.TextValue;
import yubi.agent.domain.ToolResultLimits;
import yubi.query.api.DataAssetDetail;
import yubi.query.api.DescribeDataAssetRequest;
import yubi.query.api.ExecuteQueryCommand;
import yubi.query.api.ExecuteQueryUseCase;
import yubi.query.api.QueryExecutionContext;
import yubi.query.api.QueryMetadataUseCase;
import yubi.query.api.QueryResult;
import yubi.query.api.SearchDataAssetsRequest;
import yubi.query.api.SearchDataAssetsResult;
import yubi.query.domain.QueryModels.AggregateType;
import yubi.query.domain.QueryModels.Channel;
import yubi.query.domain.QueryModels.ColumnMetadata;
import yubi.query.domain.QueryModels.FilterType;
import yubi.query.domain.QueryModels.OrderType;
import yubi.query.domain.QueryModels.Page;
import yubi.query.domain.QueryModels.ValueType;
import yubi.query.domain.QueryModels.VariableType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecuteViewToolSecurityTest {

    private static final QueryExecutionContext CONTEXT = new QueryExecutionContext(
            Channel.AUTHENTICATED, "user-1", "org-1", "correlation-1");

    @Test
    void numericAndBooleanInputsMustBeStrictlyParsedAndCanonicalized() {
        Harness harness = harness(new ToolResultLimits(100, 64 * 1024L));
        harness.metadata.detail = detail(
                List.of(field(List.of("orders", "amount"), ValueType.NUMERIC),
                        field(List.of("orders", "enabled"), ValueType.BOOLEAN)),
                List.of(variable("LIMIT", ValueType.NUMERIC, false, false),
                        variable("FLAG", ValueType.BOOLEAN, false, false)));

        ObjectValue arguments = object(
                "viewId", text("view-1"),
                "columns", array(object("path", path("orders", "amount"))),
                "filters", array(
                        filter("amount", "001.2300", "NUMERIC"),
                        filter("enabled", "TRUE", "BOOLEAN")),
                "parameters", array(
                        parameter("LIMIT", "001.2300"),
                        parameter("FLAG", "TRUE")));

        harness.tool.execute(arguments, CONTEXT);

        ExecuteQueryCommand command = harness.execute.command;
        assertEquals("1.23", command.filters().get(0).values().getFirst().value());
        assertEquals("true", command.filters().get(1).values().getFirst().value());
        assertEquals(java.util.Set.of("1.23"), command.parameters().get("LIMIT"));
        assertEquals(java.util.Set.of("true"), command.parameters().get("FLAG"));

        Harness numericInjection = harness(ToolResultLimits.defaults());
        numericInjection.metadata.detail = detail(
                List.of(field(List.of("orders", "amount"), ValueType.NUMERIC)),
                List.of(variable("LIMIT", ValueType.NUMERIC, false, false)));
        ObjectValue injectedParameter = object(
                "viewId", text("view-1"),
                "columns", array(object("path", path("orders", "amount"))),
                "parameters", array(parameter("LIMIT", "0) OR 1=1 --")));

        ToolInputException rejected = assertThrows(ToolInputException.class,
                () -> numericInjection.tool.execute(injectedParameter, CONTEXT));
        assertEquals("INVALID_TOOL_INPUT", rejected.code());
        assertEquals(1, numericInjection.metadata.describeCalls);
        assertEquals(0, numericInjection.execute.calls);

        ObjectValue injectedFilter = object(
                "viewId", text("view-1"),
                "columns", array(object("path", path("orders", "amount"))),
                "filters", array(filter("amount", "0) OR 1=1 --", "NUMERIC")));
        assertThrows(ToolInputException.class,
                () -> numericInjection.tool.execute(injectedFilter, CONTEXT));
        assertEquals(1, numericInjection.metadata.describeCalls);
        assertEquals(0, numericInjection.execute.calls);

        ObjectValue invalidBoolean = object(
                "viewId", text("view-1"),
                "columns", array(object("path", path("orders", "amount"))),
                "parameters", array(parameter("FLAG", "yes OR true")));
        assertThrows(ToolInputException.class,
                () -> harness.tool.execute(invalidBoolean, CONTEXT));
        assertEquals(1, harness.execute.calls);
    }

    @Test
    void emptyOrUnsupportedRequiredQueryVariablesMustBeRejected() {
        Harness empty = harness(ToolResultLimits.defaults());
        ObjectValue emptyParameter = object(
                "viewId", text("view-1"),
                "columns", array(object("path", path("orders", "amount"))),
                "parameters", array(object("name", text("LIMIT"), "values", array())));

        assertThrows(ToolInputException.class, () -> empty.tool.execute(emptyParameter, CONTEXT));
        assertEquals(0, empty.metadata.describeCalls);
        assertEquals(0, empty.execute.calls);

        Harness expression = harness(ToolResultLimits.defaults());
        expression.metadata.detail = detail(
                List.of(field(List.of("orders", "amount"), ValueType.NUMERIC)),
                List.of(variable("CALCULATED", ValueType.NUMERIC, true, true)));

        ToolInputException unsupported = assertThrows(ToolInputException.class,
                () -> expression.tool.execute(selection("orders", "amount"), CONTEXT));
        assertEquals("UNSUPPORTED_VIEW_VARIABLE", unsupported.code());
        assertEquals(1, expression.metadata.describeCalls);
        assertEquals(0, expression.execute.calls);

        Harness required = harness(ToolResultLimits.defaults());
        required.metadata.detail = detail(
                List.of(field(List.of("orders", "amount"), ValueType.NUMERIC)),
                List.of(variable("LIMIT", ValueType.NUMERIC, true, false)));
        ToolInputException missing = assertThrows(ToolInputException.class,
                () -> required.tool.execute(selection("orders", "amount"), CONTEXT));
        assertEquals("INVALID_TOOL_INPUT", missing.code());
        assertEquals(0, required.execute.calls);
    }

    @Test
    void authorizedFieldPathsMustMatchEverySegment() {
        Harness harness = harness(ToolResultLimits.defaults());
        harness.metadata.detail = detail(
                List.of(field(List.of("orders", "amount"), ValueType.NUMERIC)), List.of());

        ToolInputException failure = assertThrows(ToolInputException.class,
                () -> harness.tool.execute(selection("orders.amount"), CONTEXT));

        assertEquals("UNAUTHORIZED_FIELD", failure.code());
        assertEquals(1, harness.metadata.describeCalls);
        assertEquals(0, harness.execute.calls);
    }

    @Test
    void filterValueCardinalityMustBeRejectedBeforeMetadataOrQueryExecution() {
        Map<FilterType, List<Integer>> invalidCardinalities = new EnumMap<>(FilterType.class);
        for (FilterType operator : List.of(
                FilterType.EQ, FilterType.NE, FilterType.GT, FilterType.LT,
                FilterType.GTE, FilterType.LTE,
                FilterType.LIKE, FilterType.PREFIX_LIKE, FilterType.SUFFIX_LIKE,
                FilterType.NOT_LIKE, FilterType.PREFIX_NOT_LIKE, FilterType.SUFFIX_NOT_LIKE)) {
            invalidCardinalities.put(operator, List.of(0, 2));
        }
        invalidCardinalities.put(FilterType.IN, List.of(0));
        invalidCardinalities.put(FilterType.NOT_IN, List.of(0));
        invalidCardinalities.put(FilterType.BETWEEN, List.of(0, 1, 3));
        invalidCardinalities.put(FilterType.NOT_BETWEEN, List.of(0, 1, 3));
        invalidCardinalities.put(FilterType.IS_NULL, List.of(1));
        invalidCardinalities.put(FilterType.NOT_NULL, List.of(1));
        assertEquals(EnumSet.allOf(FilterType.class), invalidCardinalities.keySet());

        invalidCardinalities.forEach((operator, sizes) -> sizes.forEach(size -> {
            Harness harness = harness(ToolResultLimits.defaults());
            ObjectValue arguments = selectionWithFilter(filter(operator.name(), size));

            ToolInputException failure = assertThrows(ToolInputException.class,
                    () -> harness.tool.execute(arguments, CONTEXT), operator.name() + " values=" + size);

            assertEquals("INVALID_TOOL_INPUT", failure.code(), operator.name() + " values=" + size);
            assertEquals(0, harness.metadata.describeCalls, operator.name() + " values=" + size);
            assertEquals(0, harness.execute.calls, operator.name() + " values=" + size);
        }));
    }

    @Test
    void everyFilterOperatorMustAcceptOnlyItsDocumentedCardinality() {
        Map<FilterType, List<Integer>> validCardinalities = new EnumMap<>(FilterType.class);
        for (FilterType operator : List.of(
                FilterType.EQ, FilterType.NE, FilterType.GT, FilterType.LT,
                FilterType.GTE, FilterType.LTE,
                FilterType.LIKE, FilterType.PREFIX_LIKE, FilterType.SUFFIX_LIKE,
                FilterType.NOT_LIKE, FilterType.PREFIX_NOT_LIKE, FilterType.SUFFIX_NOT_LIKE)) {
            validCardinalities.put(operator, List.of(1));
        }
        validCardinalities.put(FilterType.IN, List.of(1, 2));
        validCardinalities.put(FilterType.NOT_IN, List.of(1, 2));
        validCardinalities.put(FilterType.BETWEEN, List.of(2));
        validCardinalities.put(FilterType.NOT_BETWEEN, List.of(2));
        validCardinalities.put(FilterType.IS_NULL, List.of(0));
        validCardinalities.put(FilterType.NOT_NULL, List.of(0));
        assertEquals(EnumSet.allOf(FilterType.class), validCardinalities.keySet());

        validCardinalities.forEach((operator, sizes) -> sizes.forEach(size -> {
            Harness harness = harness(ToolResultLimits.defaults());

            harness.tool.execute(selectionWithFilter(filter(operator.name(), size)), CONTEXT);

            String scenario = operator.name() + " values=" + size;
            assertEquals(1, harness.metadata.describeCalls, scenario);
            assertEquals(1, harness.execute.calls, scenario);
            assertEquals(expectedFilterValues(size), harness.execute.command.filters().getFirst().values()
                    .stream().map(value -> (String) value.value()).toList(), scenario);
        }));
    }

    @Test
    void aggregateFilterMustBeRejectedWithoutDisablingAggregateOrder() {
        Harness filterHarness = harness(ToolResultLimits.defaults());
        ObjectValue aggregateFilter = object(
                "aggregateOperator", text("COUNT"),
                "operator", text("EQ"),
                "column", path("orders", "amount"),
                "values", filterValues(1));

        ToolInputException failure = assertThrows(ToolInputException.class,
                () -> filterHarness.tool.execute(selectionWithFilter(aggregateFilter), CONTEXT));

        assertEquals("INVALID_TOOL_INPUT", failure.code());
        assertEquals(0, filterHarness.metadata.describeCalls);
        assertEquals(0, filterHarness.execute.calls);

        Harness orderHarness = harness(ToolResultLimits.defaults());
        ObjectValue aggregateOrder = object(
                "viewId", text("view-1"),
                "columns", array(object("path", path("orders", "amount"))),
                "orders", array(object(
                        "aggregateOperator", text("COUNT"),
                        "operator", text("DESC"),
                        "column", path("orders", "amount"))));

        orderHarness.tool.execute(aggregateOrder, CONTEXT);

        assertEquals(1, orderHarness.metadata.describeCalls);
        assertEquals(1, orderHarness.execute.calls);
        assertEquals(AggregateType.COUNT,
                orderHarness.execute.command.orders().getFirst().aggregateOperator());
        assertEquals(OrderType.DESC, orderHarness.execute.command.orders().getFirst().operator());
        assertEquals(List.of("orders", "amount"),
                orderHarness.execute.command.orders().getFirst().column());
    }

    @Test
    void completeOutputMustBoundHugeColumnMetadataAndRows() {
        ToolResultLimits limits = new ToolResultLimits(100, 64 * 1024L);
        String huge = "x".repeat(100_000);
        List<ColumnMetadata> columns = new ArrayList<>();
        for (int index = 0; index < 150; index++) {
            columns.add(new ColumnMetadata(List.of("orders", huge + index), ValueType.STRING,
                    huge, List.of()));
        }
        QueryResult result = new QueryResult(huge, huge, huge, huge, columns,
                List.of(List.of(huge, new ExplosiveCell())), new Page(1, 1, 1, true), null);

        ToolOutput output = new ExecuteViewOutputMapper(limits).map(result);

        assertTrue(output.size().truncated());
        assertTrue(output.size().observedBytes() > output.size().returnedBytes());
        assertTrue(output.size().returnedBytes() <= limits.maximumBytes());
        assertEquals(StructuredValues.utf8Bytes(output.value()), output.size().returnedBytes());
    }

    @Test
    void hugeAndUnknownCellsMustUseBoundedDeterministicProjectionWithoutCallingArbitraryToString() {
        ToolResultLimits limits = new ToolResultLimits(10, 10_000);
        String huge = "金额".repeat(20_000);
        QueryResult hugeResult = new QueryResult("result-1", "Orders", null, null,
                List.of(new ColumnMetadata(List.of("orders", "amount"), ValueType.STRING, null, List.of())),
                List.of(List.of(huge)), new Page(1, 1, 1, true), null);

        ToolOutput output = new ExecuteViewOutputMapper(limits).map(hugeResult);

        assertEquals(1, output.size().returnedItems());
        assertTrue(output.size().truncated());
        assertTrue(output.size().returnedBytes() <= limits.maximumBytes());
        ArrayValue rows = (ArrayValue) output.value().values().get("rows");
        ArrayValue row = (ArrayValue) rows.values().getFirst();
        ObjectValue firstCell = (ObjectValue) row.values().getFirst();
        String returned = ((TextValue) firstCell.values().get("value")).value();
        assertTrue(returned.length() < huge.length());

        QueryResult unknownResult = new QueryResult("result-2", "Orders", null, null,
                List.of(new ColumnMetadata(List.of("orders", "amount"), ValueType.STRING, null, List.of())),
                List.of(List.of(new ExplosiveCell())), new Page(1, 1, 1, true), null);
        ToolOutput unknownOutput = new ExecuteViewOutputMapper(limits).map(unknownResult);
        ArrayValue unknownRows = (ArrayValue) unknownOutput.value().values().get("rows");
        ArrayValue unknownRow = (ArrayValue) unknownRows.values().getFirst();
        ObjectValue unknownCell = (ObjectValue) unknownRow.values().getFirst();
        String marker = ((TextValue) unknownCell.values().get("value")).value();
        assertTrue(marker.startsWith("<unsupported:"));
        assertFalse(marker.contains("explosive-secret"));
        assertTrue(unknownOutput.size().truncated());
    }

    @Test
    void jdbcAndUtilDateCellsMustUseStableBoundedTextProjection() {
        ToolResultLimits limits = new ToolResultLimits(10, 2_000);
        List<Object> temporalValues = List.of(
                java.sql.Timestamp.valueOf("2026-07-12 10:11:12.123456789"),
                java.sql.Date.valueOf("2026-07-12"),
                java.sql.Time.valueOf("10:11:12"),
                java.util.Date.from(java.time.Instant.parse("2026-07-12T02:11:12.345Z")));
        QueryResult result = new QueryResult("result-1", "Orders", null, null,
                List.of(
                        new ColumnMetadata(List.of("created_at"), ValueType.DATE, null, List.of()),
                        new ColumnMetadata(List.of("business_date"), ValueType.DATE, null, List.of()),
                        new ColumnMetadata(List.of("business_time"), ValueType.DATE, null, List.of()),
                        new ColumnMetadata(List.of("legacy_date"), ValueType.DATE, null, List.of())),
                List.of(temporalValues), new Page(1, 1, 1, true), null);

        ToolOutput output = new ExecuteViewOutputMapper(limits).map(result);

        ArrayValue rows = (ArrayValue) output.value().values().get("rows");
        ArrayValue row = (ArrayValue) rows.values().getFirst();
        List<String> projected = row.values().stream()
                .map(ObjectValue.class::cast)
                .map(cell -> ((TextValue) cell.values().get("value")).value())
                .toList();
        assertEquals(List.of(
                "2026-07-12T10:11:12.123456789",
                "2026-07-12",
                "10:11:12",
                "2026-07-12T02:11:12.345Z"), projected);
        assertEquals(StructuredValues.utf8Bytes(output.value()), output.size().returnedBytes());
        assertTrue(output.size().returnedBytes() <= limits.maximumBytes());

        for (Object temporalValue : temporalValues) {
            StructuredValues.BoundedText bounded = StructuredValues.boundedCell(temporalValue, 8);
            assertTrue(bounded.truncated());
            assertTrue(bounded.returnedBytes() <= 8);
            assertEquals(StructuredValues.utf8Bytes(bounded.value()), bounded.returnedBytes());
        }
    }

    private Harness harness(ToolResultLimits limits) {
        CapturingMetadata metadata = new CapturingMetadata();
        metadata.detail = detail(
                List.of(field(List.of("orders", "amount"), ValueType.NUMERIC)), List.of());
        CapturingExecute execute = new CapturingExecute();
        execute.result = new QueryResult("result-1", "Orders", null, null,
                List.of(new ColumnMetadata(List.of("orders", "amount"), ValueType.NUMERIC, null, List.of())),
                List.of(List.of(42)), new Page(1, 1, 1, true), null);
        return new Harness(new ExecuteViewTool(execute, metadata, limits), metadata, execute);
    }

    private DataAssetDetail detail(List<DataAssetDetail.FieldDescription> fields,
                                   List<DataAssetDetail.VariableDescription> variables) {
        return new DataAssetDetail("view-1", "Orders", null, fields, variables,
                List.of(), Optional.empty());
    }

    private DataAssetDetail.FieldDescription field(List<String> path, ValueType type) {
        return new DataAssetDetail.FieldDescription(path, String.join(".", path), type, null);
    }

    private DataAssetDetail.VariableDescription variable(String name,
                                                         ValueType type,
                                                         boolean required,
                                                         boolean expression) {
        return new DataAssetDetail.VariableDescription(name, null, VariableType.QUERY, type,
                required, expression, null, DataAssetDetail.VariableScope.VIEW);
    }

    private ObjectValue selection(String... path) {
        return object("viewId", text("view-1"),
                "columns", array(object("path", path(path))));
    }

    private ObjectValue selectionWithFilter(ObjectValue filter) {
        return object(
                "viewId", text("view-1"),
                "columns", array(object("path", path("orders", "amount"))),
                "filters", array(filter));
    }

    private ObjectValue filter(String operator, int valueCount) {
        return object(
                "operator", text(operator),
                "column", path("orders", "amount"),
                "values", filterValues(valueCount));
    }

    private ArrayValue filterValues(int count) {
        List<StructuredValue> values = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            values.add(object(
                    "value", text(Integer.toString(index + 1)),
                    "valueType", text("NUMERIC")));
        }
        return new ArrayValue(values);
    }

    private List<String> expectedFilterValues(int count) {
        List<String> values = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            values.add(Integer.toString(index + 1));
        }
        return List.copyOf(values);
    }

    private ObjectValue filter(String field, String value, String valueType) {
        return object(
                "operator", text("EQ"),
                "column", path("orders", field),
                "values", array(object("value", text(value), "valueType", text(valueType))));
    }

    private ObjectValue parameter(String name, String... values) {
        return object("name", text(name),
                "values", array(java.util.Arrays.stream(values).map(this::text)
                        .toArray(StructuredValue[]::new)));
    }

    private ArrayValue path(String... values) {
        return array(java.util.Arrays.stream(values).map(this::text).toArray(StructuredValue[]::new));
    }

    private ObjectValue object(Object... entries) {
        Map<String, StructuredValue> values = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            values.put((String) entries[index], (StructuredValue) entries[index + 1]);
        }
        return new ObjectValue(values);
    }

    private ArrayValue array(StructuredValue... values) {
        return new ArrayValue(List.of(values));
    }

    private TextValue text(String value) {
        return new TextValue(value);
    }

    private record Harness(ExecuteViewTool tool,
                           CapturingMetadata metadata,
                           CapturingExecute execute) {
    }

    private static final class CapturingMetadata implements QueryMetadataUseCase {
        private DataAssetDetail detail;
        private int describeCalls;

        @Override
        public SearchDataAssetsResult search(SearchDataAssetsRequest request, QueryExecutionContext context) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DataAssetDetail describe(DescribeDataAssetRequest request, QueryExecutionContext context) {
            describeCalls++;
            return detail;
        }
    }

    private static final class CapturingExecute implements ExecuteQueryUseCase {
        private QueryResult result;
        private ExecuteQueryCommand command;
        private int calls;

        @Override
        public QueryResult execute(ExecuteQueryCommand command, QueryExecutionContext context) {
            calls++;
            this.command = command;
            return result;
        }
    }

    private static final class ExplosiveCell {
        @Override
        public String toString() {
            throw new AssertionError("explosive-secret");
        }
    }
}
