package ac.grim.grimac.platform.minestom.command;

import ac.grim.grimac.platform.api.command.PlayerSelector;
import ac.grim.grimac.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.grim.grimac.platform.api.sender.Sender;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class MinestomCloudCommandArguments implements CloudPlatformCommandArguments {

    private static final SuggestionProvider<Sender> ONLINE_PLAYERS = SuggestionProvider.blocking(
            (context, input) -> MinecraftServer.getConnectionManager().getOnlinePlayers().stream()
                    .map(player -> Suggestion.suggestion(player.getUsername()))
                    .toList());

    @Override
    public ParserDescriptor<Sender, PlayerSelector> singlePlayerSelectorParser() {
        return ParserDescriptor.of(new SinglePlayerParser(), PlayerSelector.class);
    }

    @Override
    public SuggestionProvider<Sender> onlinePlayerSuggestions() {
        return ONLINE_PLAYERS;
    }

    private static final class SinglePlayerParser implements ArgumentParser<Sender, PlayerSelector> {

        @Override
        public @NotNull ArgumentParseResult<@NotNull PlayerSelector> parse(
                @NotNull CommandContext<@NotNull Sender> commandContext,
                @NotNull CommandInput commandInput
        ) {
            String token = commandInput.readString();
            Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUsername(token);
            if (player == null) {
                try {
                    player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(UUID.fromString(token));
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (player == null) {
                return ArgumentParseResult.failure(new IllegalArgumentException("Player '" + token + "' is not online"));
            }
            return ArgumentParseResult.success(new MinestomPlayerSelector(player, token));
        }

        @Override
        public @NotNull SuggestionProvider<Sender> suggestionProvider() {
            return ONLINE_PLAYERS;
        }
    }

}
