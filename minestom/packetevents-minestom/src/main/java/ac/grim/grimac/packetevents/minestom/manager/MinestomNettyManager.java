package ac.grim.grimac.packetevents.minestom.manager;

import ac.grim.grimac.packetevents.minestom.channel.MinestomChannelOperator;
import com.github.retrooper.packetevents.netty.NettyManager;
import com.github.retrooper.packetevents.netty.buffer.ByteBufAllocationOperator;
import com.github.retrooper.packetevents.netty.buffer.ByteBufOperator;
import com.github.retrooper.packetevents.netty.channel.ChannelOperator;
import io.github.retrooper.packetevents.impl.netty.buffer.ByteBufAllocationOperatorImpl;
import io.github.retrooper.packetevents.impl.netty.buffer.ByteBufOperatorImpl;

/**
 * Buffers are real Netty ByteBufs (io.netty:netty-buffer on the classpath),
 * so PE's stock netty-common operators serve buffer work; only the channel
 * operations are rerouted to the Minestom fake channel.
 */
public class MinestomNettyManager implements NettyManager {

    private final ByteBufOperator byteBufOperator = new ByteBufOperatorImpl();
    private final ByteBufAllocationOperator byteBufAllocationOperator = new ByteBufAllocationOperatorImpl();
    private final ChannelOperator channelOperator = new MinestomChannelOperator();

    @Override
    public ChannelOperator getChannelOperator() {
        return channelOperator;
    }

    @Override
    public ByteBufOperator getByteBufOperator() {
        return byteBufOperator;
    }

    @Override
    public ByteBufAllocationOperator getByteBufAllocationOperator() {
        return byteBufAllocationOperator;
    }
}
