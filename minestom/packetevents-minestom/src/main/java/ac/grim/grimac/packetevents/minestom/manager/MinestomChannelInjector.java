package ac.grim.grimac.packetevents.minestom.manager;

import com.github.retrooper.packetevents.injector.ChannelInjector;
import com.github.retrooper.packetevents.protocol.player.User;

/**
 * No Netty pipeline exists to inject into: interception happens through
 * Minestom's packet events (see MinestomPacketBridge), so every injector
 * operation is a no-op.
 */
public class MinestomChannelInjector implements ChannelInjector {

    @Override
    public boolean isServerBound() {
        return true;
    }

    @Override
    public void inject() {
    }

    @Override
    public void uninject() {
    }

    @Override
    public void updateUser(Object channel, User user) {
    }
}
