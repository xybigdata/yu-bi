package yubi.data.provider.calcite;

import yubi.core.base.exception.BaseException;
import yubi.core.data.provider.QueryScript;
import yubi.data.provider.calcite.dialect.MysqlSqlStdOperatorSupport;
import yubi.data.provider.script.SqlStringUtils;
import org.apache.calcite.sql.SqlDialect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlQueryScriptProcessorTest {

    private final SqlDialect mysqlDialect = new MysqlSqlStdOperatorSupport();

    @Test
    void shouldUseSingleQueryWithComments() {
        QueryScriptProcessResult result = processor(false).process(queryScript(
                "-- ignored before query\n" +
                        "SELECT `id`, `status` FROM `orders` WHERE `status` = 'paid';"
        ));

        assertEquals("YUBI_VTABLE", result.getTablePrefix());
        assertTrue(result.isWithDefaultPrefix());
        assertEquals(
                "( SELECT `id`, `status` FROM `orders` WHERE `status` = 'paid' ) AS YUBI_VTABLE",
                cleanup(SqlNodeUtils.toSql(result.getFrom(), mysqlDialect, false))
        );
    }

    @Test
    void shouldRejectMultipleQueries() {
        BaseException exception = assertThrows(
                BaseException.class,
                () -> processor(false).process(queryScript(
                        "SELECT * FROM `orders`; SELECT * FROM `refunds`;"
                ))
        );

        assertEquals("message.provider.sql.multi.query", exception.getMessage());
    }

    @Test
    void shouldFallbackToLastStatementWhenParserCannotParseButSqlLooksLikeQuery() {
        QueryScriptProcessResult result = processor(false).process(queryScript(
                "SELECT * FROM orders TABLESAMPLE SYSTEM (10)"
        ));

        assertEquals(
                "( SELECT * FROM orders TABLESAMPLE SYSTEM (10) ) AS YUBI_VTABLE",
                cleanup(SqlNodeUtils.toSql(result.getFrom(), mysqlDialect, false))
        );
    }

    @Test
    void shouldKeepModernQueryFallbackCompatibility() {
        List<String> queries = List.of(
                "WITH latest_orders AS (SELECT * FROM orders WHERE created_at >= DATE '2026-01-01') SELECT * FROM latest_orders",
                "SELECT customer_id, ROW_NUMBER() OVER (PARTITION BY customer_id ORDER BY created_at DESC) AS row_num FROM orders",
                "SELECT payload->>'$.status' AS status FROM order_events",
                "SELECT * FROM orders TABLESAMPLE SYSTEM (10)"
        );

        for (String query : queries) {
            QueryScriptProcessResult result = processor(false).process(queryScript(query));

            assertEquals(
                    "( " + query + " ) AS YUBI_VTABLE",
                    cleanup(SqlNodeUtils.toSql(result.getFrom(), mysqlDialect, false))
            );
            assertEquals("YUBI_VTABLE", result.getTablePrefix());
            assertTrue(result.isWithDefaultPrefix());
        }
    }

    @Test
    void shouldRejectNonQueryStatementWhenSpecialSqlDisabled() {
        BaseException exception = assertThrows(
                BaseException.class,
                () -> processor(false).process(queryScript("SHOW TABLES"))
        );

        assertEquals("message.sql.op.forbidden", exception.getMessage());
    }

    @Test
    void shouldFallbackToLastStatementWhenSpecialSqlEnabledAndNoQueryExists() {
        QueryScriptProcessResult result = processor(true).process(queryScript("SHOW TABLES"));

        assertEquals(
                "( SHOW TABLES ) AS YUBI_VTABLE",
                cleanup(SqlNodeUtils.toSql(result.getFrom(), mysqlDialect, false))
        );
        assertEquals("YUBI_VTABLE", result.getTablePrefix());
        assertTrue(result.isWithDefaultPrefix());
    }

    @Test
    void shouldUseOnlyQueryStatementWhenSpecialSqlPrecedesQuery() {
        QueryScriptProcessResult result = processor(true).process(queryScript(
                "SHOW TABLES; SELECT * FROM `orders`"
        ));

        assertEquals(
                "( SELECT * FROM `orders` ) AS YUBI_VTABLE",
                cleanup(SqlNodeUtils.toSql(result.getFrom(), mysqlDialect, false))
        );
        assertEquals("YUBI_VTABLE", result.getTablePrefix());
        assertTrue(result.isWithDefaultPrefix());
    }

    private SqlQueryScriptProcessor processor(boolean enableSpecialSql) {
        return new SqlQueryScriptProcessor(enableSpecialSql, mysqlDialect);
    }

    private QueryScript queryScript(String script) {
        QueryScript queryScript = new QueryScript();
        queryScript.setScript(script);
        queryScript.setVariables(List.of());
        return queryScript;
    }

    private String cleanup(String sql) {
        return SqlStringUtils.cleanupSql(sql).replaceAll("\\s+", " ");
    }
}
