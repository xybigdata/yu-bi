package yubi.agent.application;

import org.junit.jupiter.api.Test;
import yubi.agent.api.AgentExecutionContext;
import yubi.agent.api.AgentRequest;
import yubi.agent.api.ExecuteViewToolSchema;
import yubi.agent.domain.AgentModels.AgentAuditEvent;
import yubi.agent.domain.AgentModels.FailureCategory;
import yubi.agent.domain.AgentModels.ResultSize;
import yubi.agent.domain.AgentModels.ToolMetric;
import yubi.agent.domain.AgentModels.ToolMetricStatus;
import yubi.agent.domain.AgentModels.ToolOutput;
import yubi.agent.domain.AgentRuntimePolicy;
import yubi.agent.domain.ModelProtocol.FinalAnswer;
import yubi.agent.domain.ModelProtocol.ToolCall;
import yubi.agent.port.ReadOnlyTool;
import yubi.query.api.MetadataToolSchema;
import yubi.query.api.QueryExecutionContext;
import yubi.query.domain.QueryModels.Channel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AgentObservabilityTest {

    @Test
    void shouldEmitOneLowCardinalityMetricAndTraceForEveryToolCall() {
        List<ToolMetric> metrics = new ArrayList<>();
        List<AgentAuditEvent> traces = new ArrayList<>();
        AtomicInteger turns = new AtomicInteger();
        var gateway = (yubi.agent.port.ModelGateway) turn -> turns.getAndIncrement() == 0
                ? new ToolCall("execute_view", StructuredValues.object("viewId", StructuredValues.text("asset-secret")))
                : new FinalAnswer("done");
        ReadOnlyTool execute = tool("execute_view", ExecuteViewToolSchema.schema(),
                new ToolOutput(new yubi.agent.domain.StructuredValue.ObjectValue(Map.of()),
                        new ResultSize(7, 5, 120, 80, 5, 256, true)));
        var registry = new DefaultReadOnlyToolRegistry(List.of(
                tool("search_data_assets", yubi.query.api.QueryMetadataToolSchemas.searchDataAssets(), empty()),
                tool("describe_data_asset", yubi.query.api.QueryMetadataToolSchemas.describeDataAsset(), empty()),
                execute));
        DefaultAgentRuntime runtime = new DefaultAgentRuntime(gateway, registry, traces::add, session -> { },
                new IncrementingClock(), AgentRuntimePolicy.defaults(),
                (tool, arguments, context) -> tool.execute(arguments, context), metrics::add);

        var result = runtime.run(new AgentRequest("do not leak prompt-token"), context());

        assertEquals(1, metrics.size());
        ToolMetric metric = metrics.getFirst();
        assertEquals("execute_view", metric.toolName());
        assertEquals(ToolMetricStatus.SUCCEEDED, metric.status());
        assertEquals(7, metric.queryRows());
        assertEquals(80, metric.resultBytes());
        assertFalse(metric.toString().contains("asset-secret"));
        assertFalse(metric.toString().contains("prompt-token"));
        assertEquals(List.of("SESSION_STARTED", "STEP_COMPLETED", "STEP_COMPLETED", "SESSION_COMPLETED"),
                traces.stream().map(value -> value.eventType().name()).toList());
        AgentAuditEvent toolTrace = traces.get(1);
        assertEquals("session-ref", toolTrace.sessionId());
        assertEquals("request-ref", toolTrace.requestId());
        assertEquals("subject-ref", toolTrace.subjectId());
        assertEquals("correlation-ref", toolTrace.correlationId());
        assertEquals("execute_view", toolTrace.toolName());
        assertEquals("SUCCEEDED", traces.getLast().status().name());
        assertEquals("COMPLETED", result.session().status().name());
    }

    @Test
    void shouldClassifyTimeoutAndMetricsFailureMustNotChangeResult() {
        List<ToolMetric> metrics = new ArrayList<>();
        var gateway = (yubi.agent.port.ModelGateway) turn -> new ToolCall(
                "search_data_assets", StructuredValues.object("query", StructuredValues.text("orders")));
        var registry = registry();
        DefaultAgentRuntime runtime = new DefaultAgentRuntime(gateway, registry, event -> { }, session -> { },
                System::nanoTime, new AgentRuntimePolicy(2, 0, 1_000),
                (tool, arguments, context) -> { throw new ToolTimeoutException(); }, metric -> {
                    metrics.add(metric);
                    throw new IllegalStateException("metric-secret");
                });

        var result = runtime.run(new AgentRequest("query"), context());

        assertEquals(FailureCategory.TIMEOUT, result.session().failure().category());
        assertEquals(ToolMetricStatus.TIMED_OUT, metrics.getFirst().status());
        assertEquals(1, metrics.size());
    }

    private DefaultReadOnlyToolRegistry registry() {
        return new DefaultReadOnlyToolRegistry(List.of(
                tool("search_data_assets", yubi.query.api.QueryMetadataToolSchemas.searchDataAssets(), empty()),
                tool("describe_data_asset", yubi.query.api.QueryMetadataToolSchemas.describeDataAsset(), empty()),
                tool("execute_view", ExecuteViewToolSchema.schema(), empty())));
    }

    private ToolOutput empty() {
        return new ToolOutput(new yubi.agent.domain.StructuredValue.ObjectValue(Map.of()), ResultSize.empty());
    }

    private ReadOnlyTool tool(String name, MetadataToolSchema schema, ToolOutput output) {
        return new ReadOnlyTool() {
            @Override
            public MetadataToolSchema schema() {
                assertEquals(name, schema.name());
                return schema;
            }

            @Override
            public ToolOutput execute(yubi.agent.domain.StructuredValue.ObjectValue arguments,
                                      QueryExecutionContext context) {
                return output;
            }
        };
    }

    private AgentExecutionContext context() {
        return new AgentExecutionContext("session-ref", "request-ref",
                new QueryExecutionContext(Channel.AUTHENTICATED, "subject-ref", "organization-ref", "correlation-ref"));
    }

    private static final class IncrementingClock implements yubi.agent.port.AgentClockPort {
        private long value;

        @Override
        public long nanoTime() {
            value += 1_000_000;
            return value;
        }
    }
}
