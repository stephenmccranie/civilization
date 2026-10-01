package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/** Starts source flow even when world generation placed it without neighbor updates. */
@EventBusSubscriber(modid = Civilization.MOD_ID)
final class SurfaceOilFlow {
    private SurfaceOilFlow() {}

    static void activate(LevelAccessor level, BlockPos source) {
        if (level.getBlockState(source).is(IndustrialContent.SURFACE_OIL.get()))
            level.scheduleTick(source, IndustrialContent.CRUDE.get(), 1);
    }

    @SubscribeEvent public static void loaded(ChunkEvent.Load event) {
        if (!(event.getChunk() instanceof LevelChunk chunk)
                || !(chunk.getLevel() instanceof ServerLevel level)) return;
        int cx = chunk.getPos().x, cz = chunk.getPos().z;
        // Candidate centers are always in the inner 96 blocks of their 256-block cell.
        int localX = Math.floorMod(cx, 16), localZ = Math.floorMod(cz, 16);
        if (localX < 5 || localX > 10 || localZ < 5 || localZ > 10) return;
        level.getServer().execute(() -> {
            if (!level.hasChunk(cx, cz)) return;
            var site = Deposits.candidate(level, Math.floorDiv(cx, 16), Math.floorDiv(cz, 16));
            if (site != null && site.kind() == Deposits.Kind.OIL && site.x() >> 4 == cx && site.z() >> 4 == cz)
                activate(level, new BlockPos(site.x(), site.y(), site.z()));
        });
    }
}
