package dev.civilization.mixin;

import dev.civilization.CivicAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class ProtectedHopperMixin {
    @Inject(method = "ejectItems", at = @At("HEAD"), cancellable = true)
    private static void civilization$output(Level level, BlockPos pos, HopperBlockEntity hopper, CallbackInfoReturnable<Boolean> callback) {
        var target = pos.relative(hopper.getBlockState().getValue(HopperBlock.FACING));
        if (!CivicAccess.boundary(level, pos, target)) callback.setReturnValue(false);
    }
    @Inject(method = "suckInItems", at = @At("HEAD"), cancellable = true)
    private static void civilization$input(Level level, Hopper hopper, CallbackInfoReturnable<Boolean> callback) {
        var pos = BlockPos.containing(hopper.getLevelX(), hopper.getLevelY(), hopper.getLevelZ());
        if (!CivicAccess.boundary(level, pos, pos.above())) callback.setReturnValue(false);
    }
}
