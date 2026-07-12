package yubi.query.api;

import yubi.query.domain.QueryModels.FailureCategory;

public final class QueryAccessDeniedException extends QueryException {
    public QueryAccessDeniedException(String message, Throwable cause) {
        super(FailureCategory.ACCESS_DENIED, message, cause);
    }
}
