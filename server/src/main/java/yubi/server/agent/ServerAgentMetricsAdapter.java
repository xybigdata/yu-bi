package yubi.server.agent;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import yubi.agent.application.DefaultReadOnlyToolRegistry;
import yubi.agent.domain.AgentModels.ToolMetric;
import yubi.agent.port.AgentMetricsPort;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class ServerAgentMetricsAdapter implements AgentMetricsPort {

    private static final String UNREGISTERED = "unregistered";
    private final MeterRegistry registry;

    public ServerAgentMetricsAdapter(MeterRegistry registry) {
        this.registry = java.util.Objects.requireNonNull(registry, "registry");
    }

    @Override
    public void record(ToolMetric metric) {
        String tool = DefaultReadOnlyToolRegistry.TOOL_NAMES.contains(metric.toolName())
                ? metric.toolName() : UNREGISTERED;
        String status = metric.status().name().toLowerCase(Locale.ROOT);
        String failure = metric.failureCategory() == null
                ? "none" : metric.failureCategory().name().toLowerCase(Locale.ROOT);
        String[] tags = {"tool", tool, "status", status, "failure", failure};

        registry.counter("yubi.agent.tool.calls", tags).increment();
        Timer.builder("yubi.agent.tool.duration")
                .tags(tags)
                .register(registry)
                .record(metric.durationMillis(), TimeUnit.MILLISECONDS);
        summary("yubi.agent.tool.argument.nodes", tags).record(metric.argumentNodes());
        summary("yubi.agent.tool.result.items", tags).record(metric.resultItems());
        summary("yubi.agent.tool.result.bytes", tags).record(metric.resultBytes());
        summary("yubi.agent.tool.query.rows", tags).record(metric.queryRows());
    }

    private DistributionSummary summary(String name, String[] tags) {
        return DistributionSummary.builder(name).tags(tags).register(registry);
    }
}
