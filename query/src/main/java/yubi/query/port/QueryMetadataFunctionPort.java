package yubi.query.port;

import yubi.query.domain.QueryMetadataModels.FunctionDescriptor;

import java.util.List;

public interface QueryMetadataFunctionPort {

    List<FunctionDescriptor> load(String sourceId);
}
