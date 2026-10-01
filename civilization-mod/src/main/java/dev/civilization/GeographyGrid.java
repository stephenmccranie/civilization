package dev.civilization;

import java.util.LinkedHashMap;
import java.util.Map;

/** Seed-derived, overlapping properties on Minecraft's four-block biome grid. No world reads. */
public final class GeographyGrid {
    public record Region(boolean river, boolean woodland) {}
    public record Column(boolean woodland, int riverDistanceSquared) {
        public boolean riverBand() { return riverDistanceSquared >= 0; }
    }
    @FunctionalInterface public interface Sampler { Region sample(int quartX, int quartZ); }
    private final Sampler sampler;
    private final int radius;
    private final Map<Long, Region> regions;
    private final Map<Long, Column> columns;

    public GeographyGrid(Sampler sampler, int radius) { this(sampler, radius, 32768, 8192); }
    GeographyGrid(Sampler sampler, int radius, int regionLimit, int columnLimit) {
        if (radius < 0 || radius > 64) throw new IllegalArgumentException("River radius must be 0–64");
        this.sampler = sampler;
        this.radius = radius;
        regions = bounded(regionLimit);
        columns = bounded(columnLimit);
    }
    private static <V> Map<Long, V> bounded(int limit) {
        return new LinkedHashMap<>(16, 0.75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<Long, V> eldest) { return size() > limit; }
        };
    }
    private static long key(int x, int z) { return ((long)x << 32) | (z & 0xffffffffL); }
    private Region region(int x, int z) {
        return regions.computeIfAbsent(key(x, z), ignored -> sampler.sample(x, z));
    }
    public boolean woodland(int blockX, int blockZ) { return region(blockX >> 2, blockZ >> 2).woodland(); }
    public Column column(int blockX, int blockZ) {
        int x = blockX >> 2, z = blockZ >> 2;
        return columns.computeIfAbsent(key(x, z), ignored -> {
            int nearest = -1, reach = radius / 4;
            for (int dx = -reach; dx <= reach; dx++) for (int dz = -reach; dz <= reach; dz++) {
                int distance = 16 * (dx * dx + dz * dz);
                if (distance <= radius * radius && (nearest < 0 || distance < nearest)
                        && region(x + dx, z + dz).river()) nearest = distance;
            }
            return new Column(region(x, z).woodland(), nearest);
        });
    }
    public static boolean heightEligible(int soilY, int waterY, int below, int above) {
        return soilY >= waterY - below && soilY <= waterY + above;
    }
    int regionCount() { return regions.size(); }
    int columnCount() { return columns.size(); }
}
