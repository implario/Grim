package ac.grim.grimac.platform.minestom.player;

import ac.grim.grimac.platform.api.entity.GrimEntity;
import ac.grim.grimac.platform.api.player.BlockTranslator;
import ac.grim.grimac.platform.api.player.PlatformInventory;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.minestom.MinestomPlatformServices;
import ac.grim.grimac.platform.minestom.entity.MinestomGrimEntity;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.util.Vector3d;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

public class MinestomPlatformPlayer extends MinestomGrimEntity<Player> implements PlatformPlayer {

    private final MinestomPlatformInventory inventory;

    public MinestomPlatformPlayer(Player player) {
        super(player);
        this.inventory = new MinestomPlatformInventory(this);
    }

    private Player player() {
        return this.entity;
    }

    @Override
    public void kickPlayer(String textReason) {
        player().kick(textReason);
    }

    @Override
    public boolean isSneaking() {
        return player().isSneaking();
    }

    @Override
    public void setSneaking(boolean sneaking) {
        player().setSneaking(sneaking);
    }

    @Override
    public boolean hasPermission(String node) {
        return getSender().hasPermission(node);
    }

    @Override
    public boolean hasPermission(String node, boolean defaultIfUnset) {
        return getSender().hasPermission(node, defaultIfUnset);
    }

    @Override
    public void sendMessage(String message) {
        player().sendMessage(message);
    }

    @Override
    public void sendMessage(Component message) {
        player().sendMessage(message);
    }

    @Override
    public boolean isOnline() {
        return player().isOnline();
    }

    @Override
    public String getName() {
        return player().getUsername();
    }

    @Override
    public void updateInventory() {
        player().getInventory().update(player());
    }

    @Override
    public Vector3d getPosition() {
        Pos pos = player().getPosition();
        return new Vector3d(pos.x(), pos.y(), pos.z());
    }

    @Override
    public PlatformInventory getInventory() {
        return inventory;
    }

    @Override
    public @Nullable GrimEntity getVehicle() {
        Entity vehicle = player().getVehicle();
        return vehicle == null ? null : new MinestomGrimEntity<>(vehicle);
    }

    @Override
    public GameMode getGameMode() {
        return switch (player().getGameMode()) {
            case SURVIVAL -> GameMode.SURVIVAL;
            case CREATIVE -> GameMode.CREATIVE;
            case ADVENTURE -> GameMode.ADVENTURE;
            case SPECTATOR -> GameMode.SPECTATOR;
        };
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        player().setGameMode(switch (gameMode) {
            case SURVIVAL -> net.minestom.server.entity.GameMode.SURVIVAL;
            case CREATIVE -> net.minestom.server.entity.GameMode.CREATIVE;
            case ADVENTURE -> net.minestom.server.entity.GameMode.ADVENTURE;
            case SPECTATOR -> net.minestom.server.entity.GameMode.SPECTATOR;
        });
    }

    @Override
    public boolean isExternalPlayer() {
        return false;
    }

    @Override
    public void sendPluginMessage(String channelName, byte[] byteArray) {
        player().sendPluginMessage(channelName, byteArray);
    }

    @Override
    public Sender getSender() {
        return MinestomPlatformServices.senderFactory().wrap(player());
    }

    @Override
    public void replaceNativePlayer(Object nativePlayerObject) {
        setNativeEntity((Player) nativePlayerObject);
    }

    @Override
    public BlockTranslator getBlockTranslator() {
        return BlockTranslator.IDENTITY;
    }
}
