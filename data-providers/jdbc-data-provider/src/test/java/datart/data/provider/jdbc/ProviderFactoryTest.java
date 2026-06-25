package datart.data.provider.jdbc;

import datart.core.base.exception.BaseException;
import datart.data.provider.JdbcDataProvider;
import datart.data.provider.calcite.dialect.CustomSqlDialect;
import datart.data.provider.calcite.dialect.ClickHouseSqlDialectSupport;
import datart.data.provider.calcite.dialect.H2Dialect;
import datart.data.provider.calcite.dialect.MysqlSqlStdOperatorSupport;
import datart.data.provider.calcite.dialect.OracleSqlStdOperatorSupport;
import datart.data.provider.jdbc.adapters.ClickHouseDataProviderAdapter;
import datart.data.provider.jdbc.adapters.JdbcDataProviderAdapter;
import datart.data.provider.jdbc.adapters.OracleDataProviderAdapter;
import datart.data.provider.jdbc.adapters.PrestoDataProviderAdapter;
import org.apache.calcite.sql.SqlDialect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderFactoryTest {

    private static final Set<String> CUSTOM_DIALECT_FALLBACK_DB_TYPES = Set.of(
            "ACCESS",
            "BIG_QUERY",
            "CALCITE",
            "DB2",
            "DERBY",
            "DM",
            "FIREBIRD",
            "HSQLDB",
            "INFOBRIGHT",
            "INFORMIX",
            "INGRES",
            "INTERBASE",
            "JETHRO",
            "LUCIDDB",
            "NEOVIEW",
            "NETEZZA",
            "PHOENIX",
            "PRESTO",
            "SPARK",
            "SQLSTREAM",
            "SYBASE",
            "TERADATA",
            "VERTICA"
    );

    private static final Set<String> EXPLICIT_OR_STANDARD_DIALECT_DB_TYPES = Set.of(
            "CLICKHOUSE",
            "H2",
            "HIVE",
            "MSSQL",
            "MYSQL",
            "ORACLE",
            "POSTGRESQL"
    );

    @Test
    void shouldCreateMysqlDefaultAdapterAndDialectWithoutInitializingDatasource() {
        JdbcDataProviderAdapter adapter = createAdapter("MYSQL");

        assertInstanceOf(JdbcDataProviderAdapter.class, adapter);
        assertFalse(adapter.isInit());
        assertInstanceOf(MysqlSqlStdOperatorSupport.class, adapter.getSqlDialect());
        assertTrue(adapter.supportPaging());
    }

    @Test
    void shouldCreateConfiguredAdaptersAndDialectsWithoutInitializingDatasource() {
        assertAdapter("H2", JdbcDataProviderAdapter.class, H2Dialect.class, true);
        assertAdapter("ORACLE", OracleDataProviderAdapter.class, OracleSqlStdOperatorSupport.class, false);
        assertAdapter("CLICKHOUSE", ClickHouseDataProviderAdapter.class, ClickHouseSqlDialectSupport.class, true);
    }

    @Test
    void shouldCreatePrestoAdapterAndCustomDialectWithQuoteMetadata() {
        JdbcDataProviderAdapter adapter = createAdapter("PRESTO");
        JdbcDriverInfo presto = adapter.getDriverInfo();

        assertInstanceOf(PrestoDataProviderAdapter.class, adapter);
        assertFalse(adapter.isInit());
        assertInstanceOf(CustomSqlDialect.class, adapter.getSqlDialect());
        assertEquals("'", presto.getLiteralQuote());
        assertEquals("\"", presto.getIdentifierQuote());
        assertFalse(adapter.supportPaging());
    }

    @Test
    void shouldLoadDriverInfoWithUppercaseDbTypeAndDefaults() {
        JdbcDataProviderAdapter adapter = createAdapter("mysql");
        JdbcDriverInfo mysql = adapter.getDriverInfo();

        assertEquals("MYSQL", mysql.getDbType());
        assertEquals(JdbcDataProvider.DEFAULT_ADAPTER, mysql.getAdapterClass());
        assertTrue(mysql.getQuoteIdentifiers());
    }

    @Test
    void shouldRejectUnknownDbType() {
        BaseException exception = assertThrows(
                BaseException.class,
                () -> createAdapter("UNKNOWN_DB")
        );

        assertEquals("message.provider.jdbc.dbtype", exception.getMessage());
    }

    @Test
    void shouldLoadAllBuiltInDriverInfoWithStableDefaults() {
        List<JdbcDriverInfo> driverInfos = builtInDbTypes().map(this::createAdapter)
                .map(JdbcDataProviderAdapter::getDriverInfo)
                .toList();

        assertEquals(30, driverInfos.size());
        assertTrue(driverInfos.stream().map(JdbcDriverInfo::getDbType).allMatch(dbType -> dbType.equals(dbType.toUpperCase())));
        assertTrue(driverInfos.stream().allMatch(driverInfo -> JdbcDataProvider.DEFAULT_ADAPTER.equals(driverInfo.getAdapterClass())
                || !driverInfo.getAdapterClass().isBlank()));
        assertTrue(driverInfos.stream().allMatch(JdbcDriverInfo::getQuoteIdentifiers));
    }

    @Test
    void shouldKeepBuiltInDialectFallbackBoundaryExplicit() {
        Set<String> dbTypes = builtInDbTypes().collect(toSet());
        assertEquals(
                dbTypes,
                union(CUSTOM_DIALECT_FALLBACK_DB_TYPES, EXPLICIT_OR_STANDARD_DIALECT_DB_TYPES)
        );

        for (String dbType : dbTypes) {
            JdbcDataProviderAdapter adapter = createAdapter(dbType);
            boolean usesCustomFallback = adapter.getSqlDialect()
                    .getClass()
                    .equals(CustomSqlDialect.class);

            assertEquals(
                    CUSTOM_DIALECT_FALLBACK_DB_TYPES.contains(dbType),
                    usesCustomFallback,
                    dbType + " 方言 fallback 分类发生变化"
            );
        }
    }

    @TestFactory
    Stream<DynamicTest> shouldCreateAdapterAndDialectForEveryBuiltInDriver() {
        return builtInDbTypes()
                .map(dbType -> DynamicTest.dynamicTest(dbType, () -> {
                    JdbcDataProviderAdapter adapter = createAdapter(dbType);

                    assertFalse(adapter.isInit());
                    assertNotNull(adapter.getDriverInfo().getDriverClass());
                    assertNotNull(adapter.getSqlDialect());
                }));
    }

    private void assertAdapter(
            String dbType,
            Class<? extends JdbcDataProviderAdapter> adapterType,
            Class<? extends SqlDialect> dialectType,
            boolean supportPaging
    ) {
        JdbcDataProviderAdapter adapter = createAdapter(dbType);

        assertInstanceOf(adapterType, adapter);
        assertFalse(adapter.isInit());
        assertInstanceOf(dialectType, adapter.getSqlDialect());
        assertEquals(supportPaging, adapter.supportPaging());
    }

    private JdbcDataProviderAdapter createAdapter(String dbType) {
        JdbcProperties properties = new JdbcProperties();
        properties.setDbType(dbType);
        properties.setUrl("jdbc:test://localhost");
        properties.setDriverClass("");
        return JdbcDataProvider.ProviderFactory.createDataProvider(properties, false);
    }

    private Stream<String> builtInDbTypes() {
        try (InputStream inputStream = JdbcDataProvider.ProviderFactory.class.getResourceAsStream("/jdbc-driver.yml")) {
            assertNotNull(inputStream);
            Yaml yaml = new Yaml();
            Map<String, Map<String, String>> driverInfo = yaml.load(inputStream);
            return driverInfo.keySet().stream().sorted();
        } catch (Exception e) {
            throw new AssertionError("failed to load built-in jdbc driver metadata", e);
        }
    }

    private Set<String> union(Set<String> left, Set<String> right) {
        return Stream.concat(left.stream(), right.stream()).collect(toSet());
    }
}
