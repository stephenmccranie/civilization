package dev.civilization;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Local presentation only; does not change temperature or calorie rules. */
public final class TemperatureWashConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue INTENSITY;

    static {
        var builder = new ModConfigSpec.Builder();
        ENABLED = builder.comment("Tint the world, HUD and open screens according to the player's temperature, which gradually follows local air.")
                .define("enabled", true);
        INTENSITY = builder.comment("Local strength of the temperature color wash. Zero disables it.")
                .defineInRange("intensity", 1.0, 0.0, 2.0);
        SPEC = builder.build();
    }

    private TemperatureWashConfig() {}
}
