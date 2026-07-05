package ac.grim.grimac.packetevents.minestom.bridge;

import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.common.DisconnectPacket;
import net.minestom.server.network.packet.server.common.KeepAlivePacket;
import net.minestom.server.network.packet.server.common.PingPacket;
import net.minestom.server.network.packet.server.common.PingResponsePacket;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;
import net.minestom.server.network.packet.server.common.TagsPacket;
import net.minestom.server.network.packet.server.play.*;

import java.util.Set;

/**
 * Clientbound packet types Grim listens to. Only these are run through the
 * PE pipeline (and pay the cancel/re-enqueue pass); everything else — chat,
 * scoreboards, sounds, titles — bypasses the bridge untouched.
 *
 * <p>The set mirrors the packet types Grim's common listeners consume:
 * world state for CompensatedWorld, entity tracking for reach/knockback,
 * player state (teleports, velocity, abilities, effects), inventory sync,
 * and the transaction/keepalive machinery.</p>
 */
public final class OutgoingPacketFilter {

    private static final Set<Class<? extends ServerPacket>> RELEVANT = Set.of(
            // Transactions / timing
            PingPacket.class,
            PingResponsePacket.class,
            KeepAlivePacket.class,
            DisconnectPacket.class,
            PluginMessagePacket.class,
            // Join / respawn / world switches
            JoinGamePacket.class,
            RespawnPacket.class,
            ChangeGameStatePacket.class,
            SpawnPositionPacket.class,
            TagsPacket.class,
            ServerDifficultyPacket.class,
            // Player state
            PlayerPositionAndLookPacket.class,
            PlayerRotationPacket.class,
            PlayerAbilitiesPacket.class,
            UpdateHealthPacket.class,
            SetExperiencePacket.class,
            HeldItemChangePacket.class,
            EntityStatusPacket.class,
            SetCooldownPacket.class,
            // Knockback / explosions
            EntityVelocityPacket.class,
            ExplosionPacket.class,
            // World state
            ChunkDataPacket.class,
            ChunkBiomesPacket.class,
            UnloadChunkPacket.class,
            BlockChangePacket.class,
            MultiBlockChangePacket.class,
            BlockActionPacket.class,
            AcknowledgeBlockChangePacket.class,
            WorldEventPacket.class,
            // Entity tracking
            SpawnEntityPacket.class,
            DestroyEntitiesPacket.class,
            EntityPositionPacket.class,
            EntityPositionAndRotationPacket.class,
            EntityPositionSyncPacket.class,
            EntityRotationPacket.class,
            EntityHeadLookPacket.class,
            EntityTeleportPacket.class,
            EntityMetaDataPacket.class,
            EntityAttributesPacket.class,
            EntityEffectPacket.class,
            RemoveEntityEffectPacket.class,
            EntityEquipmentPacket.class,
            SetPassengersPacket.class,
            AttachEntityPacket.class,
            VehicleMovePacket.class,
            MoveMinecartPacket.class,
            // Inventory
            WindowItemsPacket.class,
            SetSlotPacket.class,
            SetCursorItemPacket.class,
            SetPlayerInventorySlotPacket.class,
            OpenWindowPacket.class,
            CloseWindowPacket.class,
            OpenHorseWindowPacket.class,
            // World border affects collision
            InitializeWorldBorderPacket.class,
            WorldBorderCenterPacket.class,
            WorldBorderLerpSizePacket.class,
            WorldBorderSizePacket.class
    );

    private OutgoingPacketFilter() {
    }

    public static boolean isRelevant(ServerPacket packet) {
        return RELEVANT.contains(packet.getClass());
    }
}
