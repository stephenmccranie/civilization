package dev.civilization.mixin.compat;

import dev.civilization.FlightChunkHints;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="com.ishland.c2me.notickvd.common.PlayerNoTickLoader",remap=false)
public abstract class C2meFlightLoaderMixin {
    @Shadow @Final private ChunkMap tacs;
    @Inject(method="lambda$new$0",at=@At("RETURN"),require=1)
    private void civilization$direction(long source,CallbackInfoReturnable<Object> ci){
        int heading=FlightChunkHints.heading(tacs,source);
        if(heading>=0&&ci.getReturnValue() instanceof FlightChunkHints.OrderedIterator iterator)iterator.civilization$setHeading(heading);
    }
}
