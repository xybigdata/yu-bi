package yubi.server.agent.write;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import yubi.agent.application.CreateChartWriteProposalTool;
import yubi.agent.application.DefaultControlledWriteUseCase;
import yubi.agent.application.DefaultWriteProposalToolRegistry;
import yubi.agent.application.RenameDashboardWriteProposalTool;
import yubi.agent.port.VisualizationWritePort;
import yubi.agent.port.WriteAuditPort;
import yubi.agent.port.WriteClockPort;
import yubi.agent.port.WriteIdGeneratorPort;
import yubi.agent.port.WriteOperationStorePort;
import yubi.agent.port.WriteProposalToolRegistry;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Configuration
public class ControlledWriteConfiguration {

    @Bean
    public WriteProposalToolRegistry writeProposalToolRegistry() {
        return new DefaultWriteProposalToolRegistry(List.of(
                new CreateChartWriteProposalTool(),
                new RenameDashboardWriteProposalTool()));
    }

    @Bean
    public WriteIdGeneratorPort writeIdGeneratorPort() {
        return () -> UUID.randomUUID().toString();
    }

    @Bean
    public WriteClockPort writeClockPort() {
        return Instant::now;
    }

    @Bean
    public DefaultControlledWriteUseCase controlledWriteUseCase(
            WriteProposalToolRegistry registry,
            VisualizationWritePort visualizationWritePort,
            WriteOperationStorePort operationStore,
            WriteAuditPort auditPort,
            WriteIdGeneratorPort idGenerator,
            WriteClockPort clock,
            @Value("${yubi.agent.workspace.approval-lifetime-minutes:15}") long approvalLifetimeMinutes) {
        return new DefaultControlledWriteUseCase(registry, visualizationWritePort, operationStore,
                auditPort, idGenerator, clock, Duration.ofMinutes(approvalLifetimeMinutes));
    }
}
