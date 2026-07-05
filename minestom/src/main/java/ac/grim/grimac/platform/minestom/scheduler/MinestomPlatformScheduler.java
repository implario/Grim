package ac.grim.grimac.platform.minestom.scheduler;

import ac.grim.grimac.platform.api.scheduler.AsyncScheduler;
import ac.grim.grimac.platform.api.scheduler.EntityScheduler;
import ac.grim.grimac.platform.api.scheduler.GlobalRegionScheduler;
import ac.grim.grimac.platform.api.scheduler.PlatformScheduler;
import ac.grim.grimac.platform.api.scheduler.RegionScheduler;
import org.jetbrains.annotations.NotNull;

public final class MinestomPlatformScheduler implements PlatformScheduler {

    private final MinestomAsyncScheduler asyncScheduler = new MinestomAsyncScheduler();
    private final MinestomGlobalRegionScheduler globalRegionScheduler = new MinestomGlobalRegionScheduler();
    private final MinestomEntityScheduler entityScheduler = new MinestomEntityScheduler();
    private final MinestomRegionScheduler regionScheduler = new MinestomRegionScheduler(globalRegionScheduler);

    @Override
    public @NotNull AsyncScheduler getAsyncScheduler() {
        return asyncScheduler;
    }

    @Override
    public @NotNull GlobalRegionScheduler getGlobalRegionScheduler() {
        return globalRegionScheduler;
    }

    @Override
    public @NotNull EntityScheduler getEntityScheduler() {
        return entityScheduler;
    }

    @Override
    public @NotNull RegionScheduler getRegionScheduler() {
        return regionScheduler;
    }

    public void shutdown() {
        asyncScheduler.shutdown();
    }
}
