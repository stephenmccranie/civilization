package dev.civilization;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Resolves the player's current attachment after save loading or respawn replaces it. */
public final class AccessorySlot extends Slot {
    private final Player player;
    private final int index;

    public AccessorySlot(Player player, int index, int x, int y) {
        super(new SimpleContainer(3), index, x, y);
        this.player = player;
        this.index = index;
    }

    private Accessories.AccessoryInventory inventory() { return player.getData(Accessories.INVENTORY); }

    @Override public ItemStack getItem() { return inventory().getStackInSlot(index); }
    @Override public void set(ItemStack stack) { inventory().setStackInSlot(index, stack); }
    @Override public ItemStack remove(int amount) { return inventory().extractItem(index, amount, false); }
    @Override public boolean mayPlace(ItemStack stack) { return inventory().isItemValid(index, stack); }
    @Override public boolean mayPickup(Player player) { return !getItem().isEmpty(); }
    @Override public int getMaxStackSize() { return 1; }
    @Override public int getMaxStackSize(ItemStack stack) { return 1; }
}
