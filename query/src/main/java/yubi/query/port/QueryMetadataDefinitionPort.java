package yubi.query.port;

import yubi.query.domain.QueryMetadataModels.MetadataDefinition;

public interface QueryMetadataDefinitionPort {

    MetadataDefinition load(String assetId, boolean includeScript, boolean scriptVisible);
}
