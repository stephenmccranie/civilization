package dev.civilization.mixin;

import dev.civilization.Grade;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Automation inserting a graded material joins its units onto a differing stack of it; the offered stack stays untouched. */
@Mixin(InvWrapper.class)
public abstract class GradeInvWrapperMixin {
    @Shadow public abstract Container getInv();
    @Shadow public abstract int getSlotLimit(int slot);

    @Inject(method = "insertItem", at = @At("HEAD"), cancellable = true)
    private void civilization$join(int slot, ItemStack stack, boolean simulate, CallbackInfoReturnable<ItemStack> left) {
        var held = getInv().getItem(slot);
        if (!Grade.graded(stack) || held.isEmpty() || !Grade.stacks(held, stack)) return;
        if (held.getCount() >= Math.min(held.getMaxStackSize(), getSlotLimit(slot)) || !getInv().canPlaceItem(slot, stack)) { left.setReturnValue(stack); return; }
        var rest = stack.copy();
        int room = Math.min(stack.getMaxStackSize(), getSlotLimit(slot)) - held.getCount();
        if (simulate) rest.shrink(Math.min(room, rest.getCount()));
        else {
            Grade.join(held, rest, room);
            getInv().setItem(slot, held);
            getInv().setChanged();
        }
        left.setReturnValue(rest.isEmpty() ? ItemStack.EMPTY : rest);
    }
}
