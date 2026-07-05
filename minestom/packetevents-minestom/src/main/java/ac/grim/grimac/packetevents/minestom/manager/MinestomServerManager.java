package ac.grim.grimac.packetevents.minestom.manager;

import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import net.minestom.server.MinecraftServer;

public class MinestomServerManager implements ServerManager {

    private ServerVersion version;

    @Override
    public ServerVersion getVersion() {
        if (version == null) {
            version = resolve();
        }
        return version;
    }

    private static ServerVersion resolve() {
        int protocol = MinecraftServer.PROTOCOL_VERSION;
        for (ServerVersion candidate : ServerVersion.values()) {
            if (candidate.getProtocolVersion() == protocol) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "PacketEvents has no mapping for Minestom protocol " + protocol
                        + " (" + MinecraftServer.VERSION_NAME + "). Update the PacketEvents dependency.");
    }
}
