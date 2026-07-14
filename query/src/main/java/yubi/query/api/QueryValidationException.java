package yubi.query.api;

import yubi.query.domain.QueryModels.FailureCategory;

public final class QueryValidationException extends QueryException {
    public QueryValidationException(String message) {
        super(FailureCategory.VALIDATION, message);
    }
}
