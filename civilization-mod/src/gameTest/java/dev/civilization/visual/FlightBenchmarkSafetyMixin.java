package dev.civilization.visual;

import dev.civilization.AirshipSystem;
import dev.civilization.FlightBenchmark;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observe the production safety result without changing motion or loading. */
@Mixin(AirshipSystem.class)
public abstract class FlightBenchmarkSafetyMixin {
    @Inject(method="clearFlight",at=@At("RETURN"),remap=false)
    private static void benchmark$safety(ServerSubLevel ship,@Coerce Object drive,Vec3 origin,Vector3d velocity,double seconds,CallbackInfoReturnable<Boolean> result){
        FlightBenchmark.safety(ship,drive,origin,velocity,seconds,result.getReturnValue());
    }
}
