package dev.civilization;

import java.lang.reflect.Method;

/** Optional Terrain Diffusion API: natural river water, the lines rivers run along and the climate's air. Never examines player-placed water. */
final class TerrainDiffusion {
    private TerrainDiffusion() {}
    static boolean supported(Object source) {
        return source.getClass().getName().equals("com.github.xandergos.terraindiffusionmc.world.TerrainDiffusionBiomeSource");
    }
    private static final class Api {
        static final Method WATER, GROUND, LINES, AIR, METRES, SEA;
        static {
            try {
                var water = Class.forName("com.github.xandergos.terraindiffusionmc.world.TerrainWater");
                var climate = Class.forName("com.github.xandergos.terraindiffusionmc.world.TerrainClimate");
                WATER = water.getMethod("waterLevelY", int.class, int.class);
                GROUND = water.getMethod("groundY", int.class, int.class);
                LINES = water.getMethod("riverLines", int.class, int.class, int.class);
                AIR = climate.getMethod("seaLevelAirC", int.class, int.class, int.class, int.class, int.class);
                METRES = climate.getMethod("metresPerBlock");
                SEA = climate.getMethod("seaLevelY");
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("This Terrain Diffusion build lacks the river and climate API Civilization needs", ex);
            }
        }
    }
    private static Object call(Method method, Object... args) {
        try {
            return method.invoke(null, args);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Terrain Diffusion call failed: " + method.getName(), ex);
        }
    }
    static int surfaceWaterY(int x, int z) {
        int water = (int) call(Api.WATER, x, z);
        if (water == Integer.MIN_VALUE || (int) call(Api.GROUND, x, z) >= water)
            return Integer.MIN_VALUE; // Dry corridor or underground reach, not a surface river.
        // Aquifer's fluid level is the first air block; farms use the top water block.
        return water - 1;
    }
    /**
     * The rivers within reach blocks of (x, z) as lines running downstream, four floats a point: block x, block z,
     * blocks of river above the point and the river's half-width. Waits while their river regions build.
     */
    static float[][] riverLines(int x, int z, int reach) { return (float[][]) call(Api.LINES, x, z, reach); }
    /** Sea-level air in °C for nx by nz columns from (x0, z0), step blocks apart, x fastest. Waits on inference. */
    static float[] seaLevelAirC(int x0, int z0, int step, int nx, int nz) {
        return (float[]) call(Api.AIR, x0, z0, step, nx, nz);
    }
    static float metresPerBlock() { return (float) call(Api.METRES); }
    static int seaLevelY() { return (int) call(Api.SEA); }
}
