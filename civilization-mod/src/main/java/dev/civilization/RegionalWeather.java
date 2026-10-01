package dev.civilization;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
/** A persisted district schedule. No terrain loading, atmosphere simulation or per-district tickers. */
public final class RegionalWeather extends SavedData {
 public static final int SIZE=512;
 public static final class District {public long end,sequence,coolingAt;public boolean rain;public double cooling;}
 private final Map<Long,District> districts=new HashMap<>();
 public long clock;
 public static boolean enabled(Level l){return l.dimension().equals(Level.OVERWORLD);}
 public static long key(int x,int z){return net.minecraft.world.level.ChunkPos.asLong(Math.floorDiv(x,SIZE),Math.floorDiv(z,SIZE));}
 public static RegionalWeather get(ServerLevel l){return l.getDataStorage().computeIfAbsent(new Factory<>(RegionalWeather::new,RegionalWeather::load),"civilization_weather");}
 public static long waitTicks(double u,double meanHours){return Math.max(1,(long)Math.ceil(-Math.log1p(-u)*meanHours*72000));}
 private double sample(ServerLevel l,long key,District d){return new java.util.SplittableRandom(l.getSeed()^key^(++d.sequence*0x9e3779b97f4a7c15L)).nextDouble();}
 private static double coolingAt(District d,long tick){
  double elapsed=Math.max(0,tick-d.coolingAt);
  return d.rain?1-(1-d.cooling)*Math.exp(-elapsed/ThermalRules.RAIN_COOLING_TICKS)
          :d.cooling*Math.exp(-elapsed/ThermalRules.RAIN_RECOVERY_TICKS);
 }
 private static void advance(District d,long tick){d.cooling=coolingAt(d,tick);d.coolingAt=tick;}
 public District district(ServerLevel l,long key){
  var d=districts.get(key);if(d==null){d=new District();d.coolingAt=clock;d.end=clock+waitTicks(sample(l,key,d),WeatherConfig.CLEAR_HOURS.get());districts.put(key,d);setDirty();}
  while(clock>=d.end){advance(d,d.end);d.rain=!d.rain;d.end+=d.rain?30000+(long)(sample(l,key,d)*12001):waitTicks(sample(l,key,d),WeatherConfig.CLEAR_HOURS.get());setDirty();}
  return d;
 }
 public static boolean raining(ServerLevel l,BlockPos p){return enabled(l)?get(l).district(l,key(p.getX(),p.getZ())).rain:l.isRaining();}
 public double coolingFor(ServerLevel l,BlockPos p){return coolingAt(district(l,key(p.getX(),p.getZ())),clock);}
 public static double cooling(ServerLevel l,BlockPos p){return enabled(l)?get(l).coolingFor(l,p):l.isRaining()?1:0;}
 public boolean call(ServerLevel l,BlockPos p){long k=key(p.getX(),p.getZ());var d=district(l,k);if(d.rain)return false;advance(d,clock);d.rain=true;d.end=clock+36000;setDirty();return true;}
 public void clear(ServerLevel l,BlockPos p){long k=key(p.getX(),p.getZ());var d=district(l,k);advance(d,clock);d.rain=false;d.end=clock+waitTicks(sample(l,k,d),WeatherConfig.CLEAR_HOURS.get());setDirty();}
 public static String describe(ServerLevel l,BlockPos p){if(!enabled(l))return "Regional weather: Overworld only";return "District "+Math.floorDiv(p.getX(),SIZE)+", "+Math.floorDiv(p.getZ(),SIZE)+" | "+(raining(l,p)?"Raining":"Clear");}
 public static RegionalWeather load(CompoundTag t,HolderLookup.Provider lookup){var w=new RegionalWeather();w.clock=t.getLong("clock");for(var n:t.getList("districts",10)){var e=(CompoundTag)n;var d=new District();d.end=e.getLong("end");d.sequence=e.getLong("seq");d.rain=e.getBoolean("rain");d.coolingAt=e.contains("coolingAt")?e.getLong("coolingAt"):w.clock;d.cooling=Math.clamp(e.getDouble("cooling"),0,1);w.districts.put(e.getLong("key"),d);}return w;}
 @Override public CompoundTag save(CompoundTag t,HolderLookup.Provider lookup){t.putLong("clock",clock);var list=new ListTag();districts.forEach((k,d)->{var e=new CompoundTag();e.putLong("key",k);e.putLong("end",d.end);e.putLong("seq",d.sequence);e.putBoolean("rain",d.rain);e.putLong("coolingAt",d.coolingAt);e.putDouble("cooling",d.cooling);list.add(e);});t.put("districts",list);return t;}
}
