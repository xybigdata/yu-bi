package yubi.data.provider.calcite;

import yubi.core.base.consts.ValueType;
import yubi.core.base.consts.VariableTypeEnum;
import yubi.core.data.provider.ScriptVariable;
import yubi.data.provider.calcite.dialect.MsSqlStdOperatorSupport;
import yubi.data.provider.calcite.dialect.MysqlSqlStdOperatorSupport;
import yubi.data.provider.calcite.dialect.OracleSqlStdOperatorSupport;
import yubi.data.provider.jdbc.RegexVariableResolver;
import yubi.data.provider.jdbc.SqlParserVariableResolver;
import yubi.data.provider.script.ReplacementPair;
import yubi.data.provider.script.VariablePlaceholder;
import org.apache.calcite.sql.SqlDialect;
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
    private final SqlDialect oracleDialect = new OracleSqlStdOperatorSupport();
    private final SqlDialect msSqlDialect = new MsSqlStdOperatorSupport();

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
    void shouldParseOracleQueryWithDoubleQuotedIdentifiers() throws SqlParseException {
        SqlNode sqlNode = SqlParserUtils.createParser(
                "SELECT \"ORDER AMOUNT\", SUM(\"SALES\") FROM \"ORDERS\" GROUP BY \"ORDER AMOUNT\"",
                oracleDialect
        ).parseQuery();

        assertEquals(
                "SELECT \"ORDER AMOUNT\", SUM(\"SALES\")\nFROM \"ORDERS\"\nGROUP BY \"ORDER AMOUNT\"",
                SqlNodeUtils.toSql(sqlNode, oracleDialect, false)
        );
    }

    @Test
    void shouldParseMsSqlQueryWithBracketQuotedIdentifiers() throws SqlParseException {
        SqlNode sqlNode = SqlParserUtils.createParser(
                "SELECT [order amount], SUM([sales]) FROM [orders] GROUP BY [order amount]",
                msSqlDialect
        ).parseQuery();

        assertEquals(
                "SELECT [order amount], SUM([sales])\nFROM [orders]\nGROUP BY [order amount]",
                SqlNodeUtils.toSql(sqlNode, msSqlDialect, false)
        );
    }

    @Test
    void shouldKeepSnippetBracketQuotingIndependentFromRuntimeDialect() throws SqlParseException {
        SqlNode sqlNode = SqlParserUtils.parseSnippet("[order amount] + [tax amount]");

        assertEquals(
                "SELECT [order amount] + [tax amount]\nFROM YUBI_VTABLE",
                SqlNodeUtils.toSql(sqlNode, msSqlDialect, false)
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

    @Test
    void shouldReduceQueryVariableRangeWithParserVisitor() throws SqlParseException {
        ScriptVariable minAmount = new ScriptVariable(
                "$minAmount$",
                VariableTypeEnum.QUERY,
                ValueType.NUMERIC,
                new LinkedHashSet<>(List.of("100", "50", "300")),
                false
        );

        List<VariablePlaceholder> placeholders = SqlParserVariableResolver.resolve(
                mysqlDialect,
                "SELECT * FROM `orders` WHERE `amount` >= $minAmount$",
                Map.of(minAmount.getName(), minAmount)
        );

        assertEquals(1, placeholders.size());
        ReplacementPair replacementPair = placeholders.get(0).replacementPair();
        assertEquals("`amount` >= $minAmount$", replacementPair.getPattern());
        assertEquals("`amount` >= 50.0", replacementPair.getReplacement());
    }

    @Test
    void shouldReplaceEmptyQueryVariableExpressionAsIsNull() throws SqlParseException {
        ScriptVariable status = new ScriptVariable(
                "$status$",
                VariableTypeEnum.QUERY,
                ValueType.STRING,
                new LinkedHashSet<>(),
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
        assertEquals("`status` IS NULL", replacementPair.getReplacement());
    }

    @Test
    void shouldReplaceDisabledPermissionVariableAsTrueCondition() throws SqlParseException {
        ScriptVariable orgId = new ScriptVariable(
                "$orgId$",
                VariableTypeEnum.PERMISSION,
                ValueType.NUMERIC,
                new LinkedHashSet<>(List.of("10", "20")),
                false
        );
        orgId.setDisabled(true);

        List<VariablePlaceholder> placeholders = SqlParserVariableResolver.resolve(
                mysqlDialect,
                "SELECT * FROM `orders` WHERE `org_id` = $orgId$",
                Map.of(orgId.getName(), orgId)
        );

        assertEquals(1, placeholders.size());
        ReplacementPair replacementPair = placeholders.get(0).replacementPair();
        assertEquals("`org_id` = $orgId$", replacementPair.getPattern());
        assertEquals("1=1", replacementPair.getReplacement());
    }

    @Test
    void shouldResolveVariableWithRegexFallbackWhenParserCannotParseSql() {
        ScriptVariable status = new ScriptVariable(
                "$status$",
                VariableTypeEnum.QUERY,
                ValueType.STRING,
                new LinkedHashSet<>(List.of("paid", "refunded")),
                false
        );

        List<VariablePlaceholder> placeholders = RegexVariableResolver.resolve(
                mysqlDialect,
                "SELECT * FROM orders TABLESAMPLE SYSTEM (10) WHERE status = $status$",
                Map.of(status.getName(), status)
        );

        assertEquals(1, placeholders.size());
        ReplacementPair replacementPair = placeholders.get(0).replacementPair();
        assertEquals("status = $status$", replacementPair.getPattern());
        assertEquals("status IN ('paid', 'refunded')", replacementPair.getReplacement());
    }
}
