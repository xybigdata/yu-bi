package yubi.server.agent;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import yubi.agent.api.AgentUseCase;
import yubi.agent.application.DefaultReadOnlyToolRegistry;
import yubi.agent.domain.ModelProtocol.FinalAnswer;
import yubi.agent.port.AgentAuditPort;
import yubi.agent.port.AgentSessionStorePort;
import yubi.agent.port.ModelGateway;
import yubi.agent.port.ReadOnlyToolRegistry;
import yubi.query.api.DataAssetDetail;
import yubi.query.api.DescribeDataAssetRequest;
import yubi.query.api.ExecuteQueryCommand;
import yubi.query.api.ExecuteQueryUseCase;
import yubi.query.api.QueryExecutionContext;
import yubi.query.api.QueryMetadataUseCase;
import yubi.query.api.QueryResult;
import yubi.query.api.SearchDataAssetsRequest;
import yubi.query.api.SearchDataAssetsResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentCapabilityConfigurationTest {

    @Test
    void shouldExposeExactRegistryWithoutCreatingRuntimeWhenGatewayAndStoreAreAbsent() {
        try (AnnotationConfigApplicationContext context = baseContext(false)) {
            assertFalse(context.getBeansOfType(AgentUseCase.class).containsKey("agentUseCase"));
            ReadOnlyToolRegistry registry = context.getBean(ReadOnlyToolRegistry.class);
            assertEquals(DefaultReadOnlyToolRegistry.TOOL_NAMES,
                    registry.schemas().stream().map(value -> value.name()).toList());
        }
    }

    @Test
    void shouldUseServerAsCompositionRootWhenFakeGatewayAndSessionPortAreProvided() {
        try (AnnotationConfigApplicationContext context = baseContext(true)) {
            assertTrue(context.containsBean("agentUseCase"));
            assertTrue(context.getBean(AgentUseCase.class) instanceof yubi.agent.application.DefaultAgentRuntime);
        }
    }

    private AnnotationConfigApplicationContext baseContext(boolean runtimePorts) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean(QueryMetadataUseCase.class, EmptyMetadataUseCase::new);
        context.registerBean(ExecuteQueryUseCase.class, () -> (command, executionContext) -> QueryResult.empty());
        context.registerBean(AgentAuditPort.class, () -> event -> { });
        if (runtimePorts) {
            context.registerBean(ModelGateway.class, () -> turn -> new FinalAnswer("done"));
            context.registerBean(AgentSessionStorePort.class, () -> session -> { });
        }
        context.register(AgentCapabilityConfiguration.class);
        context.refresh();
        return context;
    }

    private static final class EmptyMetadataUseCase implements QueryMetadataUseCase {
        @Override
        public SearchDataAssetsResult search(SearchDataAssetsRequest request, QueryExecutionContext context) {
            return new SearchDataAssetsResult(List.of());
        }

        @Override
        public DataAssetDetail describe(DescribeDataAssetRequest request, QueryExecutionContext context) {
            return new DataAssetDetail(request.assetId(), "asset", null,
                    List.of(), List.of(), List.of(), java.util.Optional.empty());
        }
    }
}
