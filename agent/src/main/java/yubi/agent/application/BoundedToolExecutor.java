package yubi.agent.application;

import yubi.agent.domain.AgentModels.ToolOutput;
import yubi.agent.domain.StructuredValue.ObjectValue;
import yubi.agent.domain.ToolExecutionPolicy;
import yubi.agent.port.ReadOnlyTool;
import yubi.agent.port.ToolExecutionPort;
import yubi.query.api.QueryExecutionContext;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class BoundedToolExecutor implements ToolExecutionPort, AutoCloseable {

    private final long timeoutMillis;
    private final ThreadPoolExecutor executor;
    private final Semaphore permits;
    private final java.util.Set<ToolTask> inFlight = ConcurrentHashMap.newKeySet();
    private final Object lifecycleLock = new Object();
    private boolean closed;

    public BoundedToolExecutor(ToolExecutionPolicy policy) {
        this(policy, threadFactory());
    }

    BoundedToolExecutor(ToolExecutionPolicy policy, ThreadFactory threadFactory) {
        java.util.Objects.requireNonNull(policy, "policy");
        java.util.Objects.requireNonNull(threadFactory, "threadFactory");
        this.timeoutMillis = policy.timeoutMillis();
        int concurrency = policy.maximumConcurrentCalls();
        this.permits = new Semaphore(concurrency);
        this.executor = new ThreadPoolExecutor(concurrency, concurrency, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(concurrency), threadFactory, new ThreadPoolExecutor.AbortPolicy());
    }

    @Override
    public ToolOutput execute(ReadOnlyTool tool, ObjectValue arguments, QueryExecutionContext context) {
        if (!permits.tryAcquire()) {
            throw new ToolConcurrencyLimitException();
        }
        ToolTask future = new ToolTask(() -> tool.execute(arguments, context), new PermitLease(permits));
        synchronized (lifecycleLock) {
            if (closed) {
                future.releaseIfNotRunning();
                throw new ToolConcurrencyLimitException();
            }
            inFlight.add(future);
            try {
                executor.execute(future);
            } catch (RuntimeException | Error failure) {
                cancel(future, false);
                if (failure instanceof RejectedExecutionException) {
                    throw new ToolConcurrencyLimitException();
                }
                throw failure;
            }
        }
        try {
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            cancel(future, true);
            throw new ToolTimeoutException();
        } catch (InterruptedException exception) {
            cancel(future, true);
            Thread.currentThread().interrupt();
            throw new ToolExecutionInterruptedException(exception);
        } catch (CancellationException exception) {
            throw new ToolConcurrencyLimitException();
        } catch (ExecutionException exception) {
            return rethrow(exception.getCause());
        }
    }

    private void cancel(ToolTask task, boolean mayInterruptIfRunning) {
        task.cancel(mayInterruptIfRunning);
        executor.remove(task);
        task.releaseIfNotRunning();
    }

    private ToolOutput rethrow(Throwable failure) {
        if (failure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (failure instanceof Error error) {
            throw error;
        }
        throw new IllegalStateException("只读工具发生未预期受检异常", failure);
    }

    private static ThreadFactory threadFactory() {
        AtomicInteger sequence = new AtomicInteger();
        return task -> {
            Thread thread = new Thread(task, "yubi-agent-tool-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    @Override
    public void close() {
        java.util.List<ToolTask> tasks;
        synchronized (lifecycleLock) {
            if (closed) {
                return;
            }
            closed = true;
            executor.shutdownNow();
            tasks = List.copyOf(inFlight);
        }
        tasks.forEach(task -> cancel(task, true));
    }

    private final class ToolTask extends FutureTask<ToolOutput> {
        private final AtomicBoolean runEntered = new AtomicBoolean();
        private final PermitLease lease;

        private ToolTask(Callable<ToolOutput> callable, PermitLease lease) {
            super(releasing(callable, lease));
            this.lease = lease;
        }

        @Override
        public void run() {
            runEntered.set(true);
            try {
                super.run();
            } finally {
                lease.release();
                inFlight.remove(this);
            }
        }

        private void releaseIfNotRunning() {
            if (!runEntered.get()) {
                lease.release();
                inFlight.remove(this);
            }
        }
    }

    private static final class PermitLease {
        private final Semaphore permits;
        private final AtomicBoolean released = new AtomicBoolean();

        private PermitLease(Semaphore permits) {
            this.permits = permits;
        }

        private void release() {
            if (released.compareAndSet(false, true)) {
                permits.release();
            }
        }
    }

    private static Callable<ToolOutput> releasing(Callable<ToolOutput> callable, PermitLease lease) {
        return () -> {
            try {
                return callable.call();
            } finally {
                lease.release();
            }
        };
    }
}
