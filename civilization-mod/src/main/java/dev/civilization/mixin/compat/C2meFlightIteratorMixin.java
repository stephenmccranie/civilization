package dev.civilization.mixin.compat;

import dev.civilization.FlightChunkHints;
import dev.civilization.FlightChunkOrder;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="com.ishland.c2me.notickvd.common.iterators.SpiralIterator",remap=false)
public abstract class C2meFlightIteratorMixin implements FlightChunkHints.OrderedIterator {
    @Shadow @Final private int originX;
    @Shadow @Final private int originZ;
    @Shadow @Final private int radius;
    @Unique private FlightChunkOrder.Cursor civilization$order;
    public void civilization$setHeading(int heading){
        civilization$order=FlightChunkOrder.create(radius,heading);
        if(civilization$order!=null)FlightChunkHints.reordered.incrementAndGet();
    }
    @Inject(method="hasNext",at=@At("HEAD"),cancellable=true,require=1)
    private void civilization$hasNext(CallbackInfoReturnable<Boolean> ci){if(civilization$order!=null)ci.setReturnValue(civilization$order.hasNext());}
    @Inject(method="next()Lnet/minecraft/world/level/ChunkPos;",at=@At("HEAD"),cancellable=true,require=1)
    private void civilization$next(CallbackInfoReturnable<ChunkPos> ci){
        if(civilization$order!=null){long p=civilization$order.next();ci.setReturnValue(new ChunkPos(originX+FlightChunkOrder.x(p),originZ+FlightChunkOrder.z(p)));}
    }
    @Inject(method="remaining",at=@At("HEAD"),cancellable=true,require=1)
    private void civilization$remaining(CallbackInfoReturnable<Long> ci){if(civilization$order!=null)ci.setReturnValue(civilization$order.remaining());}
}
