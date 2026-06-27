package datart.data.provider.calcite;

import datart.core.base.PageInfo;
import datart.core.base.consts.ValueType;
import datart.core.data.provider.ExecuteParam;
import datart.core.data.provider.QueryScript;
import datart.core.data.provider.SelectColumn;
import datart.core.data.provider.SingleTypedValue;
import datart.core.data.provider.sql.AggregateOperator;
import datart.core.data.provider.sql.FilterOperator;
import datart.core.data.provider.sql.FunctionColumn;
import datart.core.data.provider.sql.GroupByOperator;
import datart.core.data.provider.sql.OrderOperator;
import datart.core.data.provider.sql.SelectKeyword;
import datart.data.provider.calcite.dialect.MysqlSqlStdOperatorSupport;
import datart.data.provider.script.SqlStringUtils;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.parser.SqlParseException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlBuilderTest {

    private final SqlDialect mysqlDialect = new MysqlSqlStdOperatorSupport();

    @Test
    void shouldBuildSelectWithGroupAggregateFilterOrderAndPage() throws SqlParseException {
        ExecuteParam executeParam = new ExecuteParam();
        executeParam.setKeywords(List.of(SelectKeyword.DISTINCT));
        executeParam.setGroups(List.of(group("status_alias", "status")));
        executeParam.setAggregators(List.of(aggregate("total_amount", AggregateOperator.SqlOperator.SUM, "amount")));
        executeParam.setFilters(List.of(
                filter(FilterOperator.SqlOperator.IN, "region", value("north"), value("south")),
                filter(FilterOperator.SqlOperator.BETWEEN, "created_at", value("2026-01-01"), value("2026-01-31"))
        ));
        executeParam.setOrders(List.of(order(OrderOperator.SqlOperator.DESC, AggregateOperator.SqlOperator.SUM, "amount")));
        executeParam.setPageInfo(PageInfo.builder().pageNo(3).pageSize(20).build());

        assertEquals(
                "SELECT DISTINCT `DATART_VTABLE`.`status` AS `status_alias`, SUM(`DATART_VTABLE`.`amount`) AS `total_amount` " +
                        "FROM ( SELECT * FROM `orders` ) AS `DATART_VTABLE` " +
                        "WHERE `DATART_VTABLE`.`region` IN ('north', 'south') " +
                        "AND `DATART_VTABLE`.`created_at` BETWEEN '2026-01-01' AND '2026-01-31' " +
                        "GROUP BY `DATART_VTABLE`.`status` " +
                        "ORDER BY SUM(`DATART_VTABLE`.`amount`) DESC " +
                        "LIMIT 20 OFFSET 40",
                build(executeParam, true)
        );
    }

    @Test
    void shouldBuildFunctionColumnWithDefaultTablePrefix() throws SqlParseException {
        FunctionColumn functionColumn = new FunctionColumn();
        functionColumn.setAlias("month_key");
        functionColumn.setSnippet("DATE_FORMAT(created_at, '%Y-%m')");

        ExecuteParam executeParam = new ExecuteParam();
        executeParam.setFunctionColumns(List.of(functionColumn));
        executeParam.setGroups(List.of(group("month_key", "month_key")));
        executeParam.setAggregators(List.of(aggregate("order_count", AggregateOperator.SqlOperator.COUNT_DISTINCT, "id")));

        assertEquals(
                "SELECT DATE_FORMAT(`DATART_VTABLE`.`created_at`, '%Y-%m') AS `month_key`, " +
                        "COUNT(DISTINCT `DATART_VTABLE`.`id`) AS `order_count` " +
                        "FROM ( SELECT * FROM `orders` ) AS `DATART_VTABLE` " +
                        "GROUP BY DATE_FORMAT(`DATART_VTABLE`.`created_at`, '%Y-%m')",
                build(executeParam, true)
        );
    }

    @Test
    void shouldBuildHavingWhenFilterTargetsAggregate() throws SqlParseException {
        FilterOperator filter = filter(FilterOperator.SqlOperator.GTE, "amount", numericValue(1000));
        filter.setAggOperator(AggregateOperator.SqlOperator.SUM);

        ExecuteParam executeParam = new ExecuteParam();
        executeParam.setGroups(List.of(group("status_alias", "status")));
        executeParam.setAggregators(List.of(aggregate("total_amount", AggregateOperator.SqlOperator.SUM, "amount")));
        executeParam.setFilters(List.of(filter));

        assertEquals(
                "SELECT `DATART_VTABLE`.`status` AS `status_alias`, SUM(`DATART_VTABLE`.`amount`) AS `total_amount` " +
                        "FROM ( SELECT * FROM `orders` ) AS `DATART_VTABLE` " +
                        "GROUP BY `DATART_VTABLE`.`status` " +
                        "HAVING SUM(`DATART_VTABLE`.`amount`) >= 1000",
                build(executeParam, true)
        );
    }

    @Test
    void shouldKeepUnquotedIdentifiersWhenQuoteIdentifiersDisabled() throws SqlParseException {
        ExecuteParam executeParam = new ExecuteParam();
        executeParam.setColumns(List.of(SelectColumn.of("status_alias", "status")));

        assertEquals(
                "SELECT DATART_VTABLE.status AS `status_alias` FROM ( SELECT * FROM `orders` ) AS DATART_VTABLE",
                build(executeParam, false)
        );
    }

    private String build(ExecuteParam executeParam, boolean quoteIdentifiers) throws SqlParseException {
        QueryScriptProcessResult result = new SqlQueryScriptProcessor(false, mysqlDialect).process(queryScript());
        return cleanup(SqlBuilder.builder()
                .withDialect(mysqlDialect)
                .withQueryScriptProcessResult(result)
                .withExecuteParam(executeParam)
                .withAddDefaultNamePrefix(result.isWithDefaultPrefix())
                .withDefaultNamePrefix(result.getTablePrefix())
                .withPage(executeParam.getPageInfo() != null)
                .withQuoteIdentifiers(quoteIdentifiers)
                .build());
    }

    private QueryScript queryScript() {
        QueryScript queryScript = new QueryScript();
        queryScript.setScript("SELECT * FROM `orders`");
        queryScript.setVariables(List.of());
        return queryScript;
    }

    private GroupByOperator group(String alias, String... column) {
        GroupByOperator group = new GroupByOperator();
        group.setAlias(alias);
        group.setColumn(column);
        return group;
    }

    private AggregateOperator aggregate(String alias, AggregateOperator.SqlOperator operator, String... column) {
        AggregateOperator aggregate = new AggregateOperator();
        aggregate.setAlias(alias);
        aggregate.setSqlOperator(operator);
        aggregate.setColumn(column);
        return aggregate;
    }

    private FilterOperator filter(FilterOperator.SqlOperator operator, String column, SingleTypedValue... values) {
        FilterOperator filter = new FilterOperator();
        filter.setSqlOperator(operator);
        filter.setColumn(column);
        filter.setValues(values);
        return filter;
    }

    private OrderOperator order(
            OrderOperator.SqlOperator orderOperator,
            AggregateOperator.SqlOperator aggregateOperator,
            String... column
    ) {
        OrderOperator order = new OrderOperator();
        order.setOperator(orderOperator);
        order.setAggOperator(aggregateOperator);
        order.setColumn(column);
        return order;
    }

    private SingleTypedValue value(String value) {
        return new SingleTypedValue(value, ValueType.STRING);
    }

    private SingleTypedValue numericValue(Number value) {
        return new SingleTypedValue(value, ValueType.NUMERIC);
    }

    private String cleanup(String sql) {
        return SqlStringUtils.cleanupSql(sql).replaceAll("\\s+", " ");
    }
}
