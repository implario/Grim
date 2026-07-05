package ac.grim.grimac.platform.minestom.api;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.GrimAbstractAPI;
import ac.grim.grimac.packetevents.minestom.MinestomPacketEventsConfig;
import ac.grim.grimac.platform.minestom.GrimMinestomLoader;
import ac.grim.grimac.platform.minestom.GrimMinestomPlatform;
import ac.grim.grimac.platform.minestom.MinestomPlatformServices;
import ac.grim.grimac.platform.minestom.initables.MinestomTickEndEvent;
import ac.grim.grimac.platform.minestom.scheduler.MinestomPlatformScheduler;
import ac.grim.grimac.platform.minestom.sender.MinestomSenderFactory;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerFlag;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Entry point for embedding Grim into a Minestom server application.
 *
 * <pre>{@code
 * GrimMinestom grim = GrimMinestom.builder(Path.of("config/grimac"))
 *         .permissionResolver((sender, node) -> myPerms.check(sender, node))
 *         .build();
 * // after MinecraftServer.init(...)
 * grim.enable();
 * }</pre>
 *
 * The server MUST run with {@code -Dminestom.viewable-packet=false}: with
 * viewable packet grouping enabled Minestom broadcasts entity packets as
 * pre-serialized buffers that bypass the per-player packet events Grim's
 * packet bridge is built on, leaving the anticheat blind to other entities.
 */
public final class GrimMinestom {

    private final GrimMinestomLoader loader;
    private boolean enabled;

    private GrimMinestom(Builder builder) {
        if (ServerFlag.VIEWABLE_PACKET) {
            throw new IllegalStateException(
                    "GrimAC requires -Dminestom.viewable-packet=false. Viewable packet grouping "
                            + "sends entity broadcasts as raw buffers that bypass PlayerPacketOutEvent, "
                            + "so Grim cannot track entities with it enabled.");
        }

        EventNode<Event> grimNode = EventNode.all("grimac");
        Objects.requireNonNullElseGet(builder.parentNode, MinecraftServer::getGlobalEventHandler)
                .addChild(grimNode);
        GrimMinestomPlatform.init(grimNode);

        MinestomSenderFactory senderFactory = new MinestomSenderFactory();
        MinestomPlatformServices.init(senderFactory, builder.permissionResolver, builder.clientVersionResolver);

        MinestomPacketEventsConfig packetEventsConfig = new MinestomPacketEventsConfig(
                grimNode,
                player -> MinestomPlatformServices.clientVersionResolver().resolve(player));

        this.loader = new GrimMinestomLoader(builder.dataDirectory.toFile(), senderFactory, packetEventsConfig);
        GrimAPI.INSTANCE.load(loader, new MinestomTickEndEvent());
    }

    public static Builder builder(@NotNull Path dataDirectory) {
        return new Builder(dataDirectory);
    }

    /**
     * Registers Grim's commands and starts the anticheat. Call once during
     * server bootstrap, after {@code MinecraftServer.init(...)}.
     */
    public synchronized void enable() {
        if (enabled) return;
        loader.getCommandService().registerCommands();
        GrimAPI.INSTANCE.start();
        enabled = true;
        MinecraftServer.getSchedulerManager().buildShutdownTask(this::disable);
    }

    /**
     * Stops the anticheat and releases Grim's scheduler threads. Runs
     * automatically on server shutdown once {@link #enable()} has been called.
     */
    public synchronized void disable() {
        if (!enabled) return;
        enabled = false;
        GrimAPI.INSTANCE.stop();
        ((MinestomPlatformScheduler) loader.getScheduler()).shutdown();
    }

    /**
     * Grim's public API: alert/flag event bus, exemptions and player handles,
     * for wiring the network's own infrastructure (Discord alerts, punishments).
     */
    public GrimAbstractAPI api() {
        return GrimAPI.INSTANCE.getExternalAPI();
    }

    public static final class Builder {

        private final Path dataDirectory;
        private @Nullable EventNode<Event> parentNode;
        private PermissionResolver permissionResolver = PermissionResolver.DEFAULTS_ONLY;
        private ClientVersionResolver clientVersionResolver;

        private Builder(@NotNull Path dataDirectory) {
            this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
            this.clientVersionResolver = player ->
                    com.github.retrooper.packetevents.PacketEvents.getAPI()
                            .getServerManager().getVersion().toClientVersion();
        }

        /** Event node to attach Grim's listeners under; defaults to the global handler. */
        public Builder eventNode(@NotNull EventNode<Event> parentNode) {
            this.parentNode = Objects.requireNonNull(parentNode);
            return this;
        }

        /** Bridges permission checks to the network's own permission system. */
        public Builder permissionResolver(@NotNull PermissionResolver permissionResolver) {
            this.permissionResolver = Objects.requireNonNull(permissionResolver);
            return this;
        }

        /** Supplies real client versions when a proxy translates newer clients. */
        public Builder clientVersionResolver(@NotNull ClientVersionResolver clientVersionResolver) {
            this.clientVersionResolver = Objects.requireNonNull(clientVersionResolver);
            return this;
        }

        public GrimMinestom build() {
            return new GrimMinestom(this);
        }
    }
}
