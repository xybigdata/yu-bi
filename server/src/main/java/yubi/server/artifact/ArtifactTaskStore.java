package yubi.server.artifact;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

interface ArtifactTaskStore {

    boolean insertIfOwnerBelowLimit(StoredArtifactTask task, int maxActiveTasks);

    Optional<StoredArtifactTask> find(String id);

    List<StoredArtifactTask> findActiveByOwner(String ownerKey);

    List<StoredArtifactTask> findTerminalByOwner(String ownerKey, int offset, int limit);

    boolean updateIfState(String id, Set<ArtifactTaskState> expected, UnaryOperator<StoredArtifactTask> update);

    Optional<StoredArtifactTask> deleteIfState(String id, Set<ArtifactTaskState> expected);

    List<StoredArtifactTask> findMaintenanceCandidates(Instant deadlineAtOrBefore,
                                                        Instant completedBefore,
                                                        int limit);
}

record StoredArtifactTask(String id,
                          String ownerKey,
                          ArtifactDescriptor descriptor,
                          ArtifactTaskState state,
                          Instant acceptedAt,
                          Instant deadlineAt,
                          Instant completedAt,
                          Instant expiresAt,
                          String blobKey,
                          ArtifactFailure failure,
                          String traceId) {

    StoredArtifactTask(String id,
                       String ownerKey,
                       ArtifactDescriptor descriptor,
                       ArtifactTaskState state,
                       Instant acceptedAt,
                       Instant deadlineAt,
                       Instant completedAt,
                       String blobKey,
                       ArtifactFailure failure,
                       String traceId) {
        this(id, ownerKey, descriptor, state, acceptedAt, deadlineAt, completedAt,
                null, blobKey, failure, traceId);
    }

    StoredArtifactTask running() {
        return new StoredArtifactTask(id, ownerKey, descriptor, ArtifactTaskState.RUNNING,
                acceptedAt, deadlineAt, null, null, null, null, traceId);
    }

    StoredArtifactTask ready(String blobKey, Instant completedAt, Instant expiresAt) {
        return new StoredArtifactTask(id, ownerKey, descriptor, ArtifactTaskState.READY,
                acceptedAt, deadlineAt, completedAt, expiresAt, blobKey, null, traceId);
    }

    StoredArtifactTask ready(String blobKey, Instant completedAt) {
        return ready(blobKey, completedAt, null);
    }

    StoredArtifactTask failed(ArtifactTaskState terminalState,
                              ArtifactFailure failure,
                              Instant completedAt,
                              Instant expiresAt) {
        return new StoredArtifactTask(id, ownerKey, descriptor, terminalState,
                acceptedAt, deadlineAt, completedAt, expiresAt, null, failure, traceId);
    }

    StoredArtifactTask failed(ArtifactTaskState terminalState,
                              ArtifactFailure failure,
                              Instant completedAt) {
        return failed(terminalState, failure, completedAt, null);
    }

    StoredArtifactTask delivered(Instant expiresAt) {
        return new StoredArtifactTask(id, ownerKey, descriptor, state,
                acceptedAt, deadlineAt, completedAt, expiresAt, blobKey, failure, traceId);
    }
}
