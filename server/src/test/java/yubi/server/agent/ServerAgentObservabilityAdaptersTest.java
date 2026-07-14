package yubi.server.agent;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import yubi.agent.domain.AgentModels.AgentAuditEvent;
import yubi.agent.domain.AgentModels.ArgumentSummary;
import yubi.agent.domain.AgentModels.AuditEventType;
import yubi.agent.domain.AgentModels.AuditStatus;
import yubi.agent.domain.AgentModels.FailureCategory;
import yubi.agent.domain.AgentModels.ResultSize;
import yubi.agent.domain.AgentModels.ToolMetric;
import yubi.agent.domain.AgentModels.ToolMetricStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerAgentObservabilityAdaptersTest {

    @Test
    void shouldKeepMetricTagsLowCardinalityAndExcludeSensitiveIdentifiers() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ServerAgentMetricsAdapter adapter = new ServerAgentMetricsAdapter(registry);
        adapter.record(new ToolMetric("execute_view", ToolMetricStatus.SUCCEEDED, null,
                12, 8, 5, 512, 7));
        adapter.record(new ToolMetric("token-password-source-config", ToolMetricStatus.FAILED,
                FailureCategory.VALIDATION, 1, 1, 0, 0, 0));

        assertEquals(1, registry.get("yubi.agent.tool.calls")
                .tags("tool", "execute_view", "status", "succeeded", "failure", "none")
                .counter().count());
        assertEquals(1, registry.get("yubi.agent.tool.calls")
                .tags("tool", "unregistered", "status", "failed", "failure", "validation")
                .counter().count());
        assertEquals(7, registry.get("yubi.agent.tool.query.rows")
                .tags("tool", "execute_view", "status", "succeeded", "failure", "none")
                .summary().totalAmount());
        String meters = registry.getMeters().toString();
        assertFalse(meters.contains("token-password-source-config"));
        assertFalse(meters.contains("subject-ref"));
        assertFalse(meters.contains("request-ref"));
    }

    @Test
    void shouldTraceTrustedIdentifiersAndFinalStatusWithoutSensitiveValues() {
        Logger logger = (Logger) LoggerFactory.getLogger(ServerAgentAuditAdapter.class);
        ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            AgentAuditEvent event = new AgentAuditEvent(AuditEventType.SESSION_COMPLETED,
                    "session-ref", "request-ref", "subject-ref", "organization-ref", "correlation-ref",
                    3, null, new ArgumentSummary(List.of("viewId"), 1, 2, 1, 2),
                    15, new ResultSize(7, 5, 600, 400, 100, 65_536, true),
                    AuditStatus.SUCCEEDED, null);

            new ServerAgentAuditAdapter().record(event);

            String message = appender.list.getFirst().getFormattedMessage();
            assertTrue(message.contains("session=session-ref"));
            assertTrue(message.contains("request=request-ref"));
            assertTrue(message.contains("subject=subject-ref"));
            assertTrue(message.contains("correlation=correlation-ref"));
            assertTrue(message.contains("status=SUCCEEDED"));
            assertFalse(message.contains("raw-token-secret"));
            assertFalse(message.contains("jdbc:mysql"));
            assertFalse(message.contains("password="));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
