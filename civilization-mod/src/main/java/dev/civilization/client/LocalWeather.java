package dev.civilization.client;
import dev.civilization.*;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
/** Last nearby district snapshot, scoped to the actual client level instance. */
@net.neoforged.fml.common.EventBusSubscriber(modid="civilization",value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class LocalWeather {
 @net.neoforged.bus.api.SubscribeEvent public static void unload(net.neoforged.neoforge.event.level.LevelEvent.Unload e){if(e.getLevel()==world){world=null;data=null;fades.clear();}}
 private static Level world;private static WeatherPayload data;
 private record Fade(float from,boolean wet,long began){}
 private static final java.util.Map<Long,Fade> fades=new java.util.HashMap<>();
 public static boolean ready(Level l){return l==world&&data!=null;}
 public static float strength(Level l,BlockPos pos){if(l!=world)return 0;var f=fades.get(RegionalWeather.key(pos.getX(),pos.getZ()));if(f==null)return 0;float t=Math.clamp((l.getGameTime()-f.began)/40f,0,1);return f.from+((f.wet?1:0)-f.from)*t;}

 public static void accept(Level l,WeatherPayload p){if(world!=l)fades.clear();world=l;data=p;var retained=new java.util.HashSet<Long>();for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){var at=new BlockPos((p.x()+dx)*512,64,(p.z()+dz)*512);long key=RegionalWeather.key(at.getX(),at.getZ());retained.add(key);boolean wet=(p.rainMask()&(1<<((dz+1)*3+dx+1)))!=0;var old=fades.get(key);if(old==null||old.wet!=wet)fades.put(key,new Fade(strength(l,at),wet,l.getGameTime()));}fades.keySet().retainAll(retained);}
 public static boolean raining(Level l,BlockPos pos){if(l!=world||data==null)return false;int dx=Math.floorDiv(pos.getX(),512)-data.x(),dz=Math.floorDiv(pos.getZ(),512)-data.z();return Math.abs(dx)<=1&&Math.abs(dz)<=1&&(data.rainMask()&(1<<((dz+1)*3+dx+1)))!=0;}
 public static float sky(Level l){var camera=Minecraft.getInstance().getCameraEntity();if(camera==null||l!=world)return 0;double x=camera.getX(),z=camera.getZ();float value=0;for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)value+=strength(l,BlockPos.containing(x+dx*8,camera.getY(),z+dz*8))/9;return value;}
 public static float nearby(Level l){var camera=Minecraft.getInstance().getCameraEntity();if(camera==null)return 0;float value=0;for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)value=Math.max(value,strength(l,camera.blockPosition().offset(dx*16,0,dz*16)));return value;}
}
