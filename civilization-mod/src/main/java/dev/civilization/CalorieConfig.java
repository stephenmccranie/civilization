package dev.civilization;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class CalorieConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue CAPACITY, WALK, ROW, ASCENT, SPRINT_MULTIPLIER, BREAK, PLACE,
            JUMP, ATTACK, IGNITE, HEAL, SLEEP, SPRINT_MINIMUM, HUNGER, FOOD_FALLBACK, CAKE,
            RECOVERY, DEPLETED_SPEED, STARVATION_FLOOR, FORAGE_KCAL, LIGHT_BREAK, HARVEST, PLANT, FERTILIZE,
            COLD_EXPOSURE;
    public static final ModConfigSpec.IntValue FORAGE_TICKS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> FOODS;

    static {
        var b = new ModConfigSpec.Builder();
        b.comment("Single calorie reserve. Vanilla saturation and exhaustion are disabled. Values are tunable game kcal.");
        CAPACITY = b.defineInRange("capacityKcal", 2400.0, 100.0, 100000.0);
        WALK = b.comment("Walking kcal per horizontal block, independent of speed; swimming includes vertical distance. Ascent is charged separately.")
                .defineInRange("walkKcalPerBlock", 0.1, 0.001, 100.0);
        ROW = b.comment("Controlling a paddled vanilla boat or chest boat costs this many kcal per horizontal block traveled; drifting and passengers cost nothing.")
                .defineInRange("rowKcalPerBlock", 0.05, 0.0, 100.0);
        ASCENT = b.comment("Additional kcal per block gained vertically, independent of gait; jumps still pay their separate charge.")
                .defineInRange("ascentKcalPerBlock", 1.0, 0.0, 100.0);
        SPRINT_MULTIPLIER = b.comment("Sprinting costs this many times the walking rate per block; road/effect speed does not change that rate.")
                .defineInRange("sprintMultiplier", 3.0, 1.0, 20.0);
        BREAK = b.defineInRange("breakKcal", 4.0, 0.0, 1000.0);
        LIGHT_BREAK = b.defineInRange("lightVegetationBreakKcal", 0.25, 0.0, 1000.0);
        HARVEST = b.defineInRange("cropHarvestKcal", 1.0, 0.0, 1000.0);
        PLANT = b.defineInRange("cropPlantKcal", 1.0, 0.0, 1000.0);
        FERTILIZE = b.defineInRange("fertilizerApplicationKcal", 1.0, 0.0, 1000.0);
        PLACE = b.defineInRange("placeKcal", 2.0, 0.0, 1000.0);
        SLEEP = b.comment("Calories per in-game hour actually slept, including hours skipped when the night advances.")
                .defineInRange("sleepKcalPerGameHour", 50.0, 0.0, 1000.0);
        JUMP = b.defineInRange("jumpKcal", 2.0, 0.0, 1000.0);
        ATTACK = b.defineInRange("successfulAttackKcal", 3.0, 0.0, 1000.0);
        IGNITE = b.comment("Calories spent for each accepted strike of a coal-fired machine, whether or not it lights.")
                .defineInRange("machineIgnitionKcal", 1.0, 0.0, 1000.0);
        HEAL = b.comment("kcal per health point (half heart), every 4 seconds while hurt; respects naturalRegeneration.")
                .defineInRange("healKcalPerHealthPoint", 40.0, 1.0, 1000.0);
        SPRINT_MINIMUM = b.defineInRange("minimumSprintKcal", 100.0, 1.0, 100000.0);
        HUNGER = b.comment("Hunger status effect drain per second per effect level.")
                .defineInRange("hungerEffectKcalPerSecond", 2.0, 0.0, 1000.0);
        COLD_EXPOSURE = b.comment("Maximum passive kcal per second at or below 35.6 F player temperature. The player follows local air over several seconds. Drain begins below 64.4 F, rising quadratically; only while heat simulation is enabled and the player is online in Survival.")
                .defineInRange("coldExposureMaxKcalPerSecond", 0.2, 0.0, 10.0);
        FOOD_FALLBACK = b.comment("Foods absent from the vanilla catalog and overrides use nutrition times this value; ignores saturation.")
                .defineInRange("fallbackKcalPerNutrition", 100.0, 0.0, 10000.0);
        CAKE = b.defineInRange("cakeSliceKcal", 200.0, 0.0, 10000.0);
        RECOVERY = b.comment("After reaching zero, regain this many kcal to leave depletion; capped at reserve capacity.")
                .defineInRange("depletedRecoveryKcal", 200.0, 1.0, 100000.0);
        DEPLETED_SPEED = b.defineInRange("depletedMiningSpeedMultiplier", 0.25, 0.01, 1.0);
        STARVATION_FLOOR = b.comment("Starvation alone cannot reduce health below this many health points (2 per heart).")
                .defineInRange("starvationHealthFloor", 6.0, 1.0, 20.0);
        FORAGE_KCAL = b.defineInRange("foragedMorselKcal", 25.0, 1.0, 1000.0);
        FORAGE_TICKS = b.comment("Ticks spent crouching still per morsel; 20 ticks/second at normal server speed.")
                .defineInRange("forageDurationTicks", 200, 20, 2400);
        FOODS = b.comment("Explicit per-item kcal, item_id=value. Overrides the built-in vanilla food catalog; last duplicate wins. Morsels and derived field rations use their own rules.")
                .defineListAllowEmpty("foodKcal", List.of("minecraft:apple=200", "minecraft:bread=500",
                        "minecraft:baked_potato=400", "minecraft:cooked_beef=700", "minecraft:carrot=150",
                        "minecraft:golden_carrot=400", "minecraft:cookie=100", "minecraft:dried_kelp=50"),
                        () -> "minecraft:apple=200", CalorieConfig::validFood);
        SPEC = b.build();
    }

    private static boolean validFood(Object value) {
        if (!(value instanceof String text)) return false;
        String[] parts = text.split("=", -1);
        if (parts.length != 2 || !parts[0].matches("[a-z0-9_.-]+:[a-z0-9/._-]+")) return false;
        try {
            double kcal = Double.parseDouble(parts[1]);
            return Double.isFinite(kcal) && kcal >= 0 && kcal <= 100000;
        } catch (NumberFormatException ex) { return false; }
    }
}
