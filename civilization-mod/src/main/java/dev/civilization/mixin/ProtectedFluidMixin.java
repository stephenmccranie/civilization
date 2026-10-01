package dev.civilization.mixin;

import dev.civilization.CivicAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FlowingFluid.class)
public abstract class ProtectedFluidMixin {
    @Inject(method = "spreadTo", at = @At("HEAD"), cancellable = true)
    private void civilization$boundary(LevelAccessor world, BlockPos target, BlockState state, Direction direction, FluidState fluid, CallbackInfo callback) {
        if (world instanceof Level level && !CivicAccess.boundary(level, target.relative(direction.getOpposite()), target)) callback.cancel();
    }
}
