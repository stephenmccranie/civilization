package dev.civilization.perf;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Iris's final pass to the screen as a profiler section. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.FinalPassRenderer", remap = false)
public abstract class PerfIrisFinalMixin {
    @Inject(method = "renderFinalPass", at = @At("HEAD"), remap = false, require = 0)
    private void civilization$begin(CallbackInfo ci) { Minecraft.getInstance().getProfiler().push("iris_final"); }

    @Inject(method = "renderFinalPass", at = @At("RETURN"), remap = false, require = 0)
    private void civilization$end(CallbackInfo ci) { Minecraft.getInstance().getProfiler().pop(); }
}
