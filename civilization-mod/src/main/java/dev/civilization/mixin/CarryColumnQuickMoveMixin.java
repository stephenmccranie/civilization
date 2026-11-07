package dev.civilization.mixin;

import dev.civilization.BlockCarry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Hand and Pack slots shift-click into a crafting grid or an anvil's inputs, as the pockets do. These menus route
 * only the slot indices they were built with and would send the column's slots back into the player's inventory.
 */
@Mixin({CraftingMenu.class, ItemCombinerMenu.class})
public abstract class CarryColumnQuickMoveMixin extends AbstractContainerMenu {
    protected CarryColumnQuickMoveMixin(MenuType<?> type, int id) { super(type, id); }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void civilization$column(Player player, int index, CallbackInfoReturnable<ItemStack> result) {
        if (index < 0 || index >= slots.size() || !BlockCarry.column(slots.get(index)) || !slots.get(index).hasItem()) return;
        var slot = slots.get(index); var stack = slot.getItem(); var copy = stack.copy();
        boolean moved = (Object)this instanceof ItemCombinerMenu combiner
                ? moveItemStackTo(stack, combiner.getSlotToQuickMoveTo(stack), combiner.getResultSlot(), false)
                : moveItemStackTo(stack, 1, 10, false);
        if (!moved) { result.setReturnValue(ItemStack.EMPTY); return; }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        result.setReturnValue(copy);
    }
}
