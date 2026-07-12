package yubi.agent.application;

import org.junit.jupiter.api.Test;
import yubi.agent.domain.AgentModels.ResultSize;
import yubi.agent.domain.AgentModels.ToolOutput;
import yubi.agent.domain.ToolExecutionPolicy;
import yubi.agent.port.ReadOnlyTool;
import yubi.query.api.MetadataToolSchema;
import yubi.query.api.QueryExecutionContext;
import yubi.query.domain.QueryModels.Channel;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundedToolExecutorTest {

    private static final QueryExecutionContext CONTEXT = new QueryExecutionContext(
            Channel.AUTHENTICATED, "subject-ref", "organization-ref", "correlation-ref");

    @Test
    void shouldCancelTimedOutToolAndInterruptWorker() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        ReadOnlyTool tool = tool(() -> {
            started.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException exception) {
                interrupted.countDown();
                Thread.currentThread().interrupt();
            }
        });

        AtomicReference<Throwable> failure = new AtomicReference<>();
        try (BoundedToolExecutor executor = new BoundedToolExecutor(new ToolExecutionPolicy(100, 100, 1))) {
            Thread caller = Thread.ofPlatform().start(() -> {
                try {
                    executor.execute(tool, StructuredValues.object(), CONTEXT);
                } catch (Throwable exception) {
                    failure.set(exception);
                }
            });
            assertTrue(started.await(1, TimeUnit.SECONDS));
            caller.join(2_000);
            assertFalse(caller.isAlive());
            assertInstanceOf(ToolTimeoutException.class, failure.get());
            assertTrue(interrupted.await(1, TimeUnit.SECONDS));
        }
    }

    @Test
    void shouldKeepPermitUntilTimedOutToolThatIgnoresInterruptActuallyExits() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch exited = new CountDownLatch(1);
        ReadOnlyTool tool = tool(() -> {
            started.countDown();
            try {
                while (release.getCount() > 0) {
                    try {
                        release.await();
                    } catch (InterruptedException ignored) {
                        // 模拟不协作响应中断的底层 Provider。
                    }
                }
            } finally {
                exited.countDown();
            }
        });
        AtomicReference<Throwable> failure = new AtomicReference<>();

        try (BoundedToolExecutor executor = new BoundedToolExecutor(new ToolExecutionPolicy(100, 100, 1))) {
            Thread caller = Thread.ofPlatform().start(() -> {
                try {
                    executor.execute(tool, StructuredValues.object(), CONTEXT);
                } catch (Throwable exception) {
                    failure.set(exception);
                }
            });
            assertTrue(started.await(1, TimeUnit.SECONDS));
            caller.join(2_000);
            assertInstanceOf(ToolTimeoutException.class, failure.get());
            assertThrows(ToolConcurrencyLimitException.class,
                    () -> executor.execute(tool, StructuredValues.object(), CONTEXT));

            release.countDown();
            assertTrue(exited.await(1, TimeUnit.SECONDS));
            eventuallyExecute(executor, tool);
        }
    }

    @Test
    void shouldCreateWorkersLazilyAndReleaseCancelledTaskThatNeverStarted() throws Exception {
        AtomicInteger createdThreads = new AtomicInteger();
        CountDownLatch allowWorker = new CountDownLatch(1);
        AtomicInteger toolCalls = new AtomicInteger();
        ThreadFactory threadFactory = task -> {
            createdThreads.incrementAndGet();
            Thread thread = new Thread(() -> {
                try {
                    allowWorker.await();
                    task.run();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            });
            thread.setDaemon(true);
            return thread;
        };
        ReadOnlyTool delayed = tool(toolCalls::incrementAndGet);

        try (BoundedToolExecutor executor = new BoundedToolExecutor(
                new ToolExecutionPolicy(100, 100, 1), threadFactory)) {
            assertEquals(0, createdThreads.get());
            assertThrows(ToolTimeoutException.class,
                    () -> executor.execute(delayed, StructuredValues.object(), CONTEXT));
            assertEquals(1, createdThreads.get());
            assertEquals(0, toolCalls.get());

            allowWorker.countDown();
            eventuallyExecute(executor, delayed);
            assertEquals(1, toolCalls.get());
        }
    }

    @Test
    void shouldReleasePermitAfterToolFailureExactlyOnce() {
        ReadOnlyTool failing = tool(() -> {
            throw new IllegalStateException("有限测试失败");
        });
        ReadOnlyTool successful = tool(() -> { });

        try (BoundedToolExecutor executor = new BoundedToolExecutor(new ToolExecutionPolicy(100, 1_000, 1))) {
            assertThrows(IllegalStateException.class,
                    () -> executor.execute(failing, StructuredValues.object(), CONTEXT));
            assertDoesNotThrow(() -> executor.execute(successful, StructuredValues.object(), CONTEXT));
        }
    }

    @Test
    void shouldReleasePermitWhenWorkerCreationThrowsError() {
        AtomicInteger attempts = new AtomicInteger();
        AssertionError startupFailure = new AssertionError("线程启动失败夹具");
        ThreadFactory threadFactory = task -> {
            if (attempts.getAndIncrement() == 0) {
                throw startupFailure;
            }
            Thread thread = new Thread(task);
            thread.setDaemon(true);
            return thread;
        };
        ReadOnlyTool successful = tool(() -> { });

        try (BoundedToolExecutor executor = new BoundedToolExecutor(
                new ToolExecutionPolicy(100, 1_000, 1), threadFactory)) {
            assertSame(startupFailure, assertThrows(AssertionError.class,
                    () -> executor.execute(successful, StructuredValues.object(), CONTEXT)));
            assertDoesNotThrow(() -> executor.execute(successful, StructuredValues.object(), CONTEXT));
        }
    }

    @Test
    void shouldNeverOverReleasePermitAfterCompletedTask() throws Exception {
        AtomicInteger entered = new AtomicInteger();
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        AtomicReference<Throwable> unexpected = new AtomicReference<>();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch rejectionObserved = new CountDownLatch(1);
        ReadOnlyTool quick = tool(() -> { });
        ReadOnlyTool blocking = tool(() -> {
            entered.incrementAndGet();
            firstEntered.countDown();
            try {
                release.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });

        try (BoundedToolExecutor executor = new BoundedToolExecutor(new ToolExecutionPolicy(100, 5_000, 1))) {
            executor.execute(quick, StructuredValues.object(), CONTEXT);
            Runnable caller = () -> {
                ready.countDown();
                try {
                    go.await();
                    executor.execute(blocking, StructuredValues.object(), CONTEXT);
                    succeeded.incrementAndGet();
                } catch (ToolConcurrencyLimitException exception) {
                    rejected.incrementAndGet();
                    rejectionObserved.countDown();
                } catch (Throwable failure) {
                    unexpected.compareAndSet(null, failure);
                }
            };
            Thread first = Thread.ofPlatform().start(caller);
            Thread second = Thread.ofPlatform().start(caller);
            assertTrue(ready.await(1, TimeUnit.SECONDS));
            go.countDown();
            assertTrue(firstEntered.await(1, TimeUnit.SECONDS));
            boolean rejectedBeforeRelease = rejectionObserved.await(1, TimeUnit.SECONDS);
            int enteredBeforeRelease = entered.get();
            release.countDown();
            first.join(1_000);
            second.join(1_000);

            assertTrue(rejectedBeforeRelease);
            assertEquals(1, enteredBeforeRelease);
            assertEquals(1, succeeded.get());
            assertEquals(1, rejected.get());
            assertEquals(null, unexpected.get());
        }
    }

    @Test
    void shouldRejectAboveLimitWithoutQueueingOrBusyWaiting() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ReadOnlyTool tool = tool(() -> {
            started.countDown();
            try {
                release.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });
        AtomicReference<Throwable> backgroundFailure = new AtomicReference<>();

        try (BoundedToolExecutor executor = new BoundedToolExecutor(new ToolExecutionPolicy(100, 5_000, 1))) {
            Thread first = Thread.ofPlatform().start(() -> {
                try {
                    executor.execute(tool, StructuredValues.object(), CONTEXT);
                } catch (Throwable failure) {
                    backgroundFailure.set(failure);
                }
            });
            assertTrue(started.await(1, TimeUnit.SECONDS));
            assertThrows(ToolConcurrencyLimitException.class,
                    () -> executor.execute(tool, StructuredValues.object(), CONTEXT));
            release.countDown();
            first.join(1_000);
            assertEquals(null, backgroundFailure.get());
        }
    }

    @Test
    void shouldRestoreCallerInterruptStatus() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ReadOnlyTool tool = tool(() -> {
            started.countDown();
            try {
                release.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Boolean> interrupted = new AtomicReference<>(false);

        try (BoundedToolExecutor executor = new BoundedToolExecutor(new ToolExecutionPolicy(100, 5_000, 1))) {
            Thread caller = Thread.ofPlatform().start(() -> {
                try {
                    executor.execute(tool, StructuredValues.object(), CONTEXT);
                } catch (Throwable exception) {
                    failure.set(exception);
                    interrupted.set(Thread.currentThread().isInterrupted());
                }
            });
            assertTrue(started.await(1, TimeUnit.SECONDS));
            caller.interrupt();
            caller.join(1_000);
            release.countDown();
            assertInstanceOf(ToolExecutionInterruptedException.class, failure.get());
            assertTrue(interrupted.get());
        }
    }

    @Test
    void shouldCloseIdempotentlyWithoutReleasingRunningIgnoredInterrupt() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ReadOnlyTool tool = tool(() -> {
            started.countDown();
            while (release.getCount() > 0) {
                try {
                    release.await();
                } catch (InterruptedException ignored) {
                    // 模拟关闭时仍未退出的底层 Provider。
                }
            }
        });
        AtomicReference<Throwable> failure = new AtomicReference<>();
        BoundedToolExecutor executor = new BoundedToolExecutor(new ToolExecutionPolicy(100, 5_000, 1));
        Thread caller = Thread.ofPlatform().start(() -> {
            try {
                executor.execute(tool, StructuredValues.object(), CONTEXT);
            } catch (Throwable exception) {
                failure.set(exception);
            }
        });
        assertTrue(started.await(1, TimeUnit.SECONDS));

        executor.close();
        caller.join(1_000);
        assertInstanceOf(ToolConcurrencyLimitException.class, failure.get());
        assertThrows(ToolConcurrencyLimitException.class,
                () -> executor.execute(tool, StructuredValues.object(), CONTEXT));
        release.countDown();
        executor.close();
    }

    @Test
    void shouldEnforceConfiguredPageSizeBeforeCapabilities() {
        ExecuteViewInputMapper mapper = new ExecuteViewInputMapper(new ToolExecutionPolicy(25, 1_000, 1));
        ExecuteViewInput defaults = mapper.map(StructuredValues.object(
                "viewId", StructuredValues.text("view-1"),
                "columns", StructuredValues.array(java.util.List.of(StructuredValues.object(
                        "path", StructuredValues.array(java.util.List.of(StructuredValues.text("amount"))))))));
        assertEquals(25, defaults.pageSize());

        var oversized = StructuredValues.object(
                "viewId", StructuredValues.text("view-1"),
                "columns", StructuredValues.array(java.util.List.of(StructuredValues.object(
                        "path", StructuredValues.array(java.util.List.of(StructuredValues.text("amount")))))),
                "page", StructuredValues.object("pageSize", StructuredValues.integer(26)));
        assertThrows(yubi.agent.api.ToolInputException.class, () -> mapper.map(oversized));
    }

    private ReadOnlyTool tool(Runnable action) {
        return new ReadOnlyTool() {
            @Override
            public MetadataToolSchema schema() {
                return yubi.query.api.QueryMetadataToolSchemas.searchDataAssets();
            }

            @Override
            public ToolOutput execute(yubi.agent.domain.StructuredValue.ObjectValue arguments,
                                      QueryExecutionContext context) {
                action.run();
                return new ToolOutput(new yubi.agent.domain.StructuredValue.ObjectValue(Map.of()), ResultSize.empty());
            }
        };
    }

    private void eventuallyExecute(BoundedToolExecutor executor, ReadOnlyTool tool) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
        ToolConcurrencyLimitException lastFailure = null;
        while (System.nanoTime() < deadline) {
            try {
                executor.execute(tool, StructuredValues.object(), CONTEXT);
                return;
            } catch (ToolConcurrencyLimitException exception) {
                lastFailure = exception;
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
            }
        }
        if (lastFailure != null) {
            throw lastFailure;
        }
        throw new AssertionError("Tool 未在期限内执行");
    }
}
