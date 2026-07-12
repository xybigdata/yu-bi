package yubi.server.service;


import yubi.core.data.provider.*;
import yubi.core.entity.Source;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public interface DataProviderService {


    List<DataProviderInfo> getSupportedDataProviders();

    DataProviderConfigTemplate getSourceConfigTemplate(String type) throws IOException;

    Object testConnection(DataProviderSource source) throws Exception;

    Set<String> readAllDatabases(String sourceId) throws SQLException;

    Set<String> readTables(String sourceId, String database) throws SQLException;

    Set<Column> readTableColumns(String sourceId, String schema, String table) throws SQLException;

    Set<StdSqlOperator> supportedStdFunctions(String sourceId);

    boolean validateFunction(String sourceId, String snippet);

    String decryptValue(String value);

    void updateSource(Source source);

    DataProviderSource parseDataProviderConfig(Source source);

}
