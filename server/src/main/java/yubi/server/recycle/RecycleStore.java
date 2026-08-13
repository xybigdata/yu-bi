package yubi.server.recycle;

import java.util.Optional;
import java.time.Instant;
import java.util.List;

interface RecycleStore {

    void savePrepared(RecyclePreparedOperation operation);

    Optional<RecyclePreparedOperation> findPrepared(String token);

    Optional<RecycleBatch> findBatchByRequest(String actorId,
                                              String organizationId,
                                              String clientRequestId);

    Optional<RecycleBatch> findBatch(String actorId,
                                     String organizationId,
                                     String batchId);

    RecycleBatch saveBatch(String actorId,
                           String organizationId,
                           String clientRequestId,
                           RecycleBatch batch);

    void completeBatch(String actorId,
                       String organizationId,
                       RecycleBatch batch,
                       Instant completedAt);

    void saveRecord(RecycleRecord record);

    Optional<RecycleRecord> findRecord(String id);

    List<RecycleRecord> listRecords(String organizationId, RecycleResourceType resourceType);

    void deleteRecord(String id);

    Optional<RecyclePolicy> findPolicy(String organizationId, RecycleResourceType resourceType);

    void savePolicy(String organizationId,
                    RecycleResourceType resourceType,
                    RecyclePolicy policy,
                    String updatedBy,
                    Instant updatedAt);

    void recalculateExpiry(String organizationId,
                           RecycleResourceType resourceType,
                           Instant expiresAt);

    List<RecycleRecord> findExpired(Instant expiresAtOrBefore, int limit);

    void saveAudit(RecycleAuditEvent event);

    void pruneAudit(Instant createdBefore);
}
