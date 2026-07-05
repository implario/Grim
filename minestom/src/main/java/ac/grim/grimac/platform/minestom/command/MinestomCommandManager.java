package ac.grim.grimac.platform.minestom.command;

import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.minestom.MinestomPlatformServices;
import ac.grim.grimac.utils.anticheat.LogUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.ArgumentStringArray;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.internal.CommandRegistrationHandler;
import org.incendo.cloud.suggestion.Suggestion;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges Cloud onto Minestom's command system. Each Cloud root command is
 * registered as a Minestom {@link net.minestom.server.command.builder.Command}
 * whose single greedy string-array syntax funnels raw input back into Cloud
 * for parsing, execution and suggestions.
 */
public final class MinestomCommandManager extends CommandManager<Sender> {

    private MinestomCommandManager(MinestomRegistrationHandler handler) {
        super(ExecutionCoordinator.simpleCoordinator(), handler);
        handler.manager = this;
    }

    public static MinestomCommandManager create() {
        return new MinestomCommandManager(new MinestomRegistrationHandler());
    }

    @Override
    public boolean hasPermission(Sender sender, String permission) {
        return permission.isEmpty() || sender.hasPermission(permission);
    }

    private void executeFromMinestom(CommandSender minestomSender, String input) {
        Sender sender = MinestomPlatformServices.senderFactory().wrap(minestomSender);
        commandExecutor().executeCommand(sender, input).whenComplete((result, throwable) -> {
            // Cloud's exception controller already messaged the sender; only
            // truly unexpected failures are worth surfacing in the log.
            if (throwable != null && !(throwable.getCause() instanceof RuntimeException)) {
                LogUtil.warn("Unexpected error executing '" + input + "': " + throwable);
            }
        });
    }

    private static final class MinestomRegistrationHandler implements CommandRegistrationHandler<Sender> {

        private MinestomCommandManager manager;
        private final Set<String> registeredRoots = ConcurrentHashMap.newKeySet();

        @Override
        public boolean registerCommand(org.incendo.cloud.Command<Sender> command) {
            String root = command.rootComponent().name();
            if (!registeredRoots.add(root)) {
                // The Minestom command already funnels every syntax of this
                // root into Cloud; nothing further to register.
                return true;
            }

            net.minestom.server.command.builder.Command minestomCommand =
                    new net.minestom.server.command.builder.Command(root);

            minestomCommand.setDefaultExecutor((sender, context) ->
                    manager.executeFromMinestom(sender, context.getInput()));

            ArgumentStringArray args = ArgumentType.StringArray("args");
            args.setSuggestionCallback((sender, context, suggestion) -> {
                Sender cloudSender = MinestomPlatformServices.senderFactory().wrap(sender);
                String input = context.getInput();
                try {
                    for (Suggestion cloudSuggestion : manager.suggestionFactory()
                            .suggestImmediately(cloudSender, input).list()) {
                        suggestion.addEntry(new SuggestionEntry(cloudSuggestion.suggestion()));
                    }
                } catch (Exception e) {
                    LogUtil.warn("Suggestion resolution failed for '" + input + "': " + e);
                }
            });

            minestomCommand.addSyntax((sender, context) ->
                    manager.executeFromMinestom(sender, context.getInput()), args);

            MinecraftServer.getCommandManager().register(minestomCommand);
            return true;
        }
    }
}
