package ac.grim.grimac.packetevents.minestom;

import com.github.retrooper.packetevents.PacketEventsAPI;

/**
 * Entry point mirroring PE's per-platform builders
 * (SpigotPacketEventsBuilder etc.).
 */
public final class MinestomPacketEventsBuilder {

    private static PacketEventsAPI<MinestomPacketEventsConfig> instance;

    private MinestomPacketEventsBuilder() {
    }

    public static synchronized PacketEventsAPI<MinestomPacketEventsConfig> build(MinestomPacketEventsConfig config) {
        if (instance == null) {
            instance = new MinestomPacketEventsAPI(config);
        }
        return instance;
    }
}
