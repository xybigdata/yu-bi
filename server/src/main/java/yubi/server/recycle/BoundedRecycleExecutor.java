package yubi.server.recycle;

import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import yubi.security.manager.PermissionDataCache;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

final class BoundedRecycleExecutor implements Executor, AutoCloseable {

    private static final Duration SHUTDOWN_WAIT = Duration.ofSeconds(5);

    private final ThreadPoolExecutor executor;
    private final PermissionDataCache permissionDataCache;

    BoundedRecycleExecutor(int threadCount,
                           int queueCapacity,
                           PermissionDataCache permissionDataCache) {
        if (threadCount < 1 || queueCapacity < 1) {
            throw new IllegalArgumentException("回收站执行线程数和队列容量必须为正数");
        }
        this.permissionDataCache = permissionDataCache;
        AtomicInteger sequence = new AtomicInteger();
        ThreadFactory threadFactory = task -> {
            Thread thread = new Thread(task, "yubi-recycle-" + sequence.incrementAndGet());
            thread.setDaemon(false);
            return thread;
        };
        executor = new ThreadPoolExecutor(
                threadCount, threadCount, 30, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity), threadFactory,
                new ThreadPoolExecutor.AbortPolicy());
        executor.allowCoreThreadTimeOut(true);
    }

    @Override
    public void execute(Runnable command) {
        Runnable isolated = () -> {
            permissionDataCache.clear();
            try {
                command.run();
            } finally {
                permissionDataCache.clear();
            }
        };
        executor.execute(new DelegatingSecurityContextRunnable(isolated));
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
