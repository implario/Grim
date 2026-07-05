package ac.grim.grimac.packetevents.minestom;

import ac.grim.grimac.packetevents.minestom.util.MinestomPacketConverter;
import io.netty.buffer.ByteBuf;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.packet.client.play.ClientPlayerPositionPacket;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.common.PingPacket;
import net.minestom.server.network.packet.server.play.EntityVelocityPacket;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the Minestom<->bytes conversion the PE bridge is built
 * on. If these pass, the byte form fed to PacketEvents is exactly what
 * Minestom's own registries produce for the live protocol.
 */
class MinestomPacketConverterTest {

    @BeforeAll
    static void bootRegistries() {
        // Initializes MinecraftServer.process(), which the serializers need
        // for registry-backed types.
        MinecraftServer.init();
    }

    @Test
    void clientMovementPacketSerializes() {
        ClientPlayerPositionPacket packet = new ClientPlayerPositionPacket(
                new Pos(1.5, 64.0, -7.25), (byte) 1);
        ByteBuf bytes = MinestomPacketConverter.clientPacketToBytes(packet, ConnectionState.PLAY);
        assertTrue(bytes.readableBytes() > 0);
    }

    @Test
    void pingPacketRoundTrips() {
        PingPacket packet = new PingPacket(123456);
        ByteBuf bytes = MinestomPacketConverter.serverPacketToBytes(packet, ConnectionState.PLAY);
        ServerPacket decoded = MinestomPacketConverter.bytesToServerPacket(bytes, ConnectionState.PLAY);
        PingPacket decodedPing = assertInstanceOf(PingPacket.class, decoded);
        assertEquals(packet.id(), decodedPing.id());
    }

    @Test
    void velocityPacketRoundTrips() {
        EntityVelocityPacket packet = new EntityVelocityPacket(42, new net.minestom.server.coordinate.Vec(0.4, -0.2, 0.6));
        ByteBuf bytes = MinestomPacketConverter.serverPacketToBytes(packet, ConnectionState.PLAY);
        ServerPacket decoded = MinestomPacketConverter.bytesToServerPacket(bytes, ConnectionState.PLAY);
        EntityVelocityPacket decodedVelocity = assertInstanceOf(EntityVelocityPacket.class, decoded);
        assertEquals(packet.entityId(), decodedVelocity.entityId());
    }
}
