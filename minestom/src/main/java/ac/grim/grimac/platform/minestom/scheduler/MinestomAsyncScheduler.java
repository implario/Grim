package ac.grim.grimac.platform.minestom.scheduler;

import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.platform.api.scheduler.AsyncScheduler;
import ac.grim.grimac.platform.api.scheduler.TaskHandle;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class MinestomAsyncScheduler implements AsyncScheduler {

    private final ScheduledExecutorService executor;

    public MinestomAsyncScheduler() {
        AtomicInteger threadId = new AtomicInteger();
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
                Math.max(2, Runtime.getRuntime().availableProcessors() / 4),
                runnable -> {
                    Thread thread = Executors.defaultThreadFactory().newThread(runnable);
                    thread.setName("grim-async-" + threadId.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                });
        executor.setRemoveOnCancelPolicy(true);
        this.executor = executor;
    }

    @Override
    public TaskHandle runNow(@NotNull GrimPlugin plugin, @NotNull Runnable task) {
        return MinestomTaskHandle.of(executor.submit(task));
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull Runnable task, long delay, @NotNull TimeUnit timeUnit) {
        return MinestomTaskHandle.of(executor.schedule(task, delay, timeUnit));
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Runnable task, long delay, long period, @NotNull TimeUnit timeUnit) {
        return MinestomTaskHandle.of(executor.scheduleAtFixedRate(task, delay, period, timeUnit));
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return MinestomTaskHandle.of(executor.scheduleAtFixedRate(task, initialDelayTicks * 50L, periodTicks * 50L, TimeUnit.MILLISECONDS));
    }

    @Override
    public void cancel(@NotNull GrimPlugin plugin) {
        // Task handles own their cancellation; nothing tracks per-plugin tasks here.
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
