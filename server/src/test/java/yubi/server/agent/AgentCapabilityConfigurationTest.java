package yubi.server.agent;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ClassUtils;
import yubi.agent.api.AgentUseCase;
import yubi.agent.application.DefaultReadOnlyToolRegistry;
import yubi.agent.application.BoundedToolExecutor;
import yubi.agent.domain.ToolExecutionPolicy;
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

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentCapabilityConfigurationTest {

    @Test
    void shouldExposeExactRegistryWithoutCreatingRuntimeWhenGatewayAndStoreAreAbsent() {
        try (AnnotationConfigApplicationContext context = baseContext(false)) {
            assertFalse(context.getBeansOfType(AgentUseCase.class).containsKey("agentUseCase"));
            ReadOnlyToolRegistry registry = context.getBean(ReadOnlyToolRegistry.class);
            assertEquals(DefaultReadOnlyToolRegistry.TOOL_NAMES,
                    registry.schemas().stream().map(value -> value.name()).toList());
            assertEquals(ToolExecutionPolicy.defaults(), context.getBean(ToolExecutionPolicy.class));
            assertTrue(context.getBeansOfType(yubi.agent.port.ToolExecutionPort.class).isEmpty());
        }
    }

    @Test
    void shouldUseServerAsCompositionRootWhenFakeGatewayAndSessionPortAreProvided() {
        try (AnnotationConfigApplicationContext context = baseContext(true)) {
            assertTrue(context.containsBean("agentUseCase"));
            assertTrue(context.getBean(AgentUseCase.class) instanceof yubi.agent.application.DefaultAgentRuntime);
            assertTrue(context.getBean(yubi.agent.port.ToolExecutionPort.class) instanceof BoundedToolExecutor);
        }
    }

    @Test
    void shouldBindNonDefaultExternalLimitsExactly() {
        Map<String, Object> overrides = Map.of(
                "yubi.agent.query.maximum-page-size", 37,
                "yubi.agent.tool.timeout-millis", 4_321,
                "yubi.agent.tool.maximum-concurrent-calls", 2,
                "yubi.agent.result.maximum-items", 29,
                "yubi.agent.result.maximum-bytes", 4_096);

        try (AnnotationConfigApplicationContext context = baseContext(true, overrides)) {
            assertEquals(new ToolExecutionPolicy(37, 4_321, 2), context.getBean(ToolExecutionPolicy.class));
            assertEquals(new yubi.agent.domain.ToolResultLimits(29, 4_096),
                    context.getBean(yubi.agent.domain.ToolResultLimits.class));
        }
    }

    @Test
    void shouldFailFastForInvalidExternalConcurrencyLimit() {
        AnnotationConfigApplicationContext context = unrefreshedContext(false,
                Map.of("yubi.agent.tool.maximum-concurrent-calls", 65));
        try {
            BeanCreationException failure = assertThrows(BeanCreationException.class, context::refresh);
            assertInstanceOf(IllegalArgumentException.class, failure.getMostSpecificCause());
            assertEquals("Agent Tool 并发限制无效", failure.getMostSpecificCause().getMessage());
        } finally {
            context.close();
        }
    }

    @Test
    void applicationYamlMustDeclareAllAgentLimitKeys() throws Exception {
        var properties = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml"));
        assertEquals(1, properties.size());
        var source = properties.getFirst();
        assertEquals(100, source.getProperty("yubi.agent.query.maximum-page-size"));
        assertEquals(30_000, source.getProperty("yubi.agent.tool.timeout-millis"));
        assertEquals(4, source.getProperty("yubi.agent.tool.maximum-concurrent-calls"));
        assertEquals(100, source.getProperty("yubi.agent.result.maximum-items"));
        assertEquals(65_536, source.getProperty("yubi.agent.result.maximum-bytes"));
    }

    @Test
    void metricsMustHaveRegistryWithoutActuatorWebSurface() throws Exception {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        MetricsAutoConfiguration.class,
                        SimpleMetricsExportAutoConfiguration.class))
                .run(context -> assertEquals(1, context.getBeansOfType(MeterRegistry.class).size()));

        ClassLoader classLoader = AgentCapabilityConfigurationTest.class.getClassLoader();
        assertFalse(ClassUtils.isPresent(
                "org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration",
                classLoader));

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        var document = factory.newDocumentBuilder().parse(new java.io.File("pom.xml"));
        var dependencies = document.getElementsByTagNameNS("http://maven.apache.org/POM/4.0.0", "dependency");
        java.util.HashSet<String> artifacts = new java.util.HashSet<>();
        for (int index = 0; index < dependencies.getLength(); index++) {
            var dependency = dependencies.item(index);
            var children = dependency.getChildNodes();
            for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
                var child = children.item(childIndex);
                if ("artifactId".equals(child.getLocalName())) {
                    artifacts.add(child.getTextContent().trim());
                }
            }
        }
        assertTrue(artifacts.contains("spring-boot-starter-micrometer-metrics"));
        assertTrue(java.util.Collections.disjoint(artifacts, Set.of(
                "spring-boot-starter-actuator",
                "spring-boot-actuator",
                "spring-boot-actuator-autoconfigure")));
    }

    private AnnotationConfigApplicationContext baseContext(boolean runtimePorts) {
        return baseContext(runtimePorts, Map.of());
    }

    private AnnotationConfigApplicationContext baseContext(boolean runtimePorts, Map<String, Object> properties) {
        AnnotationConfigApplicationContext context = unrefreshedContext(runtimePorts, properties);
        context.refresh();
        return context;
    }

    private AnnotationConfigApplicationContext unrefreshedContext(boolean runtimePorts,
                                                                   Map<String, Object> properties) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("agent-test", properties));
        context.registerBean(QueryMetadataUseCase.class, EmptyMetadataUseCase::new);
        context.registerBean(ExecuteQueryUseCase.class, () -> (command, executionContext) -> QueryResult.empty());
        context.registerBean(AgentAuditPort.class, () -> event -> { });
        if (runtimePorts) {
            context.registerBean(ModelGateway.class, () -> turn -> new FinalAnswer("done"));
            context.registerBean(AgentSessionStorePort.class, () -> session -> { });
        }
        context.register(AgentCapabilityConfiguration.class);
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
