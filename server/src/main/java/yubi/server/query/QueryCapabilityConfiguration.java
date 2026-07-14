package yubi.server.query;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import yubi.query.application.DefaultQueryService;
import yubi.query.application.DefaultQueryMetadataService;
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

    @Bean
    public DefaultQueryMetadataService defaultQueryMetadataService(QueryMetadataCatalogPort catalogPort,
                                                                   QueryMetadataAccessPolicyPort accessPolicyPort,
                                                                   QueryMetadataDefinitionPort definitionPort,
                                                                   QueryMetadataVariablePort variablePort,
                                                                   QueryMetadataFunctionPort functionPort) {
        return new DefaultQueryMetadataService(catalogPort, accessPolicyPort, definitionPort,
                variablePort, functionPort);
    }
}
