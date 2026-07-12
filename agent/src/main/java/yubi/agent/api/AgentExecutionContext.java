package yubi.agent.api;

import yubi.query.api.QueryExecutionContext;
import yubi.query.domain.QueryModels.Channel;

public record AgentExecutionContext(String sessionId,
                                    String requestId,
                                    QueryExecutionContext queryContext) {
    public AgentExecutionContext {
        if (blank(sessionId) || blank(requestId) || queryContext == null
                || queryContext.channel() != Channel.AUTHENTICATED
                || blank(queryContext.subjectId()) || blank(queryContext.organizationId())
                || blank(queryContext.correlationId())) {
            throw new IllegalArgumentException("可信 Agent 执行上下文不完整");
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
