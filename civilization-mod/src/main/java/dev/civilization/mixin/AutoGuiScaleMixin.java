package dev.civilization.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Auto GUI scale follows window height, one step per 360 pixels (3 at 1080p, 4 at 1440p, 6 at 4K), so the interface
 * keeps the same share of the screen at every resolution instead of taking the largest scale that fits. Explicit
 * scales and the options slider's range are unchanged.
 */
@Mixin(Minecraft.class)
public abstract class AutoGuiScaleMixin {
    @ModifyArg(method = "resizeDisplay", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;setGuiScale(D)V"))
    private double civilization$proportionalAuto(double largestFit) {
        var mc = (Minecraft) (Object) this;
        if (mc.options.guiScale().get() != 0) return largestFit;
        int scale = Math.max(1, Math.round(mc.getWindow().getHeight() / 360f));
        if (mc.isEnforceUnicode()) scale += scale % 2;
        return Math.min(largestFit, scale);
    }
}
