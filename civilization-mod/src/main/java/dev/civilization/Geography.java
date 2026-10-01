package dev.civilization;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Server-thread queries. Samples the generator, never loads chunks or examines placed water/trees. */
public final class Geography {
    public static final TagKey<Biome> RIVERS = TagKey.create(Registries.BIOME, id("river_regions"));
    public static final TagKey<Biome> WOODLAND = TagKey.create(Registries.BIOME, id("woodland_regions"));
    public static final TagKey<Block> CROPS = TagKey.create(Registries.BLOCK, id("river_crops"));
    public static final TagKey<Block> TREES = TagKey.create(Registries.BLOCK, id("woodland_saplings"));
    // Values deliberately do not hold ServerLevel references, so weak keys can be collected.
    private static final Map<ServerLevel, Cached> CACHE = new WeakHashMap<>();
    private static volatile int tagGeneration;
    private record Cached(int radius, int waterY, int tags, GeographyGrid grid) {}
    public record Site(boolean supported, boolean riverBand, boolean heightEligible, boolean woodland,
                       int soilY, int minSoilY, int maxSoilY, int riverDistanceSquared) {
        public boolean farmland() { return supported && riverBand && heightEligible; }
        public String reason() {
            return !supported ? "dimension" : !riverBand ? "no_river" : !heightEligible ? "height" : "eligible";
        }
    }
    private static ResourceLocation id(String name) { return ResourceLocation.fromNamespaceAndPath("civilization", name); }
    public static boolean supported(ServerLevel level) { return level.dimensionType().natural() && !level.dimensionType().hasCeiling(); }
    public static void tagsChanged() { tagGeneration++; }
    public static void unload(ServerLevel level) { CACHE.remove(level); }
    public static void clear() { CACHE.clear(); }
    private static GeographyGrid grid(ServerLevel level) {
        int radius = GeographyConfig.RIVER_RADIUS.get(), waterY = GeographyConfig.WATER_Y.get();
        Cached cached = CACHE.get(level);
        if (cached == null || cached.radius != radius || cached.waterY != waterY || cached.tags != tagGeneration) {
            var source = level.getChunkSource().getGenerator().getBiomeSource();
            var sampler = level.getChunkSource().randomState().sampler();
            var grid = new GeographyGrid((x, z) -> {
                var biome = source.getNoiseBiome(x, (waterY + 1) >> 2, z, sampler);
                return new GeographyGrid.Region(biome.is(RIVERS), biome.is(WOODLAND));
            }, radius);
            cached = new Cached(radius, waterY, tagGeneration, grid);
            CACHE.put(level, cached);
        }
        return cached.grid;
    }
    public static Site inspect(ServerLevel level, BlockPos soil) {
        int water = GeographyConfig.WATER_Y.get(), below = GeographyConfig.BELOW.get(), above = GeographyConfig.ABOVE.get();
        boolean supported = supported(level);
        var column = supported ? grid(level).column(soil.getX(), soil.getZ()) : new GeographyGrid.Column(false, -1);
        return new Site(supported, column.riverBand(), GeographyGrid.heightEligible(soil.getY(), water, below, above),
                column.woodland(), soil.getY(), water - below, water + above, column.riverDistanceSquared());
    }
    public static boolean canFarm(ServerLevel level, BlockPos soil) {
        if (!supported(level) || !GeographyGrid.heightEligible(soil.getY(), GeographyConfig.WATER_Y.get(),
                GeographyConfig.BELOW.get(), GeographyConfig.ABOVE.get())) return false;
        return grid(level).column(soil.getX(), soil.getZ()).riverBand();
    }
    public static boolean woodland(ServerLevel level, BlockPos pos) {
        return supported(level) && grid(level).woodland(pos.getX(), pos.getZ());
    }
    public static boolean allowRandomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.is(CROPS) && !canFarm(level, pos.below())) return false;
        return !state.is(TREES) || woodland(level, pos) || random.nextInt(GeographyConfig.OUTSIDE_WOODLAND.get()) == 0;
    }
}
