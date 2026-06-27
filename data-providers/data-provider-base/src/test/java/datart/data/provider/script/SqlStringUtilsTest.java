package datart.data.provider.script;

import datart.core.base.consts.ValueType;
import datart.core.base.consts.VariableTypeEnum;
import datart.core.base.exception.BaseException;
import datart.core.data.provider.ScriptVariable;
import datart.data.provider.calcite.dialect.MsSqlStdOperatorSupport;
import datart.data.provider.calcite.dialect.MysqlSqlStdOperatorSupport;
import org.apache.calcite.sql.SqlDialect;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlStringUtilsTest {

    private final SqlDialect mysqlDialect = new MysqlSqlStdOperatorSupport();
    private final SqlDialect msSqlDialect = new MsSqlStdOperatorSupport();

    @Test
    void shouldCleanupLineAndBlockCommentsWithoutTouchingStringLiteral() {
        String sql = "-- header comment\n" +
                "SELECT 'not -- comment' AS `value`, `id` FROM `orders` /* tail comment */ WHERE `status` = 'paid'";

        assertEquals(
                "SELECT 'not -- comment' AS `value`, `id` FROM `orders` WHERE `status` = 'paid'",
                SqlStringUtils.cleanupSqlComments(sql, mysqlDialect)
        );
    }

    @Test
    void shouldCleanupCommentsWithMsSqlBracketQuoting() {
        String sql = "SELECT [order--amount], [status] FROM [orders] -- tail comment\n" +
                "WHERE [status] = 'paid'";

        assertEquals(
                "SELECT [order--amount], [status] FROM [orders]\nWHERE [status] = 'paid'",
                SqlStringUtils.cleanupSqlComments(sql, msSqlDialect)
        );
    }

    @Test
    void shouldRemoveRepeatedTrailingDelimiters() {
        assertEquals(
                "SELECT * FROM orders",
                SqlStringUtils.removeEndDelimiter(" SELECT * FROM orders;;; ")
        );
    }

    @Test
    void shouldReplaceSingleFragmentVariable() {
        ScriptVariable tableName = variable(
                "$tableName$",
                ValueType.FRAGMENT,
                "`orders`"
        );

        assertEquals(
                "SELECT * FROM `orders` WHERE status = 'paid'",
                SqlStringUtils.replaceFragmentVariables(
                        "SELECT * FROM $tableName$ WHERE status = 'paid'",
                        List.of(tableName)
                )
        );
    }

    @Test
    void shouldRejectFragmentVariableWithMultipleValues() {
        ScriptVariable tableName = variable(
                "$tableName$",
                ValueType.FRAGMENT,
                "`orders`",
                "`refunds`"
        );

        BaseException exception = assertThrows(
                BaseException.class,
                () -> SqlStringUtils.replaceFragmentVariables(
                        "SELECT * FROM $tableName$",
                        List.of(tableName)
                )
        );

        assertEquals("message.provider.variable.expression.size", exception.getMessage());
    }

    private ScriptVariable variable(String name, ValueType valueType, String... values) {
        ScriptVariable variable = new ScriptVariable();
        variable.setName(name);
        variable.setValueType(valueType);
        variable.setType(VariableTypeEnum.QUERY);
        variable.setValues(new LinkedHashSet<>(List.of(values)));
        return variable;
    }
}
