package yubi.agent.port;

import yubi.agent.api.WriteToolSchema;

import java.util.List;
import java.util.Optional;

public interface WriteProposalToolRegistry {

    List<WriteToolSchema> schemas();

    Optional<WriteProposalTool> find(String name);
}
