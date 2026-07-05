package ac.grim.grimac.platform.minestom.resolver;

import ac.grim.grimac.api.plugin.BasicGrimPlugin;
import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.internal.plugin.resolver.GrimExtensionManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Minestom has no mod/plugin container to resolve against, so every resolution
 * context maps to the single embedded Grim instance.
 */
public final class MinestomResolverRegistrar {

    private final GrimPlugin grimPlugin;

    public MinestomResolverRegistrar(File dataFolder) {
        this.grimPlugin = new BasicGrimPlugin(
                Logger.getLogger("GrimAC"),
                dataFolder,
                readVersion(),
                "Grim Anticheat (Minestom)",
                List.of("GrimAC contributors")
        );
    }

    public void registerAll(GrimExtensionManager extensionManager) {
        extensionManager.registerResolver(context -> grimPlugin);
    }

    public GrimPlugin plugin() {
        return grimPlugin;
    }

    public static String readVersion() {
        try (InputStream stream = MinestomResolverRegistrar.class.getClassLoader()
                .getResourceAsStream("grimac.properties")) {
            if (stream != null) {
                Properties properties = new Properties();
                properties.load(stream);
                String version = properties.getProperty("version");
                if (version != null && !version.contains("${")) return version;
            }
        } catch (IOException ignored) {
        }
        return "unknown";
    }
}
