package yubi.query.port;

import yubi.query.api.QueryExecutionContext;
import yubi.query.domain.QueryModels.AccessDecision;
import yubi.query.domain.QueryModels.Definition;
import yubi.query.domain.QueryModels.SourceDefinition;

public interface QueryAccessPolicyPort {
    AccessDecision authorize(Definition definition, QueryExecutionContext context);

    boolean authorizePreview(SourceDefinition source, QueryExecutionContext context);
}
