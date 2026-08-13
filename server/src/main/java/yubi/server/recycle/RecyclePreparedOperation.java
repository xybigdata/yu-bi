package yubi.server.recycle;

import java.time.Instant;
import java.util.List;

record RecyclePreparedOperation(String token,
                                String actorId,
                                String organizationId,
                                RecycleResourceType resourceType,
                                List<String> rootIds,
                                Instant expiresAt) {

    RecyclePreparedOperation {
        rootIds = List.copyOf(rootIds);
    }
}
