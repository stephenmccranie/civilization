package dev.civilization.mixin.compat;
import dev.civilization.client.DhWarpTerrain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.seibel.distanthorizons.core.api.internal.ClientApi",remap=false)
public abstract class DhWarpRenderMixin {
    @Inject(method="renderLodLayer(Z)V",at=@At("HEAD"),require=1)
    private void civilization$beginTerrain(boolean deferred,CallbackInfo ci){if(!deferred)DhWarpTerrain.begin();}
    @Inject(method="renderLodLayer(Z)V",at=@At(value="INVOKE",target="Lcom/seibel/distanthorizons/core/render/renderer/LodRenderer;render(Lcom/seibel/distanthorizons/core/render/RenderParams;Lcom/seibel/distanthorizons/core/wrapperInterfaces/minecraft/IProfilerWrapper;)V",shift=At.Shift.AFTER),require=1)
    private void civilization$terrainReady(boolean deferred,CallbackInfo ci){if(!deferred)DhWarpTerrain.rendered();}
}
