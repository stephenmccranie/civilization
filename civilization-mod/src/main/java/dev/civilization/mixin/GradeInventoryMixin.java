package dev.civilization.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.civilization.Grade;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Pickups and returned items join a graded material's existing stacks, moving their actual units. */
@Mixin(Inventory.class)
public abstract class GradeInventoryMixin {
    @ModifyReturnValue(method = "hasRemainingSpaceForItem", at = @At("RETURN"))
    private boolean civilization$room(boolean same, ItemStack held, ItemStack stack) {
        return same || Grade.graded(stack) && Grade.stacks(held, stack) && held.getCount() < ((Inventory)(Object)this).getMaxStackSize(held);
    }

    @Inject(method = "addResource(ILnet/minecraft/world/item/ItemStack;)I", at = @At("HEAD"), cancellable = true)
    private void civilization$add(int slot, ItemStack stack, CallbackInfoReturnable<Integer> left) {
        var inventory = (Inventory)(Object)this;
        var held = inventory.getItem(slot);
        if (!Grade.graded(stack) || !held.isEmpty() && !Grade.stacks(held, stack)) return;
        int n = Math.min(stack.getCount(), inventory.getMaxStackSize(held.isEmpty() ? stack : held) - held.getCount());
        if (n > 0) {
            if (held.isEmpty()) inventory.setItem(slot, stack.split(n));
            else Grade.join(held, stack, n);
            inventory.getItem(slot).setPopTime(5);
        }
        left.setReturnValue(stack.getCount());
    }
}
