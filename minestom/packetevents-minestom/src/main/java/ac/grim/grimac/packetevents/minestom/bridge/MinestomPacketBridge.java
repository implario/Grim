package ac.grim.grimac.packetevents.minestom.bridge;

import ac.grim.grimac.packetevents.minestom.MinestomPacketEventsConfig;
import ac.grim.grimac.packetevents.minestom.channel.GrimMinestomChannel;
import ac.grim.grimac.packetevents.minestom.channel.MinestomChannels;
import ac.grim.grimac.packetevents.minestom.pipeline.MinestomPipelineEmulator;
import ac.grim.grimac.packetevents.minestom.util.MinestomPacketConverter;
import io.netty.buffer.ByteBuf;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.event.player.PlayerPacketOutEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.packet.client.ClientPacket;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.StartConfigurationPacket;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The heart of the bridge: hooks Minestom's packet events and drives the PE
 * pipeline emulation.
 *
 * <p><b>Serverbound</b> ({@code PlayerPacketEvent}, tick thread): serialize
 * the packet, run PE listeners (Grim's checks execute here), map a PE cancel
 * onto the Minestom event.</p>
 *
 * <p><b>Clientbound</b> ({@code PlayerPacketOutEvent}, connection write
 * thread): packets Grim cares about are always cancelled and re-enqueued as
 * an ordered group {@code [PRE-writes…, packet, POST-writes…]} so that the
 * wire order of Grim-relevant packets equals the PE event order — the
 * property Grim's transaction sandwich depends on. Grim-irrelevant packets
 * pass through untouched; config/login-state packets are tapped read-only so
 * PE's internals see registries and state transitions without the bridge
 * interfering with Minestom's connection state machine.</p>
 */
public final class MinestomPacketBridge {

    private final MinestomUserLifecycle lifecycle;
    private final AtomicBoolean warnedIncomingModification = new AtomicBoolean();

    public MinestomPacketBridge(MinestomPacketEventsConfig config) {
        this.lifecycle = new MinestomUserLifecycle(config.clientVersionFunction());
    }

    public void register(MinestomPacketEventsConfig config) {
        var node = config.eventNode();
        node.addListener(AsyncPlayerConfigurationEvent.class, event -> lifecycle.onPlayerConfiguration(event.getPlayer()));
        node.addListener(PlayerSpawnEvent.class, event -> {
            if (event.isFirstSpawn()) lifecycle.onFirstSpawn(event.getPlayer());
        });
        node.addListener(PlayerDisconnectEvent.class, event -> lifecycle.onDisconnect(event.getPlayer()));
        node.addListener(PlayerPacketEvent.class, this::onServerbound);
        node.addListener(PlayerPacketOutEvent.class, this::onClientbound);
    }

    // ---------------------------------------------------------------- in --

    private void onServerbound(PlayerPacketEvent event) {
        Player player = event.getPlayer();
        GrimMinestomChannel channel = MinestomChannels.of(player);
        if (channel == null || channel.user() == null || !channel.isOpen()) return;

        ClientPacket packet = event.getPacket();
        channel.lock().lock();
        try {
            // PlayerPacketEvent only fires for PLAY-state packets.
            channel.user().setDecoderState(com.github.retrooper.packetevents.protocol.ConnectionState.PLAY);
            ByteBuf bytes = MinestomPacketConverter.clientPacketToBytes(packet, ConnectionState.PLAY);
            MinestomPipelineEmulator.Outcome outcome = MinestomPipelineEmulator.serverBound(channel, bytes);
            if (outcome.cancelled()) {
                event.setCancelled(true);
            } else if (outcome.modified() && warnedIncomingModification.compareAndSet(false, true)) {
                // True modification support needs cancel + reprocess of the
                // rewritten packet, deferred past MVP.
                org.slf4j.LoggerFactory.getLogger(MinestomPacketBridge.class).warn(
                        "A PacketEvents listener modified an incoming packet; the Minestom bridge "
                                + "delivers the original bytes (modification unsupported, reported once).");
            }
        } finally {
            channel.lock().unlock();
        }
    }

    // --------------------------------------------------------------- out --

    private void onClientbound(PlayerPacketOutEvent event) {
        Player player = event.getPlayer();
        GrimMinestomChannel channel = MinestomChannels.of(player);
        if (channel == null || channel.user() == null || !channel.isOpen()) return;

        ServerPacket packet = event.getPacket();

        channel.lock().lock();
        try {
            // Second pass of a packet this bridge already processed and re-enqueued.
            if (channel.consumePassthrough(packet)) return;

            ConnectionState wireState = channel.connection().getServerState();

            // Login/configuration traffic and the PLAY->CONFIGURATION switch are
            // tapped read-only: PE must see them (registry data, state flips) but
            // the bridge must not disturb Minestom's connection state machine.
            if (wireState != ConnectionState.PLAY || packet instanceof StartConfigurationPacket) {
                syncEncoderState(channel, wireState);
                tap(channel, packet, wireState);
                return;
            }

            if (!OutgoingPacketFilter.isRelevant(packet)) return;

            syncEncoderState(channel, wireState);
            processRelevant(event, channel, packet, wireState);
        } finally {
            channel.lock().unlock();
        }
    }

    /** PE sees the packet; cancellation/modification is deliberately ignored. */
    private void tap(GrimMinestomChannel channel, ServerPacket packet, ConnectionState state) {
        ByteBuf bytes = MinestomPacketConverter.serverPacketToBytes(packet, state);
        MinestomPipelineEmulator.Outcome outcome = MinestomPipelineEmulator.clientBound(channel, bytes);
        if (outcome.postTasks() != null) {
            outcome.postTasks().run();
        }
    }

    /**
     * Full ordering machinery: cancel the original write, run PE listeners,
     * then re-enqueue {@code PRE…, packet', POST…} in one atomic sequence.
     * Every Grim-relevant packet defers exactly one queue pass, so relative
     * wire order among relevant packets matches PE event order.
     */
    private void processRelevant(PlayerPacketOutEvent event, GrimMinestomChannel channel,
                                 ServerPacket packet, ConnectionState state) {
        ByteBuf bytes = MinestomPacketConverter.serverPacketToBytes(packet, state);

        GrimMinestomChannel.OutgoingContext context = channel.openOutgoingContext();
        try {
            MinestomPipelineEmulator.Outcome outcome = MinestomPipelineEmulator.clientBound(channel, bytes);

            event.setCancelled(true);

            for (ServerPacket pre : context.preWrites()) {
                channel.connection().sendPacket(pre);
            }

            if (!outcome.cancelled() && outcome.finalBuffer() != null) {
                ServerPacket toSend = outcome.modified()
                        ? MinestomPacketConverter.bytesToServerPacket(outcome.finalBuffer(), state)
                        : packet; // untouched by listeners: reuse, skip a decode
                channel.markPassthrough(toSend);
                channel.connection().sendPacket(toSend);
            }

            if (outcome.postTasks() != null) {
                context.enterPostPhase();
                outcome.postTasks().run();
            }
            for (ServerPacket post : context.postWrites()) {
                channel.connection().sendPacket(post);
            }
        } finally {
            channel.closeOutgoingContext();
        }
    }

    private static void syncEncoderState(GrimMinestomChannel channel, ConnectionState wireState) {
        channel.user().setEncoderState(
                com.github.retrooper.packetevents.protocol.ConnectionState.valueOf(wireState.name()));
    }
}
