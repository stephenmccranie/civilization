package dev.civilization;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Deliberately forgiving wheelbarrow capacity; material density stays shared. Pallets hold a fixed drawn stack instead. */
public final class CargoConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue WHEELBARROW;
    static {
        var b=new ModConfigSpec.Builder();
        WHEELBARROW=b.defineInRange("wheelbarrowMassKg",640000d,1,1e12);
        SPEC=b.build();
    }
}
