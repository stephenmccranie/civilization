package dev.civilization.mixin;

import dev.civilization.CivicAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({DispenserBlock.class, DropperBlock.class})
public abstract class ProtectedDispenserMixin {
    @Inject(method = "dispenseFrom", at = @At("HEAD"), cancellable = true)
    private void civilization$boundary(ServerLevel level, BlockState state, BlockPos pos, CallbackInfo callback) {
        if (!CivicAccess.boundary(level, pos, pos.relative(state.getValue(DispenserBlock.FACING)))) callback.cancel();
    }
}
