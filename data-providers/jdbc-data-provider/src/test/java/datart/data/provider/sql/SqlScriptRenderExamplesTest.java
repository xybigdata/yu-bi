package datart.data.provider.sql;

import datart.core.base.exception.BaseException;
import datart.core.common.MessageResolver;
import datart.core.data.provider.QueryScript;
import datart.data.provider.calcite.SqlFragment;
import datart.data.provider.jdbc.SqlScriptRender;
import datart.data.provider.script.SqlStringUtils;
import datart.data.provider.sql.common.ParamFactory;
import datart.data.provider.sql.entity.SqlTestEntity;
import datart.data.provider.sql.examples.FallbackSqlExamples;
import datart.data.provider.sql.examples.ForbiddenSqlExamples;
import datart.data.provider.sql.examples.NormalSqlExamples;
import datart.data.provider.sql.examples.SpecialSqlExamples;
import datart.data.provider.sql.examples.VariableSqlExamples;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

class SqlScriptRenderExamplesTest {

    private static final String TABLE_ALIAS = "DATART_VTABLE";

    @BeforeAll
    static void setUpMessageResolver() {
        new MessageResolver().setMessageSource(new StaticMessageSource());
    }

    @Test
    void shouldRenderNormalSqlExamplesWithoutSpringContext() throws SqlParseException {
        validateExamples(
                NormalSqlExamples.sqlList.stream()
                        .filter(sqlTest -> "MYSQL".equals(sqlTest.getSqlDialect().getDatabaseProduct().name()))
                        .limit(6)
                        .toList(),
                false
        );
    }

    @Test
    void shouldRenderVariableSqlExamplesWithoutSpringContext() throws SqlParseException {
        validateExamples(
                VariableSqlExamples.sqlList.stream()
                        .filter(sqlTest -> "MYSQL".equals(sqlTest.getSqlDialect().getDatabaseProduct().name()))
                        .limit(6)
                        .toList(),
                false
        );
    }

    @Test
    void shouldRenderFallbackSqlExamplesWithoutSpringContext() throws SqlParseException {
        validateExamples(
                FallbackSqlExamples.sqlList,
                false
        );
    }

    @Test
    void shouldRejectForbiddenSqlExamplesWithoutSpringContext() throws SqlParseException {
        for (SqlTestEntity sqlTest : ForbiddenSqlExamples.sqlList) {
            QueryScript queryScript = ParamFactory.getQueryScriptExample(sqlTest.getSql());
            SqlScriptRender render = new SqlScriptRender(queryScript, null, sqlTest.getSqlDialect(), false);

            try {
                render.render(false, false, false);
            } catch (BaseException e) {
                assertEquals("message.sql.op.forbidden", e.getMessage(), sqlTest::toString);
                continue;
            }
            fail("SQL 应被禁止: " + sqlTest);
        }
    }

    @Test
    void shouldRenderSpecialSqlExamplesWhenEnabledWithoutSpringContext() throws SqlParseException {
        validateExamples(SpecialSqlExamples.sqlList, true);
    }

    private void validateExamples(List<SqlTestEntity> sqlTests, boolean enableSpecialSql) throws SqlParseException {
        for (SqlTestEntity sqlTest : sqlTests) {
            QueryScript queryScript = ParamFactory.getQueryScriptExample(sqlTest.getSql());
            SqlScriptRender render = new SqlScriptRender(
                    queryScript,
                    sqlTest.getExecuteParam(),
                    sqlTest.getSqlDialect(),
                    enableSpecialSql
            );
            boolean withExecuteParam = sqlTest.getExecuteParam() != null;
            String actual = render.render(withExecuteParam, false, false);
            String expected = sqlTest.getDesireSql();

            if (withExecuteParam) {
                actual = SqlStringUtils.cleanupSql(actual);
            } else {
                expected = wrapExpectedSql(expected, sqlTest.getSqlDialect());
            }

            assertEquals(cleanup(expected), cleanup(actual), sqlTest::toString);
        }
    }

    @Test
    void shouldKeepRepresentativeExampleSetsEnabled() {
        assertFalse(NormalSqlExamples.sqlList.isEmpty());
        assertFalse(mysqlExamples(VariableSqlExamples.sqlList).toList().isEmpty());
        assertFalse(FallbackSqlExamples.sqlList.isEmpty());
        assertFalse(ForbiddenSqlExamples.sqlList.isEmpty());
        assertFalse(SpecialSqlExamples.sqlList.isEmpty());
    }

    private Stream<SqlTestEntity> mysqlExamples(List<SqlTestEntity> sqlTests) {
        return sqlTests.stream()
                .filter(sqlTest -> sqlTest.getSqlDialect() != null)
                .filter(sqlTest -> "MYSQL".equals(sqlTest.getSqlDialect().getDatabaseProduct().name()));
    }

    private String wrapExpectedSql(String sql, SqlDialect sqlDialect) {
        SqlBasicCall sqlBasicCall = new SqlBasicCall(
                SqlStdOperatorTable.AS,
                new SqlNode[]{
                        new SqlFragment("SELECT *  FROM  ( " + sql + " )"),
                        new SqlIdentifier(TABLE_ALIAS, SqlParserPos.ZERO.withQuoting(true))
                },
                SqlParserPos.ZERO
        );
        return sqlBasicCall.toSqlString(sqlDialect).getSql().trim();
    }

    private String cleanup(String sql) {
        return SqlStringUtils.cleanupSql(sql).replaceAll("\\s+", " ");
    }
}
