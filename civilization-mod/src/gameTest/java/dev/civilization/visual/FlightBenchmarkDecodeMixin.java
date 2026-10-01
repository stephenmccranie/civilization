package dev.civilization.visual;
import dev.civilization.FlightChunkLatency;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.chunk.storage.*;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ChunkSerializer.class)
public abstract class FlightBenchmarkDecodeMixin {
 @Inject(method="read",at=@At("HEAD"))
 private static void start(ServerLevel level,PoiManager poi,RegionStorageInfo info,ChunkPos pos,CompoundTag tag,CallbackInfoReturnable<ProtoChunk> ci){FlightChunkLatency.mark(pos.toLong(),3);}
 @Inject(method="read",at=@At("RETURN"))
 private static void end(ServerLevel level,PoiManager poi,RegionStorageInfo info,ChunkPos pos,CompoundTag tag,CallbackInfoReturnable<ProtoChunk> ci){FlightChunkLatency.mark(pos.toLong(),4);}
}
