package dev.civilization.mixin;

import dev.civilization.CivicAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FireBlock.class)
public abstract class ProtectedFireMixin {
    @Inject(method = "checkBurnOut", at = @At("HEAD"), cancellable = true)
    private void civilization$burn(Level level, BlockPos pos, int chance, RandomSource random, int age, Direction face, CallbackInfo callback) {
        if (CivicAccess.claim(level, pos) != null) callback.cancel();
    }
    @Inject(method = "getIgniteOdds(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)I", at = @At("HEAD"), cancellable = true)
    private void civilization$spread(LevelReader world, BlockPos pos, CallbackInfoReturnable<Integer> callback) {
        if (world instanceof Level level && CivicAccess.claim(level, pos) != null) callback.setReturnValue(0);
    }
}
