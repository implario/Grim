package ac.grim.grimac.packetevents.minestom.bridge;

import ac.grim.grimac.packetevents.minestom.channel.GrimMinestomChannel;
import ac.grim.grimac.packetevents.minestom.channel.MinestomChannels;
import ac.grim.grimac.packetevents.minestom.pipeline.MinestomPipelineEmulator;
import ac.grim.grimac.packetevents.minestom.util.MinestomPacketConverter;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.UserConnectEvent;
import com.github.retrooper.packetevents.event.UserLoginEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.PacketEventsImplHelper;
import io.netty.buffer.ByteBuf;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.login.LoginSuccessPacket;
import net.minestom.server.network.player.GameProfile;

import java.util.function.Function;

/**
 * Creates and tears down PE {@link User}s in step with Minestom's connection
 * lifecycle, reproducing the exact event sequence Grim's common code expects:
 *
 * <ol>
 *   <li>{@code UserConnectEvent} while still in LOGIN state;</li>
 *   <li>a synthesized LOGIN_SUCCESS {@code PacketSendEvent} — Minestom sends
 *       the real one before the {@code Player} object exists, so the bridge
 *       replays it; PE's internal listener flips the connection states and
 *       Grim's join listener creates the GrimPlayer from its send-tasks;</li>
 *   <li>{@code UserLoginEvent} at first spawn, binding the platform player;</li>
 *   <li>{@code UserDisconnectEvent} via PE's disconnection helper.</li>
 * </ol>
 */
public final class MinestomUserLifecycle {

    private final Function<Player, ClientVersion> clientVersionFunction;

    public MinestomUserLifecycle(Function<Player, ClientVersion> clientVersionFunction) {
        this.clientVersionFunction = clientVersionFunction;
    }

    /** Called from AsyncPlayerConfigurationEvent, before any tapped packet. */
    public GrimMinestomChannel onPlayerConfiguration(Player player) {
        GrimMinestomChannel existing = MinestomChannels.of(player);
        if (existing != null) return existing;

        GrimMinestomChannel channel = MinestomChannels.create(player);
        ClientVersion clientVersion = clientVersionFunction.apply(player);
        User user = new User(
                channel,
                ConnectionState.LOGIN,
                clientVersion,
                new UserProfile(player.getUuid(), player.getUsername()));
        channel.bindUser(user);
        PacketEvents.getAPI().getProtocolManager().setUser(channel, user);

        PacketEvents.getAPI().getEventManager().callEvent(new UserConnectEvent(user));

        replayLoginSuccess(channel, player);
        return channel;
    }

    /**
     * Feeds a LOGIN_SUCCESS through the PE pipeline. Serialized by Minestom's
     * own login registry, so the id and payload match the live protocol.
     */
    private void replayLoginSuccess(GrimMinestomChannel channel, Player player) {
        LoginSuccessPacket packet = new LoginSuccessPacket(
                new GameProfile(player.getUuid(), player.getUsername()));
        ByteBuf bytes = MinestomPacketConverter.serverPacketToBytes(
                packet, net.minestom.server.network.ConnectionState.LOGIN);
        channel.lock().lock();
        try {
            MinestomPipelineEmulator.Outcome outcome = MinestomPipelineEmulator.clientBound(channel, bytes);
            // The real packet already reached the client; only the side
            // effects (state flips, Grim's addUser task) matter here.
            if (outcome.postTasks() != null) {
                outcome.postTasks().run();
            }
        } finally {
            channel.lock().unlock();
        }
    }

    /** Called from PlayerSpawnEvent(firstSpawn=true). */
    public void onFirstSpawn(Player player) {
        GrimMinestomChannel channel = MinestomChannels.of(player);
        if (channel == null || channel.user() == null) return;
        User user = channel.user();
        // Serverbound configuration acks are invisible to the bridge, so the
        // decoder state may lag behind; the connection is authoritative.
        user.setEncoderState(ConnectionState.PLAY);
        user.setDecoderState(ConnectionState.PLAY);
        PacketEvents.getAPI().getEventManager().callEvent(new UserLoginEvent(user, player));
    }

    /** Called from PlayerDisconnectEvent. */
    public void onDisconnect(Player player) {
        GrimMinestomChannel channel = MinestomChannels.of(player);
        if (channel == null) return;
        try {
            PacketEventsImplHelper.handleDisconnection(channel, player.getUuid());
        } finally {
            channel.markClosed();
            MinestomChannels.remove(player);
        }
    }
}
