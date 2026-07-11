package yubi.data.provider.sql.common;

import yubi.core.common.Application;
import yubi.data.provider.JdbcDataProvider;
import yubi.data.provider.jdbc.JdbcProperties;
import yubi.data.provider.jdbc.adapters.JdbcDataProviderAdapter;
import org.apache.calcite.sql.SqlDialect;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class TestSqlDialects {

    public static final SqlDialect MYSQL;
    public static final SqlDialect ORACLE;
    public static final SqlDialect MSSQL;
    public static final SqlDialect H2;
    public static final SqlDialect HIVE;
    public static final SqlDialect POSTGRESQL;
    public static final SqlDialect PRESTO;

    private static final Map<SqlDialect, String> DIALECT_NAMES;

    static {
        String userDir = Application.userDir();
        File file = new File(userDir);
        String rootPath = file.getParentFile().getParent();
        System.setProperty("user.dir", rootPath);
        try {
            MYSQL = getSqlDialect("MYSQL");
            ORACLE = getSqlDialect("ORACLE");
            MSSQL = getSqlDialect("MSSQL");
            H2 = getSqlDialect("H2");
            HIVE = getSqlDialect("HIVE");
            POSTGRESQL = getSqlDialect("POSTGRESQL");
            PRESTO = getSqlDialect("PRESTO");
            DIALECT_NAMES = Map.of(
                    MYSQL, "MYSQL",
                    ORACLE, "ORACLE",
                    MSSQL, "MSSQL",
                    H2, "H2",
                    HIVE, "HIVE",
                    POSTGRESQL, "POSTGRESQL",
                    PRESTO, "PRESTO"
            );
        } finally {
            System.setProperty("user.dir", userDir);
        }
    }

    private static SqlDialect getSqlDialect(String dbType) {
        JdbcProperties properties = new JdbcProperties();
        properties.setDbType(dbType);
        properties.setUrl("");
        properties.setUser("");
        properties.setPassword("");
        properties.setDriverClass("");
        properties.setProperties(new Properties());
        properties.setEnableSpecialSql(false);
        JdbcDataProviderAdapter dataProvider = JdbcDataProvider.ProviderFactory.createDataProvider(properties, false);
        SqlDialect sqlDialect = dataProvider.getSqlDialect();
        dataProvider.close();
        return sqlDialect;
    }

    public static List<SqlDialect> getAllSqlDialects() {
        return List.copyOf(DIALECT_NAMES.keySet());
    }

    public static String nameOf(SqlDialect sqlDialect) {
        return DIALECT_NAMES.getOrDefault(sqlDialect, sqlDialect.getClass().getSimpleName());
    }

}
