package dev.civilization.visual;
import dev.civilization.FlightChunkHints;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Test-only A/B switch; never packaged in the release mod. */
@Mixin(FlightChunkHints.class)
public abstract class FlightBenchmarkHintsMixin {
    @Inject(method="publish",at=@At("HEAD"),cancellable=true,remap=false)
    private static void benchmark$disableDirection(CallbackInfo ci){
        if("false".equals(System.getProperty("civilization.bench.directional"))){FlightChunkHints.clear();ci.cancel();}
    }
}
