package dev.civilization;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.Set;

/** Initial game tuning, not SI thermodynamics. Energy is in air-block degree units. */
public final class ThermalRules {
    private ThermalRules() {}
    public static final int MAX_CELLS = 262144;
    public static final double COAL_WASTE_HEAT = 13000;
    public static final double STOVE_WASTE_HEAT_FACTOR = .025;
    public static final double MAX_SOURCE_HEAT_PER_SECOND = 800;
    public static final double CAMPFIRE_HEAT = 24;
    public static final double TORCH_HEAT = 6; // Heat units per loaded second, not machine work credit.
    public static final double LAVA_FACE_HEAT = 48; // Maximum per exposed face of a source block per loaded second.
    public static final double RAIN_COOLING_C = 6;
    public static final double RAIN_COOLING_TICKS = 6000; // Five real minutes to approach wet equilibrium.
    public static final double RAIN_RECOVERY_TICKS = 48000; // Forty-minute thermal recovery after rain.
    public static final double MACHINE_TOP_SHARE = .80;
    public static final int EXHAUST_RISE_CELLS = 8; // Distance a hot exhaust parcel can travel in one loaded second.
    public static final double PLAYER_RESPONSE_SECONDS = 2.5;
    private static final double COLD_MIN = 2, COLD_COMFORT = 18;
    // Per quarter-second exchange step. Sum of outward air fractions stays below one.
    public static final double AIR_MIXING = .15, EXPOSED_AIR_MIXING = .10, UPWARD_MIXING = .19, OUTDOOR_EXCHANGE = .0045;
    public static final double BUOYANT_RISE = .04;
    public static final double EXPOSED_BUOYANT_RISE = .16; // Open-to-sky columns carry a stronger rising plume.
    public static final double COMFORT_CENTER_C = (70-32)/1.8;
    private static final double COMFORT_SPREAD_C = 8/1.8;
    /** Minecraft biome temperatures are gameplay indices, not degrees Celsius. */
    public static double biomeBaseline(double biome){
        double index=Math.clamp(biome,-1,2);
        return index<0?-10+12*index:index<=.5?-10+52*index:16+(index-.5)*(28d/3);
    }
    public record Climate(double meanC, double dailySwingC, double rainFactor) {}
    private static final Set<ResourceKey<Biome>> DRY=Set.of(
            Biomes.DESERT,Biomes.BADLANDS,Biomes.ERODED_BADLANDS,Biomes.WOODED_BADLANDS);
    private static final Set<ResourceKey<Biome>> SAVANNA=Set.of(
            Biomes.SAVANNA,Biomes.SAVANNA_PLATEAU,Biomes.WINDSWEPT_SAVANNA);
    private static final Set<ResourceKey<Biome>> TAIGA=Set.of(
            Biomes.TAIGA,Biomes.OLD_GROWTH_PINE_TAIGA,Biomes.OLD_GROWTH_SPRUCE_TAIGA);
    private static final Set<ResourceKey<Biome>> HUMID=Set.of(
            Biomes.JUNGLE,Biomes.SPARSE_JUNGLE,Biomes.BAMBOO_JUNGLE,Biomes.SWAMP,Biomes.MANGROVE_SWAMP);
    private static final Set<ResourceKey<Biome>> WOODLAND=Set.of(
            Biomes.FOREST,Biomes.FLOWER_FOREST,Biomes.BIRCH_FOREST,Biomes.OLD_GROWTH_BIRCH_FOREST,
            Biomes.DARK_FOREST,Biomes.CHERRY_GROVE,Biomes.MEADOW);
    private static final Set<ResourceKey<Biome>> OPEN_PLAINS=Set.of(Biomes.PLAINS,Biomes.SUNFLOWER_PLAINS);
    private static final Set<ResourceKey<Biome>> COAST=Set.of(
            Biomes.BEACH,Biomes.SNOWY_BEACH,Biomes.STONY_SHORE,Biomes.MUSHROOM_FIELDS);
    private static final Set<ResourceKey<Biome>> OCEAN=Set.of(Biomes.OCEAN,Biomes.DEEP_OCEAN);
    private static final Set<ResourceKey<Biome>> COLD_OCEAN=Set.of(Biomes.COLD_OCEAN,Biomes.DEEP_COLD_OCEAN);
    private static final Set<ResourceKey<Biome>> LUKEWARM_OCEAN=Set.of(Biomes.LUKEWARM_OCEAN,Biomes.DEEP_LUKEWARM_OCEAN);
    private static final Set<ResourceKey<Biome>> FROZEN_OCEAN=Set.of(Biomes.FROZEN_OCEAN,Biomes.DEEP_FROZEN_OCEAN);
    /** A few keyed exceptions correct vanilla values that cannot describe local climate. */
    public static Climate profile(ResourceKey<Biome> key,double vanillaTemperature){
        if(key==null)return new Climate(biomeBaseline(vanillaTemperature),5,1);
        if(key==Biomes.LUSH_CAVES)return new Climate(16,0,0);
        if(key==Biomes.DRIPSTONE_CAVES)return new Climate(14,0,0);
        if(key==Biomes.DEEP_DARK)return new Climate(10,0,0);
        if(key==Biomes.WARM_OCEAN)return new Climate(27,3,.5);
        if(LUKEWARM_OCEAN.contains(key))return new Climate(23,3,.5);
        if(OCEAN.contains(key))return new Climate(16,3,.5);
        if(COLD_OCEAN.contains(key))return new Climate(8,3,.5);
        if(FROZEN_OCEAN.contains(key))return new Climate(-8,3,.5);
        if(SAVANNA.contains(key))return new Climate(key==Biomes.WINDSWEPT_SAVANNA?24:25,7,.8);
        if(TAIGA.contains(key))return new Climate(12,5,1);
        double mean=biomeBaseline(vanillaTemperature);
        if(key==Biomes.STONY_PEAKS)mean-=7;
        if(DRY.contains(key))return new Climate(mean,9,.8);
        if(HUMID.contains(key))return new Climate(mean+4,3,.6);
        if(WOODLAND.contains(key))return new Climate(11.5+(vanillaTemperature-.5)*2,5,1);
        if(key==Biomes.RIVER)return new Climate(12,5,1);
        if(OPEN_PLAINS.contains(key))return new Climate(12.2,5,1);
        if(COAST.contains(key))return new Climate(mean,3,.5);
        return new Climate(mean,5,1);
    }
    public static double daylightPhase(long dayTime){
        return Math.cos((dayTime%24000-6000)*Math.PI/12000);
    }
    public static double climate(Climate profile,int height,double daylight,double rainCooling){
        return profile.meanC+profile.dailySwingC*daylight-Math.max(0,height-80)*.045
                -RAIN_COOLING_C*profile.rainFactor*rainCooling;
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
        if(direction==Direction.UP)return rise*Math.max(0,from-fromBackground);
        if(direction==Direction.DOWN)return -rise*Math.max(0,to-toBackground);
        return 0;
    }
    public static double comfort(double c) {
        double distance=(c-COMFORT_CENTER_C)/COMFORT_SPREAD_C;
        return Math.exp(-.5*distance*distance);
    }
    public static double calorieFactor(double comfort) { return 1 - .20 * comfort; }
    /** A player's thermal state follows the sampled air, without changing the air field. */
    public static double playerTemperature(double previous, double air, double seconds) {
        return previous + (air - previous) * (1 - Math.exp(-Math.max(0, seconds) / PLAYER_RESPONSE_SECONDS));
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
