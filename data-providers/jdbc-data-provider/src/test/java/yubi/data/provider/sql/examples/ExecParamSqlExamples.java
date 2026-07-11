package yubi.data.provider.sql.examples;

import yubi.core.data.provider.ExecuteParam;
import yubi.data.provider.sql.common.ParamFactory;
import yubi.data.provider.sql.common.TestSqlDialects;
import yubi.data.provider.sql.entity.SqlTestEntity;
import org.junit.platform.commons.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExecParamSqlExamples {

    public static List<SqlTestEntity> sqlList = new ArrayList<>();

    private static Map<String, String> execParamTemplate = new HashMap<>();

    static {
        initExecTemplateMap();
        initScripts();
    }

    private static void initScripts() {
        ExecuteParam executeParam = ParamFactory.getExecuteScriptExample();
        sqlList.add(dealExecParam(executeParam, SqlTestEntity.createValidateSql(TestSqlDialects.MYSQL,
                "SELECT * FROM test_table WHERE name not like 'a' and id <> '123' and age != 0 and year between 1990 and 2000 ",
                "SELECT * FROM test_table WHERE name not like 'a' and id <> '123' and age != 0 and year between 1990 and 2000")));
        sqlList.add(dealExecParam(executeParam, SqlTestEntity.createValidateSql(TestSqlDialects.MYSQL,
                "SELECT * FROM test_table where 部门=$部门$ and `age`>$age$ ",
                "SELECT * FROM test_table where 部门 = '销售部' and `age` > 20")));

    }

    private static SqlTestEntity dealExecParam(ExecuteParam executeParam, SqlTestEntity sqlTest){
        String template = execParamTemplate.getOrDefault(TestSqlDialects.nameOf(sqlTest.getSqlDialect()), "");
        if (StringUtils.isBlank(template)) {
            return sqlTest;
        }
        sqlTest.setExecuteParam(executeParam);
        sqlTest.setDesireSql(template.replace("&#%xxx%#&",sqlTest.getDesireSql()));
        return sqlTest;
    }

    private static void initExecTemplateMap() {
        execParamTemplate.put(TestSqlDialects.nameOf(TestSqlDialects.MYSQL), "SELECT `YUBI_VTABLE`.`name` AS `name`, `YUBI_VTABLE`.`age` AS `age`, `YUBI_VTABLE`.`id` AS `id`, SUM(`YUBI_VTABLE`.`val`) AS `SUM(val)`  " +
                "FROM  ( &#%xxx%#& )  AS `YUBI_VTABLE`  GROUP BY `YUBI_VTABLE`.`id`  ORDER BY COUNT(`YUBI_VTABLE`.`age`) DESC");
        execParamTemplate.put(TestSqlDialects.nameOf(TestSqlDialects.ORACLE), "SELECT \"YUBI_VTABLE\".\"name\" \"name\", \"YUBI_VTABLE\".\"age\" \"age\", \"YUBI_VTABLE\".\"id\" AS \"id\", SUM(\"YUBI_VTABLE\".\"val\") \"SUM(val)\"  " +
                "FROM  ( &#%xxx%#& )  \"YUBI_VTABLE\"  GROUP BY \"YUBI_VTABLE\".\"id\"  ORDER BY COUNT(\"YUBI_VTABLE\".\"age\") DESC");
    }


}
