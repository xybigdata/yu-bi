package yubi.query.api;

import yubi.query.domain.QueryModels.FailureCategory;

public final class QueryDefinitionException extends QueryException {
    public QueryDefinitionException(String message) {
        super(FailureCategory.DEFINITION, message);
    }

    public QueryDefinitionException(String message, Throwable cause) {
        super(FailureCategory.DEFINITION, message, cause);
    }
}
