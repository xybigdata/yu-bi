package yubi.server.artifact;

import java.util.Set;

public interface ArtifactTasks {

    TaskHandle submit(ArtifactAccess access, ArtifactDescriptor descriptor, ArtifactProducer producer);

    TaskBatch inspect(ArtifactAccess access, Set<String> ids);

    TaskPage list(ArtifactAccess access, int terminalOffset, int terminalLimit);

    TaskHandle retry(ArtifactAccess access, String id);

    ArtifactContent open(ArtifactAccess access, String id);

    void confirmDelivery(ArtifactAccess access, String id);

    void delete(ArtifactAccess access, String id);
}
