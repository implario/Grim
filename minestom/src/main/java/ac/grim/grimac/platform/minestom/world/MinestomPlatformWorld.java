package ac.grim.grimac.platform.minestom.world;

import ac.grim.grimac.platform.api.world.PlatformChunk;
import ac.grim.grimac.platform.api.world.PlatformWorld;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class MinestomPlatformWorld implements PlatformWorld {

    private final Instance instance;

    private MinestomPlatformWorld(Instance instance) {
        this.instance = instance;
    }

    public static MinestomPlatformWorld of(Instance instance) {
        return new MinestomPlatformWorld(instance);
    }

    public Instance getInstance() {
        return instance;
    }

    @Override
    public boolean isChunkLoaded(int chunkX, int chunkZ) {
        return instance.isChunkLoaded(chunkX, chunkZ);
    }

    @Override
    public WrappedBlockState getBlockAt(int x, int y, int z) {
        // Minestom state ids are the vanilla protocol ids of the server's own
        // version, which is exactly the id space PE's global ids use here.
        Block block = instance.isChunkLoaded(x >> 4, z >> 4)
                ? instance.getBlock(x, y, z, Block.Getter.Condition.TYPE)
                : null;
        if (block == null) return WrappedBlockState.getByGlobalId(0); // air
        return WrappedBlockState.getByGlobalId(
                PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(),
                block.stateId()
        );
    }

    @Override
    public String getName() {
        return instance.getDimensionName();
    }

    @Override
    public @Nullable UUID getUID() {
        return instance.getUuid();
    }

    @Override
    public PlatformChunk getChunkAt(int currChunkX, int currChunkZ) {
        return new MinestomPlatformChunk(instance, currChunkX, currChunkZ);
    }

    @Override
    public boolean isLoaded() {
        return MinecraftServer.getInstanceManager().getInstances().contains(instance);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MinestomPlatformWorld other && instance.equals(other.instance);
    }

    @Override
    public int hashCode() {
        return instance.hashCode();
    }
}
