package yubi.server.recycle;

import java.time.Instant;

public record RecycleEntry(String id,
                           String rootId,
                           String name,
                           String originalParentId,
                           boolean folder,
                           int expandedItemCount,
                           String deletedBy,
                           String deletedByName,
                           Instant deletedAt,
                           Instant expiresAt) {

    static RecycleEntry from(RecycleRecord record, String deletedByName) {
        RecycleRootSnapshot snapshot = record.snapshot();
        return new RecycleEntry(
                record.id(), snapshot.rootId(), snapshot.originalName(),
                snapshot.originalParentId(), snapshot.folder(), snapshot.expandedItemCount(),
                record.deletedBy(), deletedByName, record.deletedAt(), record.expiresAt());
    }
}
