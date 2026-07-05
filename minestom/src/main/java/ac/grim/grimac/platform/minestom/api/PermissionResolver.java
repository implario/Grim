package ac.grim.grimac.platform.minestom.api;

import net.kyori.adventure.util.TriState;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * Bridges Grim's permission checks to the network's own permission system.
 * <p>
 * Minestom ships no permission API, so the embedding server application decides
 * what a permission node means. Return {@link TriState#NOT_SET} to fall back to
 * Grim's registered default for the node (console = allowed, players = the
 * node's {@code PermissionDefaultValue}).
 */
@FunctionalInterface
public interface PermissionResolver {

    PermissionResolver DEFAULTS_ONLY = (sender, node) -> TriState.NOT_SET;

    @NotNull TriState check(@NotNull CommandSender sender, @NotNull String node);
}
