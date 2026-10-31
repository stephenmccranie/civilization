package dev.civilization.mixin.compat;

import dev.civilization.client.GrassWearClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import com.llamalad7.mixinextras.sugar.Local;

/** Scale cached geometry once per grass block, not once per blade or render frame. */
@Pseudo
@Mixin(targets="com.leonardoinc22.shortgrass.client.render.GrassSectionBuilder",remap=false)
public abstract class GrassierGrassWearMixin {
    @ModifyArg(index=8,method="emitSection",at=@At(value="INVOKE",target="Lcom/leonardoinc22/shortgrass/client/render/GrassSectionBuilder;emitBlock(Lcom/leonardoinc22/shortgrass/client/render/GrassSectionBuildBuffers;IIIIIIIFIILcom/leonardoinc22/shortgrass/config/GrassConfig$GrassStyle;IFIFFZ)V"),require=1)
    private static float civilization$trample(float height,@Local(index=20) int x,@Local(index=21) int y,@Local(index=22) int z){
        return height*GrassWearClient.heightScale(x,y,z);
    }
}
