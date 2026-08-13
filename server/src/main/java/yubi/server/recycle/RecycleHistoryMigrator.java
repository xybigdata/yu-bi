package yubi.server.recycle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
final class RecycleHistoryMigrator {

    private static final Pattern ARCHIVED_NAME = Pattern.compile(
            "^(.*)\\.(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})$");
    private static final DateTimeFormatter ARCHIVED_AT = DateTimeFormatter
            .ofPattern("uuuu-MM-dd HH:mm:ss.SSS")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final int DEFAULT_RETENTION_DAYS = 30;

    private final JdbcTemplate jdbc;
    private final RecycleStore store;
    private final Clock clock;

    RecycleHistoryMigrator(JdbcTemplate jdbc,
                           RecycleStore store) {
        this.jdbc = jdbc;
        this.store = store;
        this.clock = Clock.systemDefaultZone();
    }

    @EventListener(ApplicationReadyEvent.class)
    void migrate() {
        Instant now = clock.instant();
        ZoneId zone = clock.getZone();
        for (LegacyTable table : LegacyTable.values()) {
            try {
                List<LegacyRow> rows = jdbc.query(table.selectSql(), (resultSet, rowNumber) ->
                        new LegacyRow(
                                resultSet.getString("id"),
                                resultSet.getString("name"),
                                resultSet.getString("org_id"),
                                resultSet.getString("parent_id"),
                                resultSet.getDouble("original_index"),
                                resultSet.getBoolean("is_folder"),
                                resultSet.getString("deleted_by")));
                rows.forEach(row -> migrate(table.type, row, now, zone));
            } catch (DataAccessException exception) {
                log.warn("迁移历史回收站数据失败，资源类型：{}", table.type, exception);
            }
        }
    }

    private void migrate(RecycleResourceType type,
                         LegacyRow row,
                         Instant now,
                         ZoneId zone) {
        LegacyArchiveName archive = parseArchivedName(row.name, now, zone);
        RecycleRootSnapshot snapshot = new RecycleRootSnapshot(
                row.id, archive.originalName(), row.parentId, row.index,
                row.folder, 1);
        try {
            store.saveRecord(new RecycleRecord(
                    UUID.randomUUID().toString(), row.organizationId, type, snapshot,
                    row.deletedBy == null ? "SYSTEM" : row.deletedBy,
                    archive.deletedAt(),
                    initialExpiry(archive.deletedAt(), now, DEFAULT_RETENTION_DAYS)));
        } catch (DuplicateKeyException ignored) {
            // 统一唯一键保证迁移可重复执行。
        }
    }

    static LegacyArchiveName parseArchivedName(String name, Instant migrationTime, ZoneId zone) {
        Matcher matcher = ARCHIVED_NAME.matcher(name);
        if (!matcher.matches()) {
            return new LegacyArchiveName(name, migrationTime, false);
        }
        try {
            Instant deletedAt = LocalDateTime.parse(matcher.group(2), ARCHIVED_AT)
                    .atZone(zone)
                    .toInstant();
            return new LegacyArchiveName(matcher.group(1), deletedAt, true);
        } catch (RuntimeException exception) {
            return new LegacyArchiveName(name, migrationTime, false);
        }
    }

    static Instant initialExpiry(Instant deletedAt, Instant migrationTime, int retentionDays) {
        Instant calculated = deletedAt.plus(Duration.ofDays(retentionDays));
        return calculated.isAfter(migrationTime)
                ? calculated
                : migrationTime.plus(Duration.ofDays(7));
    }

    private enum LegacyTable {
        SOURCE(RecycleResourceType.SOURCE, treeSql("source")),
        VIEW(RecycleResourceType.VIEW, treeSql("view")),
        SCHEDULE(RecycleResourceType.SCHEDULE, treeSql("schedule")),
        DATACHART(RecycleResourceType.DATACHART, leafSql("datachart")),
        DASHBOARD(RecycleResourceType.DASHBOARD, leafSql("dashboard")),
        STORYBOARD(RecycleResourceType.STORYBOARD, treeSql("storyboard"));

        private final RecycleResourceType type;
        private final String selectSql;

        LegacyTable(RecycleResourceType type, String selectSql) {
            this.type = type;
            this.selectSql = selectSql;
        }

        String selectSql() {
            return selectSql;
        }

        private static String treeSql(String table) {
            return "SELECT id, name, org_id, parent_id, COALESCE(`index`, 0) original_index, "
                    + "COALESCE(is_folder, FALSE) is_folder, COALESCE(update_by, create_by) deleted_by "
                    + "FROM `" + table + "` WHERE status = 0";
        }

        private static String leafSql(String table) {
            return "SELECT id, name, org_id, NULL parent_id, 0 original_index, "
                    + "FALSE is_folder, COALESCE(update_by, create_by) deleted_by "
                    + "FROM `" + table + "` WHERE status = 0";
        }
    }

    private record LegacyRow(String id,
                             String name,
                             String organizationId,
                             String parentId,
                             double index,
                             boolean folder,
                             String deletedBy) {
    }
}
