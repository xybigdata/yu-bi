package yubi.query.port;

import yubi.query.api.QueryExecutionContext;
import yubi.query.domain.QueryMetadataModels.AssetReference;
import yubi.query.domain.QueryMetadataModels.MetadataAccessDecision;

public interface QueryMetadataAccessPolicyPort {

    void validateContext(QueryExecutionContext context);

    MetadataAccessDecision authorize(AssetReference asset, QueryExecutionContext context);
}
