package yubi.server.query;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import yubi.query.api.ExecuteQueryUseCase;
import yubi.query.api.PreviewQueryUseCase;
import yubi.query.api.QueryMetadataUseCase;
import yubi.query.port.QueryAccessPolicyPort;
import yubi.query.port.QueryAuditPort;
import yubi.query.port.QueryDefinitionPort;
import yubi.query.port.QueryEnginePort;
import yubi.query.port.QueryVariablePort;
import yubi.query.port.QueryMetadataAccessPolicyPort;
import yubi.query.port.QueryMetadataCatalogPort;
import yubi.query.port.QueryMetadataDefinitionPort;
import yubi.query.port.QueryMetadataFunctionPort;
import yubi.query.port.QueryMetadataVariablePort;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class QueryCapabilityConfigurationTest {

    @Test
    void shouldExposeOneApplicationServiceForBothUseCases() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(QueryDefinitionPort.class, () -> mock(QueryDefinitionPort.class));
            context.registerBean(QueryAccessPolicyPort.class, () -> mock(QueryAccessPolicyPort.class));
            context.registerBean(QueryVariablePort.class, () -> mock(QueryVariablePort.class));
            context.registerBean(QueryEnginePort.class, () -> mock(QueryEnginePort.class));
            context.registerBean(QueryAuditPort.class, () -> mock(QueryAuditPort.class));
            context.registerBean(QueryMetadataCatalogPort.class, () -> mock(QueryMetadataCatalogPort.class));
            context.registerBean(QueryMetadataAccessPolicyPort.class,
                    () -> mock(QueryMetadataAccessPolicyPort.class));
            context.registerBean(QueryMetadataDefinitionPort.class,
                    () -> mock(QueryMetadataDefinitionPort.class));
            context.registerBean(QueryMetadataVariablePort.class,
                    () -> mock(QueryMetadataVariablePort.class));
            context.registerBean(QueryMetadataFunctionPort.class,
                    () -> mock(QueryMetadataFunctionPort.class));
            context.register(QueryCapabilityConfiguration.class);
            context.refresh();

            assertSame(context.getBean(ExecuteQueryUseCase.class), context.getBean(PreviewQueryUseCase.class));
            assertSame(context.getBean(QueryMetadataUseCase.class),
                    context.getBean(yubi.query.application.DefaultQueryMetadataService.class));
        }
    }
}
