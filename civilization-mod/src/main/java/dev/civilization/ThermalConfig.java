package dev.civilization;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Instance/server switch for controlled performance comparisons; stored heat is preserved. */
public final class ThermalConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    static {
        var builder=new ModConfigSpec.Builder();
        ENABLED=builder.comment("Enable heat simulation, source discovery, comfort, cold calorie drain and thermal machine modifiers. Restart after changing. Stored heat is preserved while disabled; new waste heat is not queued.").worldRestart().define("enabled",true);
        SPEC=builder.build();
    }
    public static boolean enabled(){return ENABLED.get();}
}
