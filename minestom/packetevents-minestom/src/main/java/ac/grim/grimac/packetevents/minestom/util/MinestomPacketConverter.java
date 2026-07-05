package ac.grim.grimac.packetevents.minestom.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minestom.server.MinecraftServer;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.PacketRegistry;
import net.minestom.server.network.packet.PacketVanilla;
import net.minestom.server.network.packet.client.ClientPacket;
import net.minestom.server.network.packet.server.ServerPacket;

/**
 * Converts between Minestom packet objects and the raw
 * {@code [varint packet id][payload]} byte form PacketEvents processes.
 * Serializers are Minestom's own bidirectional {@code NetworkBufferTemplate}s,
 * so the bytes are exactly what the vanilla protocol carries.
 */
public final class MinestomPacketConverter {

    private static final int INITIAL_BUFFER_SIZE = 2048;

    private MinestomPacketConverter() {
    }

    /** Serializes a serverbound packet to PE's wire form. */
    @SuppressWarnings("unchecked")
    public static ByteBuf clientPacketToBytes(ClientPacket packet, ConnectionState state) {
        PacketRegistry<? extends ClientPacket> registry = PacketVanilla.CLIENT_PACKET_PARSER.stateRegistry(state);
        PacketRegistry.PacketInfo<ClientPacket> info =
                ((PacketRegistry<ClientPacket>) registry).packetInfo(packet);
        return serialize(info, packet);
    }

    /** Serializes a clientbound packet to PE's wire form. */
    @SuppressWarnings("unchecked")
    public static ByteBuf serverPacketToBytes(ServerPacket packet, ConnectionState state) {
        PacketRegistry<? extends ServerPacket> registry = PacketVanilla.SERVER_PACKET_PARSER.stateRegistry(state);
        PacketRegistry.PacketInfo<ServerPacket> info =
                ((PacketRegistry<ServerPacket>) registry).packetInfo(packet);
        return serialize(info, packet);
    }

    private static <T> ByteBuf serialize(PacketRegistry.PacketInfo<T> info, T packet) {
        NetworkBuffer buffer = NetworkBuffer.resizableBuffer(INITIAL_BUFFER_SIZE, MinecraftServer.process());
        buffer.write(NetworkBuffer.VAR_INT, info.id());
        buffer.write(info.serializer(), packet);
        int length = (int) (buffer.writeIndex() - buffer.readIndex());
        byte[] bytes = new byte[length];
        buffer.copyTo(buffer.readIndex(), bytes, 0, length);
        return Unpooled.wrappedBuffer(bytes);
    }

    /**
     * Parses PE-written bytes back into a Minestom clientbound packet so it
     * can be delivered through the player's connection.
     */
    public static ServerPacket bytesToServerPacket(ByteBuf byteBuf, ConnectionState state) {
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.getBytes(byteBuf.readerIndex(), bytes);
        NetworkBuffer buffer = NetworkBuffer.wrap(bytes, 0, bytes.length, MinecraftServer.process());
        int packetId = buffer.read(NetworkBuffer.VAR_INT);
        return PacketVanilla.SERVER_PACKET_PARSER.stateRegistry(state).create(packetId, buffer);
    }

    /** Maps PE's connection state enum onto Minestom's (identical constants). */
    public static ConnectionState toMinestomState(com.github.retrooper.packetevents.protocol.ConnectionState state) {
        return ConnectionState.valueOf(state.name());
    }
}
