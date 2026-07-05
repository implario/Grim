package ac.grim.grimac.platform.minestom.initables;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.start.AbstractTickEndEvent;
import ac.grim.grimac.platform.minestom.GrimMinestomPlatform;
import ac.grim.grimac.player.GrimPlayer;
import net.minestom.server.event.server.ServerTickMonitorEvent;

public final class MinestomTickEndEvent extends AbstractTickEndEvent {

    @Override
    public void start() {
        if (!super.shouldInjectEndTick()) {
            return;
        }
        // ServerTickMonitorEvent fires after the tick's work has completed,
        // which matches the "end of tick" hook the reach check expects.
        GrimMinestomPlatform.eventNode().addListener(ServerTickMonitorEvent.class, event -> tickAllPlayers());
    }

    private void tickAllPlayers() {
        for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            if (player.disableGrim) continue;
            super.onEndOfTick(player, true);
        }
    }
}
