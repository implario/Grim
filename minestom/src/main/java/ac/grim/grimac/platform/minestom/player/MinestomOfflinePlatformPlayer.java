package ac.grim.grimac.platform.minestom.player;

import ac.grim.grimac.platform.api.player.OfflinePlatformPlayer;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Minestom keeps no offline player storage, so this is a plain identity holder;
 * name or uuid may be unknown when the player has never been online this run.
 */
public final class MinestomOfflinePlatformPlayer implements OfflinePlatformPlayer {

    private final @Nullable UUID uuid;
    private final @Nullable String name;

    public MinestomOfflinePlatformPlayer(@Nullable UUID uuid, @Nullable String name) {
        this.uuid = uuid;
        this.name = name;
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public boolean isOnline() {
        return uuid != null && MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uuid) != null;
    }

    @Override
    public String getName() {
        return name;
    }
}
