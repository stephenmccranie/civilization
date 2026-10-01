package dev.civilization.visual;
import dev.civilization.FlightChunkLatency;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
@Mixin(ThreadedLevelLightEngine.class)
public abstract class FlightBenchmarkLightQueueMixin {
 @Unique private long bench$start;
 @WrapOperation(method="initializeLight",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ThreadedLevelLightEngine;addTask(IILnet/minecraft/server/level/ThreadedLevelLightEngine$TaskType;Ljava/lang/Runnable;)V"))
 private void init(ThreadedLevelLightEngine engine,int x,int z,@Coerce Object type,Runnable task,Operation<Void> call){call.call(engine,x,z,type,(Runnable)()->{FlightChunkLatency.mark(ChunkPos.asLong(x,z),15);try{task.run();}finally{FlightChunkLatency.mark(ChunkPos.asLong(x,z),16);}});}
 @WrapOperation(method="lightChunk",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ThreadedLevelLightEngine;addTask(IILnet/minecraft/server/level/ThreadedLevelLightEngine$TaskType;Ljava/lang/Runnable;)V"))
 private void light(ThreadedLevelLightEngine engine,int x,int z,@Coerce Object type,Runnable task,Operation<Void> call){call.call(engine,x,z,type,(Runnable)()->{FlightChunkLatency.mark(ChunkPos.asLong(x,z),17);try{task.run();}finally{FlightChunkLatency.mark(ChunkPos.asLong(x,z),18);}});}
 @Inject(method="runUpdate",at=@At("HEAD")) private void start(CallbackInfo ci){bench$start=System.nanoTime();}
 @Inject(method="runUpdate",at=@At("RETURN")) private void end(CallbackInfo ci){FlightChunkLatency.batch(bench$start);}
}
