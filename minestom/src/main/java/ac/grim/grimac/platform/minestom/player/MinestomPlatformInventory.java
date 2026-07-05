package ac.grim.grimac.platform.minestom.player;

import ac.grim.grimac.packetevents.minestom.util.MinestomItemStackConverter;
import ac.grim.grimac.platform.api.player.PlatformInventory;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.PlayerInventory;

public final class MinestomPlatformInventory implements PlatformInventory {

    private final MinestomPlatformPlayer platformPlayer;

    MinestomPlatformInventory(MinestomPlatformPlayer platformPlayer) {
        this.platformPlayer = platformPlayer;
    }

    private Player player() {
        return (Player) platformPlayer.getNative();
    }

    private ItemStack convert(net.minestom.server.item.ItemStack stack) {
        return MinestomItemStackConverter.toPacketEvents(stack);
    }

    @Override
    public ItemStack getItemInHand() {
        return convert(player().getItemInMainHand());
    }

    @Override
    public ItemStack getItemInOffHand() {
        return convert(player().getItemInOffHand());
    }

    @Override
    public ItemStack getStack(int bukkitSlot, int vanillaSlot) {
        return convert(stackAtBukkitSlot(bukkitSlot));
    }

    /**
     * Bukkit slot layout, which the shared inventory code addresses:
     * 0-8 hotbar, 9-35 main inventory, 36-39 armor (boots to helmet), 40 offhand.
     * Minestom shares the 0-35 layout and exposes equipment through getters.
     */
    private net.minestom.server.item.ItemStack stackAtBukkitSlot(int bukkitSlot) {
        Player player = player();
        return switch (bukkitSlot) {
            case 36 -> player.getBoots();
            case 37 -> player.getLeggings();
            case 38 -> player.getChestplate();
            case 39 -> player.getHelmet();
            case 40 -> player.getItemInOffHand();
            default -> player.getInventory().getItemStack(bukkitSlot);
        };
    }

    @Override
    public ItemStack getHelmet() {
        return convert(player().getHelmet());
    }

    @Override
    public ItemStack getChestplate() {
        return convert(player().getChestplate());
    }

    @Override
    public ItemStack getLeggings() {
        return convert(player().getLeggings());
    }

    @Override
    public ItemStack getBoots() {
        return convert(player().getBoots());
    }

    @Override
    public ItemStack[] getContents() {
        PlayerInventory inventory = player().getInventory();
        ItemStack[] items = new ItemStack[PlayerInventory.INVENTORY_SIZE];
        for (int i = 0; i < items.length; i++) {
            items[i] = convert(inventory.getItemStack(i));
        }
        return items;
    }

    @Override
    public String getOpenInventoryKey() {
        var open = player().getOpenInventory();
        return open == null ? "minecraft:player" : open.getClass().getSimpleName();
    }
}
