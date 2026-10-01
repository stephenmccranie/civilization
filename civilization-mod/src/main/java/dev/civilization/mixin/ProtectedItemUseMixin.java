package dev.civilization.mixin;

import dev.civilization.CivicAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class ProtectedItemUseMixin {
    @Inject(method = "mayUseItemAt", at = @At("HEAD"), cancellable = true)
    private void civilization$target(BlockPos pos, Direction direction, ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        var player = (Player)(Object)this;
        if (!CivicAccess.allowed(player.level(), pos, player)) callback.setReturnValue(false);
    }
}
