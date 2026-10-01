package dev.civilization.visual;
import dev.civilization.FlightChunkLatency;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="dev.civilization.AirshipTerrain")
public abstract class FlightBenchmarkTerrainMixin {
 @Inject(method="update",at=@At("RETURN"),remap=false)
 private static void trace(@Coerce Object drive,ServerLevel level,UUID id,Set<Long> cells,boolean request,int budget,CallbackInfoReturnable<Integer> ci){FlightChunkLatency.request(drive);}
}
