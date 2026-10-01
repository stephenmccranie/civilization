package dev.civilization.mixin.compat;
import dev.civilization.client.DhWarpTerrain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="com.seibel.distanthorizons.core.util.RenderUtil",remap=false)
public abstract class DhWarpClipMixin {
    @Inject(method="getNearClipPlaneInBlocks",at=@At("RETURN"),cancellable=true,require=1)
    private static void civilization$fillNearTerrain(CallbackInfoReturnable<Float> ci){
        if(DhWarpTerrain.wanted())ci.setReturnValue(.5f);
    }
}
