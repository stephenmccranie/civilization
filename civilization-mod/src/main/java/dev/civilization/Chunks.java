package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The mod's one chunk-presence test. {@code hasChunkAt} and {@code isLoaded} only say a server chunk is scheduled to be
 * full, and reading one still loading or generating makes the server thread wait for it, forever if its generation fails.
 */
public final class Chunks {
    /** Whether the chunk holding p has finished loading, so reading it never waits. */
    public static boolean loaded(Level level, BlockPos p) {
        return loaded(level, p.getX() >> 4, p.getZ() >> 4);
    }
    public static boolean loaded(Level level, int chunkX, int chunkZ) {
        return chunk(level, chunkX, chunkZ) != null;
    }
    /** The chunk if it has finished loading, otherwise null. */
    public static net.minecraft.world.level.chunk.LevelChunk chunk(Level level, int chunkX, int chunkZ) {
        return level.getChunkSource().getChunkNow(chunkX, chunkZ);
    }
    /** Whether every chunk within {@code reach} blocks of a point, horizontally, has finished loading. */
    public static boolean loadedAround(Level level, Vec3 at, double reach) {
        for (int x = Mth.floor(at.x - reach) >> 4; x <= Mth.floor(at.x + reach) >> 4; x++)
            for (int z = Mth.floor(at.z - reach) >> 4; z <= Mth.floor(at.z + reach) >> 4; z++)
                if (!loaded(level, x, z)) return false;
        return true;
    }
}
