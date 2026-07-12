package yubi.server.agent;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import yubi.agent.api.AgentUseCase;
import yubi.agent.application.DefaultAgentRuntime;
import yubi.agent.application.BoundedToolExecutor;
import yubi.agent.application.DefaultReadOnlyToolRegistry;
import yubi.agent.application.DescribeDataAssetTool;
import yubi.agent.application.ExecuteViewTool;
import yubi.agent.application.SearchDataAssetsTool;
import yubi.agent.domain.AgentRuntimePolicy;
import yubi.agent.domain.ToolResultLimits;
import yubi.agent.domain.ToolExecutionPolicy;
import yubi.agent.port.AgentAuditPort;
import yubi.agent.port.AgentClockPort;
import yubi.agent.port.AgentMetricsPort;
import yubi.agent.port.AgentSessionStorePort;
import yubi.agent.port.ModelGateway;
import yubi.agent.port.ReadOnlyToolRegistry;
import yubi.agent.port.ToolExecutionPort;
import yubi.query.api.ExecuteQueryUseCase;
import yubi.query.api.QueryMetadataUseCase;

import java.util.List;

@Configuration
public class AgentCapabilityConfiguration {

    @Bean
    public ToolResultLimits agentToolResultLimits(
            @Value("${yubi.agent.result.maximum-items:100}") int maximumItems,
            @Value("${yubi.agent.result.maximum-bytes:65536}") long maximumBytes) {
        return new ToolResultLimits(maximumItems, maximumBytes);
    }

    @Bean
    public ToolExecutionPolicy agentToolExecutionPolicy(
            @Value("${yubi.agent.query.maximum-page-size:100}") int maximumPageSize,
            @Value("${yubi.agent.tool.timeout-millis:30000}") long timeoutMillis,
            @Value("${yubi.agent.tool.maximum-concurrent-calls:4}") int maximumConcurrentCalls) {
        return new ToolExecutionPolicy(maximumPageSize, timeoutMillis, maximumConcurrentCalls);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnBean({ModelGateway.class, AgentSessionStorePort.class})
    public BoundedToolExecutor agentToolExecutor(ToolExecutionPolicy policy) {
        return new BoundedToolExecutor(policy);
    }

    @Bean
    public AgentMetricsPort agentMetricsPort(ObjectProvider<MeterRegistry> meterRegistry) {
        MeterRegistry registry = meterRegistry.getIfAvailable();
        return registry == null ? metric -> { } : new ServerAgentMetricsAdapter(registry);
    }

    @Bean
    public AgentRuntimePolicy agentRuntimePolicy() {
        return AgentRuntimePolicy.defaults();
    }

    @Bean
    public AgentClockPort agentClockPort() {
        return System::nanoTime;
    }

    @Bean
    public ReadOnlyToolRegistry readOnlyToolRegistry(QueryMetadataUseCase metadataUseCase,
                                                     ExecuteQueryUseCase executeUseCase,
                                                     ToolResultLimits limits,
                                                     ToolExecutionPolicy executionPolicy) {
        return new DefaultReadOnlyToolRegistry(List.of(
                new SearchDataAssetsTool(metadataUseCase, limits),
                new DescribeDataAssetTool(metadataUseCase, limits),
                new ExecuteViewTool(executeUseCase, metadataUseCase, limits, executionPolicy)));
    }

    /** 具体模型网关和会话存储由后续基础设施适配器提供；缺失时不创建不可用 Runtime。 */
    @Bean
    @ConditionalOnBean({ModelGateway.class, AgentSessionStorePort.class})
    public AgentUseCase agentUseCase(ModelGateway modelGateway,
                                     ReadOnlyToolRegistry toolRegistry,
                                     AgentAuditPort auditPort,
                                     AgentSessionStorePort sessionStore,
                                     AgentClockPort clock,
                                     AgentRuntimePolicy policy,
                                     ToolExecutionPort toolExecutionPort,
                                     AgentMetricsPort metricsPort) {
        return new DefaultAgentRuntime(modelGateway, toolRegistry, auditPort, sessionStore, clock, policy,
                toolExecutionPort, metricsPort);
    }
}
