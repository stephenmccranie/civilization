package dev.civilization;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Local presentation only; does not change temperature or calorie rules. */
public final class TemperatureWashConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue INTENSITY;

    static {
        var builder = new ModConfigSpec.Builder();
        ENABLED = builder.comment("Show the player's temperature, which gradually follows local air: tint the world, HUD and open screens, frost or glow at the screen edges, and shiver or sway the camera (also scaled by Distortion Effects).")
                .define("enabled", true);
        INTENSITY = builder.comment("Local strength of all temperature screen effects. Zero disables them.")
                .defineInRange("intensity", 1.0, 0.0, 2.0);
        SPEC = builder.build();
    }

    private TemperatureWashConfig() {}
}
