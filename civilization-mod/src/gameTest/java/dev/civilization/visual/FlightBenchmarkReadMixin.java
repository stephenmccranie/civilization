package dev.civilization.visual;
import dev.civilization.FlightChunkLatency;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.nbt.CompoundTag;
import java.util.*;
import java.util.concurrent.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ChunkMap.class)
public abstract class FlightBenchmarkReadMixin {
 @Inject(method="readChunk",at=@At("HEAD"))
 private void start(ChunkPos pos,CallbackInfoReturnable<CompletableFuture<Optional<CompoundTag>>> ci){FlightChunkLatency.mark(pos.toLong(),1);}
 @Inject(method="readChunk",at=@At("RETURN"))
 private void end(ChunkPos pos,CallbackInfoReturnable<CompletableFuture<Optional<CompoundTag>>> ci){ci.getReturnValue().whenComplete((value,error)->{if(error==null&&value.isPresent())FlightChunkLatency.mark(pos.toLong(),2);});}
}
