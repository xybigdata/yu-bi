package yubi.query.port;

import yubi.query.domain.QueryModels.Definition;
import yubi.query.domain.QueryModels.SourceDefinition;

public interface QueryDefinitionPort {
    Definition load(String viewId);

    SourceDefinition loadSource(String sourceId);
}
