package yubi.server.artifact;

import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

final class ArtifactTaskMaintenance {

    private static final Set<ArtifactTaskState> ACTIVE_STATES = Set.of(
            ArtifactTaskState.QUEUED,
            ArtifactTaskState.RUNNING
    );
    private static final Set<ArtifactTaskState> TERMINAL_STATES = Set.of(
            ArtifactTaskState.READY,
            ArtifactTaskState.FAILED,
            ArtifactTaskState.TIMED_OUT
    );

    private final ArtifactTaskStore taskStore;
    private final ArtifactBlobStore blobStore;
    private final Clock clock;
    private final Duration retention;
    private final int batchSize;
    private final ArtifactRetryRegistry retryRegistry;

    ArtifactTaskMaintenance(ArtifactTaskStore taskStore,
                            ArtifactBlobStore blobStore,
                            Clock clock,
                            Duration retention,
                            int batchSize) {
        this(taskStore, blobStore, clock, retention, batchSize, new ArtifactRetryRegistry());
    }

    ArtifactTaskMaintenance(ArtifactTaskStore taskStore,
                            ArtifactBlobStore blobStore,
                            Clock clock,
                            Duration retention,
                            int batchSize,
                            ArtifactRetryRegistry retryRegistry) {
        this.taskStore = Objects.requireNonNull(taskStore, "任务存储不能为空");
        this.blobStore = Objects.requireNonNull(blobStore, "产物存储不能为空");
        this.clock = Objects.requireNonNull(clock, "时钟不能为空");
        this.retention = requirePositive(retention, "产物保留时间必须为正数");
        this.retryRegistry = Objects.requireNonNull(retryRegistry, "重试注册表不能为空");
        if (batchSize < 1) {
            throw new IllegalArgumentException("维护任务批量大小必须为正数");
        }
        this.batchSize = batchSize;
    }

    @Scheduled(
            initialDelayString = "${yubi.artifact.maintenance-initial-delay-ms:60000}",
            fixedDelayString = "${yubi.artifact.maintenance-interval-ms:60000}"
    )
    public void run() {
        Instant now = clock.instant();
        for (StoredArtifactTask candidate : taskStore.findMaintenanceCandidates(
                now,
                now,
                batchSize
        )) {
            timeout(candidate, now);
            deleteExpired(candidate.id(), now);
        }
    }

    private void timeout(StoredArtifactTask candidate, Instant now) {
        if (candidate.state().isTerminal() || candidate.deadlineAt().isAfter(now)) {
            return;
        }
        taskStore.updateIfState(candidate.id(), ACTIVE_STATES, task -> task.failed(
                ArtifactTaskState.TIMED_OUT,
                new ArtifactFailure(
                        "ARTIFACT_TIMED_OUT",
                        "产物生成超时，请重新发起",
                        task.traceId()
                ),
                now,
                now.plus(retention)
        ));
    }

    private void deleteExpired(String taskId, Instant now) {
        StoredArtifactTask current = taskStore.find(taskId).orElse(null);
        if (current == null || !current.state().isTerminal() || current.completedAt() == null) {
            return;
        }
        Instant expiresAt = current.expiresAt() == null
                ? current.completedAt().plus(retention)
                : current.expiresAt();
        if (!now.isAfter(expiresAt)) {
            return;
        }
        if (current.blobKey() != null) {
            blobStore.delete(current.blobKey());
        }
        taskStore.deleteIfState(taskId, TERMINAL_STATES)
                .ifPresent(ignored -> retryRegistry.remove(taskId));
    }

    private static Duration requirePositive(Duration value, String message) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
