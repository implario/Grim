package ac.grim.grimac.platform.minestom.player;

import ac.grim.grimac.platform.api.player.AbstractPlatformPlayerFactory;
import ac.grim.grimac.platform.api.player.OfflinePlatformPlayer;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;

public final class MinestomPlatformPlayerFactory extends AbstractPlatformPlayerFactory<Player> {

    @Override
    protected Player getNativePlayer(@NotNull UUID uuid) {
        return MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uuid);
    }

    @Override
    protected Player getNativePlayer(@NotNull String name) {
        return MinecraftServer.getConnectionManager().getOnlinePlayerByUsername(name);
    }

    @Override
    protected PlatformPlayer createPlatformPlayer(@NotNull Player nativePlayer) {
        return new MinestomPlatformPlayer(nativePlayer);
    }

    @Override
    protected UUID getPlayerUUID(@NotNull Player nativePlayer) {
        return nativePlayer.getUuid();
    }

    @Override
    protected Collection<Player> getNativeOnlinePlayers() {
        return MinecraftServer.getConnectionManager().getOnlinePlayers();
    }

    @Override
    public OfflinePlatformPlayer getOfflineFromUUID(@NotNull UUID uuid) {
        Player online = getNativePlayer(uuid);
        return online != null
                ? new MinestomOfflinePlatformPlayer(uuid, online.getUsername())
                : new MinestomOfflinePlatformPlayer(uuid, null);
    }

    @Override
    public OfflinePlatformPlayer getOfflineFromName(@NotNull String name) {
        Player online = getNativePlayer(name);
        return online != null
                ? new MinestomOfflinePlatformPlayer(online.getUuid(), name)
                : new MinestomOfflinePlatformPlayer(null, name);
    }

    @Override
    public void replaceNativePlayer(@NotNull UUID uuid, @NotNull Player player) {
        PlatformPlayer platformPlayer = cache.getPlayer(uuid);
        if (platformPlayer != null) {
            platformPlayer.replaceNativePlayer(player);
        }
    }
}
