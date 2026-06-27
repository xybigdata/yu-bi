package datart.data.provider.calcite;

import datart.core.base.exception.BaseException;
import datart.data.provider.calcite.dialect.MysqlSqlStdOperatorSupport;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlValidateUtilsTest {

    private final SqlDialect mysqlDialect = new MysqlSqlStdOperatorSupport();

    @Test
    void shouldAcceptSelectAndWithQueriesFromTextFallback() {
        assertTrue(SqlValidateUtils.validateQuery("SELECT * FROM orders", false));
        assertTrue(SqlValidateUtils.validateQuery(
                "WITH latest AS (SELECT * FROM orders) SELECT * FROM latest",
                false
        ));
    }

    @Test
    void shouldRejectDmlAndDdlFromTextFallback() {
        assertForbidden(() -> SqlValidateUtils.validateQuery("UPDATE orders SET status = 'paid'", true));
        assertForbidden(() -> SqlValidateUtils.validateQuery("DROP TABLE orders", true));
    }

    @Test
    void shouldRejectSpecialSqlWhenDisabledAndIgnoreItWhenEnabled() {
        assertForbidden(() -> SqlValidateUtils.validateQuery("SHOW TABLES", false));
        assertFalse(SqlValidateUtils.validateQuery("SHOW TABLES", true));
    }

    @Test
    void shouldAcceptParsedQueryNode() throws SqlParseException {
        SqlNode sqlNode = SqlParserUtils.createParser("SELECT * FROM `orders`", mysqlDialect).parseQuery();

        assertTrue(SqlValidateUtils.validateQuery(sqlNode, false));
    }

    @Test
    void shouldRejectParsedDmlNodeEvenWhenSpecialSqlEnabled() throws SqlParseException {
        SqlNode sqlNode = SqlParserUtils.createParser("UPDATE `orders` SET `status` = 'paid'", mysqlDialect).parseStmt();

        assertForbidden(() -> SqlValidateUtils.validateQuery(sqlNode, true));
    }

    @Test
    void shouldTreatBlankTextAsInvalidButNotForbidden() {
        assertDoesNotThrow(() -> assertFalse(SqlValidateUtils.validateQuery("   ", false)));
    }

    private void assertForbidden(Runnable runnable) {
        BaseException exception = assertThrows(BaseException.class, runnable::run);
        org.junit.jupiter.api.Assertions.assertEquals("message.sql.op.forbidden", exception.getMessage());
    }
}
