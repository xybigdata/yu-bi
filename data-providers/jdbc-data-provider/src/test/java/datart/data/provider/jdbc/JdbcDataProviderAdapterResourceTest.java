package datart.data.provider.jdbc;

import datart.data.provider.jdbc.adapters.JdbcDataProviderAdapter;
import datart.data.provider.jdbc.adapters.MsSqlDataProviderAdapter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcDataProviderAdapterResourceTest {

    private static final String TEST_DRIVER_URL = "jdbc:yu-bi-test:resource";

    @Test
    void shouldClosePreparedStatementAndResultSetWhenExecutingCountSql() throws Exception {
        ResourceFixture fixture = new ResourceFixture();
        TestJdbcDataProviderAdapter adapter = new TestJdbcDataProviderAdapter(fixture.connection());

        assertEquals(42, adapter.executeCountSql("SELECT * FROM orders"));
        assertEquals(1, fixture.closeCount("connection"));
        assertEquals(1, fixture.closeCount("preparedStatement"));
        assertEquals(1, fixture.closeCount("resultSet"));
        assertEquals("SELECT COUNT(*) FROM (SELECT * FROM orders) V_T", fixture.lastPreparedSql);
    }

    @Test
    void shouldCloseStatementAndResultSetWhenExecutingMsSqlCountSql() throws Exception {
        ResourceFixture fixture = new ResourceFixture();
        TestMsSqlDataProviderAdapter adapter = new TestMsSqlDataProviderAdapter(fixture.connection());

        assertEquals(42, adapter.executeCountSql("SELECT * FROM orders"));
        assertEquals(1, fixture.closeCount("connection"));
        assertEquals(1, fixture.closeCount("statement"));
        assertEquals(1, fixture.closeCount("resultSet"));
        assertEquals("SELECT * FROM orders", fixture.lastExecutedSql);
        assertEquals(ResultSet.TYPE_SCROLL_INSENSITIVE, fixture.lastResultSetType);
        assertEquals(ResultSet.CONCUR_READ_ONLY, fixture.lastResultSetConcurrency);
    }

    @Test
    void shouldCloseConnectionWhenTestingJdbcProperties() throws Exception {
        ResourceFixture fixture = new ResourceFixture();
        TestDriver driver = new TestDriver(fixture.connection());
        DriverManager.registerDriver(driver);
        try {
            TestJdbcDataProviderAdapter adapter = new TestJdbcDataProviderAdapter(fixture.connection());

            assertTrue(adapter.test(jdbcProperties()));
            assertEquals(1, fixture.closeCount("connection"));
            assertEquals(TEST_DRIVER_URL, driver.lastUrl);
        } finally {
            DriverManager.deregisterDriver(driver);
        }
    }

    private static final class TestJdbcDataProviderAdapter extends JdbcDataProviderAdapter {
        private final Connection connection;

        private TestJdbcDataProviderAdapter(Connection connection) {
            this.connection = connection;
        }

        @Override
        protected Connection getConn() {
            return connection;
        }
    }

    private static final class TestMsSqlDataProviderAdapter extends MsSqlDataProviderAdapter {
        private final Connection connection;

        private TestMsSqlDataProviderAdapter(Connection connection) {
            this.connection = connection;
        }

        @Override
        protected Connection getConn() {
            return connection;
        }
    }

    private static final class ResourceFixture {
        private final Map<String, Integer> closeCounts = new HashMap<>();
        private String lastPreparedSql;
        private String lastExecutedSql;
        private int lastResultSetType;
        private int lastResultSetConcurrency;

        private Connection connection() {
            return proxy(Connection.class, (proxy, method, args) -> switch (method.getName()) {
                case "prepareStatement" -> {
                    lastPreparedSql = (String) args[0];
                    yield preparedStatement();
                }
                case "createStatement" -> {
                    lastResultSetType = (int) args[0];
                    lastResultSetConcurrency = (int) args[1];
                    yield statement();
                }
                case "close" -> {
                    closeCounts.merge("connection", 1, Integer::sum);
                    yield null;
                }
                default -> defaultValue(method.getReturnType());
            });
        }

        private PreparedStatement preparedStatement() {
            return proxy(PreparedStatement.class, (proxy, method, args) -> switch (method.getName()) {
                case "executeQuery" -> resultSet();
                case "close" -> {
                    closeCounts.merge("preparedStatement", 1, Integer::sum);
                    yield null;
                }
                default -> defaultValue(method.getReturnType());
            });
        }

        private Statement statement() {
            return proxy(Statement.class, (proxy, method, args) -> switch (method.getName()) {
                case "executeQuery" -> {
                    lastExecutedSql = (String) args[0];
                    yield resultSet();
                }
                case "close" -> {
                    closeCounts.merge("statement", 1, Integer::sum);
                    yield null;
                }
                default -> defaultValue(method.getReturnType());
            });
        }

        private ResultSet resultSet() {
            return proxy(ResultSet.class, (proxy, method, args) -> switch (method.getName()) {
                case "next", "last" -> true;
                case "getInt", "getRow" -> 42;
                case "close" -> {
                    closeCounts.merge("resultSet", 1, Integer::sum);
                    yield null;
                }
                default -> defaultValue(method.getReturnType());
            });
        }

        private int closeCount(String name) {
            return closeCounts.getOrDefault(name, 0);
        }
    }

    private JdbcProperties jdbcProperties() {
        JdbcProperties jdbcProperties = new JdbcProperties();
        jdbcProperties.setDbType("TEST");
        jdbcProperties.setUrl(TEST_DRIVER_URL);
        jdbcProperties.setDriverClass(TestDriver.class.getName());
        jdbcProperties.setUser("tester");
        jdbcProperties.setPassword("secret");
        return jdbcProperties;
    }

    public static final class TestDriver implements Driver {
        private final Connection connection;
        private String lastUrl;

        private TestDriver(Connection connection) {
            this.connection = connection;
        }

        @Override
        public Connection connect(String url, Properties info) {
            if (!acceptsURL(url)) {
                return null;
            }
            lastUrl = url;
            return connection;
        }

        @Override
        public boolean acceptsURL(String url) {
            return TEST_DRIVER_URL.equals(url);
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
            return new DriverPropertyInfo[0];
        }

        @Override
        public int getMajorVersion() {
            return 1;
        }

        @Override
        public int getMinorVersion() {
            return 0;
        }

        @Override
        public boolean jdbcCompliant() {
            return false;
        }

        @Override
        public Logger getParentLogger() {
            return Logger.getGlobal();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, handler);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(type)) {
            return false;
        }
        if (void.class.equals(type)) {
            return null;
        }
        return 0;
    }
}
