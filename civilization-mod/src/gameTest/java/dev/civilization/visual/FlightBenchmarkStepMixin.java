package dev.civilization.visual;
import dev.civilization.FlightChunkLatency;
import net.minecraft.world.level.chunk.status.*;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import java.util.concurrent.CompletableFuture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ChunkStep.class)
public abstract class FlightBenchmarkStepMixin {
 private int stage(){var s=((ChunkStep)(Object)this).targetStatus();return s==ChunkStatus.INITIALIZE_LIGHT?7:s==ChunkStatus.LIGHT?9:s==ChunkStatus.SPAWN?11:-1;}
 @Inject(method="apply",at=@At("HEAD"))
 private void start(WorldGenContext ctx,StaticCache2D<GenerationChunkHolder> cache,ChunkAccess chunk,CallbackInfoReturnable<CompletableFuture<ChunkAccess>> ci){int s=stage();if(s>=0)FlightChunkLatency.mark(chunk.getPos().toLong(),s);}
 @Inject(method="apply",at=@At("RETURN"))
 private void end(WorldGenContext ctx,StaticCache2D<GenerationChunkHolder> cache,ChunkAccess chunk,CallbackInfoReturnable<CompletableFuture<ChunkAccess>> ci){int s=stage();if(s>=0)ci.getReturnValue().whenComplete((v,e)->{if(e==null)FlightChunkLatency.mark(chunk.getPos().toLong(),s+1);});}
}
