package yubi.query.api;

public interface ExecuteQueryUseCase {
    QueryResult execute(ExecuteQueryCommand command, QueryExecutionContext context);
}
