package ac.grim.grimac.packetevents.minestom.channel;

import ac.grim.grimac.packetevents.minestom.util.MinestomPacketConverter;
import com.github.retrooper.packetevents.netty.channel.ChannelOperator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.kyori.adventure.text.Component;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.packet.server.ServerPacket;

import java.net.SocketAddress;
import java.util.List;

/**
 * PE channel operations mapped onto the fake {@link GrimMinestomChannel}.
 * Raw writes decode PE bytes into Minestom packets (marked passthrough so the
 * outgoing event does not re-process them) and enqueue them on the connection.
 * There is no Netty pipeline, so the pipeline/context accessors are inert.
 */
public class MinestomChannelOperator implements ChannelOperator {

    private static GrimMinestomChannel channel(Object channel) {
        return (GrimMinestomChannel) channel;
    }

    @Override
    public SocketAddress remoteAddress(Object channel) {
        return channel(channel).connection().getRemoteAddress();
    }

    @Override
    public SocketAddress localAddress(Object channel) {
        return null;
    }

    @Override
    public boolean isOpen(Object channel) {
        return channel(channel).isOpen();
    }

    @Override
    public Object close(Object channelObject) {
        GrimMinestomChannel channel = channel(channelObject);
        channel.player().kick(Component.translatable("multiplayer.disconnect.generic"));
        channel.markClosed();
        return null;
    }

    @Override
    public Object write(Object channelObject, Object byteBuf) {
        return writeAndFlush(channelObject, byteBuf);
    }

    @Override
    public Object flush(Object channel) {
        // Minestom's connection write thread flushes on its own schedule.
        return null;
    }

    @Override
    public Object writeAndFlush(Object channelObject, Object byteBuf) {
        GrimMinestomChannel channel = channel(channelObject);
        if (!channel.isOpen()) return null;
        channel.lock().lock();
        try {
            ConnectionState state = channel.user() != null && channel.user().getEncoderState() != null
                    ? MinestomPacketConverter.toMinestomState(channel.user().getEncoderState())
                    : ConnectionState.PLAY;
            ServerPacket packet = MinestomPacketConverter.bytesToServerPacket((ByteBuf) byteBuf, state);
            channel.markPassthrough(packet);
            GrimMinestomChannel.OutgoingContext context = channel.outgoingContext();
            if (context != null) {
                context.collect(packet);
            } else {
                channel.connection().sendPacket(packet);
            }
        } finally {
            channel.lock().unlock();
        }
        return null;
    }

    @Override
    public Object fireChannelRead(Object channel, Object obj) {
        return null;
    }

    @Override
    public Object writeInContext(Object channel, String ctx, Object obj) {
        return write(channel, obj);
    }

    @Override
    public Object flushInContext(Object channel, String ctx) {
        return null;
    }

    @Override
    public Object writeAndFlushInContext(Object channel, String ctx, Object obj) {
        return writeAndFlush(channel, obj);
    }

    @Override
    public Object fireChannelReadInContext(Object channel, String ctx, Object obj) {
        return null;
    }

    @Override
    public List<String> pipelineHandlerNames(Object channel) {
        // No pipeline exists; a stable marker keeps /grim dump readable.
        return List.of("minestom-bridge");
    }

    @Override
    public Object getPipelineHandler(Object channel, String name) {
        return null;
    }

    @Override
    public Object getPipelineContext(Object channel, String name) {
        return null;
    }

    @Override
    public Object getPipeline(Object channel) {
        return null;
    }

    @Override
    public void runInEventLoop(Object channel, Runnable task) {
        channel(channel).runInEventLoop(task);
    }

    @Override
    public Object pooledByteBuf(Object channel) {
        return Unpooled.buffer();
    }
}
