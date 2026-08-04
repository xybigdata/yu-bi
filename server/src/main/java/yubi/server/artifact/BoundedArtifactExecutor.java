package yubi.server.artifact;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

final class BoundedArtifactExecutor implements ArtifactExecutor, AutoCloseable {

    private static final Duration SHUTDOWN_WAIT = Duration.ofSeconds(5);

    private final ThreadPoolExecutor executor;

    BoundedArtifactExecutor(int threadCount, int queueCapacity) {
        if (threadCount < 1) {
            throw new IllegalArgumentException("产物执行线程数必须为正数");
        }
        if (queueCapacity < 1) {
            throw new IllegalArgumentException("产物执行队列容量必须为正数");
        }
        AtomicInteger sequence = new AtomicInteger();
        ThreadFactory threadFactory = task -> {
            Thread thread = new Thread(task, "yubi-artifact-" + sequence.incrementAndGet());
            thread.setDaemon(false);
            return thread;
        };
        executor = new ThreadPoolExecutor(
                threadCount,
                threadCount,
                30,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy());
        executor.allowCoreThreadTimeOut(true);
    }

    @Override
    public void execute(Runnable task) {
        executor.execute(task);
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_WAIT.toMillis(), TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
