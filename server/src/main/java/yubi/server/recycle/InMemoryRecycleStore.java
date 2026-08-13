package yubi.server.recycle;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.time.Instant;
import java.util.List;

final class InMemoryRecycleStore implements RecycleStore {

    private final Map<String, RecyclePreparedOperation> preparedOperations = new HashMap<>();
    private final Map<String, RecycleBatch> batchesByRequest = new HashMap<>();
    private final Map<String, RecycleRecord> records = new HashMap<>();
    private final Map<String, RecyclePolicy> policies = new HashMap<>();
    private final Map<String, RecycleAuditEvent> audits = new HashMap<>();

    @Override
    public synchronized void savePrepared(RecyclePreparedOperation operation) {
        preparedOperations.put(operation.token(), operation);
    }

    @Override
    public synchronized Optional<RecyclePreparedOperation> findPrepared(String token) {
        return Optional.ofNullable(preparedOperations.get(token));
    }

    @Override
    public synchronized Optional<RecycleBatch> findBatchByRequest(String actorId,
                                                                  String organizationId,
                                                                  String clientRequestId) {
        return Optional.ofNullable(batchesByRequest.get(key(actorId, organizationId, clientRequestId)));
    }

    @Override
    public synchronized Optional<RecycleBatch> findBatch(String actorId,
                                                         String organizationId,
                                                         String batchId) {
        return batchesByRequest.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(
                        actorId + '\u0000' + organizationId + '\u0000'))
                .map(Map.Entry::getValue)
                .filter(batch -> batch.id().equals(batchId))
                .findFirst();
    }

    @Override
    public synchronized RecycleBatch saveBatch(String actorId,
                                               String organizationId,
                                               String clientRequestId,
                                               RecycleBatch batch) {
        return batchesByRequest.computeIfAbsent(
                key(actorId, organizationId, clientRequestId),
                ignored -> batch
        );
    }

    @Override
    public synchronized void completeBatch(String actorId,
                                           String organizationId,
                                           RecycleBatch batch,
                                           Instant completedAt) {
        batchesByRequest.replaceAll((key, current) ->
                key.startsWith(actorId + '\u0000' + organizationId + '\u0000')
                        && current.id().equals(batch.id()) ? batch : current);
    }

    @Override
    public synchronized void saveRecord(RecycleRecord record) {
        records.put(record.id(), record);
    }

    @Override
    public synchronized Optional<RecycleRecord> findRecord(String id) {
        return Optional.ofNullable(records.get(id));
    }

    @Override
    public synchronized List<RecycleRecord> listRecords(String organizationId,
                                                        RecycleResourceType resourceType) {
        return records.values().stream()
                .filter(record -> record.organizationId().equals(organizationId))
                .filter(record -> record.resourceType() == resourceType)
                .sorted(java.util.Comparator.comparing(RecycleRecord::deletedAt).reversed())
                .toList();
    }

    @Override
    public synchronized void deleteRecord(String id) {
        records.remove(id);
    }

    @Override
    public synchronized Optional<RecyclePolicy> findPolicy(String organizationId,
                                                           RecycleResourceType resourceType) {
        return Optional.ofNullable(policies.get(policyKey(organizationId, resourceType)));
    }

    @Override
    public synchronized void savePolicy(String organizationId,
                                        RecycleResourceType resourceType,
                                        RecyclePolicy policy,
                                        String updatedBy,
                                        Instant updatedAt) {
        policies.put(policyKey(organizationId, resourceType), policy);
    }

    @Override
    public synchronized void recalculateExpiry(String organizationId,
                                               RecycleResourceType resourceType,
                                               Instant expiresAt) {
        records.replaceAll((id, record) -> record.organizationId().equals(organizationId)
                && record.resourceType() == resourceType
                ? record.withExpiresAt(expiresAt)
                : record);
    }

    @Override
    public synchronized List<RecycleRecord> findExpired(Instant expiresAtOrBefore, int limit) {
        return records.values().stream()
                .filter(record -> record.expiresAt() != null)
                .filter(record -> !record.expiresAt().isAfter(expiresAtOrBefore))
                .sorted(java.util.Comparator.comparing(RecycleRecord::expiresAt))
                .limit(limit)
                .toList();
    }

    @Override
    public synchronized void saveAudit(RecycleAuditEvent event) {
        audits.put(event.id(), event);
    }

    @Override
    public synchronized void pruneAudit(Instant createdBefore) {
        audits.values().removeIf(event -> event.createdAt().isBefore(createdBefore));
    }

    private String key(String actorId, String organizationId, String clientRequestId) {
        return actorId + '\u0000' + organizationId + '\u0000' + clientRequestId;
    }

    private String policyKey(String organizationId, RecycleResourceType resourceType) {
        return organizationId + '\u0000' + resourceType;
    }
}
