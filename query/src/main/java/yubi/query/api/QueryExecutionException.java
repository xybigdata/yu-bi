package yubi.query.api;

import yubi.query.domain.QueryModels.FailureCategory;

public final class QueryExecutionException extends QueryException {
    public QueryExecutionException(String message, Throwable cause) {
        super(FailureCategory.EXECUTION, message, cause);
    }
}
