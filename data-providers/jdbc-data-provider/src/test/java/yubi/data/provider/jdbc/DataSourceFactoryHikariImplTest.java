package yubi.data.provider.jdbc;

import com.zaxxer.hikari.HikariConfig;
import yubi.data.provider.JdbcDataProvider;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DataSourceFactoryHikariImplTest {

    @Test
    void shouldMapJdbcPropertiesToHikariConfigWithoutCreatingPool() {
        JdbcProperties properties = new JdbcProperties();
        properties.setDriverClass("org.h2.Driver");
        properties.setUrl("jdbc:h2:mem:yu-bi");
        properties.setUser("yu-bi-user");
        properties.setPassword("yu-bi-password");
        Properties dataSourceProperties = new Properties();
        dataSourceProperties.setProperty("schema", "PUBLIC");
        dataSourceProperties.put("socketTimeout", 30);
        properties.setProperties(dataSourceProperties);

        HikariConfig config = new DataSourceFactoryHikariImpl().createConfig(properties);

        assertEquals("org.h2.Driver", config.getDriverClassName());
        assertEquals("jdbc:h2:mem:yu-bi", config.getJdbcUrl());
        assertEquals("yu-bi-user", config.getUsername());
        assertEquals("yu-bi-password", config.getPassword());
        assertEquals(JdbcDataProvider.DEFAULT_MAX_WAIT.longValue(), config.getConnectionTimeout());
        assertEquals(1L, config.getInitializationFailTimeout());
        assertEquals("PUBLIC", config.getDataSourceProperties().getProperty("schema"));
        assertEquals(30, config.getDataSourceProperties().get("socketTimeout"));
    }

    @Test
    void shouldKeepOptionalCredentialsUnsetWhenMissing() {
        JdbcProperties properties = new JdbcProperties();
        properties.setDriverClass("org.h2.Driver");
        properties.setUrl("jdbc:h2:mem:yu-bi");

        HikariConfig config = new DataSourceFactoryHikariImpl().createConfig(properties);

        assertNull(config.getUsername());
        assertNull(config.getPassword());
        assertEquals("jdbc:h2:mem:yu-bi", config.getJdbcUrl());
    }
}
