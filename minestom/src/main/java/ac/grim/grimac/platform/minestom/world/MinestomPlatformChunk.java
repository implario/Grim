package ac.grim.grimac.platform.minestom.world;

import ac.grim.grimac.platform.api.world.PlatformChunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;

public final class MinestomPlatformChunk implements PlatformChunk {

    private final Instance instance;
    private final int chunkX;
    private final int chunkZ;

    public MinestomPlatformChunk(Instance instance, int chunkX, int chunkZ) {
        this.instance = instance;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    @Override
    public int getBlockID(int x, int y, int z) {
        // Coordinates are chunk-relative in this contract; resolve against the
        // chunk this wrapper was created for.
        int worldX = (chunkX << 4) + (x & 15);
        int worldZ = (chunkZ << 4) + (z & 15);
        Block block = instance.isChunkLoaded(chunkX, chunkZ)
                ? instance.getBlock(worldX, y, worldZ, Block.Getter.Condition.TYPE)
                : null;
        return block == null ? 0 : block.stateId();
    }
}
