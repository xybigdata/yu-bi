package yubi.server.recycle;

import java.time.Instant;

record RecycleRecord(String id,
                     String organizationId,
                     RecycleResourceType resourceType,
                     RecycleRootSnapshot snapshot,
                     String deletedBy,
                     Instant deletedAt,
                     Instant expiresAt) {

    RecycleRecord withExpiresAt(Instant newExpiresAt) {
        return new RecycleRecord(
                id, organizationId, resourceType, snapshot,
                deletedBy, deletedAt, newExpiresAt);
    }
}
