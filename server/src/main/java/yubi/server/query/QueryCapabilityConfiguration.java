package yubi.server.query;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import yubi.query.application.DefaultQueryService;
import yubi.query.port.QueryAccessPolicyPort;
import yubi.query.port.QueryAuditPort;
import yubi.query.port.QueryDefinitionPort;
import yubi.query.port.QueryEnginePort;
import yubi.query.port.QueryVariablePort;

@Configuration
public class QueryCapabilityConfiguration {

    @Bean
    public DefaultQueryService defaultQueryService(QueryDefinitionPort definitionPort,
                                                   QueryAccessPolicyPort accessPolicyPort,
                                                   QueryVariablePort variablePort,
                                                   QueryEnginePort enginePort,
                                                   QueryAuditPort auditPort) {
        return new DefaultQueryService(definitionPort, accessPolicyPort, variablePort, enginePort, auditPort);
    }
}
