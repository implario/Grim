package ac.grim.grimac.platform.minestom.manager;

import ac.grim.grimac.platform.api.PlatformPlugin;
import ac.grim.grimac.platform.api.manager.PlatformPluginManager;

/**
 * Minestom has no plugin system — the server is a single application with Grim
 * embedded as a library, so Grim itself is the only "plugin" ever reported.
 */
public final class MinestomPlatformPluginManager implements PlatformPluginManager {

    private final PlatformPlugin grimPlugin;

    public MinestomPlatformPluginManager(String version) {
        this.grimPlugin = new PlatformPlugin() {
            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public String getName() {
                return "GrimAC";
            }

            @Override
            public String getVersion() {
                return version;
            }
        };
    }

    @Override
    public PlatformPlugin[] getPlugins() {
        return new PlatformPlugin[]{grimPlugin};
    }

    @Override
    public PlatformPlugin getPlugin(String pluginName) {
        return grimPlugin.getName().equalsIgnoreCase(pluginName) ? grimPlugin : null;
    }
}
