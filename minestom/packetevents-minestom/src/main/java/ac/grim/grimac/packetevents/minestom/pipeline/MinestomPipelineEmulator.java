package ac.grim.grimac.packetevents.minestom.pipeline;

import ac.grim.grimac.packetevents.minestom.channel.GrimMinestomChannel;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.EventCreationUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;

/**
 * Re-implements the listener/re-encode/post-task phases of PacketEvents'
 * {@code PacketEventsImplHelper} so the bridge can interleave packets Grim
 * writes from inside listeners (PRE) and from post-send tasks (POST) around
 * the packet being processed — the ordering Grim's transaction sandwiches
 * rely on. A netty platform gets this ordering for free from the pipeline;
 * here it must be reproduced explicitly.
 *
 * <p>Every call must happen while holding the channel lock.</p>
 */
public final class MinestomPipelineEmulator {

    private MinestomPipelineEmulator() {
    }

    /** Result of running a packet through the PE listener chain. */
    public record Outcome(boolean cancelled, boolean modified, ByteBuf finalBuffer, @Nullable Runnable postTasks) {
    }

    /**
     * Runs a clientbound packet through PE listeners. The returned post-task
     * runnable MUST be executed by the caller after the packet's bytes have
     * been handed to the connection (that is Netty's "after send" moment).
     */
    public static Outcome clientBound(GrimMinestomChannel channel, ByteBuf buffer) {
        User user = channel.user();
        PacketSendEvent event = EventCreationUtil.createSendEvent(
                channel, user, channel.player(), buffer, false);
        PacketEvents.getAPI().getEventManager().callEvent(event, () -> reEncode(event, buffer));

        ByteBuf finalBuffer = event.isCancelled() ? null : (ByteBuf) event.getByteBuf();
        Runnable postTasks = event.hasTasksAfterSend()
                ? () -> event.getTasksAfterSend().forEach(Runnable::run)
                : null;
        return new Outcome(event.isCancelled(), event.needsReEncode(), finalBuffer, postTasks);
    }

    /**
     * Runs a serverbound packet through PE listeners; Grim's checks execute
     * inside this call. Post tasks run before returning, matching netty
     * semantics where they execute after the handler chain for the packet.
     */
    public static Outcome serverBound(GrimMinestomChannel channel, ByteBuf buffer) {
        User user = channel.user();
        PacketReceiveEvent event = EventCreationUtil.createReceiveEvent(
                channel, user, channel.player(), buffer, false);
        PacketEvents.getAPI().getEventManager().callEvent(event, () -> reEncode(event, buffer));

        if (event.hasPostTasks()) {
            event.getPostTasks().forEach(Runnable::run);
        }
        ByteBuf finalBuffer = event.isCancelled() ? null : (ByteBuf) event.getByteBuf();
        return new Outcome(event.isCancelled(), event.needsReEncode(), finalBuffer, null);
    }

    /**
     * PE's post-call hook: when a listener grabbed a wrapper and marked the
     * event for re-encoding, the wrapper's current field values are written
     * back over the buffer (id + payload), exactly like
     * {@code PacketEventsImplHelper} does after the listener chain.
     */
    private static void reEncode(com.github.retrooper.packetevents.event.ProtocolPacketEvent event, ByteBuf buffer) {
        if (event.needsReEncode() && event.getLastUsedWrapper() != null) {
            ByteBufHelper.clear(event.getByteBuf());
            event.getLastUsedWrapper().writeVarInt(event.getPacketId());
            event.getLastUsedWrapper().write();
        }
    }
}
