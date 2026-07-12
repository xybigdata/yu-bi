package yubi.server.query.web;

import java.util.List;
import java.util.Set;

public record QueryPreviewRequest(String sourceId,
                                  String script,
                                  String scriptType,
                                  List<QueryExecuteRequest.Column> columns,
                                  List<Variable> variables,
                                  Integer size) {

    public record Variable(String name,
                           String type,
                           String valueType,
                           Set<String> values,
                           Boolean expression,
                           Boolean disabled,
                           String format) {
    }
}
