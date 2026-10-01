package dev.civilization.mixin;

import dev.civilization.Geography;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** One tag-filtered dispatch gate also covers pitcher crops and mangroves, which lack a shared growth event. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class RegionalGrowthMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void civilization$region(ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo callback) {
        if (dev.civilization.SoilSystem.randomTick(level,pos,(BlockState)(Object)this)){callback.cancel();return;}
        if (!Geography.allowRandomTick((BlockState)(Object)this, level, pos, random)) callback.cancel();
    }
}
