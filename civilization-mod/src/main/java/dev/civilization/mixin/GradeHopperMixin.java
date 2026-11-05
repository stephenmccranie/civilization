package dev.civilization.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.civilization.Grade;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Hoppers join graded units onto differing stacks, and a unit they could not place goes back as itself. */
@Mixin(HopperBlockEntity.class)
public abstract class GradeHopperMixin {
    // The join happens here, so vanilla's own count changes never run for graded units.
    @WrapOperation(method = "tryMoveInItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;canMergeItems(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean civilization$join(ItemStack held, ItemStack stack, Operation<Boolean> original, @Local(argsOnly = true, ordinal = 1) Container destination) {
        if (!Grade.graded(stack) || !Grade.stacks(held, stack) || held.getCount() > held.getMaxStackSize()) return original.call(held, stack);
        if (Grade.join(held, stack, stack.getMaxStackSize() - held.getCount()) > 0) destination.setChanged();
        return false;
    }

    // Vanilla undoes a refused transfer by restoring the source's count, which would duplicate its worst unit.
    @WrapOperation(method = {"ejectItems", "tryTakeInItemFromSlot"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;setCount(I)V"))
    private static void civilization$restore(ItemStack source, int count, Operation<Void> original, @Local(ordinal = 1) ItemStack refused) {
        if (!source.isEmpty() && Grade.graded(source)) Grade.join(source, refused, refused.getCount());
        else original.call(source, count);
    }
}
