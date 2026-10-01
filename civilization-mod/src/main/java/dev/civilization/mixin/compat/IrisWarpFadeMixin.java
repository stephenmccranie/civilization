package dev.civilization.mixin.compat;

import dev.civilization.client.DhWarpTerrain;
import org.lwjgl.opengl.GL20C;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Photon owns a second near-terrain fade, independent of DH's projection clip. */
@Pseudo
@Mixin(targets="net.irisshaders.iris.compat.dh.IrisLodRenderProgram",remap=false)
public abstract class IrisWarpFadeMixin {
    @Shadow @Final private int id;
    @Unique private int civilization$warpUniform=-2;

    @ModifyVariable(method="<init>",at=@At("HEAD"),argsOnly=true,ordinal=5,require=1)
    private static String civilization$patchFade(String source){
        return dev.civilization.client.DhWarpShader.patch(source);
    }

    @Inject(method="fillUniformData",at=@At("TAIL"),require=1)
    private void civilization$updateFade(CallbackInfo ci){
        if(civilization$warpUniform==-2) civilization$warpUniform=GL20C.glGetUniformLocation(id,"civilizationWarpTerrain");
        if(civilization$warpUniform>=0){
            GL20C.glUniform1i(civilization$warpUniform,DhWarpTerrain.wanted()?1:0);
            if(DhWarpTerrain.wanted()) dev.civilization.client.DhWarpShader.applied++;
        }
    }
}
