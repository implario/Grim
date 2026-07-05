package ac.grim.grimac.platform.minestom.command;

import ac.grim.grimac.platform.api.command.PlayerSelector;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.minestom.MinestomPlatformServices;
import net.minestom.server.entity.Player;

import java.util.Collection;
import java.util.List;

public final class MinestomPlayerSelector implements PlayerSelector {

    private final Player player;
    private final String input;

    public MinestomPlayerSelector(Player player, String input) {
        this.player = player;
        this.input = input;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Sender getSinglePlayer() {
        return MinestomPlatformServices.senderFactory().wrap(player);
    }

    @Override
    public Collection<Sender> getPlayers() {
        return List.of(getSinglePlayer());
    }

    @Override
    public String inputString() {
        return input;
    }
}
