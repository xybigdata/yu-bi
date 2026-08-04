package yubi.server.artifact;

import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public final class DefaultArtifactTasks implements ArtifactTasks {

    private static final String GENERATION_FAILED = "ARTIFACT_GENERATION_FAILED";
    private static final String TIMED_OUT = "ARTIFACT_TIMED_OUT";
    private static final String NOT_FOUND = "ARTIFACT_NOT_FOUND";
    private static final String NOT_READY = "ARTIFACT_NOT_READY";
    private static final String EXECUTOR_REJECTED = "ARTIFACT_EXECUTOR_REJECTED";
    private static final String CONCURRENCY_LIMIT = "ARTIFACT_CONCURRENCY_LIMIT";
    private static final String ACTIVE_TASK = "ARTIFACT_TASK_ACTIVE";
    private static final String DELETE_FAILED = "ARTIFACT_DELETE_FAILED";
    private static final String RETRY_NOT_ALLOWED = "ARTIFACT_RETRY_NOT_ALLOWED";
    private static final String RETRY_UNAVAILABLE = "ARTIFACT_RETRY_UNAVAILABLE";
    private static final Set<ArtifactTaskState> TERMINAL_STATES = Set.of(
            ArtifactTaskState.READY,
            ArtifactTaskState.FAILED,
            ArtifactTaskState.TIMED_OUT
    );

    private final ArtifactTaskStore taskStore;
    private final ArtifactBlobStore blobStore;
    private final ArtifactExecutor executor;
    private final Clock clock;
    private final Duration timeout;
    private final Duration undeliveredRetention;
    private final Duration deliveredRetention;
    private final int maxConcurrentPerOwner;
    private final Object schedulerLock = new Object();
    private final Map<String, ArrayDeque<PendingExecution>> pendingByOwner = new HashMap<>();
    private final Map<String, Integer> activeByOwner = new HashMap<>();
    private final ArtifactRetryRegistry retryRegistry;
    private final ConcurrentMap<String, Thread> activeProducerThreads = new ConcurrentHashMap<>();

    public DefaultArtifactTasks(ArtifactTaskStore taskStore,
                                ArtifactBlobStore blobStore,
                                ArtifactExecutor executor,
                                Clock clock,
                                Duration timeout,
                                Duration retention,
                                int maxConcurrentPerOwner) {
        this(taskStore, blobStore, executor, clock, timeout, retention, retention,
                maxConcurrentPerOwner, new ArtifactRetryRegistry());
    }

    public DefaultArtifactTasks(ArtifactTaskStore taskStore,
                                ArtifactBlobStore blobStore,
                                ArtifactExecutor executor,
                                Clock clock,
                                Duration timeout,
                                Duration undeliveredRetention,
                                Duration deliveredRetention,
                                int maxConcurrentPerOwner) {
        this(taskStore, blobStore, executor, clock, timeout, undeliveredRetention,
                deliveredRetention, maxConcurrentPerOwner, new ArtifactRetryRegistry());
    }

    DefaultArtifactTasks(ArtifactTaskStore taskStore,
                         ArtifactBlobStore blobStore,
                         ArtifactExecutor executor,
                         Clock clock,
                         Duration timeout,
                         Duration undeliveredRetention,
                         Duration deliveredRetention,
                         int maxConcurrentPerOwner,
                         ArtifactRetryRegistry retryRegistry) {
        this.taskStore = Objects.requireNonNull(taskStore, "任务存储不能为空");
        this.blobStore = Objects.requireNonNull(blobStore, "产物存储不能为空");
        this.executor = Objects.requireNonNull(executor, "执行器不能为空");
        this.clock = Objects.requireNonNull(clock, "时钟不能为空");
        this.timeout = requirePositive(timeout, "任务超时必须为正数");
        this.undeliveredRetention = requirePositive(
                undeliveredRetention, "未交付产物保留时间必须为正数");
        this.deliveredRetention = requirePositive(
                deliveredRetention, "已交付产物保留时间必须为正数");
        this.retryRegistry = Objects.requireNonNull(retryRegistry, "重试注册表不能为空");
        if (maxConcurrentPerOwner < 1) {
            throw new IllegalArgumentException("每个 owner 的并发数必须为正数");
        }
        this.maxConcurrentPerOwner = maxConcurrentPerOwner;
    }

    @Override
    public TaskHandle submit(ArtifactAccess access,
                             ArtifactDescriptor descriptor,
                             ArtifactProducer producer) {
        Objects.requireNonNull(access, "访问身份不能为空");
        Objects.requireNonNull(descriptor, "产物描述不能为空");
        Objects.requireNonNull(producer, "产物 producer 不能为空");
        Instant acceptedAt = clock.instant();
        StoredArtifactTask stored = new StoredArtifactTask(
                UUID.randomUUID().toString(),
                access.ownerKey(),
                descriptor,
                ArtifactTaskState.QUEUED,
                acceptedAt,
                acceptedAt.plus(timeout),
                null,
                null,
                null,
                UUID.randomUUID().toString()
        );
        if (!taskStore.insertIfOwnerBelowLimit(stored, maxConcurrentPerOwner)) {
            throw problem(CONCURRENCY_LIMIT, "当前生成任务已达上限，请稍后重试", stored.traceId());
        }
        retryRegistry.register(stored.id(), producer);
        enqueue(new PendingExecution(stored.id(), stored.ownerKey(), access.executionUser(), producer));
        return handle(stored);
    }

    @Override
    public TaskBatch inspect(ArtifactAccess access, Set<String> ids) {
        Objects.requireNonNull(access, "访问身份不能为空");
        Objects.requireNonNull(ids, "任务 ID 集合不能为空");
        List<TaskView> views = new ArrayList<>();
        Set<String> missing = new LinkedHashSet<>();
        for (String id : ids) {
            StoredArtifactTask stored = findCurrent(id);
            if (stored == null || !stored.ownerKey().equals(access.ownerKey())) {
                missing.add(id);
            } else {
                views.add(view(stored));
            }
        }
        return new TaskBatch(views, missing);
    }

    @Override
    public TaskPage list(ArtifactAccess access, int terminalOffset, int terminalLimit) {
        Objects.requireNonNull(access, "访问身份不能为空");
        if (terminalOffset < 0 || terminalLimit < 1 || terminalLimit > 100) {
            throw new IllegalArgumentException("分页参数无效");
        }
        List<StoredArtifactTask> active = taskStore.findActiveByOwner(access.ownerKey());
        active.forEach(this::expireTimedOut);
        active = taskStore.findActiveByOwner(access.ownerKey());
        List<StoredArtifactTask> terminal = taskStore.findTerminalByOwner(
                access.ownerKey(), terminalOffset, terminalLimit + 1);
        boolean hasMore = terminal.size() > terminalLimit;
        List<TaskView> views = new ArrayList<>(active.size() + Math.min(terminal.size(), terminalLimit));
        active.stream().map(this::view).forEach(views::add);
        terminal.stream().limit(terminalLimit).map(this::view).forEach(views::add);
        return new TaskPage(views, hasMore ? terminalOffset + terminalLimit : null);
    }

    @Override
    public TaskHandle retry(ArtifactAccess access, String id) {
        Objects.requireNonNull(access, "访问身份不能为空");
        StoredArtifactTask stored = findCurrent(id);
        if (stored == null || !stored.ownerKey().equals(access.ownerKey())) {
            throw problem(NOT_FOUND, "产物任务不存在或已过期", UUID.randomUUID().toString());
        }
        if (stored.state() != ArtifactTaskState.FAILED
                && stored.state() != ArtifactTaskState.TIMED_OUT) {
            throw problem(RETRY_NOT_ALLOWED, "仅失败或超时的任务可以重试", stored.traceId());
        }
        Thread activeProducer = activeProducerThreads.get(id);
        if (activeProducer != null && activeProducer.isAlive()) {
            throw problem(RETRY_NOT_ALLOWED, "原任务仍在终止中，请稍后重试", stored.traceId());
        }
        ArtifactProducer producer = retryRegistry.claim(id);
        if (producer == null) {
            throw problem(RETRY_UNAVAILABLE, "原任务重试信息已失效，请重新发起导出", stored.traceId());
        }
        try {
            TaskHandle retried = submit(access, stored.descriptor(), producer);
            retryRegistry.completeClaim(id, producer);
            return retried;
        } catch (RuntimeException exception) {
            retryRegistry.restore(id, producer);
            throw exception;
        }
    }

    @Override
    public ArtifactContent open(ArtifactAccess access, String id) {
        Objects.requireNonNull(access, "访问身份不能为空");
        StoredArtifactTask stored = findCurrent(id);
        if (stored == null || !stored.ownerKey().equals(access.ownerKey())) {
            throw problem(NOT_FOUND, "产物任务不存在或已过期", UUID.randomUUID().toString());
        }
        if (stored.state() != ArtifactTaskState.READY) {
            throw problem(NOT_READY, "产物任务尚未完成", stored.traceId());
        }
        StoredBlob blob = blobStore.open(stored.blobKey());
        return new ArtifactContent(stored.descriptor().fileName(), stored.descriptor().mediaType(),
                blob.length(), blob.stream());
    }

    @Override
    public void confirmDelivery(ArtifactAccess access, String id) {
        Objects.requireNonNull(access, "访问身份不能为空");
        StoredArtifactTask stored = findCurrent(id);
        if (stored == null || !stored.ownerKey().equals(access.ownerKey())) {
            throw problem(NOT_FOUND, "产物任务不存在或已过期", UUID.randomUUID().toString());
        }
        if (stored.state() != ArtifactTaskState.READY) {
            throw problem(NOT_READY, "产物任务尚未完成", stored.traceId());
        }
        taskStore.updateIfState(id, Set.of(ArtifactTaskState.READY),
                task -> task.delivered(clock.instant().plus(deliveredRetention)));
    }

    @Override
    public void delete(ArtifactAccess access, String id) {
        Objects.requireNonNull(access, "访问身份不能为空");
        StoredArtifactTask stored = findCurrent(id);
        if (stored == null || !stored.ownerKey().equals(access.ownerKey())) {
            return;
        }
        if (!stored.state().isTerminal()) {
            throw problem(ACTIVE_TASK, "正在生成的任务不能清除", stored.traceId());
        }
        try {
            if (stored.blobKey() != null) {
                blobStore.delete(stored.blobKey());
            }
            taskStore.deleteIfState(id, TERMINAL_STATES)
                    .ifPresent(ignored -> retryRegistry.remove(id));
        } catch (RuntimeException exception) {
            log.error("清除产物任务失败，taskId={}，traceId={}", id, stored.traceId(), exception);
            throw problem(DELETE_FAILED, "清除失败，请稍后重试", stored.traceId());
        }
    }

    private boolean run(String taskId,
                        String ownerKey,
                        String executionUser,
                        ArtifactProducer producer) {
        expireTimedOut(taskStore.find(taskId).orElse(null));
        if (!taskStore.updateIfState(taskId, Set.of(ArtifactTaskState.QUEUED), StoredArtifactTask::running)) {
            return true;
        }
        StoredArtifactTask running = taskStore.find(taskId).orElseThrow();
        try (ArtifactBlobWriter writer = blobStore.begin(taskId)) {
            ProductionOutcome outcome = produceUntilDeadline(
                    taskId, ownerKey, running, executionUser, producer, writer);
            if (!outcome.completed()) {
                return outcome.releaseOwnerNow();
            }
            expireTimedOut(taskStore.find(taskId).orElse(null));
            if (taskStore.find(taskId).map(StoredArtifactTask::state).orElse(null)
                    != ArtifactTaskState.RUNNING) {
                writer.abort();
                return true;
            }
            String blobKey = writer.commit();
            boolean completed = taskStore.updateIfState(taskId, Set.of(ArtifactTaskState.RUNNING), task -> {
                Instant completedAt = clock.instant();
                return task.ready(blobKey, completedAt, completedAt.plus(undeliveredRetention));
            });
            if (!completed) {
                blobStore.delete(blobKey);
            } else {
                retryRegistry.remove(taskId);
            }
        } catch (Exception exception) {
            log.error("产物任务生成失败，taskId={}，traceId={}", taskId, running.traceId(), exception);
            ArtifactFailure failure = generationFailure(exception, running.traceId());
            taskStore.updateIfState(taskId, Set.of(ArtifactTaskState.RUNNING), task -> {
                Instant completedAt = clock.instant();
                return task.failed(ArtifactTaskState.FAILED, failure, completedAt,
                        completedAt.plus(undeliveredRetention));
            });
        }
        return true;
    }

    private ProductionOutcome produceUntilDeadline(String taskId,
                                                    String ownerKey,
                                                    StoredArtifactTask running,
                                                    String executionUser,
                                                    ArtifactProducer producer,
                                                    ArtifactBlobWriter writer) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread producerThread = Thread.ofVirtual()
                .name("yubi-artifact-producer-" + taskId)
                .unstarted(() -> {
                    try {
                        producer.produce(new ArtifactWorkContext(
                                writer.output(),
                                running.deadlineAt(),
                                executionUser
                        ));
                    } catch (Throwable throwable) {
                        failure.set(throwable);
                    } finally {
                        activeProducerThreads.remove(taskId, Thread.currentThread());
                    }
                });
        activeProducerThreads.put(taskId, producerThread);
        try {
            producerThread.start();
        } catch (RuntimeException | Error failureToStart) {
            activeProducerThreads.remove(taskId, producerThread);
            throw failureToStart;
        }
        Duration remaining = Duration.between(clock.instant(), running.deadlineAt());
        boolean completed = !remaining.isNegative()
                && !remaining.isZero()
                && producerThread.join(remaining);
        if (!completed) {
            producerThread.interrupt();
            writer.abort();
            markTimedOut(taskId, clock.instant());
            boolean releaseOwnerNow = !producerThread.isAlive()
                    || !releaseOwnerWhenProducerTerminates(taskId, ownerKey, producerThread);
            return new ProductionOutcome(false, releaseOwnerNow);
        }
        Throwable throwable = failure.get();
        if (throwable instanceof Exception exception) {
            throw exception;
        }
        if (throwable instanceof Error error) {
            throw error;
        }
        return new ProductionOutcome(true, true);
    }

    private boolean releaseOwnerWhenProducerTerminates(String taskId,
                                                        String ownerKey,
                                                        Thread producerThread) {
        try {
            Thread.ofVirtual()
                    .name("yubi-artifact-release-" + taskId)
                    .start(() -> {
                        boolean interrupted = false;
                        while (producerThread.isAlive()) {
                            try {
                                producerThread.join();
                            } catch (InterruptedException exception) {
                                interrupted = true;
                            }
                        }
                        release(ownerKey);
                        if (interrupted) {
                            Thread.currentThread().interrupt();
                        }
                    });
            return true;
        } catch (RuntimeException | Error exception) {
            log.error("等待超时 producer 退出失败，taskId={}", taskId, exception);
            return false;
        }
    }

    private void enqueue(PendingExecution pending) {
        synchronized (schedulerLock) {
            pendingByOwner.computeIfAbsent(pending.ownerKey(), ignored -> new ArrayDeque<>())
                    .addLast(pending);
        }
        dispatch(pending.ownerKey());
    }

    private void dispatch(String ownerKey) {
        List<PendingExecution> ready = new ArrayList<>();
        synchronized (schedulerLock) {
            ArrayDeque<PendingExecution> queue = pendingByOwner.get(ownerKey);
            int active = activeByOwner.getOrDefault(ownerKey, 0);
            while (queue != null && !queue.isEmpty() && active < maxConcurrentPerOwner) {
                ready.add(queue.removeFirst());
                active++;
            }
            if (queue != null && queue.isEmpty()) {
                pendingByOwner.remove(ownerKey);
            }
            if (active > 0) {
                activeByOwner.put(ownerKey, active);
            }
        }
        for (PendingExecution pending : ready) {
            try {
                executor.execute(() -> runAndRelease(pending));
            } catch (RuntimeException exception) {
                failRejected(pending.taskId());
                release(ownerKey);
            }
        }
    }

    private void runAndRelease(PendingExecution pending) {
        boolean releaseOwnerNow = true;
        try {
            releaseOwnerNow = run(
                    pending.taskId(), pending.ownerKey(), pending.executionUser(), pending.producer());
        } finally {
            if (releaseOwnerNow) {
                release(pending.ownerKey());
            }
        }
    }

    private void release(String ownerKey) {
        synchronized (schedulerLock) {
            int remaining = activeByOwner.getOrDefault(ownerKey, 1) - 1;
            if (remaining <= 0) {
                activeByOwner.remove(ownerKey);
            } else {
                activeByOwner.put(ownerKey, remaining);
            }
        }
        dispatch(ownerKey);
    }

    private void failRejected(String taskId) {
        taskStore.find(taskId).ifPresent(task ->
                log.warn("产物任务执行器拒绝任务，taskId={}，traceId={}", task.id(), task.traceId()));
        taskStore.updateIfState(taskId, Set.of(ArtifactTaskState.QUEUED), task -> {
            Instant completedAt = clock.instant();
            return task.failed(
                    ArtifactTaskState.FAILED,
                    new ArtifactFailure(EXECUTOR_REJECTED,
                            "产物任务暂时无法执行，请稍后重试",
                            task.traceId()),
                    completedAt,
                    completedAt.plus(undeliveredRetention)
            );
        });
    }

    private TaskHandle handle(StoredArtifactTask task) {
        return new TaskHandle(task.id(), task.descriptor(), task.state(), task.acceptedAt(),
                task.deadlineAt(), task.traceId());
    }

    private TaskView view(StoredArtifactTask task) {
        return new TaskView(task.id(), task.descriptor(), task.state(), task.acceptedAt(),
                task.deadlineAt(), task.completedAt(), task.failure(), task.traceId());
    }

    private StoredArtifactTask findCurrent(String id) {
        expireTimedOut(taskStore.find(id).orElse(null));
        return findRetained(id);
    }

    private void expireTimedOut(StoredArtifactTask stored) {
        if (stored == null || stored.state().isTerminal() || clock.instant().isBefore(stored.deadlineAt())) {
            return;
        }
        markTimedOut(stored.id(), clock.instant());
    }

    private void markTimedOut(String taskId, Instant completedAt) {
        taskStore.updateIfState(taskId, Set.of(ArtifactTaskState.QUEUED, ArtifactTaskState.RUNNING), task -> {
            ArtifactFailure failure = new ArtifactFailure(
                    TIMED_OUT,
                    "产物生成超时，请重新发起",
                    task.traceId()
            );
            return task.failed(ArtifactTaskState.TIMED_OUT, failure, completedAt,
                    completedAt.plus(undeliveredRetention));
        });
    }

    private StoredArtifactTask findRetained(String id) {
        StoredArtifactTask stored = taskStore.find(id).orElse(null);
        if (stored == null || !stored.state().isTerminal() || stored.completedAt() == null) {
            return stored;
        }
        Instant expiresAt = stored.expiresAt() == null
                ? stored.completedAt().plus(undeliveredRetention)
                : stored.expiresAt();
        if (!clock.instant().isAfter(expiresAt)) {
            return stored;
        }
        if (stored.blobKey() != null) {
            blobStore.delete(stored.blobKey());
        }
        taskStore.deleteIfState(id, TERMINAL_STATES)
                .ifPresent(ignored -> retryRegistry.remove(id));
        return taskStore.find(id).orElse(null);
    }

    private static Duration requirePositive(Duration value, String message) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static ArtifactTaskException problem(String code, String hint, String traceId) {
        return new ArtifactTaskException(code, hint, traceId);
    }

    private static ArtifactFailure generationFailure(Exception exception, String traceId) {
        if (exception instanceof ArtifactGenerationException classified) {
            return new ArtifactFailure(classified.code(), classified.hint(), traceId);
        }
        return new ArtifactFailure(
                GENERATION_FAILED,
                "产物生成失败，请凭追踪 ID 联系管理员",
                traceId
        );
    }

    private record PendingExecution(String taskId,
                                    String ownerKey,
                                    String executionUser,
                                    ArtifactProducer producer) {
    }

    private record ProductionOutcome(boolean completed, boolean releaseOwnerNow) {
    }
}
