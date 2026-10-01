package dev.civilization;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class WeatherConfig {
 public static final ModConfigSpec SPEC;
 public static final ModConfigSpec.DoubleValue CLEAR_HOURS;
 public static final ModConfigSpec.IntValue GROW_SECONDS;
 static{var b=new ModConfigSpec.Builder();CLEAR_HOURS=b.comment("Mean random clear interval in real hours at 20 TPS; no minimum or maximum drought.").defineInRange("meanClearHours",12.0,.01,720.0);GROW_SECONDS=b.comment("Average full-water planting-to-harvest time; repeat fruit uses the same interval.").defineInRange("cropSeconds",7200,10,604800);SPEC=b.build();}
}
