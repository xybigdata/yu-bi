package yubi.data.provider.jdbc;

import yubi.core.base.consts.ValueType;
import yubi.core.base.consts.VariableTypeEnum;
import yubi.core.data.provider.QueryScript;
import yubi.core.data.provider.ScriptType;
import yubi.core.data.provider.ScriptVariable;
import yubi.data.provider.calcite.dialect.MysqlSqlStdOperatorSupport;
import yubi.data.provider.script.SqlStringUtils;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.parser.SqlParseException;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlScriptRenderTest {

    private final SqlDialect mysqlDialect = new MysqlSqlStdOperatorSupport();

    @Test
    void shouldRenderSqlQueryAndCleanupComments() throws SqlParseException {
        SqlScriptRender render = new SqlScriptRender(
                queryScript(
                        "-- ignored\n" +
                                "SELECT `id`, `status` FROM `orders` WHERE `status` = 'paid';"
                ),
                null,
                mysqlDialect
        );

        assertEquals(
                "SELECT * FROM ( SELECT `id`, `status` FROM `orders` WHERE `status` = 'paid' ) AS `YUBI_VTABLE`",
                cleanup(render.render(false, false, false))
        );
    }

    @Test
    void shouldReplaceQueryVariableWhenRenderingSql() throws SqlParseException {
        ScriptVariable status = variable(
                "$status$",
                ValueType.STRING,
                VariableTypeEnum.QUERY,
                "paid",
                "refunded"
        );
        SqlScriptRender render = new SqlScriptRender(
                queryScript(
                        "SELECT * FROM `orders` WHERE `status` = $status$",
                        status
                ),
                null,
                mysqlDialect
        );

        assertEquals(
                "SELECT * FROM ( SELECT * FROM `orders` WHERE `status` IN ('paid', 'refunded') ) AS `YUBI_VTABLE`",
                cleanup(render.render(false, false, false))
        );
    }

    @Test
    void shouldReplaceRepeatedSimpleVariableWhenRenderingSql() throws SqlParseException {
        ScriptVariable department = variable(
                "$department$",
                ValueType.STRING,
                VariableTypeEnum.QUERY,
                "sales"
        );
        SqlScriptRender render = new SqlScriptRender(
                queryScript(
                        "SELECT $department$ AS `department` FROM `orders` ORDER BY $department$",
                        department
                ),
                null,
                mysqlDialect
        );

        assertEquals(
                "SELECT * FROM ( SELECT 'sales' AS `department` FROM `orders` ORDER BY 'sales' ) AS `YUBI_VTABLE`",
                cleanup(render.render(false, false, false))
        );
    }

    @Test
    void shouldReplaceRepeatedSimpleVariableIgnoringCase() throws SqlParseException {
        ScriptVariable status = variable(
                "$status$",
                ValueType.STRING,
                VariableTypeEnum.QUERY,
                "PAID"
        );
        SqlScriptRender render = new SqlScriptRender(
                queryScript(
                        "SELECT $Status$ AS `status` FROM `orders` ORDER BY $status$",
                        status
                ),
                null,
                mysqlDialect
        );

        assertEquals(
                "SELECT * FROM ( SELECT 'PAID' AS `status` FROM `orders` ORDER BY 'PAID' ) AS `YUBI_VTABLE`",
                cleanup(render.render(false, false, false))
        );
    }

    @Test
    void shouldReplaceRepeatedSimpleVariableWithRegexSensitiveValue() throws SqlParseException {
        ScriptVariable keyword = variable(
                "$keyword$",
                ValueType.STRING,
                VariableTypeEnum.QUERY,
                "sales$north\\team"
        );
        SqlScriptRender render = new SqlScriptRender(
                queryScript(
                        "SELECT $keyword$ AS `keyword` FROM `orders` ORDER BY $keyword$",
                        keyword
                ),
                null,
                mysqlDialect
        );

        assertEquals(
                "SELECT * FROM ( SELECT 'sales$north\\team' AS `keyword` FROM `orders` ORDER BY 'sales$north\\team' ) AS `YUBI_VTABLE`",
                cleanup(render.render(false, false, false))
        );
    }

    @Test
    void shouldReplaceFragmentVariableBeforeParsingSql() throws SqlParseException {
        ScriptVariable tableName = variable(
                "$tableName$",
                ValueType.FRAGMENT,
                VariableTypeEnum.QUERY,
                "`orders`"
        );
        SqlScriptRender render = new SqlScriptRender(
                queryScript(
                        "SELECT `id` FROM $tableName$ WHERE `status` = 'paid'",
                        tableName
                ),
                null,
                mysqlDialect
        );

        assertEquals(
                "SELECT * FROM ( SELECT `id` FROM `orders` WHERE `status` = 'paid' ) AS `YUBI_VTABLE`",
                cleanup(render.render(false, false, false))
        );
    }

    @Test
    void shouldReplaceSnippetVariableInProjection() throws SqlParseException {
        ScriptVariable amountExpression = variable(
                "$amountExpression$",
                ValueType.SNIPPET,
                VariableTypeEnum.QUERY,
                "`sales` + `tax`"
        );
        SqlScriptRender render = new SqlScriptRender(
                queryScript(
                        "SELECT $amountExpression$ AS `total_amount` FROM `orders`",
                        amountExpression
                ),
                null,
                mysqlDialect
        );

        assertEquals(
                "SELECT * FROM ( SELECT `sales` + `tax` AS `total_amount` FROM `orders` ) AS `YUBI_VTABLE`",
                cleanup(render.render(false, false, false))
        );
    }

    @Test
    void shouldRenderStructScriptJoin() throws SqlParseException {
        SqlScriptRender render = new SqlScriptRender(
                queryScript(
                        ScriptType.STRUCT,
                        """
                                {
                                  "table": ["orders"],
                                  "joins": [
                                    {
                                      "table": ["customers"],
                                      "joinType": "INNER",
                                      "conditions": [
                                        { "left": ["orders", "customer_id"], "right": ["customers", "id"] }
                                      ]
                                    }
                                  ]
                                }
                                """
                ),
                null,
                mysqlDialect
        );

        assertEquals(
                "SELECT * FROM `orders` INNER JOIN `customers` ON `orders`.`customer_id` = `customers`.`id`",
                cleanup(render.render(false, false, false))
        );
    }

    private QueryScript queryScript(String script, ScriptVariable... variables) {
        return queryScript(ScriptType.SQL, script, variables);
    }

    private QueryScript queryScript(ScriptType scriptType, String script, ScriptVariable... variables) {
        QueryScript queryScript = new QueryScript();
        queryScript.setScriptType(scriptType);
        queryScript.setScript(script);
        queryScript.setVariables(List.of(variables));
        return queryScript;
    }

    private ScriptVariable variable(String name, ValueType valueType, VariableTypeEnum type, String... values) {
        ScriptVariable variable = new ScriptVariable();
        variable.setName(name);
        variable.setValueType(valueType);
        variable.setType(type);
        variable.setValues(new LinkedHashSet<>(List.of(values)));
        return variable;
    }

    private String cleanup(String sql) {
        return SqlStringUtils.cleanupSql(sql).replaceAll("\\s+", " ");
    }
}
