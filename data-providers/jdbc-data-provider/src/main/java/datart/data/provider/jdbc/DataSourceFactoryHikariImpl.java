/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.data.provider.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import datart.data.provider.JdbcDataProvider;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Properties;

@Slf4j
public class DataSourceFactoryHikariImpl implements DataSourceFactory<HikariDataSource> {

    @Override
    public HikariDataSource createDataSource(JdbcProperties jdbcProperties) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(jdbcProperties.getDriverClass());
        config.setJdbcUrl(jdbcProperties.getUrl());
        if (jdbcProperties.getUser() != null) {
            config.setUsername(jdbcProperties.getUser());
        }
        if (jdbcProperties.getPassword() != null) {
            config.setPassword(jdbcProperties.getPassword());
        }
        config.setConnectionTimeout(JdbcDataProvider.DEFAULT_MAX_WAIT);
        config.setInitializationFailTimeout(1L);

        Properties properties = jdbcProperties.getProperties();
        if (properties != null) {
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                config.addDataSourceProperty(
                        String.valueOf(entry.getKey()),
                        entry.getValue()
                );
            }
        }

        HikariDataSource dataSource = new HikariDataSource(config);
        log.info("hikari data source created ({})", dataSource.getPoolName());
        return dataSource;
    }

    @Override
    public void destroy(DataSource dataSource) {
        ((HikariDataSource) dataSource).close();
    }
}
