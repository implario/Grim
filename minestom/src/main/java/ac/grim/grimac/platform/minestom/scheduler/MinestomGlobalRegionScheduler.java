package ac.grim.grimac.platform.minestom.scheduler;

import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.platform.api.scheduler.GlobalRegionScheduler;
import ac.grim.grimac.platform.api.scheduler.TaskHandle;
import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

public final class MinestomGlobalRegionScheduler implements GlobalRegionScheduler {

    @Override
    public void execute(@NotNull GrimPlugin plugin, @NotNull Runnable task) {
        MinecraftServer.getSchedulerManager().scheduleNextTick(task);
    }

    @Override
    public TaskHandle run(@NotNull GrimPlugin plugin, @NotNull Runnable task) {
        return MinestomTaskHandle.of(MinecraftServer.getSchedulerManager().scheduleNextTick(task));
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull Runnable task, long delay) {
        return MinestomTaskHandle.of(MinecraftServer.getSchedulerManager()
                .scheduleTask(task, TaskSchedule.tick((int) Math.max(1, delay)), TaskSchedule.stop()));
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return MinestomTaskHandle.of(MinecraftServer.getSchedulerManager()
                .scheduleTask(task,
                        TaskSchedule.tick((int) Math.max(1, initialDelayTicks)),
                        TaskSchedule.tick((int) Math.max(1, periodTicks))));
    }

    @Override
    public void cancel(@NotNull GrimPlugin plugin) {
        // Task handles own their cancellation; nothing tracks per-plugin tasks here.
    }
}
