package ac.grim.grimac.platform.minestom.entity;

import ac.grim.grimac.platform.api.entity.GrimEntity;
import ac.grim.grimac.platform.api.world.PlatformWorld;
import ac.grim.grimac.platform.minestom.world.MinestomPlatformWorld;
import ac.grim.grimac.utils.math.Location;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MinestomGrimEntity<T extends Entity> implements GrimEntity {

    protected volatile T entity;

    public MinestomGrimEntity(@NotNull T entity) {
        this.entity = Objects.requireNonNull(entity);
    }

    protected void setNativeEntity(@NotNull T entity) {
        this.entity = Objects.requireNonNull(entity);
    }

    @Override
    public UUID getUniqueId() {
        return entity.getUuid();
    }

    @Override
    public boolean eject() {
        boolean hadPassenger = !entity.getPassengers().isEmpty();
        for (Entity passenger : entity.getPassengers()) {
            entity.removePassenger(passenger);
        }
        return hadPassenger;
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Location location) {
        Pos pos = new Pos(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        return entity.teleport(pos).handle((unused, throwable) -> throwable == null);
    }

    @Override
    public @NotNull Object getNative() {
        return entity;
    }

    @Override
    public boolean isDead() {
        if (entity instanceof LivingEntity livingEntity) {
            return livingEntity.isDead();
        }
        return entity.isRemoved();
    }

    @Override
    public PlatformWorld getWorld() {
        return MinestomPlatformWorld.of(entity.getInstance());
    }

    @Override
    public Location getLocation() {
        Pos pos = entity.getPosition();
        return new Location(getWorld(), pos.x(), pos.y(), pos.z(), pos.yaw(), pos.pitch());
    }

    @Override
    public double distanceSquared(double x, double y, double z) {
        Pos pos = entity.getPosition();
        double dx = pos.x() - x;
        double dy = pos.y() - y;
        double dz = pos.z() - z;
        return dx * dx + dy * dy + dz * dz;
    }
}
