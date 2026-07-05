package ac.grim.grimac.packetevents.minestom.manager;

import ac.grim.grimac.packetevents.minestom.channel.GrimMinestomChannel;
import ac.grim.grimac.packetevents.minestom.channel.MinestomChannels;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import net.minestom.server.entity.Player;

public class MinestomPlayerManager implements PlayerManager {

    @Override
    public int getPing(Object player) {
        return ((Player) player).getLatency();
    }

    @Override
    public ClientVersion getClientVersion(Object player) {
        GrimMinestomChannel channel = MinestomChannels.of((Player) player);
        if (channel == null || channel.user() == null) return ClientVersion.UNKNOWN;
        return channel.user().getClientVersion();
    }

    @Override
    public Object getChannel(Object player) {
        return MinestomChannels.of((Player) player);
    }

    @Override
    public User getUser(Object player) {
        GrimMinestomChannel channel = MinestomChannels.of((Player) player);
        return channel == null ? null : channel.user();
    }
}
