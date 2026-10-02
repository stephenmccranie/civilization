package dev.civilization;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Prototype tuning in Minecraft units, not a real-world ballistic specification. */
public final class FirearmConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue DAMAGE,SPEED,GRAVITY,DRAG;
    public static final ModConfigSpec.IntValue COOLDOWN,RELOAD,LIFETIME;
    static {
        var b=new ModConfigSpec.Builder();
        DAMAGE=b.defineInRange("damage",8.0,0.1,100.0);
        SPEED=b.defineInRange("blocksPerTick",5.0,0.1,16.0);
        GRAVITY=b.defineInRange("gravityPerTick",0.025,0.0,1.0);
        DRAG=b.defineInRange("velocityRetention",0.995,0.5,1.0);
        COOLDOWN=b.defineInRange("shotCooldownTicks",16,4,200);
        RELOAD=b.defineInRange("reloadTicks",160,40,1200);
        LIFETIME=b.defineInRange("bulletLifetimeTicks",60,1,200);
        SPEC=b.build();
    }
}
