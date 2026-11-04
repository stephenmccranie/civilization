package dev.civilization.perf;

import dev.civilization.PerfRecorder;
import net.minecraft.client.Minecraft;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.SingleTickProfiler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** While a measurement runs, every client frame's profiler also feeds the performance recorder. */
@Mixin(Minecraft.class)
public abstract class PerfMinecraftMixin {
    @Inject(method = "constructProfiler", at = @At("RETURN"), cancellable = true)
    private void civilization$recordFrame(boolean fpsPie, @Nullable SingleTickProfiler tick, CallbackInfoReturnable<ProfilerFiller> cir) {
        if (PerfRecorder.INSTANCE.active()) cir.setReturnValue(ProfilerFiller.tee(cir.getReturnValue(), PerfRecorder.INSTANCE));
    }
}
