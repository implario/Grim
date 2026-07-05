package ac.grim.grimac.platform.minestom;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.GrimAPIProvider;
import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.command.CloudCommandService;
import ac.grim.grimac.internal.plugin.resolver.GrimExtensionManager;
import ac.grim.grimac.packetevents.minestom.MinestomPacketEventsBuilder;
import ac.grim.grimac.packetevents.minestom.MinestomPacketEventsConfig;
import ac.grim.grimac.platform.api.PlatformLoader;
import ac.grim.grimac.platform.api.PlatformServer;
import ac.grim.grimac.platform.api.command.CommandService;
import ac.grim.grimac.platform.api.manager.ItemResetHandler;
import ac.grim.grimac.platform.api.manager.MessagePlaceHolderManager;
import ac.grim.grimac.platform.api.manager.PermissionRegistrationManager;
import ac.grim.grimac.platform.api.manager.PlatformPluginManager;
import ac.grim.grimac.platform.api.player.PlatformPlayerFactory;
import ac.grim.grimac.platform.api.scheduler.PlatformScheduler;
import ac.grim.grimac.platform.api.sender.SenderFactory;
import ac.grim.grimac.platform.minestom.command.MinestomCloudCommandArguments;
import ac.grim.grimac.platform.minestom.command.MinestomCommandManager;
import ac.grim.grimac.platform.minestom.manager.MinestomItemResetHandler;
import ac.grim.grimac.platform.minestom.manager.MinestomMessagePlaceHolderManager;
import ac.grim.grimac.platform.minestom.manager.MinestomPermissionRegistrationManager;
import ac.grim.grimac.platform.minestom.manager.MinestomPlatformPluginManager;
import ac.grim.grimac.platform.minestom.player.MinestomPlatformPlayerFactory;
import ac.grim.grimac.platform.minestom.resolver.MinestomResolverRegistrar;
import ac.grim.grimac.platform.minestom.scheduler.MinestomPlatformScheduler;
import ac.grim.grimac.platform.minestom.sender.MinestomSenderFactory;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.PacketEventsAPI;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public final class GrimMinestomLoader implements PlatformLoader {

    private final MinestomPlatformScheduler scheduler = new MinestomPlatformScheduler();
    private final MinestomPlatformPlayerFactory playerFactory = new MinestomPlatformPlayerFactory();
    private final PacketEventsAPI<?> packetEvents;
    private final MinestomItemResetHandler itemResetHandler = new MinestomItemResetHandler();
    private final MinestomSenderFactory senderFactory;
    private final MinestomPermissionRegistrationManager permissionManager;
    private final MinestomMessagePlaceHolderManager messagePlaceHolderManager = new MinestomMessagePlaceHolderManager();
    private final MinestomPlatformPluginManager pluginManager;
    private final MinestomPlatformServer platformServer;
    private final CommandService commandService;
    private final GrimPlugin plugin;

    public GrimMinestomLoader(File dataFolder, MinestomSenderFactory senderFactory, MinestomPacketEventsConfig packetEventsConfig) {
        this.senderFactory = senderFactory;
        this.permissionManager = new MinestomPermissionRegistrationManager(senderFactory);
        this.packetEvents = MinestomPacketEventsBuilder.build(packetEventsConfig);
        this.platformServer = new MinestomPlatformServer(GrimMinestomPlatform.eventNode());
        this.commandService = createCommandService();

        MinestomResolverRegistrar resolverRegistrar = new MinestomResolverRegistrar(dataFolder);
        GrimExtensionManager extensionManager = GrimAPI.INSTANCE.getExtensionManager();
        resolverRegistrar.registerAll(extensionManager);
        this.plugin = extensionManager.getPlugin("GrimAC");
        this.pluginManager = new MinestomPlatformPluginManager(plugin.getVersion());
    }

    private CommandService createCommandService() {
        try {
            return new CloudCommandService(MinestomCommandManager::create, new MinestomCloudCommandArguments());
        } catch (Throwable t) {
            LogUtil.warn("IMPORTANT: Command framework failed to load (missing Cloud library?). "
                    + "Grim will run without commands enabled!");
            if (!(t instanceof NoClassDefFoundError)) {
                t.printStackTrace();
            }
            return () -> {};
        }
    }

    @Override
    public PlatformScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public PlatformPlayerFactory getPlatformPlayerFactory() {
        return playerFactory;
    }

    @Override
    public PacketEventsAPI<?> getPacketEvents() {
        return packetEvents;
    }

    @Override
    public ItemResetHandler getItemResetHandler() {
        return itemResetHandler;
    }

    @Override
    public CommandService getCommandService() {
        return commandService;
    }

    @Override
    public SenderFactory<?> getSenderFactory() {
        return senderFactory;
    }

    @Override
    public GrimPlugin getPlugin() {
        return plugin;
    }

    @Override
    public PlatformPluginManager getPluginManager() {
        return pluginManager;
    }

    @Override
    public PlatformServer getPlatformServer() {
        return platformServer;
    }

    @Override
    public void registerAPIService() {
        GrimAPIProvider.init(GrimAPI.INSTANCE.getExternalAPI());
    }

    @Override
    public @NotNull MessagePlaceHolderManager getMessagePlaceHolderManager() {
        return messagePlaceHolderManager;
    }

    @Override
    public PermissionRegistrationManager getPermissionManager() {
        return permissionManager;
    }
}
