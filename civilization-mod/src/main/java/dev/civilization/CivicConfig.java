package dev.civilization;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CivicConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue COAL_MINUTES, TAKEOVER_DAYS, PREMIUM;
    static {
        var b = new ModConfigSpec.Builder();
        COAL_MINUTES = b.comment("Minutes of Tier 1 claim upkeep per Coal; higher tiers multiply consumption. Runs while offline.")
                .defineInRange("coalMinutes", 60, 1, 1440);
        TAKEOVER_DAYS = b.comment("Minimum days after claim creation or handover before another takeover can be funded.")
                .defineInRange("takeoverDays", 30, 1, 365);
        PREMIUM = b.comment("Buyer pays this multiple of the actual remaining reserve, rounded up to whole coal.")
                .defineInRange("takeoverPriceMultiplier", 2, 2, 10);
        SPEC = b.build();
    }
    public static long coalMillis() { return COAL_MINUTES.get() * 60000L; }
    public static long takeoverMillis() { return TAKEOVER_DAYS.get() * 86400000L; }
    public static final int TIER_SWITCH_HOURS = 24;
    public static final long TIER_COOLDOWN_MILLIS = 24 * 3600000L;
    public static final long MOVE_MILLIS = 7 * 86400000L;
}
