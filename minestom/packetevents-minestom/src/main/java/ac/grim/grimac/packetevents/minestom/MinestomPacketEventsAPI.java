package ac.grim.grimac.packetevents.minestom;

import ac.grim.grimac.packetevents.minestom.bridge.MinestomPacketBridge;
import ac.grim.grimac.packetevents.minestom.manager.MinestomChannelInjector;
import ac.grim.grimac.packetevents.minestom.manager.MinestomNettyManager;
import ac.grim.grimac.packetevents.minestom.manager.MinestomPlayerManager;
import ac.grim.grimac.packetevents.minestom.manager.MinestomProtocolManager;
import ac.grim.grimac.packetevents.minestom.manager.MinestomServerManager;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.injector.ChannelInjector;
import com.github.retrooper.packetevents.manager.InternalPacketListener;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.manager.protocol.ProtocolManager;
import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.netty.NettyManager;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;

/**
 * PacketEvents platform implementation for Minestom. There is no Netty
 * pipeline to inject into; packets are intercepted through Minestom's
 * {@code PlayerPacketEvent}/{@code PlayerPacketOutEvent} and replayed through
 * the PE listener chain (see {@link MinestomPacketBridge}).
 */
public class MinestomPacketEventsAPI extends PacketEventsAPI<MinestomPacketEventsConfig> {

    private final MinestomPacketEventsConfig config;
    private final PacketEventsSettings settings = new PacketEventsSettings();
    private final MinestomServerManager serverManager = new MinestomServerManager();
    private final MinestomProtocolManager protocolManager = new MinestomProtocolManager();
    private final MinestomPlayerManager playerManager = new MinestomPlayerManager();
    private final MinestomNettyManager nettyManager = new MinestomNettyManager();
    private final MinestomChannelInjector injector = new MinestomChannelInjector();
    private final MinestomPacketBridge bridge;

    private boolean loaded;
    private boolean initialized;
    private boolean terminated;

    MinestomPacketEventsAPI(MinestomPacketEventsConfig config) {
        this.config = config;
        this.bridge = new MinestomPacketBridge(config);
    }

    @Override
    public void load() {
        if (loaded) return;
        loaded = true;
        // Resolve (and thereby validate) the protocol mapping eagerly.
        serverManager.getVersion();
        // PE's internal listener drives connection-state transitions and
        // registry capture, exactly as on Netty platforms.
        getEventManager().registerListener(new InternalPacketListener(), PacketListenerPriority.LOWEST);
        // Minestom listeners can attach before the server starts; doing it in
        // load() keeps the bridge independent of whether init() is called.
        bridge.register(config);
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public void init() {
        load();
        initialized = true;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void terminate() {
        if (terminated) return;
        terminated = true;
        initialized = false;
        getEventManager().unregisterAllListeners();
    }

    @Override
    public boolean isTerminated() {
        return terminated;
    }

    @Override
    public MinestomPacketEventsConfig getPlugin() {
        return config;
    }

    @Override
    public ServerManager getServerManager() {
        return serverManager;
    }

    @Override
    public ProtocolManager getProtocolManager() {
        return protocolManager;
    }

    @Override
    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    @Override
    public NettyManager getNettyManager() {
        return nettyManager;
    }

    @Override
    public ChannelInjector getInjector() {
        return injector;
    }

    @Override
    public PacketEventsSettings getSettings() {
        return settings;
    }
}
