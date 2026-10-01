package dev.civilization.visual;
import dev.civilization.FlightChunkLatency;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Pseudo
@Mixin(targets="com.ishland.c2me.rewrites.chunksystem.common.statuses.ServerAccessible",remap=false)
public abstract class FlightBenchmarkAccessibleMixin {
 @Inject(method="toFullChunk",at=@At("HEAD"),remap=false)
 private static void create(ProtoChunk chunk,ServerLevel level,CallbackInfoReturnable<LevelChunk> ci){FlightChunkLatency.mark(chunk.getPos().toLong(),13);}
 @Inject(method="upgrade0",at=@At("HEAD"),remap=false)
 private void publish(@Coerce Object ctx,ProtoChunk proto,LevelChunk chunk,ImposterProtoChunk imposter,CallbackInfo ci){FlightChunkLatency.mark(chunk.getPos().toLong(),14);}
}
