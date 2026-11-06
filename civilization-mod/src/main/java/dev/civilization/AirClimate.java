package dev.civilization;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.LoggerFactory;

/**
 * Background air: the air at a reference height, cooled with height above it, with the biome's daily swing and
 * rain response. On Terrain Diffusion worlds the reference is the climate's own sea-level air on a 32-block
 * lattice, cooled 6.5 °C per kilometre of the world's height; elsewhere it is the biome curve at Y80. Lattice
 * regions are fetched off the server thread when their chunks load; until one arrives its heat is unsupported,
 * like an unloaded chunk.
 */
@EventBusSubscriber(modid = "civilization")
public final class AirClimate {
    private AirClimate() {}
    private static final double LAPSE_C_PER_M = .0065;
    private static final int BIOME_REFERENCE_Y = 80;
    private static final double BIOME_COOLING_PER_BLOCK = .045;
    private static final int STEP = 32, NODES = 8, REGION = STEP * NODES, SIDE = NODES + 1;
    private static final Map<Long, float[]> AIR = new ConcurrentHashMap<>();
    private static final Set<Long> REQUESTED = ConcurrentHashMap.newKeySet();
    private static volatile int generation;
    private static final ExecutorService FETCH = Executors.newSingleThreadExecutor(task -> {
        var thread = new Thread(task, "Civilization air");
        thread.setDaemon(true);
        return thread;
    });

    static boolean terrain(ServerLevel level) {
        return TerrainDiffusion.supported(level.getChunkSource().getGenerator().getBiomeSource());
    }
    private static long region(int x, int z) {
        return (long) Math.floorDiv(x, REGION) << 32 | Math.floorDiv(z, REGION) & 0xffffffffL;
    }
    private static float[] air(int x, int z) {
        long key = region(x, z);
        float[] air = AIR.get(key);
        if (air == null && REQUESTED.add(key)) {
            int x0 = Math.floorDiv(x, REGION) * REGION, z0 = Math.floorDiv(z, REGION) * REGION, fetched = generation;
            FETCH.execute(() -> {
                try {
                    float[] lattice = TerrainDiffusion.seaLevelAirC(x0, z0, STEP, SIDE, SIDE);
                    if (fetched == generation) AIR.put(key, lattice);
                } catch (RuntimeException ex) {
                    LoggerFactory.getLogger(AirClimate.class).error("Sea-level air unavailable for region at {}, {}; its heat stays off", x0, z0, ex);
                    throw ex;
                }
            });
        }
        return air;
    }
    public static boolean ready(ServerLevel level, int x, int z) {
        return !terrain(level) || air(x, z) != null;
    }
    public static boolean readyAround(ServerLevel level, Vec3 at, double reach) {
        int x0 = Mth.floor(at.x - reach), x1 = Mth.floor(at.x + reach), z0 = Mth.floor(at.z - reach), z1 = Mth.floor(at.z + reach);
        return ready(level, x0, z0) && ready(level, x1, z0) && ready(level, x0, z1) && ready(level, x1, z1);
    }
    /** The position's climate, or null while its Terrain Diffusion air has not arrived. */
    public static ThermalRules.Climate at(ServerLevel level, BlockPos p) {
        var biome = ThermalRules.profile(level.getBiome(p));
        double mean;
        if (!terrain(level)) {
            mean = biome.meanC() - BIOME_COOLING_PER_BLOCK * Math.max(0, p.getY() - BIOME_REFERENCE_Y);
        } else {
            float[] air = air(p.getX(), p.getZ());
            if (air == null) return null;
            double u = Math.floorMod(p.getX(), REGION) / (double) STEP, v = Math.floorMod(p.getZ(), REGION) / (double) STEP;
            int i = (int) u, k = (int) v;
            double fu = u - i, fv = v - k;
            double sea = Mth.lerp(fv, Mth.lerp(fu, air[k * SIDE + i], air[k * SIDE + i + 1]),
                    Mth.lerp(fu, air[(k + 1) * SIDE + i], air[(k + 1) * SIDE + i + 1]));
            mean = sea - LAPSE_C_PER_M * TerrainDiffusion.metresPerBlock() * Math.max(0, p.getY() - TerrainDiffusion.seaLevelY());
        }
        return new ThermalRules.Climate(mean, biome.dailySwingC(), biome.rainFactor());
    }
    @SubscribeEvent public static void loaded(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && terrain(level))
            air(event.getChunk().getPos().getMinBlockX(), event.getChunk().getPos().getMinBlockZ());
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        generation++;
        AIR.clear();
        REQUESTED.clear();
    }
}
