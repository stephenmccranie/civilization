package dev.civilization.mixin;

import dev.civilization.CivicAccess;
import dev.civilization.CivicBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ProtectedLevelMixin {
    @Inject(method = "mayInteract", at = @At("HEAD"), cancellable = true)
    private void civilization$access(Player player, BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        var level = (ServerLevel)(Object)this;
        if (!(level.getBlockState(pos).getBlock() instanceof CivicBlock) && !CivicAccess.allowed(level, pos, player)) callback.setReturnValue(false);
    }
}
