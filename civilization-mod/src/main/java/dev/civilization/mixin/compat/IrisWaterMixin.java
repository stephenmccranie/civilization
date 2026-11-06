package dev.civilization.mixin.compat;

import dev.civilization.MachineParts;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A shader pack shades a machine's water as its own: the water cell, which draws nothing else, takes water's id. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings", remap = false)
public abstract class IrisWaterMixin {
    @Inject(method = "setBlockStateIds(Lit/unimi/dsi/fastutil/objects/Object2IntMap;)V", at = @At("TAIL"), require = 1)
    private void civilization$machineWater(Object2IntMap<BlockState> ids, CallbackInfo ci) {
        if (ids == null || !ids.containsKey(Blocks.WATER.defaultBlockState())) return;
        int water = ids.getInt(Blocks.WATER.defaultBlockState());
        for (var state : MachineParts.water()) ids.put(state, water);
    }
}
