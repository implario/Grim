package ac.grim.grimac.packetevents.minestom.channel;

import com.github.retrooper.packetevents.protocol.player.User;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.player.PlayerConnection;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The bridge's stand-in for a Netty channel. PacketEvents treats channels as
 * opaque {@code Object}s and performs every operation through the
 * {@code ChannelOperator}/{@code ProtocolManager} the platform provides, so
 * this class only has to carry per-player bridge state:
 *
 * <ul>
 *   <li>{@link #lock} serializes every entry into the PE pipeline for this
 *       player (Minestom tick threads, the connection write thread and Grim's
 *       timers all call in), reproducing the per-player ordering guarantee a
 *       real Netty event loop provides;</li>
 *   <li>{@link #passthrough} marks Minestom packets that already went through
 *       PE processing so their {@code PlayerPacketOutEvent} pass is a no-op;</li>
 *   <li>the injection context collects packets Grim writes from inside an
 *       outgoing packet event so they can be wired in exact PRE/POST order
 *       around the packet being processed.</li>
 * </ul>
 */
public final class GrimMinestomChannel {

    private final Player player;
    private final ReentrantLock lock = new ReentrantLock();
    private final ExecutorService eventLoop;
    private final Set<ServerPacket> passthrough = Collections.newSetFromMap(new IdentityHashMap<>());

    private volatile User user;
    private volatile boolean open = true;

    /** Non-null while a clientbound packet is being run through PE listeners. */
    private OutgoingContext outgoingContext;

    public GrimMinestomChannel(Player player) {
        this.player = player;
        this.eventLoop = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "grim-channel-" + player.getUsername());
            thread.setDaemon(true);
            return thread;
        });
    }

    public Player player() {
        return player;
    }

    public PlayerConnection connection() {
        return player.getPlayerConnection();
    }

    public ReentrantLock lock() {
        return lock;
    }

    public User user() {
        return user;
    }

    public void bindUser(User user) {
        this.user = user;
    }

    public boolean isOpen() {
        return open && connection().isOnline();
    }

    public void markClosed() {
        open = false;
        eventLoop.shutdown();
    }

    /**
     * Runs a task on this channel's serialized executor under the channel
     * lock — the bridge's equivalent of Netty's {@code runInEventLoop}.
     */
    public void runInEventLoop(Runnable task) {
        if (eventLoop.isShutdown()) return;
        eventLoop.execute(() -> {
            lock.lock();
            try {
                task.run();
            } finally {
                lock.unlock();
            }
        });
    }

    /** Marks a packet as already PE-processed. Caller must hold the lock. */
    public void markPassthrough(ServerPacket packet) {
        lock.lock();
        try {
            passthrough.add(packet);
        } finally {
            lock.unlock();
        }
    }

    /** Removes and reports a passthrough mark. Caller must hold the lock. */
    public boolean consumePassthrough(ServerPacket packet) {
        return passthrough.remove(packet);
    }

    public OutgoingContext outgoingContext() {
        return outgoingContext;
    }

    public OutgoingContext openOutgoingContext() {
        OutgoingContext context = new OutgoingContext();
        this.outgoingContext = context;
        return context;
    }

    public void closeOutgoingContext() {
        this.outgoingContext = null;
    }

    /**
     * Collects packets written re-entrantly from PE listeners while a
     * clientbound packet is processed. Writes during the listener phase land
     * before the processed packet on the wire; writes from post-send tasks
     * land after it.
     */
    public static final class OutgoingContext {
        private final java.util.List<ServerPacket> pre = new java.util.ArrayList<>(2);
        private final java.util.List<ServerPacket> post = new java.util.ArrayList<>(2);
        private boolean inPostPhase;

        public void enterPostPhase() {
            inPostPhase = true;
        }

        public void collect(ServerPacket packet) {
            (inPostPhase ? post : pre).add(packet);
        }

        public java.util.List<ServerPacket> preWrites() {
            return pre;
        }

        public java.util.List<ServerPacket> postWrites() {
            return post;
        }
    }
}
