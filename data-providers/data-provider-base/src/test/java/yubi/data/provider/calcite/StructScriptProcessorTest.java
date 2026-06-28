package yubi.data.provider.calcite;

import yubi.core.base.exception.BaseException;
import yubi.core.data.provider.QueryScript;
import yubi.data.provider.calcite.dialect.MysqlSqlStdOperatorSupport;
import yubi.data.provider.script.SqlStringUtils;
import org.apache.calcite.sql.SqlDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StructScriptProcessorTest {

    private final SqlDialect mysqlDialect = new MysqlSqlStdOperatorSupport();

    @Test
    void shouldUseBaseTableWhenNoJoinExists() {
        QueryScriptProcessResult result = processor().process(queryScript("""
                {
                  "table": ["orders"]
                }
                """));

        assertEquals("`orders`", SqlNodeUtils.toSql(result.getFrom(), mysqlDialect, true));
        assertNull(result.getTablePrefix());
        assertFalse(result.isWithDefaultPrefix());
    }

    @Test
    void shouldBuildJoinWithMultipleConditionsAndSkipInvalidConditions() {
        QueryScriptProcessResult result = processor().process(queryScript("""
                {
                  "table": ["orders"],
                  "joins": [
                    {
                      "table": ["customers"],
                      "joinType": "INNER",
                      "conditions": [
                        { "left": ["orders", "customer_id"], "right": ["customers", "id"] },
                        { "left": ["orders", "tenant_id"], "right": ["customers", "tenant_id"] },
                        { "left": [], "right": ["customers", "ignored"] }
                      ]
                    },
                    {
                      "table": ["refunds"],
                      "joinType": "LEFT",
                      "conditions": [
                        { "left": ["orders", "id"], "right": ["refunds", "order_id"] }
                      ]
                    }
                  ]
                }
                """));

        assertEquals(
                "SELECT * FROM `orders` INNER JOIN `customers` " +
                        "ON `orders`.`customer_id` = `customers`.`id` " +
                        "AND `orders`.`tenant_id` = `customers`.`tenant_id` " +
                        "LEFT JOIN `refunds` ON `orders`.`id` = `refunds`.`order_id`",
                buildSelect(result)
        );
        assertNull(result.getTablePrefix());
        assertFalse(result.isWithDefaultPrefix());
    }

    @Test
    void shouldRejectStructScriptWithoutBaseTable() {
        BaseException exception = assertThrows(
                BaseException.class,
                () -> processor().process(queryScript("""
                        {
                          "joins": []
                        }
                        """))
        );

        assertEquals("Join table can not be empty!", exception.getMessage());
    }

    private StructScriptProcessor processor() {
        return new StructScriptProcessor();
    }

    private QueryScript queryScript(String script) {
        QueryScript queryScript = new QueryScript();
        queryScript.setScript(script);
        return queryScript;
    }

    private String cleanup(String sql) {
        return SqlStringUtils.cleanupSql(sql).replaceAll("\\s+", " ");
    }

    private String buildSelect(QueryScriptProcessResult result) {
        try {
            return cleanup(SqlBuilder.builder()
                    .withDialect(mysqlDialect)
                    .withQueryScriptProcessResult(result)
                    .withAddDefaultNamePrefix(result.isWithDefaultPrefix())
                    .withDefaultNamePrefix(result.getTablePrefix())
                    .withQuoteIdentifiers(true)
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
