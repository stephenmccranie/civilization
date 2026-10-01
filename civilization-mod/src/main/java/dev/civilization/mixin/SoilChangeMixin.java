package dev.civilization.mixin;
import dev.civilization.SoilSystem;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LevelChunk.class)
public abstract class SoilChangeMixin {
 @Inject(method="setBlockState",at=@At("RETURN"))
 private void changed(BlockPos p,BlockState state,boolean moving,CallbackInfoReturnable<BlockState> ci){var chunk=(LevelChunk)(Object)this;if(ci.getReturnValue()!=null&&chunk.getLevel() instanceof ServerLevel l){SoilSystem.changed(l,p,ci.getReturnValue(),state);if(dev.civilization.BlockHeatSources.source(state)||dev.civilization.BlockHeatSources.source(ci.getReturnValue()))dev.civilization.BlockHeatSources.changed(l,p);}}
}
