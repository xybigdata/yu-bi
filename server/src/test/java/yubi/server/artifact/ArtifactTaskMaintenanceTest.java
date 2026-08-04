package yubi.server.artifact;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactTaskMaintenanceTest {

    private static final Instant NOW = Instant.parse("2026-07-24T08:00:00Z");

    @Test
    void 无需客户端再次查询也会删除超过保留期的任务和文件() {
        MutableClock clock = new MutableClock(NOW);
        InMemoryArtifactTaskStore taskStore = new InMemoryArtifactTaskStore();
        InMemoryArtifactBlobStore blobStore = new InMemoryArtifactBlobStore();
        ArtifactTasks tasks = new DefaultArtifactTasks(
                taskStore,
                blobStore,
                Runnable::run,
                clock,
                Duration.ofMinutes(15),
                Duration.ofDays(7),
                3
        );
        ArtifactAccess access = ArtifactAccess.authenticated("user-1", "alice");
        TaskHandle handle = tasks.submit(
                access,
                new ArtifactDescriptor("临时报表", "text/plain", ".txt"),
                context -> context.output().write(1)
        );
        String blobKey = taskStore.find(handle.id()).orElseThrow().blobKey();

        clock.advance(Duration.ofDays(7).plusNanos(1));
        new ArtifactTaskMaintenance(
                taskStore,
                blobStore,
                clock,
                Duration.ofDays(7),
                100
        ).run();

        assertTrue(taskStore.find(handle.id()).isEmpty());
        assertThrows(IllegalStateException.class, () -> blobStore.open(blobKey));
    }

    @Test
    void 后台维护会将宕机遗留的活动任务标记为超时且迟到执行不能覆盖() {
        MutableClock clock = new MutableClock(NOW);
        InMemoryArtifactTaskStore taskStore = new InMemoryArtifactTaskStore();
        InMemoryArtifactBlobStore blobStore = new InMemoryArtifactBlobStore();
        ManualExecutor executor = new ManualExecutor();
        ArtifactTasks tasks = new DefaultArtifactTasks(
                taskStore,
                blobStore,
                executor,
                clock,
                Duration.ofMinutes(15),
                Duration.ofDays(7),
                3
        );
        ArtifactAccess access = ArtifactAccess.authenticated("user-1", "alice");
        TaskHandle handle = tasks.submit(
                access,
                new ArtifactDescriptor("慢报表", "text/plain", ".txt"),
                context -> context.output().write(1)
        );

        clock.advance(Duration.ofMinutes(15).plusNanos(1));
        ArtifactTaskMaintenance maintenance = new ArtifactTaskMaintenance(
                taskStore,
                blobStore,
                clock,
                Duration.ofDays(7),
                100
        );
        maintenance.run();

        StoredArtifactTask timedOut = taskStore.find(handle.id()).orElseThrow();
        assertEquals(ArtifactTaskState.TIMED_OUT, timedOut.state());
        assertEquals("ARTIFACT_TIMED_OUT", timedOut.failure().code());

        executor.runNext();

        assertEquals(ArtifactTaskState.TIMED_OUT,
                tasks.inspect(access, Set.of(handle.id())).tasks().getFirst().state());
    }

    @Test
    void 文件删除失败时保留任务元数据以便下次维护重试() {
        MutableClock clock = new MutableClock(NOW);
        InMemoryArtifactTaskStore taskStore = new InMemoryArtifactTaskStore();
        InMemoryArtifactBlobStore delegate = new InMemoryArtifactBlobStore();
        ArtifactTasks tasks = new DefaultArtifactTasks(
                taskStore,
                delegate,
                Runnable::run,
                clock,
                Duration.ofMinutes(15),
                Duration.ofDays(7),
                3
        );
        TaskHandle handle = tasks.submit(
                ArtifactAccess.authenticated("user-1", "alice"),
                new ArtifactDescriptor("临时报表", "text/plain", ".txt"),
                context -> context.output().write(1)
        );
        clock.advance(Duration.ofDays(7).plusNanos(1));
        ArtifactBlobStore failingStore = new ArtifactBlobStore() {
            @Override
            public ArtifactBlobWriter begin(String taskId) {
                return delegate.begin(taskId);
            }

            @Override
            public StoredBlob open(String blobKey) {
                return delegate.open(blobKey);
            }

            @Override
            public void delete(String blobKey) {
                throw new IllegalStateException("模拟文件系统删除失败");
            }
        };

        ArtifactTaskMaintenance maintenance = new ArtifactTaskMaintenance(
                taskStore,
                failingStore,
                clock,
                Duration.ofDays(7),
                100
        );

        assertThrows(IllegalStateException.class, maintenance::run);
        assertTrue(taskStore.find(handle.id()).isPresent());
    }

    @Test
    void 后台维护删除过期失败任务时释放重试信息() {
        MutableClock clock = new MutableClock(NOW);
        InMemoryArtifactTaskStore taskStore = new InMemoryArtifactTaskStore();
        InMemoryArtifactBlobStore blobStore = new InMemoryArtifactBlobStore();
        ArtifactRetryRegistry retryRegistry = new ArtifactRetryRegistry();
        ArtifactTasks tasks = new DefaultArtifactTasks(
                taskStore,
                blobStore,
                Runnable::run,
                clock,
                Duration.ofMinutes(15),
                Duration.ofDays(7),
                Duration.ofMinutes(15),
                3,
                retryRegistry
        );
        TaskHandle handle = tasks.submit(
                ArtifactAccess.authenticated("user-1", "alice"),
                new ArtifactDescriptor("失败报表", "text/plain", ".txt"),
                context -> {
                    throw new IllegalStateException("生成失败");
                }
        );
        clock.advance(Duration.ofDays(7).plusNanos(1));

        new ArtifactTaskMaintenance(
                taskStore,
                blobStore,
                clock,
                Duration.ofDays(7),
                100,
                retryRegistry
        ).run();

        assertTrue(taskStore.find(handle.id()).isEmpty());
        assertNull(retryRegistry.claim(handle.id()));
    }

    private static final class ManualExecutor implements ArtifactExecutor {
        private final ArrayDeque<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable task) {
            tasks.addLast(task);
        }

        void runNext() {
            tasks.removeFirst().run();
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
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
