package datart.data.provider.jdbc;

import datart.core.base.consts.ValueType;
import datart.core.data.provider.Column;
import datart.core.data.provider.ForeignKey;
import datart.data.provider.jdbc.adapters.JdbcDataProviderAdapter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JdbcDataProviderAdapterMetadataTest {

    @Test
    void shouldReadCurrentCatalogAsSingleDatabaseWhenCatalogExists() throws Exception {
        MetadataFixture fixture = new MetadataFixture()
                .withCatalogs(row("TABLE_CAT", "ignored_catalog"))
                .withCatalog("current_catalog");

        TestJdbcDataProviderAdapter adapter = new TestJdbcDataProviderAdapter(fixture.connection());

        assertEquals(Set.of("current_catalog"), adapter.readAllDatabases());
    }

    @Test
    void shouldFallbackToSchemasWhenCatalogMetadataIsEmpty() throws Exception {
        MetadataFixture fixture = new MetadataFixture()
                .withSchemas(row("TABLE_SCHEM", "public"), row("TABLE_SCHEM", "analytics"))
                .withSchema(null);

        TestJdbcDataProviderAdapter adapter = new TestJdbcDataProviderAdapter(fixture.connection());

        assertEquals(Set.of("public", "analytics"), adapter.readAllDatabases());
    }

    @Test
    void shouldReadTablesFromCatalogWithCurrentSchema() throws Exception {
        MetadataFixture fixture = new MetadataFixture()
                .withCatalogs(row("TABLE_CAT", "sales"))
                .withSchema("public")
                .withTables(row("TABLE_NAME", "orders"), row("TABLE_NAME", "order_view"));

        TestJdbcDataProviderAdapter adapter = new TestJdbcDataProviderAdapter(fixture.connection());

        assertEquals(Set.of("orders", "order_view"), adapter.readAllTables("sales"));
        assertEquals("sales", fixture.lastGetTablesArgs[0]);
        assertEquals("public", fixture.lastGetTablesArgs[1]);
        assertEquals("%", fixture.lastGetTablesArgs[2]);
        assertArrayEquals(new String[]{"TABLE", "VIEW"}, (String[]) fixture.lastGetTablesArgs[3]);
    }

    @Test
    void shouldReadTablesFromSchemaWhenCatalogMetadataIsEmpty() throws Exception {
        MetadataFixture fixture = new MetadataFixture()
                .withTables(row("TABLE_NAME", "orders"));

        TestJdbcDataProviderAdapter adapter = new TestJdbcDataProviderAdapter(fixture.connection());

        assertEquals(Set.of("orders"), adapter.readAllTables("analytics"));
        assertNull(fixture.lastGetTablesArgs[0]);
        assertEquals("analytics", fixture.lastGetTablesArgs[1]);
    }

    @Test
    void shouldReadColumnsAndAttachImportedForeignKeys() throws Exception {
        MetadataFixture fixture = new MetadataFixture()
                .withImportedKeys(row(
                        "FKCOLUMN_NAME", "customer_id",
                        "PKTABLE_CAT", "crm",
                        "PKTABLE_NAME", "customers",
                        "PKCOLUMN_NAME", "id"
                ))
                .withColumns(
                        row("COLUMN_NAME", "id", "DATA_TYPE", Types.BIGINT),
                        row("COLUMN_NAME", "customer_id", "DATA_TYPE", Types.INTEGER),
                        row("COLUMN_NAME", "created_at", "DATA_TYPE", Types.TIMESTAMP)
                );

        TestJdbcDataProviderAdapter adapter = new TestJdbcDataProviderAdapter(fixture.connection());
        Set<Column> columns = adapter.readTableColumn("sales", "orders");

        Column idColumn = findColumn(columns, "id");
        Column customerIdColumn = findColumn(columns, "customer_id");
        Column createdAtColumn = findColumn(columns, "created_at");

        assertEquals(ValueType.NUMERIC, idColumn.getType());
        assertEquals(ValueType.NUMERIC, customerIdColumn.getType());
        assertEquals(ValueType.DATE, createdAtColumn.getType());
        assertNull(idColumn.getForeignKeys());
        assertEquals(1, customerIdColumn.getForeignKeys().size());

        ForeignKey foreignKey = customerIdColumn.getForeignKeys().get(0);
        assertEquals("crm", foreignKey.getDatabase());
        assertEquals("customers", foreignKey.getTable());
        assertEquals("id", foreignKey.getColumn());
    }

    @Test
    void shouldIgnoreImportedKeysWhenDriverDoesNotSupportMetadata() throws Exception {
        MetadataFixture fixture = new MetadataFixture()
                .withImportedKeysUnsupported()
                .withColumns(row("COLUMN_NAME", "id", "DATA_TYPE", Types.BIGINT));

        TestJdbcDataProviderAdapter adapter = new TestJdbcDataProviderAdapter(fixture.connection());
        Column idColumn = findColumn(adapter.readTableColumn("sales", "orders"), "id");

        assertNull(idColumn.getForeignKeys());
    }

    private static Column findColumn(Set<Column> columns, String name) {
        return columns.stream()
                .filter(column -> name.equals(column.columnName()))
                .findFirst()
                .orElseThrow();
    }

    private static Map<String, Object> row(Object... values) {
        java.util.LinkedHashMap<String, Object> row = new java.util.LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            row.put((String) values[i], values[i + 1]);
        }
        return row;
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

    private static final class MetadataFixture {
        private List<Map<String, Object>> catalogs = List.of();
        private List<Map<String, Object>> schemas = List.of();
        private List<Map<String, Object>> tables = List.of();
        private List<Map<String, Object>> columns = List.of();
        private List<Map<String, Object>> importedKeys = List.of();
        private boolean importedKeysUnsupported;
        private String catalog;
        private String schema;
        private Object[] lastGetTablesArgs;

        private MetadataFixture withCatalogs(Map<String, Object>... rows) {
            this.catalogs = List.of(rows);
            return this;
        }

        private MetadataFixture withSchemas(Map<String, Object>... rows) {
            this.schemas = List.of(rows);
            return this;
        }

        private MetadataFixture withTables(Map<String, Object>... rows) {
            this.tables = List.of(rows);
            return this;
        }

        private MetadataFixture withColumns(Map<String, Object>... rows) {
            this.columns = List.of(rows);
            return this;
        }

        private MetadataFixture withImportedKeys(Map<String, Object>... rows) {
            this.importedKeys = List.of(rows);
            return this;
        }

        private MetadataFixture withImportedKeysUnsupported() {
            this.importedKeysUnsupported = true;
            return this;
        }

        private MetadataFixture withCatalog(String catalog) {
            this.catalog = catalog;
            return this;
        }

        private MetadataFixture withSchema(String schema) {
            this.schema = schema;
            return this;
        }

        private Connection connection() {
            DatabaseMetaData metadata = proxy(DatabaseMetaData.class, (proxy, method, args) -> switch (method.getName()) {
                case "getCatalogs" -> resultSet(catalogs);
                case "getSchemas" -> resultSet(schemas);
                case "getTables" -> {
                    lastGetTablesArgs = args;
                    yield resultSet(tables);
                }
                case "getColumns" -> resultSet(columns);
                case "getImportedKeys" -> {
                    if (importedKeysUnsupported) {
                        throw new SQLFeatureNotSupportedException("imported keys metadata unsupported");
                    }
                    yield resultSet(importedKeys);
                }
                default -> defaultValue(method.getReturnType());
            });

            return proxy(Connection.class, (proxy, method, args) -> switch (method.getName()) {
                case "getMetaData" -> metadata;
                case "getCatalog" -> catalog;
                case "getSchema" -> schema;
                case "close" -> null;
                default -> defaultValue(method.getReturnType());
            });
        }
    }

    private static ResultSet resultSet(List<Map<String, Object>> rows) {
        class Cursor {
            int index = -1;
        }
        Cursor cursor = new Cursor();
        return proxy(ResultSet.class, (proxy, method, args) -> switch (method.getName()) {
            case "next" -> ++cursor.index < rows.size();
            case "getString" -> String.valueOf(getValue(rows.get(cursor.index), args[0]));
            case "getInt" -> ((Number) getValue(rows.get(cursor.index), args[0])).intValue();
            case "close" -> null;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static Object getValue(Map<String, Object> row, Object key) {
        if (key instanceof Integer index) {
            return switch (index) {
                case 1 -> firstValue(row, "TABLE_CAT", "TABLE_SCHEM");
                case 3 -> row.get("TABLE_NAME");
                case 4 -> row.get("COLUMN_NAME");
                case 5 -> row.get("DATA_TYPE");
                default -> row.values().stream().skip(index - 1L).findFirst().orElse(null);
            };
        }
        return row.get(key);
    }

    private static Object firstValue(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            if (row.containsKey(key)) {
                return row.get(key);
            }
        }
        return null;
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
