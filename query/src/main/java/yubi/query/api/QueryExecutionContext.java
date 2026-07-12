package yubi.query.api;

import yubi.query.domain.QueryModels.Channel;

import java.util.Objects;

public record QueryExecutionContext(Channel channel,
                                    String subjectId,
                                    String organizationId,
                                    String correlationId) {
    public QueryExecutionContext {
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(subjectId, "subjectId");
        Objects.requireNonNull(correlationId, "correlationId");
    }
}
