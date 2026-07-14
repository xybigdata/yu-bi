package yubi.query.api;

public interface PreviewQueryUseCase {
    QueryResult preview(PreviewQueryCommand command, QueryExecutionContext context);
}
