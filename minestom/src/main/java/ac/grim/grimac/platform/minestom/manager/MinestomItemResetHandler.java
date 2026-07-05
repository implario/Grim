package ac.grim.grimac.platform.minestom.manager;

import ac.grim.grimac.platform.api.manager.ItemResetHandler;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import org.jetbrains.annotations.Nullable;

public final class MinestomItemResetHandler implements ItemResetHandler {

    private static @Nullable Player player(@Nullable PlatformPlayer platformPlayer) {
        return platformPlayer == null ? null : (Player) platformPlayer.getNative();
    }

    @Override
    public void resetItemUsage(@Nullable PlatformPlayer platformPlayer) {
        // Minestom exposes no way to silently clear item usage without firing
        // the item's completion; usage state resynchronizes on the next action.
    }

    @Override
    public @Nullable InteractionHand getItemUsageHand(@Nullable PlatformPlayer platformPlayer) {
        Player player = player(platformPlayer);
        if (player == null || !player.isUsingItem()) return null;
        PlayerHand hand = player.getItemUseHand();
        if (hand == null) return null;
        return hand == PlayerHand.MAIN ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    @Override
    public boolean isUsingItem(@Nullable PlatformPlayer platformPlayer) {
        Player player = player(platformPlayer);
        return player != null && player.isUsingItem();
    }
}
