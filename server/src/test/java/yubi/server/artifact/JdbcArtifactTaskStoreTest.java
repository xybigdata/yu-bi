package yubi.server.artifact;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import yubi.core.common.Application;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcArtifactTaskStoreTest {

    private static final Instant NOW = Instant.parse("2026-07-24T08:00:00Z");

    private DataSource dataSource;
    private JdbcTemplate jdbc;
    private ArtifactTaskStore store;

    @TempDir
    Path storageDirectory;

    @BeforeEach
    void setUp() {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:artifact-store-" + java.util.UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=5000;DB_CLOSE_DELAY=-1");
        dataSource = source;
        jdbc = new JdbcTemplate(dataSource);
        new ResourceDatabasePopulator(
                new ClassPathResource("db/migration/V2026.07.24__2.0.0.artifact-task.sql"),
                new ClassPathResource("db/migration/V2026.07.28__2.0.0.artifact-task-metadata.sql"))
                .execute(dataSource);
        store = new JdbcArtifactTaskStore(jdbc);
    }

    @Test
    void shouldPersistAndRestoreOnlyTheSafeTaskModel() {
        StoredArtifactTask expected = queued("task-1", "owner-1");

        assertTrue(store.insertIfOwnerBelowLimit(expected, 3));

        assertEquals(expected, store.find(expected.id()).orElseThrow());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM artifact_task", Integer.class));
    }

    @Test
    void shouldAtomicallyRejectTheFourthConcurrentTaskAcrossStoreInstances() throws Exception {
        CountDownLatch ready = new CountDownLatch(4);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future<Boolean>> attempts = new ArrayList<>();
        try {
            for (int index = 0; index < 4; index++) {
                int taskNumber = index;
                attempts.add(executor.submit(() -> {
                    ArtifactTaskStore concurrentStore = new JdbcArtifactTaskStore(new JdbcTemplate(dataSource));
                    ready.countDown();
                    start.await(5, TimeUnit.SECONDS);
                    return concurrentStore.insertIfOwnerBelowLimit(
                            queued("concurrent-" + taskNumber, "shared-owner"), 3);
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            assertEquals(3, attempts.stream().filter(this::completedSuccessfully).count());
            assertEquals(3, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM artifact_task WHERE owner_key = 'shared-owner'", Integer.class));
            assertEquals(1, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM artifact_task_owner_guard WHERE owner_key = 'shared-owner'", Integer.class));
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void shouldNotCountExpiredInFlightRowsAgainstANewSubmission() {
        String owner = "owner-with-orphans";
        for (int index = 0; index < 3; index++) {
            StoredArtifactTask expired = new StoredArtifactTask(
                    "expired-" + index,
                    owner,
                    new ArtifactDescriptor("遗留任务", "application/pdf", ".pdf"),
                    ArtifactTaskState.QUEUED,
                    NOW.minusSeconds(1_800),
                    NOW.minusSeconds(900),
                    null,
                    null,
                    null,
                    "expired-trace-" + index);
            assertTrue(store.insertIfOwnerBelowLimit(expired, 3));
        }

        assertTrue(store.insertIfOwnerBelowLimit(queued("fresh-task", owner), 3));
        assertEquals(4, jdbc.queryForObject(
                "SELECT COUNT(*) FROM artifact_task WHERE owner_key = ?", Integer.class, owner));
    }

    @Test
    void shouldPreventLateWorkerFromOverwritingATerminalState() {
        StoredArtifactTask queued = queued("task-timeout", "owner-timeout");
        store.insertIfOwnerBelowLimit(queued, 3);
        ArtifactFailure timeout = new ArtifactFailure(
                "ARTIFACT_TIMED_OUT", "产物生成超时，请重新发起", "failure-trace");

        assertTrue(store.updateIfState(queued.id(), Set.of(ArtifactTaskState.QUEUED),
                StoredArtifactTask::running));
        assertTrue(store.updateIfState(queued.id(), Set.of(ArtifactTaskState.RUNNING),
                task -> task.failed(ArtifactTaskState.TIMED_OUT, timeout, NOW.plusSeconds(900))));

        assertFalse(store.updateIfState(queued.id(), Set.of(ArtifactTaskState.RUNNING),
                task -> task.ready("late-blob", NOW.plusSeconds(901))));
        StoredArtifactTask stored = store.find(queued.id()).orElseThrow();
        assertEquals(ArtifactTaskState.TIMED_OUT, stored.state());
        assertEquals(timeout, stored.failure());
        assertEquals(NOW.plusSeconds(900), stored.completedAt());
        assertEquals(null, stored.blobKey());
    }

    @Test
    void shouldDeleteConditionallyAndRemoveTheLastOwnerGuardIdempotently() {
        StoredArtifactTask first = queued("delete-1", "owner-delete");
        StoredArtifactTask second = queued("delete-2", "owner-delete");
        store.insertIfOwnerBelowLimit(first, 3);
        store.insertIfOwnerBelowLimit(second, 3);
        ArtifactFailure failure = new ArtifactFailure(
                "ARTIFACT_GENERATION_FAILED", "产物生成失败", "failure-delete");
        store.updateIfState(first.id(), Set.of(ArtifactTaskState.QUEUED),
                task -> task.failed(ArtifactTaskState.FAILED, failure, NOW.plusSeconds(1)));
        store.updateIfState(second.id(), Set.of(ArtifactTaskState.QUEUED),
                task -> task.failed(ArtifactTaskState.FAILED, failure, NOW.plusSeconds(2)));

        assertTrue(store.deleteIfState(first.id(), Set.of(ArtifactTaskState.READY)).isEmpty());
        assertEquals(first.id(), store.deleteIfState(first.id(), Set.of(ArtifactTaskState.FAILED))
                .orElseThrow().id());
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM artifact_task_owner_guard WHERE owner_key = 'owner-delete'", Integer.class));

        assertEquals(second.id(), store.deleteIfState(second.id(), Set.of(ArtifactTaskState.FAILED))
                .orElseThrow().id());
        assertTrue(store.deleteIfState(second.id(), Set.of(ArtifactTaskState.FAILED)).isEmpty());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM artifact_task", Integer.class));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM artifact_task_owner_guard WHERE owner_key = 'owner-delete'", Integer.class));
    }

    @Test
    void shouldFindOnlyExpiredMaintenanceCandidatesInDeadlineOrderAndRespectTheLimit() {
        Instant completedBefore = NOW.minus(Duration.ofDays(7));
        StoredArtifactTask expiredTerminal = stored(
                "expired-terminal", ArtifactTaskState.READY,
                NOW.minus(Duration.ofDays(8)), completedBefore.minusSeconds(1));
        StoredArtifactTask expiredActive = stored(
                "expired-active", ArtifactTaskState.QUEUED, NOW, null);
        StoredArtifactTask futureActive = stored(
                "future-active", ArtifactTaskState.RUNNING, NOW.plusMillis(1), null);
        StoredArtifactTask terminalAtBoundary = stored(
                "terminal-at-boundary", ArtifactTaskState.FAILED,
                NOW.minus(Duration.ofDays(8)), completedBefore);
        for (StoredArtifactTask task : List.of(
                expiredActive, futureActive, terminalAtBoundary, expiredTerminal)) {
            assertTrue(store.insertIfOwnerBelowLimit(task, 3));
        }

        List<StoredArtifactTask> candidates = store.findMaintenanceCandidates(NOW, completedBefore, 10);

        assertEquals(List.of("expired-terminal", "expired-active"),
                candidates.stream().map(StoredArtifactTask::id).toList());
        assertEquals(List.of("expired-terminal"),
                store.findMaintenanceCandidates(NOW, completedBefore, 1).stream()
                        .map(StoredArtifactTask::id)
                        .toList());
    }

    @Test
    void shouldRejectInvalidMaintenanceCandidateQueryArguments() {
        assertThrows(NullPointerException.class,
                () -> store.findMaintenanceCandidates(null, NOW, 1));
        assertThrows(NullPointerException.class,
                () -> store.findMaintenanceCandidates(NOW, null, 1));
        assertThrows(IllegalArgumentException.class,
                () -> store.findMaintenanceCandidates(NOW, NOW, 0));
    }

    @Test
    void configurationShouldWireProductionAdaptersWithFifteenMinuteOneDayAndThreeTaskDefaults() {
        MutableClock clock = new MutableClock(NOW);
        ArrayDeque<Runnable> pending = new ArrayDeque<>();
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "artifact-test", Map.of("yubi.env.file-path", storageDirectory.toString())));
            context.registerBean(JdbcTemplate.class, () -> jdbc);
            context.registerBean("artifactTaskClock", Clock.class, () -> clock);
            context.registerBean(ArtifactExecutor.class, () -> pending::addLast);
            context.register(Application.class, ArtifactTaskConfiguration.class);
            context.refresh();

            assertInstanceOf(JdbcArtifactTaskStore.class, context.getBean(ArtifactTaskStore.class));
            assertInstanceOf(FileSystemArtifactBlobStore.class, context.getBean(ArtifactBlobStore.class));
            assertInstanceOf(ArtifactTaskWebMapper.class, context.getBean(ArtifactTaskWebMapper.class));
            ArtifactTasks tasks = context.getBean(ArtifactTasks.class);
            ArtifactAccess access = ArtifactAccess.authenticated("config-user", "config-user");
            ArtifactDescriptor descriptor = new ArtifactDescriptor(
                    "配置测试", "application/octet-stream", ".bin");

            TaskHandle first = tasks.submit(access, descriptor, work -> work.output().write(1));
            tasks.submit(access, descriptor, work -> work.output().write(2));
            tasks.submit(access, descriptor, work -> work.output().write(3));
            ArtifactTaskException limit = assertThrows(ArtifactTaskException.class,
                    () -> tasks.submit(access, descriptor, work -> work.output().write(4)));
            assertEquals("ARTIFACT_CONCURRENCY_LIMIT", limit.code());
            assertEquals(NOW.plus(Duration.ofMinutes(15)), first.deadlineAt());

            pending.removeFirst().run();
            try (ArtifactContent content = tasks.open(access, first.id())) {
                assertEquals(1, content.stream().read());
                assertEquals(-1, content.stream().read());
            } catch (Exception exception) {
                throw new AssertionError("生产存储 Adapter 未能读回产物", exception);
            }
            clock.advance(Duration.ofDays(1).plusMillis(1));
            assertEquals(Set.of(first.id()), tasks.inspect(access, Set.of(first.id())).missingIds());
        }
    }

    @Test
    void configurationShouldHonorTimeoutRetentionAndConcurrencyOverrides() {
        MutableClock clock = new MutableClock(NOW);
        ArrayDeque<Runnable> pending = new ArrayDeque<>();
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "artifact-overrides", Map.of(
                    "yubi.env.file-path", storageDirectory.toString(),
                    "yubi.artifact.timeout-minutes", "2",
                    "yubi.artifact.undelivered-retention-hours", "12",
                    "yubi.artifact.delivered-retention-minutes", "5",
                    "yubi.artifact.max-concurrent-per-owner", "1")));
            context.registerBean(JdbcTemplate.class, () -> jdbc);
            context.registerBean("artifactTaskClock", Clock.class, () -> clock);
            context.registerBean(ArtifactExecutor.class, () -> pending::addLast);
            context.register(Application.class, ArtifactTaskConfiguration.class);
            context.refresh();

            ArtifactTasks tasks = context.getBean(ArtifactTasks.class);
            ArtifactAccess access = ArtifactAccess.authenticated("override-user", "override-user");
            ArtifactDescriptor descriptor = new ArtifactDescriptor(
                    "覆盖测试", "application/octet-stream", ".bin");
            TaskHandle task = tasks.submit(access, descriptor, work -> work.output().write(1));

            assertEquals(NOW.plus(Duration.ofMinutes(2)), task.deadlineAt());
            assertThrows(ArtifactTaskException.class,
                    () -> tasks.submit(access, descriptor, work -> work.output().write(2)));
            pending.removeFirst().run();
            clock.advance(Duration.ofHours(12).plusMillis(1));
            assertEquals(Set.of(task.id()), tasks.inspect(access, Set.of(task.id())).missingIds());
        }
    }

    private boolean completedSuccessfully(Future<Boolean> attempt) {
        try {
            return attempt.get(10, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new AssertionError("并发提交未正常结束", exception);
        }
    }

    private StoredArtifactTask queued(String id, String ownerKey) {
        return new StoredArtifactTask(id, ownerKey,
                new ArtifactDescriptor("经营报告", "application/pdf", ".pdf"),
                ArtifactTaskState.QUEUED, NOW, NOW.plusSeconds(900),
                null, null, null, "trace-1");
    }

    private StoredArtifactTask stored(String id,
                                      ArtifactTaskState state,
                                      Instant deadlineAt,
                                      Instant completedAt) {
        ArtifactFailure failure = state == ArtifactTaskState.FAILED
                ? new ArtifactFailure("ARTIFACT_GENERATION_FAILED", "产物生成失败", "failure-" + id)
                : null;
        return new StoredArtifactTask(
                id,
                "owner-" + id,
                new ArtifactDescriptor("维护任务", "application/pdf", ".pdf"),
                state,
                deadlineAt.minus(Duration.ofMinutes(15)),
                deadlineAt,
                completedAt,
                completedAt,
                state == ArtifactTaskState.READY ? "blob-" + id : null,
                failure,
                "trace-" + id);
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
