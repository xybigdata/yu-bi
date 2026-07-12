package yubi.server.config;

import org.springframework.http.HttpStatus;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import yubi.query.api.QueryAccessDeniedException;
import yubi.query.api.QueryDefinitionException;
import yubi.query.api.QueryExecutionException;
import yubi.query.api.QueryValidationException;
import yubi.server.base.dto.ResponseData;
import yubi.server.controller.PublicQueryController;
import yubi.server.controller.QueryController;

@RestControllerAdvice(assignableTypes = {QueryController.class, PublicQueryController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class QueryWebExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(QueryValidationException.class)
    public ResponseData<Void> validation(QueryValidationException exception) {
        return ResponseData.failure(exception.getMessage());
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(QueryAccessDeniedException.class)
    public ResponseData<Void> accessDenied(QueryAccessDeniedException exception) {
        return ResponseData.failure(exception.getMessage());
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    @ExceptionHandler(QueryDefinitionException.class)
    public ResponseData<Void> definition(QueryDefinitionException exception) {
        return ResponseData.failure(exception.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(QueryExecutionException.class)
    public ResponseData<Void> execution(QueryExecutionException exception) {
        return ResponseData.failure(exception.getMessage());
    }
}
