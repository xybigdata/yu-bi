package yubi.server.artifact;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

final class ArtifactRetryRegistry {

    private final ConcurrentMap<String, Entry> entries = new ConcurrentHashMap<>();

    void register(String taskId, ArtifactProducer producer) {
        entries.put(
                Objects.requireNonNull(taskId, "任务 ID 不能为空"),
                new Available(Objects.requireNonNull(producer, "产物 producer 不能为空"))
        );
    }

    ArtifactProducer claim(String taskId) {
        AtomicReference<ArtifactProducer> claimed = new AtomicReference<>();
        entries.computeIfPresent(taskId, (ignored, entry) -> {
            if (entry instanceof Available available) {
                claimed.set(available.producer());
                return new Claimed(available.producer());
            }
            return entry;
        });
        return claimed.get();
    }

    void restore(String taskId, ArtifactProducer producer) {
        entries.computeIfPresent(taskId, (ignored, entry) ->
                entry instanceof Claimed claimed && claimed.producer() == producer
                        ? new Available(producer)
                        : entry);
    }

    void completeClaim(String taskId, ArtifactProducer producer) {
        entries.computeIfPresent(taskId, (ignored, entry) ->
                entry instanceof Claimed claimed && claimed.producer() == producer
                        ? null
                        : entry);
    }

    void remove(String taskId) {
        entries.remove(taskId);
    }

    private sealed interface Entry permits Available, Claimed {
    }

    private record Available(ArtifactProducer producer) implements Entry {
    }

    private record Claimed(ArtifactProducer producer) implements Entry {
    }
}
