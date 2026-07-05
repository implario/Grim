package ac.grim.grimac.packetevents.minestom.manager;

import ac.grim.grimac.packetevents.minestom.channel.GrimMinestomChannel;
import ac.grim.grimac.packetevents.minestom.channel.MinestomChannels;
import ac.grim.grimac.packetevents.minestom.pipeline.MinestomPipelineEmulator;
import ac.grim.grimac.packetevents.minestom.util.MinestomPacketConverter;
import com.github.retrooper.packetevents.manager.protocol.ProtocolManager;
import com.github.retrooper.packetevents.protocol.ProtocolVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import io.netty.buffer.ByteBuf;
import net.minestom.server.MinecraftServer;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.packet.PacketVanilla;
import net.minestom.server.network.packet.client.ClientPacket;
import net.minestom.server.network.packet.server.ServerPacket;

/**
 * PE's send/receive entry points, backed by Minestom's connection instead of
 * a Netty channel. Grim-written clientbound packets run through the PE
 * listener chain themselves (Grim records its own transactions from the
 * resulting PacketSendEvents, exactly as on Netty platforms), are decoded
 * into Minestom packets, marked passthrough and enqueued on the connection.
 */
public class MinestomProtocolManager implements ProtocolManager {

    @Override
    public ProtocolVersion getPlatformVersion() {
        return ProtocolVersion.UNKNOWN;
    }

    @Override
    public void sendPacket(Object channel, Object byteBuf) {
        writePacket(channel, byteBuf);
        // Minestom's connection write thread flushes on its own.
    }

    @Override
    public void sendPacketSilently(Object channel, Object byteBuf) {
        writePacketSilently(channel, byteBuf);
    }

    @Override
    public void writePacket(Object channelObject, Object byteBuf) {
        GrimMinestomChannel channel = (GrimMinestomChannel) channelObject;
        if (!channel.isOpen()) return;
        channel.lock().lock();
        try {
            MinestomPipelineEmulator.Outcome outcome =
                    MinestomPipelineEmulator.clientBound(channel, (ByteBuf) byteBuf);
            if (!outcome.cancelled() && outcome.finalBuffer() != null) {
                deliver(channel, outcome.finalBuffer());
            }
            if (outcome.postTasks() != null) {
                outcome.postTasks().run();
            }
        } finally {
            channel.lock().unlock();
        }
    }

    @Override
    public void writePacketSilently(Object channelObject, Object byteBuf) {
        GrimMinestomChannel channel = (GrimMinestomChannel) channelObject;
        if (!channel.isOpen()) return;
        channel.lock().lock();
        try {
            deliver(channel, (ByteBuf) byteBuf);
        } finally {
            channel.lock().unlock();
        }
    }

    /**
     * Decodes PE-written bytes into a Minestom packet and hands it off. When
     * called re-entrantly from inside an outgoing packet event the packet is
     * collected into the PRE/POST injection context (its queue position is
     * fixed when the processed packet is re-enqueued); otherwise it goes
     * straight onto the connection's queue.
     */
    private void deliver(GrimMinestomChannel channel, ByteBuf byteBuf) {
        ConnectionState state = encoderState(channel);
        ServerPacket packet = MinestomPacketConverter.bytesToServerPacket(byteBuf, state);
        channel.markPassthrough(packet);
        GrimMinestomChannel.OutgoingContext context = channel.outgoingContext();
        if (context != null) {
            context.collect(packet);
        } else {
            channel.connection().sendPacket(packet);
        }
    }

    private static ConnectionState encoderState(GrimMinestomChannel channel) {
        User user = channel.user();
        if (user == null || user.getEncoderState() == null) return ConnectionState.PLAY;
        return MinestomPacketConverter.toMinestomState(user.getEncoderState());
    }

    @Override
    public void receivePacket(Object channelObject, Object byteBuf) {
        GrimMinestomChannel channel = (GrimMinestomChannel) channelObject;
        if (!channel.isOpen()) return;
        channel.lock().lock();
        try {
            MinestomPipelineEmulator.Outcome outcome =
                    MinestomPipelineEmulator.serverBound(channel, (ByteBuf) byteBuf);
            if (!outcome.cancelled() && outcome.finalBuffer() != null) {
                processIntoServer(channel, outcome.finalBuffer());
            }
        } finally {
            channel.lock().unlock();
        }
    }

    @Override
    public void receivePacketSilently(Object channelObject, Object byteBuf) {
        GrimMinestomChannel channel = (GrimMinestomChannel) channelObject;
        if (!channel.isOpen()) return;
        processIntoServer(channel, (ByteBuf) byteBuf);
    }

    private void processIntoServer(GrimMinestomChannel channel, ByteBuf byteBuf) {
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.getBytes(byteBuf.readerIndex(), bytes);
        var buffer = net.minestom.server.network.NetworkBuffer.wrap(bytes, 0, bytes.length, MinecraftServer.process());
        int packetId = buffer.read(net.minestom.server.network.NetworkBuffer.VAR_INT);
        ConnectionState state = decoderState(channel);
        ClientPacket packet = PacketVanilla.CLIENT_PACKET_PARSER.stateRegistry(state).create(packetId, buffer);
        MinecraftServer.process().packetListener().processClientPacket(packet, channel.connection());
    }

    private static ConnectionState decoderState(GrimMinestomChannel channel) {
        User user = channel.user();
        if (user == null || user.getDecoderState() == null) return ConnectionState.PLAY;
        return MinestomPacketConverter.toMinestomState(user.getDecoderState());
    }

    @Override
    public ClientVersion getClientVersion(Object channelObject) {
        User user = ((GrimMinestomChannel) channelObject).user();
        return user == null ? ClientVersion.UNKNOWN : user.getClientVersion();
    }

    @Override
    public void setClientVersion(Object channelObject, ClientVersion version) {
        User user = ((GrimMinestomChannel) channelObject).user();
        if (user != null) {
            user.setClientVersion(version);
        }
    }

    @Override
    public User getUser(Object channelObject) {
        return ((GrimMinestomChannel) channelObject).user();
    }

    @Override
    public void setUser(Object channelObject, User user) {
        ((GrimMinestomChannel) channelObject).bindUser(user);
        USERS.put(channelObject, user);
    }

    @Override
    public Object getChannel(String username) {
        return MinestomChannels.byName(username);
    }
}
