package yubi.query.api;

import yubi.query.domain.QueryModels.FailureCategory;

public abstract class QueryException extends RuntimeException {

    private final FailureCategory category;

    protected QueryException(FailureCategory category, String message) {
        super(message);
        this.category = category;
    }

    protected QueryException(FailureCategory category, String message, Throwable cause) {
        super(message, cause);
        this.category = category;
    }

    public FailureCategory category() {
        return category;
    }
}
