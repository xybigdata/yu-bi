/*
 * YuBi
 * <p>
 * Copyright 2021 (originally Datart by running-elephant)
 * Copyright 2024-2026 YuBi Contributors
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

package yubi.server.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import yubi.core.base.exception.BaseException;
import yubi.core.base.exception.Exceptions;
import yubi.core.data.provider.Column;
import yubi.core.data.provider.DataProviderConfigTemplate;
import yubi.core.data.provider.DataProviderInfo;
import yubi.core.data.provider.DataProviderManager;
import yubi.core.data.provider.DataProviderSource;
import yubi.core.data.provider.StdSqlOperator;
import yubi.core.entity.Source;
import yubi.server.query.ServerSourceConfigMapper;
import yubi.server.service.BaseService;
import yubi.server.service.DataProviderService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 非查询的数据源能力。 */
@Service
public class DataProviderServiceImpl extends BaseService implements DataProviderService {

    private final DataProviderManager dataProviderManager;
    private final ServerSourceConfigMapper sourceConfigMapper;

    public DataProviderServiceImpl(DataProviderManager dataProviderManager,
                                   ServerSourceConfigMapper sourceConfigMapper) {
        this.dataProviderManager = dataProviderManager;
        this.sourceConfigMapper = sourceConfigMapper;
    }

    @Override
    public List<DataProviderInfo> getSupportedDataProviders() {
        return dataProviderManager.getSupportedDataProviders();
    }

    @Override
    public DataProviderConfigTemplate getSourceConfigTemplate(String type) throws IOException {
        return dataProviderManager.getSourceConfigTemplate(type);
    }

    @Override
    public Object testConnection(DataProviderSource source) throws Exception {
        Map<String, Object> properties = source.getProperties();
        if (!CollectionUtils.isEmpty(properties)) {
            properties.replaceAll((key, value) -> value instanceof String text
                    ? sourceConfigMapper.decrypt(text) : value);
        }
        return dataProviderManager.testConnection(source);
    }

    @Override
    public Set<String> readAllDatabases(String sourceId) throws SQLException {
        Source source = retrieve(sourceId, Source.class, false);
        return dataProviderManager.readAllDatabases(parseDataProviderConfig(source));
    }

    @Override
    public Set<String> readTables(String sourceId, String database) throws SQLException {
        Source source = retrieve(sourceId, Source.class, false);
        return dataProviderManager.readTables(parseDataProviderConfig(source), database);
    }

    @Override
    public Set<Column> readTableColumns(String sourceId, String database, String table) throws SQLException {
        Source source = retrieve(sourceId, Source.class, false);
        return dataProviderManager.readTableColumns(parseDataProviderConfig(source), database, table);
    }

    @Override
    public Set<StdSqlOperator> supportedStdFunctions(String sourceId) {
        Source source = retrieve(sourceId, Source.class, false);
        return dataProviderManager.supportedStdFunctions(parseDataProviderConfig(source));
    }

    @Override
    public boolean validateFunction(String sourceId, String snippet) {
        Source source = retrieve(sourceId, Source.class);
        return dataProviderManager.validateFunction(parseDataProviderConfig(source), snippet);
    }

    @Override
    public String decryptValue(String value) {
        return sourceConfigMapper.decrypt(value);
    }

    @Override
    public void updateSource(Source source) {
        dataProviderManager.updateSource(parseDataProviderConfig(source));
    }

    @Override
    public DataProviderSource parseDataProviderConfig(Source source) {
        try {
            return sourceConfigMapper.providerSource(source);
        } catch (Exception ex) {
            Exceptions.tr(BaseException.class, "message.provider.config.error");
            throw ex;
        }
    }

}
