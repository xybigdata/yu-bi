package datart.data.provider.calcite;

import datart.core.base.exception.BaseException;
import datart.core.data.provider.QueryScript;
import datart.data.provider.calcite.dialect.MysqlSqlStdOperatorSupport;
import datart.data.provider.script.SqlStringUtils;
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

        assertEquals("DATART_VTABLE", result.getTablePrefix());
        assertTrue(result.isWithDefaultPrefix());
        assertEquals(
                "( SELECT `id`, `status` FROM `orders` WHERE `status` = 'paid' ) AS DATART_VTABLE",
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
                "( SELECT * FROM orders TABLESAMPLE SYSTEM (10) ) AS DATART_VTABLE",
                cleanup(SqlNodeUtils.toSql(result.getFrom(), mysqlDialect, false))
        );
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
