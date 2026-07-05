package ac.grim.grimac.packetevents.minestom.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minestom.server.MinecraftServer;
import net.minestom.server.network.NetworkBuffer;

/**
 * Converts Minestom items to PacketEvents items by round-tripping through the
 * network serialization both sides implement — slower than a field-by-field
 * mapping but guaranteed consistent with what the client actually sees.
 */
public final class MinestomItemStackConverter {

    private MinestomItemStackConverter() {
    }

    public static ItemStack toPacketEvents(net.minestom.server.item.ItemStack stack) {
        if (stack == null || stack.isAir()) return ItemStack.EMPTY;
        NetworkBuffer buffer = NetworkBuffer.resizableBuffer(512, MinecraftServer.process());
        buffer.write(net.minestom.server.item.ItemStack.NETWORK_TYPE, stack);
        int length = (int) (buffer.writeIndex() - buffer.readIndex());
        byte[] bytes = new byte[length];
        buffer.copyTo(buffer.readIndex(), bytes, 0, length);

        ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
        try {
            PacketWrapper<?> wrapper = PacketWrapper.createUniversalPacketWrapper(byteBuf);
            wrapper.setServerVersion(PacketEvents.getAPI().getServerManager().getVersion());
            return wrapper.readItemStack();
        } finally {
            byteBuf.release();
        }
    }
}
