package dev.civilization.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.civilization.DhTravelPolicy;
import dev.civilization.DhWarpBands;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.civilization.client.DhTravelClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.seibel.distanthorizons.core.render.QuadTree.LodQuadTree",remap=false)
public abstract class DhTravelMixin {
    @Unique private volatile DhTravelPolicy.View civilization$travel=new DhTravelPolicy.View(1);
    @Unique private final DhWarpBands civilization$bands=new DhWarpBands();
    // One policy per traversal: do not let the client tick alter decisions halfway through the tree.
    @Inject(method="updateAllRenderSections",at=@At("HEAD"),require=1)
    private void civilization$snapshot(CallbackInfo ci){civilization$travel=DhTravelClient.view();civilization$bands.begin(civilization$travel.multiplier()>1);}
    @Inject(method="calcExpectedDetailLevel(Lcom/seibel/distanthorizons/core/pos/blockPos/DhBlockPos2D;IID)B",
        at=@At("RETURN"),cancellable=true,require=1)
    private void civilization$fixedBands(@Coerce Object player,int x,int z,double radius,CallbackInfoReturnable<Byte> ci,@Local(ordinal=1) double distance){
        if(civilization$travel.multiplier()>1)ci.setReturnValue(civilization$bands.detail(x,z,radius,distance));
    }
    // Warp uses physical distance, independent of camera zoom. Outside warp DH owns quality.
    @ModifyExpressionValue(method="calcExpectedDetailLevel(Lcom/seibel/distanthorizons/core/pos/blockPos/DhBlockPos2D;IID)B",
        at=@At(value="FIELD",target="Lcom/seibel/distanthorizons/core/render/CameraZoom;magnification:D"),require=1)
    private double civilization$warpZoom(double zoom){return civilization$travel.multiplier()>1?1:zoom;}
    // DH permits finer existing nodes during zoom transitions. Warp's much steeper
    // distance curve needs the same normal onDesiredDetailLevel fallback/handoff.
    // This changes only that branch guard, never the camera or zoom calculation.
    @ModifyExpressionValue(method="recursivelyUpdateRenderSectionNode",
        at=@At(value="FIELD",target="Lcom/seibel/distanthorizons/core/render/CameraZoom;magnification:D"),require=1)
    private double civilization$allowFineFallback(double magnification){
        return civilization$travel.multiplier()>1?Math.max(2,magnification):magnification;
    }
}
