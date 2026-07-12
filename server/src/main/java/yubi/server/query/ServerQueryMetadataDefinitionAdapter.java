package yubi.server.query;

import org.springframework.stereotype.Component;
import yubi.query.domain.QueryMetadataModels.MetadataDefinition;
import yubi.query.domain.QueryModels.Definition;
import yubi.query.port.QueryDefinitionPort;
import yubi.query.port.QueryMetadataDefinitionPort;

@Component
public class ServerQueryMetadataDefinitionAdapter implements QueryMetadataDefinitionPort {

    private final QueryDefinitionPort definitionPort;

    public ServerQueryMetadataDefinitionAdapter(QueryDefinitionPort definitionPort) {
        this.definitionPort = definitionPort;
    }

    @Override
    public MetadataDefinition load(String assetId, boolean includeScript, boolean scriptVisible) {
        Definition definition = definitionPort.load(assetId);
        return definition == null ? null : new MetadataDefinition(
                definition.viewId(), definition.organizationId(), definition.sourceId(),
                includeScript && scriptVisible ? definition.script() : null, definition.schema());
    }
}
