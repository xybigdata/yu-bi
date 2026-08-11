package yubi.server.recycle;

import java.time.Instant;

record RecycleAuditEvent(String id,
                         String batchId,
                         String recordId,
                         String organizationId,
                         RecycleResourceType resourceType,
                         String rootId,
                         RecycleOperation action,
                         String result,
                         String reason,
                         String actorId,
                         Instant createdAt) {
}
