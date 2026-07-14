package yubi.server.query;

import org.springframework.stereotype.Component;
import yubi.query.domain.QueryMetadataModels.FunctionDescriptor;
import yubi.query.port.QueryMetadataFunctionPort;
import yubi.server.service.DataProviderService;

import java.util.List;

@Component
public class ServerQueryMetadataFunctionAdapter implements QueryMetadataFunctionPort {

    private final DataProviderService dataProviderService;

    public ServerQueryMetadataFunctionAdapter(DataProviderService dataProviderService) {
        this.dataProviderService = dataProviderService;
    }

    @Override
    public List<FunctionDescriptor> load(String sourceId) {
        return dataProviderService.supportedStdFunctions(sourceId).stream()
                .map(operator -> new FunctionDescriptor(operator.name(), operator.getSymbol()))
                .toList();
    }
}
