package dev.civilization.perf;

import com.mojang.blaze3d.vertex.MeshData;
import dev.civilization.PerfRecorder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every batched draw, vanilla's and Iris's alike, is a profiler section named by its render type and texture. */
@Mixin(RenderType.class)
public abstract class PerfDrawMixin {
    @Inject(method = "draw", at = @At("HEAD"))
    private void civilization$begin(MeshData mesh, CallbackInfo ci) {
        if (PerfRecorder.INSTANCE.active()) Minecraft.getInstance().getProfiler().push(PerfRecorder.INSTANCE.drawSection((RenderType) (Object) this));
    }

    @Inject(method = "draw", at = @At("RETURN"))
    private void civilization$end(MeshData mesh, CallbackInfo ci) {
        if (PerfRecorder.INSTANCE.active()) Minecraft.getInstance().getProfiler().pop();
    }
}
