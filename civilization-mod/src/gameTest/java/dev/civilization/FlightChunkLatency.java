package dev.civilization;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
/** Bounded, development-only timing; never requests chunks or waits on their futures. */
public final class FlightChunkLatency {
 static final int LIMIT=131072;
 static final ConcurrentHashMap<Long,Trace> traces=new ConcurrentHashMap<>();
 static final ConcurrentLinkedQueue<String> batches=new ConcurrentLinkedQueue<>();
 public static void batch(long start){if(FlightBenchmark.started&&!FlightBenchmark.done&&batches.size()<20000)batches.add((start-FlightBenchmark.epoch)/1e9+","+(System.nanoTime()-start)/1e6);}
 static final ArrayList<String> blockers=new ArrayList<>();
 static final class Trace { final long[] times=new long[19]; synchronized void mark(int stage){if(times[stage]==0)times[stage]=System.nanoTime();} }
 public static void mark(long key,int stage){
  if(!FlightBenchmark.ENABLED||!FlightBenchmark.started||FlightBenchmark.done)return;
  Trace t=traces.get(key);if(t==null){if(traces.size()>=LIMIT)return;t=traces.computeIfAbsent(key,k->new Trace());}t.mark(stage);
 }
 public static void request(Object state){for(long cell:((AirshipSystem.Drive)state).tickets)mark(cell,0);}
 public static void snapshot(Vec3 pos,Vector3d v,double distance,FlightTerrain.Snapshot terrain){
  for(long cell:terrain.loaded())mark(cell,6);
  double length=Math.hypot(v.x,v.z);if(length<1||distance>=4096)return;
  double x=pos.x+v.x/length*(distance+1),z=pos.z+v.z/length*(distance+1);long now=System.nanoTime();
  for(int a=(int)Math.floor((x-28)/16);a<=(int)Math.floor((x+28)/16);a++)for(int b=(int)Math.floor((z-28)/16);b<=(int)Math.floor((z+28)/16);b++){
   long key=ChunkPos.asLong(a,b);if(terrain.loaded().contains(key))continue;
   Trace t=traces.get(key);String state="unobserved";double age=-1;
   if(!terrain.checked().contains(key))state="snapshot_bound";
   else if(t!=null)synchronized(t){long[] q=t.times;state=q[5]!=0?"loaded_not_snapshot":q[14]!=0?"publishing":q[13]!=0?"full_to_main_queue":q[12]!=0?"spawn_done_to_full":q[11]!=0?"spawn_pending":q[10]!=0?"light_done_to_spawn":q[9]!=0?(q[17]==0?"light_wait_queue":"light_run_or_completion"):q[8]!=0?"init_light_done_to_light":q[7]!=0?"init_light_pending":q[4]!=0?"post_decode_before_light":q[3]!=0?"decoding":q[2]!=0?"decode_queue":q[1]!=0?"nbt_pending":q[0]!=0?"before_nbt":"unrequested";if(q[0]!=0)age=(now-q[0])/1e6;}
   if(blockers.size()<20000)blockers.add(String.format(Locale.ROOT,"%.6f,%s,%d,%d,%s,%.3f",(now-FlightBenchmark.epoch)/1e9,FlightBenchmark.phase,a,b,state,age));
  }
 }
 static void finish(Path output)throws Exception{
  var rows=new ArrayList<String>();rows.add("x,z,request_ms,nbt_start_ms,nbt_ready_ms,decode_start_ms,decode_end_ms,load_ms,snapshot_ms,init_light_start_ms,init_light_end_ms,light_start_ms,light_end_ms,spawn_start_ms,spawn_end_ms,full_create_ms,publish_start_ms,init_pre_start_ms,init_pre_end_ms,light_pre_start_ms,light_pre_end_ms");
  for(var e:traces.entrySet()){StringBuilder s=new StringBuilder().append((int)(long)e.getKey()).append(',').append((int)(e.getKey()>>>32));synchronized(e.getValue()){for(long t:e.getValue().times)s.append(',').append(t==0?"":Double.toString((t-FlightBenchmark.epoch)/1e6));}rows.add(s.toString());}
  Files.write(output.resolve("chunk-latency.csv"),rows);var batchRows=new ArrayList<String>();batchRows.add("elapsed_s,duration_ms");batchRows.addAll(batches);Files.write(output.resolve("light-batches.csv"),batchRows);var b=new ArrayList<String>();b.add("elapsed_s,phase,x,z,state,request_age_ms");b.addAll(blockers);Files.write(output.resolve("chunk-blockers.csv"),b);
  Files.writeString(output.resolve("chunk-latency-limits.txt"),"traces="+traces.size()+" limit="+LIMIT+" blockers="+blockers.size()+" limit=20000; first-observation timestamps, not reload lifecycle tracking\n");
 }
}
