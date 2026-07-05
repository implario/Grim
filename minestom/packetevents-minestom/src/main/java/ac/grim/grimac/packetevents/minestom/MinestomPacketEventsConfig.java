package ac.grim.grimac.packetevents.minestom;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;

import java.util.Objects;
import java.util.function.Function;

/**
 * Wiring the embedding application supplies to the PacketEvents bridge.
 *
 * @param eventNode             node the bridge's Minestom listeners attach to
 * @param clientVersionFunction real protocol version of a client, for setups
 *                              where a fronting proxy translates newer clients
 */
public record MinestomPacketEventsConfig(
        EventNode<Event> eventNode,
        Function<Player, ClientVersion> clientVersionFunction
) {
    public MinestomPacketEventsConfig {
        Objects.requireNonNull(eventNode, "eventNode");
        Objects.requireNonNull(clientVersionFunction, "clientVersionFunction");
    }
}
