package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

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
        return level.getChunkSource().getChunkNow(chunkX, chunkZ) != null;
    }
}
