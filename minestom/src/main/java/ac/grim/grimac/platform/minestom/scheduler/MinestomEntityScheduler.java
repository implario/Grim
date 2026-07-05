package ac.grim.grimac.platform.minestom.scheduler;

import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.platform.api.entity.GrimEntity;
import ac.grim.grimac.platform.api.scheduler.EntityScheduler;
import ac.grim.grimac.platform.api.scheduler.TaskHandle;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Minestom is not region-threaded, so entity tasks run on the global scheduler.
 * The {@code retired} callback still fires instead of the task when the entity
 * has been removed by execution time, mirroring Folia semantics.
 */
public final class MinestomEntityScheduler implements EntityScheduler {

    private Runnable guarded(GrimEntity entity, Runnable task, @Nullable Runnable retired) {
        return () -> {
            Entity nativeEntity = (Entity) entity.getNative();
            if (nativeEntity.isRemoved()) {
                if (retired != null) retired.run();
            } else {
                task.run();
            }
        };
    }

    @Override
    public void execute(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable run, @Nullable Runnable retired, long delay) {
        MinecraftServer.getSchedulerManager().scheduleTask(
                guarded(entity, run, retired),
                TaskSchedule.tick((int) Math.max(1, delay)),
                TaskSchedule.stop());
    }

    @Override
    public TaskHandle run(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired) {
        return MinestomTaskHandle.of(MinecraftServer.getSchedulerManager()
                .scheduleNextTick(guarded(entity, task, retired)));
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired, long delayTicks) {
        return MinestomTaskHandle.of(MinecraftServer.getSchedulerManager()
                .scheduleTask(guarded(entity, task, retired),
                        TaskSchedule.tick((int) Math.max(1, delayTicks)),
                        TaskSchedule.stop()));
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired, long initialDelayTicks, long periodTicks) {
        return MinestomTaskHandle.of(MinecraftServer.getSchedulerManager()
                .scheduleTask(guarded(entity, task, retired),
                        TaskSchedule.tick((int) Math.max(1, initialDelayTicks)),
                        TaskSchedule.tick((int) Math.max(1, periodTicks))));
    }
}
