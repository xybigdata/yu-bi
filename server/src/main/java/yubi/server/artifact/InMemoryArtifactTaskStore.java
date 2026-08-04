package yubi.server.artifact;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

final class InMemoryArtifactTaskStore implements ArtifactTaskStore {

    private final Map<String, StoredArtifactTask> tasks = new HashMap<>();

    @Override
    public synchronized boolean insertIfOwnerBelowLimit(StoredArtifactTask task, int maxActiveTasks) {
        long activeTasks = tasks.values().stream()
                .filter(existing -> existing.ownerKey().equals(task.ownerKey()))
                .filter(existing -> !existing.state().isTerminal())
                .filter(existing -> existing.deadlineAt().isAfter(task.acceptedAt()))
                .count();
        if (activeTasks >= maxActiveTasks) {
            return false;
        }
        if (tasks.putIfAbsent(task.id(), task) != null) {
            throw new IllegalStateException("产物任务 ID 已存在");
        }
        return true;
    }

    @Override
    public synchronized Optional<StoredArtifactTask> find(String id) {
        return Optional.ofNullable(tasks.get(id));
    }

    @Override
    public synchronized List<StoredArtifactTask> findActiveByOwner(String ownerKey) {
        return tasks.values().stream()
                .filter(task -> task.ownerKey().equals(ownerKey))
                .filter(task -> !task.state().isTerminal())
                .sorted(Comparator.comparing(StoredArtifactTask::acceptedAt).reversed()
                        .thenComparing(StoredArtifactTask::id))
                .toList();
    }

    @Override
    public synchronized List<StoredArtifactTask> findTerminalByOwner(String ownerKey,
                                                                     int offset,
                                                                     int limit) {
        return tasks.values().stream()
                .filter(task -> task.ownerKey().equals(ownerKey))
                .filter(task -> task.state().isTerminal())
                .sorted(Comparator.comparingInt(InMemoryArtifactTaskStore::terminalGroup)
                        .thenComparing(StoredArtifactTask::acceptedAt, Comparator.reverseOrder())
                        .thenComparing(StoredArtifactTask::id))
                .skip(offset)
                .limit(limit)
                .toList();
    }

    private static int terminalGroup(StoredArtifactTask task) {
        return task.state() == ArtifactTaskState.READY ? 1 : 0;
    }

    @Override
    public synchronized boolean updateIfState(String id,
                                              Set<ArtifactTaskState> expected,
                                              UnaryOperator<StoredArtifactTask> update) {
        StoredArtifactTask current = tasks.get(id);
        if (current == null || !expected.contains(current.state())) {
            return false;
        }
        tasks.put(id, update.apply(current));
        return true;
    }

    @Override
    public synchronized Optional<StoredArtifactTask> deleteIfState(String id,
                                                                   Set<ArtifactTaskState> expected) {
        StoredArtifactTask current = tasks.get(id);
        if (current == null || !expected.contains(current.state())) {
            return Optional.empty();
        }
        tasks.remove(id);
        return Optional.of(current);
    }

    @Override
    public synchronized List<StoredArtifactTask> findMaintenanceCandidates(
            Instant deadlineAtOrBefore,
            Instant completedBefore,
            int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("维护任务批量大小必须为正数");
        }
        return tasks.values().stream()
                .filter(task -> isExpiredActive(task, deadlineAtOrBefore)
                        || isExpiredTerminal(task, completedBefore))
                .sorted(Comparator.comparing(StoredArtifactTask::acceptedAt)
                        .thenComparing(StoredArtifactTask::id))
                .limit(limit)
                .toList();
    }

    private boolean isExpiredActive(StoredArtifactTask task, Instant cutoff) {
        return !task.state().isTerminal() && !task.deadlineAt().isAfter(cutoff);
    }

    private boolean isExpiredTerminal(StoredArtifactTask task, Instant cutoff) {
        return task.state().isTerminal()
                && task.completedAt() != null
                && (task.expiresAt() == null
                    ? task.completedAt().isBefore(cutoff)
                    : task.expiresAt().isBefore(cutoff));
    }
}
