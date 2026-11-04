package dev.civilization.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.civilization.Grade;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.VanillaInventoryCodeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** NeoForge's hopper insertion offers a graded unit to differing stacks of its material; the item handler joins it. */
@Mixin(VanillaInventoryCodeHooks.class)
public abstract class GradeHopperHooksMixin {
    @WrapOperation(method = "insertStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isSameItemSameComponents(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean civilization$offer(ItemStack held, ItemStack stack, Operation<Boolean> original) {
        return original.call(held, stack) || Grade.graded(stack) && Grade.stacks(held, stack);
    }
}
