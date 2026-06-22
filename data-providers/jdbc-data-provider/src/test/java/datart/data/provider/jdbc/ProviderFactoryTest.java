package datart.data.provider.jdbc;

import datart.core.base.exception.BaseException;
import datart.data.provider.JdbcDataProvider;
import datart.data.provider.calcite.dialect.ClickHouseSqlDialectSupport;
import datart.data.provider.calcite.dialect.H2Dialect;
import datart.data.provider.calcite.dialect.MysqlSqlStdOperatorSupport;
import datart.data.provider.calcite.dialect.OracleSqlStdOperatorSupport;
import datart.data.provider.jdbc.adapters.ClickHouseDataProviderAdapter;
import datart.data.provider.jdbc.adapters.JdbcDataProviderAdapter;
import datart.data.provider.jdbc.adapters.OracleDataProviderAdapter;
import org.apache.calcite.sql.SqlDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderFactoryTest {

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
}
