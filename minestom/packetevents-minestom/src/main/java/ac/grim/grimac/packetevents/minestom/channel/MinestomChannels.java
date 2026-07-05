package ac.grim.grimac.packetevents.minestom.channel;

import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Channel registry: the channel handle lives as a transient tag on the player
 * plus uuid/name indexes for the lookups PacketEvents' managers need.
 */
public final class MinestomChannels {

    private static final Tag<GrimMinestomChannel> CHANNEL_TAG = Tag.Transient("grimac-pe-channel");

    private static final Map<UUID, GrimMinestomChannel> BY_UUID = new ConcurrentHashMap<>();
    private static final Map<String, GrimMinestomChannel> BY_NAME = new ConcurrentHashMap<>();

    private MinestomChannels() {
    }

    public static GrimMinestomChannel create(Player player) {
        GrimMinestomChannel channel = new GrimMinestomChannel(player);
        player.setTag(CHANNEL_TAG, channel);
        BY_UUID.put(player.getUuid(), channel);
        BY_NAME.put(player.getUsername().toLowerCase(java.util.Locale.ROOT), channel);
        return channel;
    }

    public static @Nullable GrimMinestomChannel of(Player player) {
        GrimMinestomChannel channel = player.getTag(CHANNEL_TAG);
        return channel != null ? channel : BY_UUID.get(player.getUuid());
    }

    public static @Nullable GrimMinestomChannel byUuid(UUID uuid) {
        return BY_UUID.get(uuid);
    }

    public static @Nullable GrimMinestomChannel byName(String name) {
        return BY_NAME.get(name.toLowerCase(java.util.Locale.ROOT));
    }

    public static void remove(Player player) {
        player.removeTag(CHANNEL_TAG);
        BY_UUID.remove(player.getUuid());
        BY_NAME.remove(player.getUsername().toLowerCase(java.util.Locale.ROOT));
    }

    public static Iterable<GrimMinestomChannel> all() {
        return BY_UUID.values();
    }
}
