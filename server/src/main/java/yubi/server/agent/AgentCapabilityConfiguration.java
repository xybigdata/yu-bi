package yubi.server.agent;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import yubi.agent.api.AgentUseCase;
import yubi.agent.application.DefaultAgentRuntime;
import yubi.agent.application.DefaultReadOnlyToolRegistry;
import yubi.agent.application.DescribeDataAssetTool;
import yubi.agent.application.ExecuteViewTool;
import yubi.agent.application.SearchDataAssetsTool;
import yubi.agent.domain.AgentRuntimePolicy;
import yubi.agent.domain.ToolResultLimits;
import yubi.agent.port.AgentAuditPort;
import yubi.agent.port.AgentClockPort;
import yubi.agent.port.AgentSessionStorePort;
import yubi.agent.port.ModelGateway;
import yubi.agent.port.ReadOnlyToolRegistry;
import yubi.query.api.ExecuteQueryUseCase;
import yubi.query.api.QueryMetadataUseCase;

import java.util.List;

@Configuration
public class AgentCapabilityConfiguration {

    @Bean
    public ToolResultLimits agentToolResultLimits() {
        return ToolResultLimits.defaults();
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
                                                     ToolResultLimits limits) {
        return new DefaultReadOnlyToolRegistry(List.of(
                new SearchDataAssetsTool(metadataUseCase, limits),
                new DescribeDataAssetTool(metadataUseCase, limits),
                new ExecuteViewTool(executeUseCase, metadataUseCase, limits)));
    }

    /** 具体模型网关和会话存储由后续基础设施适配器提供；缺失时不创建不可用 Runtime。 */
    @Bean
    @ConditionalOnBean({ModelGateway.class, AgentSessionStorePort.class})
    public AgentUseCase agentUseCase(ModelGateway modelGateway,
                                     ReadOnlyToolRegistry toolRegistry,
                                     AgentAuditPort auditPort,
                                     AgentSessionStorePort sessionStore,
                                     AgentClockPort clock,
                                     AgentRuntimePolicy policy) {
        return new DefaultAgentRuntime(modelGateway, toolRegistry, auditPort, sessionStore, clock, policy);
    }
}
