package dev.civilization.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.civilization.Grade;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.VanillaInventoryCodeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** NeoForge's hopper insertion offers a graded unit to differing stacks of its material, where the item handler joins it, and its pull joins the unit itself. */
@Mixin(VanillaInventoryCodeHooks.class)
public abstract class GradeHopperHooksMixin {
    @WrapOperation(method = "insertStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isSameItemSameComponents(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean civilization$offer(ItemStack held, ItemStack stack, Operation<Boolean> original) {
        return original.call(held, stack) || Grade.graded(stack) && Grade.stacks(held, stack);
    }

    // extractHook's body is this lambda: a pulled graded unit goes onto a differing stack of its material...
    @WrapOperation(method = "lambda$extractHook$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isSameItemSameComponents(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean civilization$accept(ItemStack pulled, ItemStack held, Operation<Boolean> original) {
        return original.call(pulled, held) || Grade.graded(pulled) && Grade.stacks(held, pulled);
    }
    // ...as the unit it is, where growing the held stack would copy that stack's worst unit instead.
    @WrapOperation(method = "lambda$extractHook$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;grow(I)V"))
    private static void civilization$join(ItemStack held, int n, Operation<Void> original, @Local(ordinal = 0) ItemStack pulled) {
        if (Grade.graded(pulled)) Grade.join(held, pulled, n);
        else original.call(held, n);
    }
}
