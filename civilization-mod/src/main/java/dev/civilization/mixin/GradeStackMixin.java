package dev.civilization.mixin;

import dev.civilization.Grade;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Every count change keeps a stack's units consistent with its count; a split hands over the top units. */
@Mixin(ItemStack.class)
public abstract class GradeStackMixin {
    @Inject(method = "setCount", at = @At("HEAD"))
    private void civilization$resize(int count, CallbackInfo ci) { Grade.resize((ItemStack)(Object)this, count); }

    @Inject(method = "split", at = @At("HEAD"), cancellable = true)
    private void civilization$split(int count, CallbackInfoReturnable<ItemStack> result) {
        var part = Grade.split((ItemStack)(Object)this, count);
        if (part != null) result.setReturnValue(part);
    }
}
