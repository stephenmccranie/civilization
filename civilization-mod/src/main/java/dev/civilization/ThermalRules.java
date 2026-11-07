package dev.civilization;

import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import net.neoforged.neoforge.common.Tags;

/** Initial game tuning, not SI thermodynamics. Energy is in air-block degree units. */
public final class ThermalRules {
    private ThermalRules() {}
    public static final int MAX_CELLS = 262144;
    public static final double COAL_WASTE_HEAT = 13000;
    public static final double STOVE_WASTE_HEAT_FACTOR = .05;
    public static final double MAX_SOURCE_HEAT_PER_SECOND = 800;
    public static final double CAMPFIRE_HEAT = 24;
    public static final double FIRE_HEAT = 24; // Vanilla flame maintained while its actual block exists.
    public static final double TORCH_HEAT = 6; // Heat units per loaded second, not machine work credit.
    public static final double LAVA_FACE_HEAT = 48; // Maximum per exposed face of a source block per loaded second.
    public static final double RAIN_COOLING_C = 6;
    public static final double RAIN_COOLING_TICKS = 6000; // Five real minutes to approach wet equilibrium.
    public static final double RAIN_RECOVERY_TICKS = 48000; // Forty-minute thermal recovery after rain.
    public static final double MACHINE_TOP_SHARE = .80;
    // A body closes its gap to what it feels at gap * (1 + |gap| / PLAYER_RESPONSE_GAP_C) / PLAYER_RESPONSE_SECONDS per
    // second: a mild gap drifts on a 40 s time constant, and the constant shrinks as the gap grows, as convection and
    // radiation outpace the difference that drives them, so fire, lava or deep cold move a body within seconds.
    public static final double PLAYER_RESPONSE_SECONDS = 40, PLAYER_RESPONSE_GAP_C = 15 / 1.8;
    // Felt temperature blends torso air with the mean radiant temperature of what a body sees.
    public static final double RADIANT_SHARE = .4, RADIANT_REACH = 8;
    // Radiant output as bodies feel it, softened over about a block. Tuning, not SI.
    public static final double FLAME_RADIANCE = 30000, TORCH_RADIANCE = 3000, LAVA_RADIANCE = 60000;
    // Climate temperatures are shade air; direct sun is felt on top. A body absorbs about an eighth of the sun's beam
    // per unit of its surface, and a clear sky's scattered light (per sine of sun height) offsets its cold by day.
    public static final double SUN_ABSORBED = .125, SKY_DIFFUSE = 110, MIN_SUN_HEIGHT = .05, SUN_REACH = 64;
    // Clothing pulls felt temperature toward the skin by a full set's share. Metal armor is padded with the same
    // leather and cloth whatever its plates are, so it insulates alike and less than a full leather suit.
    public static final double SKIN_C = 33;
    private static final Map<Holder<ArmorMaterial>, Double> SET_INSULATION = Map.of(ArmorMaterials.LEATHER, .40,
            ArmorMaterials.CHAIN, .25, ArmorMaterials.IRON, .25, ArmorMaterials.GOLD, .25, ArmorMaterials.DIAMOND, .25,
            ArmorMaterials.NETHERITE, .25);
    private static final Map<EquipmentSlot, Double> COVERAGE = Map.of(EquipmentSlot.HEAD, .15, EquipmentSlot.CHEST, .40,
            EquipmentSlot.LEGS, .30, EquipmentSlot.FEET, .15);
    private static final double SUN_PATH_TILT = Math.toRadians(-35); // Photon's default, so shade matches its shadows.
    private static final double COLD_MIN = 2, COLD_COMFORT = 18;
    // Per quarter-second exchange step. Sum of outward air fractions stays below one.
    public static final double AIR_MIXING = .15, EXPOSED_AIR_MIXING = .10, UPWARD_MIXING = .19, OUTDOOR_EXCHANGE = .0045;
    public static final double BUOYANT_RISE = .04;
    public static final double EXPOSED_BUOYANT_RISE = .16; // Open-to-sky columns carry a stronger rising plume.
    public static final double COMFORT_CENTER_C = (70-32)/1.8;
    private static final double COMFORT_SPREAD_C = 8/1.8;
    /** Mean air, daily swing and rain response; a biome's profile gives its mean at the biome reference height. */
    public record Climate(double meanC, double dailySwingC, double rainFactor) {}
    // A biome's game temperature is an index, not degrees. One curve turns it into mean air, so every
    // biome of any mod with the same index shares a climate; all temperate land lies on one 12 C band.
    private static final double[] INDEX = {-1, 0, .25, .8, .95, 1.2, 2};
    private static final double[] MEAN_C = {-22, -10, 12, 12, 24, 25, 30};
    // The game gives every ocean the same index, so oceans name the warmth of their water.
    private static final Map<ResourceKey<Biome>, Double> OCEAN_C = Map.of(Biomes.WARM_OCEAN, 27d,
            Biomes.LUKEWARM_OCEAN, 23d, Biomes.DEEP_LUKEWARM_OCEAN, 23d, Biomes.OCEAN, 16d, Biomes.DEEP_OCEAN, 16d,
            Biomes.COLD_OCEAN, 8d, Biomes.DEEP_COLD_OCEAN, 8d, Biomes.FROZEN_OCEAN, -8d, Biomes.DEEP_FROZEN_OCEAN, -8d);
    public static double meanC(double index) {
        double i = Math.clamp(index, INDEX[0], INDEX[INDEX.length - 1]);
        int k = 1;
        while (i > INDEX[k]) k++;
        return MEAN_C[k - 1] + (MEAN_C[k] - MEAN_C[k - 1]) * (i - INDEX[k - 1]) / (INDEX[k] - INDEX[k - 1]);
    }
    /** Dry air swings further between day and night; water and caves hold their warmth. */
    public static Climate profile(Holder<Biome> biome) {
        var settings = biome.value().getModifiedClimateSettings();
        double mean = biome.unwrapKey().map(OCEAN_C::get).orElse(meanC(settings.temperature()));
        if (biome.is(Tags.Biomes.IS_CAVE)) return new Climate(mean, 0, 0);
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(Tags.Biomes.IS_BEACH)) return new Climate(mean, 3, .5);
        return new Climate(mean, 3 + 4 * (1 - Math.clamp(settings.downfall(), 0, 1)), 1);
    }
    public static double temperaturePhase(long dayTime){
        return DailyTemperatureCycle.phase(dayTime);
    }
    /** A position's air for the time of day and its district's rain; its mean already includes its height. */
    public static double climate(Climate profile,double daylight,double rainCooling){
        return profile.meanC+profile.dailySwingC*daylight-RAIN_COOLING_C*profile.rainFactor*rainCooling;
    }
    /** Shared interface rate for the thermal solver and the survey helmet's flow estimate. */
    public static double exchangeRate(boolean fromAir, boolean toAir, Direction direction, double from, double to,
                                      double fromConductance, double toConductance) {
        if(fromAir!=toAir)return .10*Math.sqrt(Math.min(fromConductance,toConductance));
        if(!fromAir)return .03*Math.min(fromConductance,toConductance);
        if (direction.getAxis().isVertical()
                && ((direction == Direction.UP && from > to) || (direction == Direction.DOWN && to > from)))
            return UPWARD_MIXING;
        return AIR_MIXING;
    }
    /** Signed additional flux from a hot lower air cell to the cell above it. */
    public static double buoyantFlux(boolean fromAir,boolean toAir,boolean fromOutside,boolean toOutside,
                                     Direction direction,double from,double to,double fromBackground,double toBackground){
        if(!fromAir||!toAir)return 0;
        double rise=fromOutside&&toOutside?EXPOSED_BUOYANT_RISE:BUOYANT_RISE;
        // Unobstructed sky columns have a continuous upward exit: equal inflow
        // and outflow carry a warm parcel onward without accumulating it at a
        // ceiling. Enclosed air has no such replacement-flow assumption.
        if(fromOutside&&toOutside){
            if(direction==Direction.UP)return rise*Math.max(0,from-fromBackground);
            if(direction==Direction.DOWN)return -rise*Math.max(0,to-toBackground);
        }
        // The lattice stores energy, not moving air mass. An unrestricted upward
        // pump concentrates uniform warmth at a ceiling without replacement air.
        // Limit this coarse convection to the warmer-lower temperature gradient.
        if(direction==Direction.UP)return rise*Math.max(0,Math.min(from-fromBackground,from-to));
        if(direction==Direction.DOWN)return -rise*Math.max(0,Math.min(to-toBackground,to-from));
        return 0;
    }
    public static double comfort(double c) {
        double distance=(c-COMFORT_CENTER_C)/COMFORT_SPREAD_C;
        return Math.exp(-.5*distance*distance);
    }
    /** Felt temperature: air blended with surroundings, then pulled toward the skin by the clothing's insulation share. */
    public static double felt(double air, double radiant, double insulation) {
        double bare = air + RADIANT_SHARE * (radiant - air);
        return bare + insulation * (SKIN_C - bare);
    }
    /** Clothing's share of a body's warmth: each worn piece adds its coverage of its material's full-set insulation. */
    public static double insulation(LivingEntity body) {
        double total = 0;
        for (var piece : COVERAGE.entrySet())
            if (body.getItemBySlot(piece.getKey()).getItem() instanceof ArmorItem armor)
                total += piece.getValue() * SET_INSULATION.getOrDefault(armor.getMaterial(), 0d);
        return total;
    }
    /** Unit vector toward the sun for a vanilla time of day, which is 0 at noon. */
    public static Vec3 sun(double timeOfDay) {
        double angle = timeOfDay * Math.TAU;
        return new Vec3(-Math.sin(angle), Math.cos(angle) * Math.cos(SUN_PATH_TILT), -Math.cos(angle) * Math.sin(SUN_PATH_TILT));
    }
    /** Clear-sky direct sunlight in W/m² for the sine of the sun's height, thinning through more air near the horizon. */
    public static double directSun(double height) {
        return height < MIN_SUN_HEIGHT ? 0 : 1353 * Math.pow(.7, Math.pow(1 / height, .678));
    }
    public static double calorieFactor(double comfort) { return 1 - .20 * comfort; }
    /**
     * A player's thermal state approaching what they feel, without changing the air field. The exact solution of the
     * gap's equation: gap / (G + gap) decays as exp(-t / PLAYER_RESPONSE_SECONDS), so no step can overshoot.
     */
    public static double playerTemperature(double previous, double target, double seconds) {
        double gap = Math.abs(previous - target), g = PLAYER_RESPONSE_GAP_C;
        double share = gap / (g + gap) * Math.exp(-Math.max(0, seconds) / PLAYER_RESPONSE_SECONDS);
        return target + Math.signum(previous - target) * g * share / (1 - share);
    }
    /** Felt cold costs up to the configured kcal/s; mild cold is deliberately inexpensive. */
    public static double coldKcalPerSecond(double c, double maximum) {
        double exposure=Math.clamp((COLD_COMFORT-c)/(COLD_COMFORT-COLD_MIN),0,1);
        return maximum*exposure*exposure;
    }
    public static double efficiency(double c, boolean sheltered) {
        return Math.clamp(1 + .005 * Math.max(0,c-26) - .01 * Math.max(0,-c) * (sheltered ? .5 : 1), .70, 1.10);
    }
}
