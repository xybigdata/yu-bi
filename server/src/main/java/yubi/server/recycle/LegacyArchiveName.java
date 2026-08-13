package yubi.server.recycle;

import java.time.Instant;

record LegacyArchiveName(String originalName,
                         Instant deletedAt,
                         boolean timestampParsed) {
}
