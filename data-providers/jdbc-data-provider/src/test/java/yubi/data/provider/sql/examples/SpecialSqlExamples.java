package yubi.data.provider.sql.examples;

import yubi.data.provider.sql.entity.SqlTestEntity;
import yubi.data.provider.sql.common.TestSqlDialects;
import org.apache.calcite.sql.SqlDialect;

import java.util.ArrayList;
import java.util.List;

public class SpecialSqlExamples {

    public static List<SqlTestEntity> sqlList = new ArrayList<>();

    static {
        initScripts(TestSqlDialects.MYSQL, TestSqlDialects.ORACLE);
    }

    private static void initScripts(SqlDialect... sqlDialects) {
        for (SqlDialect sqlDialect : sqlDialects) {
            sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                    "Special sql",
                    "Special sql"));
            sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                    "replace INTO test_table (id) VALUES(123)",
                    "replace INTO test_table (id) VALUES(123)"));
        }
    }

}
