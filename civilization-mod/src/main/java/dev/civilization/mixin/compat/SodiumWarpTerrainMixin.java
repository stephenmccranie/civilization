package dev.civilization.mixin.compat;
import dev.civilization.client.DhWarpTerrain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager",remap=false)
public abstract class SodiumWarpTerrainMixin {
    // Sable draws its vessels separately; never cancel LevelRenderer or SodiumWorldRenderer.
    @Inject(method="renderLayer",at=@At("HEAD"),cancellable=true,require=1)
    private void civilization$warpTerrain(CallbackInfo ci){
        if(DhWarpTerrain.hideTerrain()){DhWarpTerrain.skipped();ci.cancel();}
    }
    // Finish already-submitted work and normal unload/cleanup, but submit no new terrain meshes.
    @Inject(method="submitSectionTasks(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/executor/ChunkJobCollector;Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/executor/ChunkJobCollector;Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/executor/ChunkJobCollector;Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/estimation/UploadResourceBudget;)V",
        at=@At("HEAD"),cancellable=true,require=1)
    private void civilization$deferTerrainMeshes(CallbackInfo ci){
        if(DhWarpTerrain.deferMeshes()){DhWarpTerrain.skippedBuild();ci.cancel();}
    }
}
