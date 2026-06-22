package datart.data.provider.calcite;

import datart.core.base.consts.ValueType;
import datart.core.base.consts.VariableTypeEnum;
import datart.core.data.provider.ScriptVariable;
import datart.data.provider.calcite.dialect.MysqlSqlStdOperatorSupport;
import datart.data.provider.jdbc.SqlParserVariableResolver;
import datart.data.provider.script.ReplacementPair;
import datart.data.provider.script.VariablePlaceholder;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlFunction;
import org.apache.calcite.sql.SqlFunctionCategory;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlParserUtilsTest {

    private final MysqlSqlStdOperatorSupport mysqlDialect = new MysqlSqlStdOperatorSupport();

    @Test
    void shouldParseCommonSnippetExpressions() throws SqlParseException {
        List<String> snippets = List.of(
                "SUM(num)",
                "COUNT(*)",
                "CASE WHEN AGE >= 18 THEN 'adult' ELSE 'minor' END",
                "DATE_FORMAT(created_at, '%Y-%m-%d')",
                "[order amount] / 100"
        );

        for (String snippet : snippets) {
            SqlParserUtils.parseSnippet(snippet);
        }
    }

    @Test
    void shouldParseMysqlQueryWithBacktickQuotedIdentifiers() throws SqlParseException {
        SqlNode sqlNode = SqlParserUtils.createParser(
                "SELECT `order amount`, SUM(`sales`) FROM `orders` GROUP BY `order amount`",
                mysqlDialect
        ).parseQuery();

        assertEquals(
                "SELECT `order amount`, SUM(`sales`)\nFROM `orders`\nGROUP BY `order amount`",
                SqlNodeUtils.toSql(sqlNode, mysqlDialect, false)
        );
    }

    @Test
    void shouldKeepMysqlDateAggregateUnparseContract() {
        SqlCall call = SqlNodeUtils.createSqlBasicCall(
                new SqlFunction(
                        new SqlIdentifier("AGG_DATE_MONTH", SqlParserPos.ZERO),
                        null,
                        null,
                        null,
                        null,
                        SqlFunctionCategory.USER_DEFINED_FUNCTION
                ),
                List.of(
                        SqlNodeUtils.createSqlIdentifier("created_at")
                )
        );

        assertEquals(
                "DATE_FORMAT(`created_at`,'%Y-%m')",
                SqlNodeUtils.toSql(call, mysqlDialect, false)
        );
    }

    @Test
    void shouldRejectInvalidSnippetSyntax() {
        assertThrows(
                SqlParseException.class,
                () -> SqlParserUtils.parseSnippet("SUM(")
        );
    }

    @Test
    void shouldResolveQueryVariablePlaceholdersWithParserVisitor() throws SqlParseException {
        ScriptVariable status = new ScriptVariable(
                "$status$",
                VariableTypeEnum.QUERY,
                ValueType.STRING,
                new LinkedHashSet<>(List.of("paid", "refunded")),
                false
        );

        List<VariablePlaceholder> placeholders = SqlParserVariableResolver.resolve(
                mysqlDialect,
                "SELECT * FROM `orders` WHERE `status` = $status$",
                Map.of(status.getName(), status)
        );

        assertEquals(1, placeholders.size());
        ReplacementPair replacementPair = placeholders.get(0).replacementPair();
        assertEquals("`status` = $status$", replacementPair.getPattern());
        assertEquals("`status` IN ('paid', 'refunded')", replacementPair.getReplacement());
    }
}
