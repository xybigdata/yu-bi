package yubi.server.artifact;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactTasksTest {

    private static final Instant NOW = Instant.parse("2026-07-24T08:00:00Z");

    private ManualArtifactExecutor executor;
    private MutableClock clock;
    private ArtifactTasks tasks;

    @BeforeEach
    void setUp() {
        executor = new ManualArtifactExecutor();
        clock = new MutableClock(NOW);
        tasks = new DefaultArtifactTasks(
                new InMemoryArtifactTaskStore(),
                new InMemoryArtifactBlobStore(),
                executor,
                clock,
                Duration.ofMinutes(15),
                Duration.ofDays(7),
                3
        );
    }

    @Test
    void shouldSubmitInspectAndOpenCompletedArtifact() throws Exception {
        ArtifactAccess access = ArtifactAccess.authenticated("user-1", "alice");
        ArtifactDescriptor descriptor = new ArtifactDescriptor("季度报表", "text/plain", ".txt");

        TaskHandle handle = tasks.submit(access, descriptor,
                context -> context.output().write("已完成".getBytes(StandardCharsets.UTF_8)));

        assertEquals(ArtifactTaskState.QUEUED, handle.state());
        assertEquals(ArtifactTaskState.QUEUED,
                tasks.inspect(access, Set.of(handle.id())).tasks().getFirst().state());

        executor.runNext();

        TaskView completed = tasks.inspect(access, Set.of(handle.id())).tasks().getFirst();
        assertEquals(ArtifactTaskState.READY, completed.state());
        assertEquals(NOW, completed.completedAt());
        try (ArtifactContent content = tasks.open(access, handle.id())) {
            assertEquals("季度报表.txt", content.fileName());
            assertEquals("text/plain", content.mediaType());
            assertArrayEquals("已完成".getBytes(StandardCharsets.UTF_8), content.stream().readAllBytes());
        }
    }

    @Test
    void shouldStopExposingTerminalArtifactAfterRetentionWindow() {
        ArtifactAccess access = ArtifactAccess.authenticated("user-1", "alice");
        TaskHandle handle = tasks.submit(access,
                new ArtifactDescriptor("临时报表", "text/plain", ".txt"),
                context -> context.output().write(1));
        executor.runNext();

        clock.advance(Duration.ofDays(7));
        assertEquals(ArtifactTaskState.READY,
                tasks.inspect(access, Set.of(handle.id())).tasks().getFirst().state());

        clock.advance(Duration.ofNanos(1));
        TaskBatch expired = tasks.inspect(access, Set.of(handle.id()));
        assertEquals(0, expired.tasks().size());
        assertEquals(Set.of(handle.id()), expired.missingIds());
    }

    @Test
    void successfulDeliveryStartsAndRefreshesFifteenMinuteRetention() throws Exception {
        ArtifactAccess access = ArtifactAccess.authenticated("user-1", "org-1", "alice");
        TaskHandle handle = tasksWithRetention(Duration.ofDays(1), Duration.ofMinutes(15))
                .submit(access,
                        new ArtifactDescriptor("临时报表", "text/plain", ".txt"),
                        context -> context.output().write(1));

        executor.runNext();
        clock.advance(Duration.ofHours(12));
        tasks.confirmDelivery(access, handle.id());
        clock.advance(Duration.ofMinutes(14));
        tasks.confirmDelivery(access, handle.id());
        clock.advance(Duration.ofMinutes(15));

        assertEquals(ArtifactTaskState.READY,
                tasks.inspect(access, Set.of(handle.id())).tasks().getFirst().state());

        clock.advance(Duration.ofNanos(1));
        assertEquals(Set.of(handle.id()), tasks.inspect(access, Set.of(handle.id())).missingIds());
    }

    @Test
    void neverDeliveredArtifactRemainsAvailableForOneDay() {
        ArtifactAccess access = ArtifactAccess.authenticated("user-1", "org-1", "alice");
        ArtifactTasks retainedTasks = tasksWithRetention(Duration.ofDays(1), Duration.ofMinutes(15));
        TaskHandle handle = retainedTasks.submit(access,
                new ArtifactDescriptor("未下载报表", "text/plain", ".txt"),
                context -> context.output().write(1));
        executor.runNext();

        clock.advance(Duration.ofDays(1));
        assertEquals(ArtifactTaskState.READY,
                retainedTasks.inspect(access, Set.of(handle.id())).tasks().getFirst().state());
        clock.advance(Duration.ofNanos(1));
        assertEquals(Set.of(handle.id()), retainedTasks.inspect(access, Set.of(handle.id())).missingIds());
    }

    private ArtifactTasks tasksWithRetention(Duration undeliveredRetention,
                                             Duration deliveredRetention) {
        tasks = new DefaultArtifactTasks(
                new InMemoryArtifactTaskStore(),
                new InMemoryArtifactBlobStore(),
                executor,
                clock,
                Duration.ofMinutes(15),
                undeliveredRetention,
                deliveredRetention,
                3
        );
        return tasks;
    }

    @Test
    void shouldBindSharedArtifactToClientAndShareScopeWithoutPersistingExecutionUserAsOwner() {
        ArtifactAccess owner = ArtifactAccess.shared("client-1", "share-1", "visitor", "share-owner");
        TaskHandle handle = tasks.submit(owner,
                new ArtifactDescriptor("分享报表", "text/plain", ".txt"),
                context -> context.output().write(1));
        executor.runNext();

        ArtifactAccess sameOwnerWithAnotherExecutionUser =
                ArtifactAccess.shared("client-1", "share-1", "visitor", "refreshed-owner");
        assertEquals(ArtifactTaskState.READY,
                tasks.inspect(sameOwnerWithAnotherExecutionUser, Set.of(handle.id()))
                        .tasks().getFirst().state());

        ArtifactAccess wrongClient = ArtifactAccess.shared("client-2", "share-1", "visitor", "share-owner");
        assertEquals(Set.of(handle.id()), tasks.inspect(wrongClient, Set.of(handle.id())).missingIds());
        ArtifactAccess wrongShareScope = ArtifactAccess.shared("client-1", "share-2", "visitor", "share-owner");
        assertEquals(Set.of(handle.id()), tasks.inspect(wrongShareScope, Set.of(handle.id())).missingIds());
        ArtifactTaskException error = assertThrows(ArtifactTaskException.class,
                () -> tasks.open(wrongClient, handle.id()));
        assertEquals("ARTIFACT_NOT_FOUND", error.code());
        assertFalse(error.traceId().isBlank());
    }

    @Test
    void shouldKeepTimeoutTerminalWhenProducerReturnsLateResult() {
        ArtifactAccess access = ArtifactAccess.authenticated("user-1", "alice");
        AtomicReference<String> taskId = new AtomicReference<>();
        AtomicReference<ArtifactTaskState> stateObservedInsideProducer = new AtomicReference<>();
        TaskHandle handle = tasks.submit(access,
                new ArtifactDescriptor("慢报表", "text/plain", ".txt"),
                context -> {
                    clock.advance(Duration.ofMinutes(15).plusNanos(1));
                    stateObservedInsideProducer.set(tasks.inspect(access, Set.of(taskId.get()))
                            .tasks().getFirst().state());
                    context.output().write("迟到内容".getBytes(StandardCharsets.UTF_8));
                });
        taskId.set(handle.id());

        executor.runNext();

        TaskView timedOut = tasks.inspect(access, Set.of(handle.id())).tasks().getFirst();
        assertEquals(ArtifactTaskState.TIMED_OUT, stateObservedInsideProducer.get());
        assertEquals(ArtifactTaskState.TIMED_OUT, timedOut.state());
        assertEquals("ARTIFACT_TIMED_OUT", timedOut.failure().code());
        assertEquals(handle.traceId(), timedOut.failure().traceId());
        ArtifactTaskException openError = assertThrows(ArtifactTaskException.class,
                () -> tasks.open(access, handle.id()));
        assertEquals("ARTIFACT_NOT_READY", openError.code());
    }

    @Test
    void shouldInterruptTimedOutProducerAndReleaseExecutorThread() throws InterruptedException {
        InMemoryArtifactTaskStore taskStore = new InMemoryArtifactTaskStore();
        CountDownLatch interrupted = new CountDownLatch(1);
        ArtifactTasks shortTasks = new DefaultArtifactTasks(
                taskStore,
                new InMemoryArtifactBlobStore(),
                Runnable::run,
                Clock.systemUTC(),
                Duration.ofMillis(20),
                Duration.ofDays(7),
                3
        );
        ArtifactAccess access = ArtifactAccess.authenticated("user-1", "alice");

        TaskHandle handle = shortTasks.submit(
                access,
                new ArtifactDescriptor("阻塞报表", "text/plain", ".txt"),
                context -> {
                    try {
                        new CountDownLatch(1).await();
                    } catch (InterruptedException exception) {
                        interrupted.countDown();
                        throw exception;
                    }
                }
        );

        assertTrue(interrupted.await(2, TimeUnit.SECONDS));
        assertEquals(
                ArtifactTaskState.TIMED_OUT,
                shortTasks.inspect(access, Set.of(handle.id())).tasks().getFirst().state()
        );

        TaskHandle nextHandle = shortTasks.submit(
                access,
                new ArtifactDescriptor("后续报表", "text/plain", ".txt"),
                context -> context.output().write(1)
        );
        assertEquals(
                ArtifactTaskState.READY,
                shortTasks.inspect(access, Set.of(nextHandle.id())).tasks().getFirst().state()
        );
    }

    @Test
    void 超时producer尚未退出时不允许并发重试() throws Exception {
        ManualArtifactExecutor manualExecutor = new ManualArtifactExecutor();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        ArtifactTasks shortTasks = new DefaultArtifactTasks(
                new InMemoryArtifactTaskStore(),
                new InMemoryArtifactBlobStore(),
                manualExecutor,
                Clock.systemUTC(),
                Duration.ofMillis(20),
                Duration.ofDays(7),
                1
        );
        ArtifactAccess access = ArtifactAccess.authenticated("user-1", "alice");
        TaskHandle handle = shortTasks.submit(
                access,
                new ArtifactDescriptor("忽略中断的报表", "text/plain", ".txt"),
                context -> {
                    started.countDown();
                    while (release.getCount() > 0) {
                        try {
                            release.await();
                        } catch (InterruptedException ignored) {
                            // 模拟无法及时响应中断的外部渲染服务。
                        }
                    }
                    finished.countDown();
                }
        );
        manualExecutor.runNext();
        assertTrue(started.await(2, TimeUnit.SECONDS));

        ArtifactTaskException active = assertThrows(
                ArtifactTaskException.class,
                () -> shortTasks.retry(access, handle.id())
        );
        assertEquals("ARTIFACT_RETRY_NOT_ALLOWED", active.code());

        shortTasks.submit(
                access,
                new ArtifactDescriptor("后续报表", "text/plain", ".txt"),
                context -> context.output().write(1)
        );
        assertEquals(0, manualExecutor.pendingCount());

        release.countDown();
        assertTrue(finished.await(2, TimeUnit.SECONDS));
        long schedulingDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (manualExecutor.pendingCount() == 0 && System.nanoTime() < schedulingDeadline) {
            Thread.sleep(1);
        }
        assertEquals(1, manualExecutor.pendingCount());
        manualExecutor.runNext();
        TaskHandle retried = shortTasks.retry(access, handle.id());
        assertFalse(retried.id().equals(handle.id()));
    }

    @Test
    void shouldExposeSanitizedFailureWithStableCodeAndTraceId() {
        ArtifactAccess access = ArtifactAccess.authenticated("user-1", "alice");
        TaskHandle handle = tasks.submit(access,
                new ArtifactDescriptor("失败报表", "text/plain", ".txt"),
                context -> {
                    throw new IllegalStateException("敏感 SQL: select salary from secret_table");
                });

        executor.runNext();

        TaskView failed = tasks.inspect(access, Set.of(handle.id())).tasks().getFirst();
        assertEquals(ArtifactTaskState.FAILED, failed.state());
        assertEquals("ARTIFACT_GENERATION_FAILED", failed.failure().code());
        assertEquals(handle.traceId(), failed.failure().traceId());
        assertFalse(failed.failure().hint().contains("salary"));
        assertFalse(failed.failure().hint().contains("secret_table"));
    }

    @Test
    void 应保留PDF渲染服务不可用分类并隐藏底层异常() {
        ArtifactAccess access = ArtifactAccess.authenticated("user-1", "alice");
        TaskHandle handle = tasks.submit(access,
                new ArtifactDescriptor("失败 PDF", "application/pdf", ".pdf"),
                context -> {
                    throw new ArtifactGenerationException(
                            "ARTIFACT_PDF_RENDERER_UNAVAILABLE",
                            "PDF 渲染服务暂时不可用，请稍后重试",
                            new IllegalStateException("renderer.internal token=secret")
                    );
                });

        executor.runNext();

        ArtifactFailure failure = tasks.inspect(access, Set.of(handle.id()))
                .tasks().getFirst().failure();
        assertEquals("ARTIFACT_PDF_RENDERER_UNAVAILABLE", failure.code());
        assertEquals("PDF 渲染服务暂时不可用，请稍后重试", failure.hint());
        assertFalse(failure.hint().contains("renderer.internal"));
        assertFalse(failure.hint().contains("secret"));
    }

    @Test
    void 失败任务可在新的浏览器Session中通过任务ID重试() throws Exception {
        ArtifactAccess access = ArtifactAccess.authenticated("user-1", "alice");
        AtomicInteger attempts = new AtomicInteger();
        TaskHandle failedHandle = tasks.submit(access,
                new ArtifactDescriptor("跨 Session 报表", "text/plain", ".txt"),
                context -> {
                    if (attempts.incrementAndGet() == 1) {
                        throw new IllegalStateException("首次生成失败");
                    }
                    context.output().write("重试成功".getBytes(StandardCharsets.UTF_8));
                });
        executor.runNext();

        TaskHandle retryHandle = tasks.retry(access, failedHandle.id());
        executor.runNext();

        assertFalse(retryHandle.id().equals(failedHandle.id()));
        assertEquals(ArtifactTaskState.READY,
                tasks.inspect(access, Set.of(retryHandle.id())).tasks().getFirst().state());
        assertArrayEquals("重试成功".getBytes(StandardCharsets.UTF_8),
                tasks.open(access, retryHandle.id()).stream().readAllBytes());
    }

    @Test
    void 同一失败任务被两个Session并发重试时只创建一个替代任务() throws Exception {
        ArtifactAccess access = ArtifactAccess.authenticated("user-1", "alice");
        TaskHandle failedHandle = tasks.submit(access,
                new ArtifactDescriptor("并发重试报表", "text/plain", ".txt"),
                context -> {
                    throw new IllegalStateException("生成失败");
                });
        executor.runNext();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<TaskHandle> handles = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<ArtifactTaskException> errors = new ConcurrentLinkedQueue<>();
        Runnable retry = () -> {
            ready.countDown();
            try {
                start.await();
                handles.add(tasks.retry(access, failedHandle.id()));
            } catch (ArtifactTaskException exception) {
                errors.add(exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError(exception);
            }
        };
        Thread first = Thread.ofPlatform().start(retry);
        Thread second = Thread.ofPlatform().start(retry);
        assertTrue(ready.await(2, TimeUnit.SECONDS));

        start.countDown();
        first.join(2_000);
        second.join(2_000);

        assertEquals(1, handles.size());
        assertEquals(1, errors.size());
        assertEquals("ARTIFACT_RETRY_UNAVAILABLE", errors.peek().code());
    }

    @Test
    void shouldRejectTaskBeyondOwnerLimitWithoutBlockingAnotherOwner() throws Exception {
        ArtifactAccess firstOwner = ArtifactAccess.authenticated("user-1", "alice");
        ArtifactAccess secondOwner = ArtifactAccess.authenticated("user-2", "bob");
        CountDownLatch started = new CountDownLatch(4);
        CountDownLatch release = new CountDownLatch(1);
        ArtifactProducer blockingProducer = context -> {
            started.countDown();
            release.await();
            context.output().write(1);
        };
        List<TaskHandle> firstOwnerTasks = new ArrayList<>();
        for (int index = 0; index < 3; index++) {
            firstOwnerTasks.add(tasks.submit(firstOwner,
                    new ArtifactDescriptor("报表-" + index, "text/plain", ".txt"),
                    blockingProducer));
        }
        ArtifactTaskException limitError = assertThrows(ArtifactTaskException.class,
                () -> tasks.submit(firstOwner,
                        new ArtifactDescriptor("超限报表", "text/plain", ".txt"),
                        blockingProducer));
        assertEquals("ARTIFACT_CONCURRENCY_LIMIT", limitError.code());
        assertFalse(limitError.traceId().isBlank());
        TaskHandle secondOwnerTask = tasks.submit(secondOwner,
                new ArtifactDescriptor("另一用户报表", "text/plain", ".txt"),
                blockingProducer);

        List<Thread> workers = executor.startAll();
        assertTrue(started.await(2, TimeUnit.SECONDS));
        try {
            Set<String> firstOwnerIds = Set.of(
                    firstOwnerTasks.get(0).id(),
                    firstOwnerTasks.get(1).id(),
                    firstOwnerTasks.get(2).id()
            );
            List<TaskView> views = tasks.inspect(firstOwner, firstOwnerIds).tasks();
            assertEquals(3, views.stream().filter(view -> view.state() == ArtifactTaskState.RUNNING).count());
            assertEquals(ArtifactTaskState.RUNNING,
                    tasks.inspect(secondOwner, Set.of(secondOwnerTask.id())).tasks().getFirst().state());
        } finally {
            release.countDown();
            for (Thread worker : workers) {
                worker.join(2_000);
            }
        }

        assertEquals(3, tasks.inspect(firstOwner, Set.of(
                        firstOwnerTasks.get(0).id(),
                        firstOwnerTasks.get(1).id(),
                        firstOwnerTasks.get(2).id()
                )).tasks().stream()
                .filter(view -> view.state() == ArtifactTaskState.READY)
                .count());
    }

    private static final class ManualArtifactExecutor implements ArtifactExecutor {

        private final ArrayDeque<Runnable> pending = new ArrayDeque<>();

        @Override
        public synchronized void execute(Runnable task) {
            pending.addLast(task);
        }

        void runNext() {
            Runnable task;
            synchronized (this) {
                task = pending.removeFirst();
            }
            task.run();
        }

        synchronized List<Thread> startAll() {
            List<Thread> workers = new ArrayList<>();
            while (!pending.isEmpty()) {
                Thread worker = Thread.ofPlatform().start(pending.removeFirst());
                workers.add(worker);
            }
            return workers;
        }

        synchronized int pendingCount() {
            return pending.size();
        }
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
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

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
