package dev.civilization.perf;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Iris's full-screen shader passes (begin, prepare, deferred, composite) as a profiler section; where it sits names the stage. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.CompositeRenderer", remap = false)
public abstract class PerfIrisCompositeMixin {
    @Inject(method = "renderAll", at = @At("HEAD"), remap = false, require = 0)
    private void civilization$begin(CallbackInfo ci) { Minecraft.getInstance().getProfiler().push("iris_passes"); }

    @Inject(method = "renderAll", at = @At("RETURN"), remap = false, require = 0)
    private void civilization$end(CallbackInfo ci) { Minecraft.getInstance().getProfiler().pop(); }
}
