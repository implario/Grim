package ac.grim.grimac.platform.minestom.api;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Supplies the real protocol version of a client.
 * <p>
 * A Minestom server only ever speaks its native protocol, but when a proxy in
 * front translates newer clients down (ViaVersion on Velocity etc.) the actual
 * client version differs from what the server sees. Feeding Grim the real
 * version lets the movement simulation account for client-side differences.
 */
@FunctionalInterface
public interface ClientVersionResolver {

    @NotNull ClientVersion resolve(@NotNull Player player);
}
