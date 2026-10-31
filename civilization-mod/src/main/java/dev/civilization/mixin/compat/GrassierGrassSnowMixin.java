package dev.civilization.mixin.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep biome-tinted blades green instead of whitening them merely because it is cold. */
@Pseudo
@Mixin(targets = "com.leonardoinc22.shortgrass.client.render.GrassSectionBuilder", remap = false)
public abstract class GrassierGrassSnowMixin {
    @Inject(method = "snowBlend(ZFFF)F", at = @At("HEAD"), cancellable = true, require = 1)
    private static void civilization$disableSnowTint(boolean snowy, float length, float noise,
            float height, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(0.0F);
    }
}
