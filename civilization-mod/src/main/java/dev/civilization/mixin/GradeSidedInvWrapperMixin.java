package dev.civilization.mixin;

import dev.civilization.Grade;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The sided form of {@link GradeInvWrapperMixin}. */
@Mixin(SidedInvWrapper.class)
public abstract class GradeSidedInvWrapperMixin {
    @Shadow @Final protected WorldlyContainer inv;
    @Shadow @Final protected Direction side;
    @Shadow public abstract int getSlotLimit(int slot);

    @Inject(method = "insertItem", at = @At("HEAD"), cancellable = true)
    private void civilization$join(int slot, ItemStack stack, boolean simulate, CallbackInfoReturnable<ItemStack> left) {
        int target = SidedInvWrapper.getSlot(inv, slot, side);
        if (target == -1 || !Grade.graded(stack)) return;
        var held = inv.getItem(target);
        if (held.isEmpty() || !Grade.stacks(held, stack)) return;
        if (held.getCount() >= Math.min(held.getMaxStackSize(), getSlotLimit(slot)) || !inv.canPlaceItemThroughFace(target, stack, side) || !inv.canPlaceItem(target, stack)) { left.setReturnValue(stack); return; }
        var rest = stack.copy();
        int room = Math.min(stack.getMaxStackSize(), getSlotLimit(slot)) - held.getCount();
        if (simulate) rest.shrink(Math.min(room, rest.getCount()));
        else {
            Grade.join(held, rest, room);
            inv.setChanged();
            inv.setItem(target, held);
        }
        left.setReturnValue(rest.isEmpty() ? ItemStack.EMPTY : rest);
    }
}
