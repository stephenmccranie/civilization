package dev.civilization;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Deliberately forgiving game capacities; material density stays shared. */
public final class CargoConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue WHEELBARROW, STOCKPILE;
    static {
        var b=new ModConfigSpec.Builder();
        WHEELBARROW=b.defineInRange("wheelbarrowMassKg",640000d,1,1e12);
        STOCKPILE=b.defineInRange("stockpileMassKg",1024000d,1,1e12);
        SPEC=b.build();
    }
}
