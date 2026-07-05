package ac.grim.grimac.platform.minestom.sender;

import ac.grim.grimac.platform.api.permissions.PermissionDefaultValue;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.api.sender.SenderFactory;
import ac.grim.grimac.platform.minestom.MinestomPlatformServices;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wraps Minestom's {@link CommandSender}. Permission resolution order: the
 * network's {@link ac.grim.grimac.platform.minestom.api.PermissionResolver}
 * wins when it answers; otherwise the node's registered
 * {@link PermissionDefaultValue} decides; unregistered nodes are denied for
 * players (console always passes via {@link Sender}'s console short-circuit).
 */
public final class MinestomSenderFactory extends SenderFactory<CommandSender> {

    private final Map<String, PermissionDefaultValue> permissionDefaults = new ConcurrentHashMap<>();

    public void registerPermissionDefault(String permission, PermissionDefaultValue defaultValue) {
        permissionDefaults.put(permission, defaultValue);
    }

    @Override
    protected UUID getUniqueId(CommandSender sender) {
        if (sender instanceof Player player) return player.getUuid();
        return Sender.CONSOLE_UUID;
    }

    @Override
    protected String getName(CommandSender sender) {
        if (sender instanceof Player player) return player.getUsername();
        return Sender.CONSOLE_NAME;
    }

    @Override
    protected void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(message);
    }

    @Override
    protected void sendMessage(CommandSender sender, Component message) {
        sender.sendMessage(message);
    }

    @Override
    protected boolean hasPermission(CommandSender sender, String node) {
        TriState resolved = MinestomPlatformServices.permissionResolver().check(sender, node);
        if (resolved != TriState.NOT_SET) return resolved == TriState.TRUE;
        PermissionDefaultValue defaultValue = permissionDefaults.get(node);
        if (defaultValue == null) return sender instanceof ConsoleSender;
        return resolve(defaultValue, sender);
    }

    @Override
    protected boolean hasPermission(CommandSender sender, String node, boolean defaultIfUnset) {
        TriState resolved = MinestomPlatformServices.permissionResolver().check(sender, node);
        if (resolved != TriState.NOT_SET) return resolved == TriState.TRUE;
        PermissionDefaultValue defaultValue = permissionDefaults.get(node);
        if (defaultValue == null) return defaultIfUnset;
        return resolve(defaultValue, sender);
    }

    private boolean resolve(PermissionDefaultValue defaultValue, CommandSender sender) {
        return switch (defaultValue) {
            case TRUE -> true;
            case FALSE -> false;
            // Minestom has no operator concept; only the network's resolver can
            // elevate a player, so OP-default nodes stay denied for players.
            case OP -> sender instanceof ConsoleSender;
            case NOT_OP -> !(sender instanceof ConsoleSender);
        };
    }

    @Override
    protected void performCommand(CommandSender sender, String command) {
        MinecraftServer.getCommandManager().execute(sender, command);
    }

    @Override
    protected boolean isConsole(CommandSender sender) {
        return sender instanceof ConsoleSender;
    }

    @Override
    protected boolean isPlayer(CommandSender sender) {
        return sender instanceof Player;
    }
}
