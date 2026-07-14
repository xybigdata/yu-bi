package yubi.query.port;

import yubi.query.api.QueryExecutionContext;
import yubi.query.domain.QueryModels.Variable;

import java.util.List;

public interface QueryVariablePort {
    List<Variable> loadForView(String viewId, String organizationId, QueryExecutionContext context);

    List<Variable> loadForSource(String organizationId, QueryExecutionContext context);
}
