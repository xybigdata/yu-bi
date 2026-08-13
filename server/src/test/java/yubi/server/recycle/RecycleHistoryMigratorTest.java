package yubi.server.recycle;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecycleHistoryMigratorTest {

    private static final Instant NOW = Instant.parse("2026-08-07T06:00:00Z");
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Test
    void shouldStrictlyParseTheLegacyArchiveSuffix() {
        LegacyArchiveName parsed = RecycleHistoryMigrator.parseArchivedName(
                "订单视图.2026-08-01 06:30:15.123", NOW, ZONE);

        assertTrue(parsed.timestampParsed());
        assertEquals("订单视图", parsed.originalName());
        assertEquals(Instant.parse("2026-07-31T22:30:15.123Z"), parsed.deletedAt());
    }

    @Test
    void shouldUseMigrationTimeWhenTheSuffixIsNotStrictlyValid() {
        LegacyArchiveName parsed = RecycleHistoryMigrator.parseArchivedName(
                "订单视图.2026-8-1 6:30:15.123", NOW, ZONE);

        assertFalse(parsed.timestampParsed());
        assertEquals("订单视图.2026-8-1 6:30:15.123", parsed.originalName());
        assertEquals(NOW, parsed.deletedAt());
    }

    @Test
    void shouldGiveAlreadyExpiredLegacyRowsASevenDayGracePeriod() {
        assertEquals(
                NOW.plus(Duration.ofDays(7)),
                RecycleHistoryMigrator.initialExpiry(
                        NOW.minus(Duration.ofDays(60)), NOW, 30));
        assertEquals(
                NOW.plus(Duration.ofDays(20)),
                RecycleHistoryMigrator.initialExpiry(
                        NOW.minus(Duration.ofDays(10)), NOW, 30));
    }
}
