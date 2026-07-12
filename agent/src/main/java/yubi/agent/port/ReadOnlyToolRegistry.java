package yubi.agent.port;

import yubi.query.api.MetadataToolSchema;

import java.util.List;
import java.util.Optional;

public interface ReadOnlyToolRegistry {
    List<MetadataToolSchema> schemas();

    Optional<ReadOnlyTool> find(String name);
}
