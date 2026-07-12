package yubi.query.port;

import yubi.query.api.QueryExecutionContext;
import yubi.query.domain.QueryMetadataModels.VariableDescriptor;

import java.util.List;

public interface QueryMetadataVariablePort {

    List<VariableDescriptor> load(String organizationId, String assetId, QueryExecutionContext context);
}
