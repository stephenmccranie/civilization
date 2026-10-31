package dev.civilization;

import java.lang.reflect.Method;
import org.slf4j.LoggerFactory;

/** Optional Terrain Diffusion surface-water API. Never examines player-placed water. */
final class TerrainRiver {
    private TerrainRiver() {}
    static boolean supported(Object source) {
        return source.getClass().getName().equals("com.github.xandergos.terraindiffusionmc.world.TerrainDiffusionBiomeSource")
                && Api.WATER != null;
    }
    private static final class Api {
        static final Method WATER, GROUND;
        static {
            Method water = null, ground = null;
            try {
                var type = Class.forName("com.github.xandergos.terraindiffusionmc.world.TerrainWater");
                water = type.getMethod("waterLevelY", int.class, int.class);
                ground = type.getMethod("groundY", int.class, int.class);
            } catch (ClassNotFoundException ignored) {
                // Vanilla and other generators keep their configured water elevation.
            } catch (ReflectiveOperationException | LinkageError ex) {
                LoggerFactory.getLogger(TerrainRiver.class).warn("Terrain Diffusion river API unavailable; using configured river elevation", ex);
            }
            WATER = ground == null ? null : water; GROUND = ground;
        }
    }
    static int surfaceWaterY(int x, int z) {
        try {
            int water = (int) Api.WATER.invoke(null, x, z);
            if (water == Integer.MIN_VALUE || (int) Api.GROUND.invoke(null, x, z) >= water)
                return Integer.MIN_VALUE; // Dry corridor or underground reach, not a surface river.
            // Aquifer's fluid level is the first air block; farms use the top water block.
            return water - 1;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to read natural Terrain Diffusion river elevation", ex);
        }
    }
}
