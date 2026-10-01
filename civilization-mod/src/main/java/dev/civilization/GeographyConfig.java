package dev.civilization;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class GeographyConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue RIVER_RADIUS, WATER_Y, BELOW, ABOVE, OUTSIDE_WOODLAND;
    static {
        var b = new ModConfigSpec.Builder();
        RIVER_RADIUS = b.comment("River biome band radius in blocks, evaluated on a four-block grid.")
                .defineInRange("riverRadius", 48, 0, 64);
        WATER_Y = b.comment("Natural river water surface Y; vanilla Overworld water is Y=62. Also anchors biome sampling.")
                .defineInRange("riverWaterY", 62, -64, 319);
        BELOW = b.comment("Eligible soil blocks below the river water surface.")
                .defineInRange("farmlandBelowWater", 4, 0, 16);
        ABOVE = b.comment("Eligible soil blocks above the river water surface.")
                .defineInRange("farmlandAboveWater", 8, 0, 32);
        OUTSIDE_WOODLAND = b.comment("Outside woodland, accept one in N sapling random ticks; woodland retains vanilla speed.")
                .defineInRange("outsideWoodlandDivisor", 4, 1, 32);
        SPEC = b.build();
    }
}
