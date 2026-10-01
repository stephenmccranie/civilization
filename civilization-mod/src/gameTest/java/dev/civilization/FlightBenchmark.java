package dev.civilization;

import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import com.sun.management.OperatingSystemMXBean;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.joml.Vector3d;
import jdk.jfr.*;

/** Development-only benchmark. Streaming stress and production physics are deliberately separate modes. */
@EventBusSubscriber(modid="civilization")
public final class FlightBenchmark {
    static final boolean ENABLED="flight-benchmark".equals(System.getProperty("civilization.previewCheck"));
    static final String MODE=property("mode","stream");
    static final double TARGET=Double.parseDouble(property("speed","200"));
    static final double SECONDS=Double.parseDouble(property("seconds","20")),WARMUP=Double.parseDouble(property("warmup","5"));
    static final int OBSTACLE=Integer.parseInt(property("obstacle","0"));
    private static double furthestX,peakBeforeWall;
    static final boolean DIRECTIONAL=Boolean.parseBoolean(property("directional","true"));
    static volatile boolean started,done;static volatile String phase="setup";static volatile Throwable failure;
    static Path output;static long epoch;private static long tickStart,lastTick,lastCpuTime,lastCpuWall,ticks;
    private static double lastX,lastZ,pathX,cpuPercent,hostPercent;
    private static final OperatingSystemMXBean OS=(OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
    private static final AtomicLong loads=new AtomicLong(),generated=new AtomicLong();
    private static final java.util.concurrent.ConcurrentLinkedQueue<String> safetyRows=new java.util.concurrent.ConcurrentLinkedQueue<>();
    private static BufferedWriter csv;private static Recording recording;private static ServerSubLevel ship;private static AirshipSystem.Drive drive;
    static String property(String name,String fallback){String v=System.getProperty("civilization.bench."+name,"");return v.isBlank()?fallback:v;}
    static void start(ServerPlayer p)throws Exception{
        var l=p.serverLevel();var root=l.getServer().getWorldPath(LevelResource.ROOT);
        if(!Files.exists(root.resolve("BENCHMARK_COPY.json"))||!(l.getChunkSource().getGenerator() instanceof FlatLevelSource))throw new IllegalStateException("Not an isolated benchmark copy");
        if(!Set.of("stream","airship").contains(MODE)||TARGET<=0||TARGET>5000||SECONDS<=0||WARMUP<0||TARGET*(SECONDS+WARMUP)>250000)throw new IllegalArgumentException("Benchmark parameters exceed corridor");
        output=Path.of(property("output",""));Files.createDirectories(output);
        var mods=new TreeMap<String,String>();net.neoforged.fml.ModList.get().getMods().forEach(m->mods.put(m.getModId(),m.getVersion().toString()));
        Files.writeString(output.resolve("loaded-mods.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(mods));
        Files.writeString(output.resolve("runtime.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of("java",System.getProperty("java.version"),"processors",OS.getAvailableProcessors(),"vm_arguments",ManagementFactory.getRuntimeMXBean().getInputArguments(),"epoch_utc",java.time.Instant.now().toString())));
        p.stopRiding();p.setGameMode(MODE.equals("stream")?GameType.SPECTATOR:GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();
        p.teleportTo(l,0.5,128.1,.5,Set.of(),-90,15);
        if(OBSTACLE!=0){
            if(!MODE.equals("airship")||OBSTACLE<1024||OBSTACLE>65536)throw new IllegalArgumentException("Invalid benchmark wall");
            for(int z=-32;z<=32;z++)for(int y=l.getMinBuildHeight();y<l.getMaxBuildHeight();y++)l.setBlock(new BlockPos(OBSTACLE,y,z),Blocks.BEDROCK.defaultBlockState(),2);
        }
        if(MODE.equals("airship")){
            var rootBlock=new BlockPos(0,128,0);var blocks=new ArrayList<BlockPos>();
            for(int x=-2;x<=2;x++)for(int z=-3;z<=3;z++){var b=rootBlock.offset(x,-1,z);l.setBlockAndUpdate(b,Blocks.OAK_PLANKS.defaultBlockState());blocks.add(b);}
            l.setBlockAndUpdate(rootBlock,AirshipContent.CONTROLLER.get().defaultBlockState().setValue(CivicBlock.FACING,Direction.EAST));blocks.add(rootBlock);
            p.teleportTo(l,-.5,128.05,.5,Set.of(),-90,15);ship=AirshipSystem.launch(l,rootBlock,p,blocks);
            var controller=AirshipSystem.controller(ship);var at=ship.logicalPose().transformPosition(Vec3.atLowerCornerOf(controller).add(-.5,.05,.5));
            p.teleportTo(l,at.x,at.y,at.z,Set.of(),-90,15);
            ((AirshipBlockEntity)l.getBlockEntity(controller)).power(1e7);AirshipSystem.pilot(ship,p);
            var field=AirshipSystem.class.getDeclaredField("drives");field.setAccessible(true);
            drive=(AirshipSystem.Drive)((Map<?,?>)field.get(null)).get(ship.getUniqueId());
        }
        csv=Files.newBufferedWriter(output.resolve("ticks.csv"));csv.write("elapsed_s,phase,tick,tick_work_ms,tick_interval_ms,x,z,target_bps,wall_bps,game_bps,body_bps,held,pilot_attached,process_cpu_pct,host_cpu_pct,heap_mib,server_chunk_loads,new_chunks,c2me_iterators,sweep_loaded_pct,target_snapshot_fits,streaming_brake,ready_distance,checked_chunks,ready_chunks\n");
        recording=new Recording(Configuration.getConfiguration("profile"));recording.setName("Civilization flight benchmark");recording.start();
        epoch=System.nanoTime();lastTick=epoch;lastCpuWall=epoch;lastCpuTime=OS.getProcessCpuTime();lastX=0.5;lastZ=.5;pathX=.5;started=true;
        System.out.println("FLIGHT_BENCHMARK_STARTED mode="+MODE+" target="+TARGET+" seconds="+SECONDS+" directional="+DIRECTIONAL);
    }
    @SubscribeEvent public static void pre(ServerTickEvent.Pre e){if(ENABLED)tickStart=System.nanoTime();}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void post(ServerTickEvent.Post e){
        if(!ENABLED||!started||done)return;
        try{
            var p=e.getServer().getPlayerList().getPlayers().getFirst();var l=p.serverLevel();long now=System.nanoTime();double elapsed=(now-epoch)/1e9;
            phase=elapsed<10?"settle":elapsed<10+WARMUP?"warmup":"measure";
            if(elapsed>=10+WARMUP+SECONDS){finish(p);return;}
            boolean moving=!phase.equals("settle");
            if(MODE.equals("stream")){
                if(moving){pathX+=TARGET/20;p.connection.teleport(pathX,128.1,.5,-90,15);}
                if(moving&&DIRECTIONAL&&TARGET>=80)FlightChunkHints.publish(Map.of(l.getChunkSource().chunkMap,List.of(new FlightChunkHints.Hint(((int)Math.floor(pathX))>>4,0,0))));
                else FlightChunkHints.clear();
            }else{
                var c=(AirshipBlockEntity)l.getBlockEntity(AirshipSystem.controller(ship));
                double mass=ship.getMassTracker().getMass(),area=ship.getUserDataTag().getDouble("areaX");
                c.power(moving?mass*100+.6*area*TARGET*TARGET*(10+TARGET):1e7);
                double measured=elapsed-10-WARMUP;
                int turn=Boolean.parseBoolean(property("turn","false"))?(measured>=2&&measured<2.2?1:measured>=2.4&&measured<2.6?-1:0):0;
                AirshipSystem.input(p,new AirshipPayload.Input(moving?1:0,turn,0,false));
                if(!DIRECTIONAL)FlightChunkHints.clear();
            }
            Vec3 pos=ship==null?p.position():AirshipSystem.world(ship,AirshipSystem.controller(ship));
            double distance=Math.hypot(pos.x-lastX,pos.z-lastZ),dt=(now-lastTick)/1e9;
            double body=ship==null?Double.NaN:AirshipSystem.speed(ship);
            furthestX=Math.max(furthestX,pos.x);
            if(OBSTACLE!=0){
                if(pos.x<OBSTACLE-32)peakBeforeWall=Math.max(peakBeforeWall,body);
                if(pos.x>OBSTACLE+.5)throw new IllegalStateException("Ship tunneled through wall: x="+pos.x);
            }
            int cx=((int)Math.floor(pos.x))>>4,cz=((int)Math.floor(pos.z))>>4;
            int minX=((int)Math.floor(pos.x-24))>>4,maxX=((int)Math.floor(pos.x+TARGET*.05+24))>>4;
            int minZ=((int)Math.floor(pos.z-24))>>4,maxZ=((int)Math.floor(pos.z+24))>>4;
            int needed=0,loaded=0;for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++){needed++;if(l.hasChunk(x,z))loaded++;}
            double coverage=100.0*loaded/needed;boolean snapshotFits=drive==null?minX>=cx-8&&maxX<=cx+8&&minZ>=cz-8&&maxZ<=cz+8:new FlightTerrain.Snapshot(drive.terrain.checked(),drive.terrain.checked()).sweep(pos.x,pos.z,TARGET*.05,0);
            boolean held=drive!=null&&!drive.hold.isEmpty();boolean attached=ship==null||p.getVehicle() instanceof AirshipPilotSeat;
            if(now-lastCpuWall>=250_000_000L){long cpu=OS.getProcessCpuTime();cpuPercent=100.0*(cpu-lastCpuTime)/(now-lastCpuWall)/OS.getAvailableProcessors();hostPercent=100*OS.getCpuLoad();lastCpuTime=cpu;lastCpuWall=now;}
            var terrain=drive==null?FlightTerrain.Snapshot.EMPTY:drive.terrain;
            var velocity=ship==null?new Vector3d():RigidBodyHandle.of(ship).getLinearVelocity(new Vector3d());
            double readyDistance=ship==null?Double.NaN:terrain.distance(pos.x,pos.z,velocity.x,velocity.z,FlightTerrain.MAX_DISTANCE);
            if(ship!=null)FlightChunkLatency.snapshot(pos,velocity,readyDistance,terrain);
            double work=(System.nanoTime()-tickStart)/1e6;
            csv.write(String.format(Locale.ROOT,"%.6f,%s,%d,%.4f,%.4f,%.5f,%.5f,%.1f,%.4f,%.4f,%.4f,%s,%s,%.3f,%.3f,%.3f,%d,%d,%d,%.3f,%s,%s,%.3f,%d,%d%n",elapsed,phase,++ticks,work,dt*1000,pos.x,pos.z,TARGET,distance/Math.max(dt,1e-9),distance*20,body,held,attached,cpuPercent,hostPercent,(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1048576.0,loads.get(),generated.get(),FlightChunkHints.reordered.get(),coverage,snapshotFits,drive!=null&&drive.streamingBrake,readyDistance,terrain.checked().size(),terrain.loaded().size()));
            lastTick=now;lastX=pos.x;lastZ=pos.z;
            if(ticks%100==0)System.out.printf(Locale.ROOT,"BENCH_PROGRESS %s %.1fs x=%.1f target=%.0f actual=%.1f hold=%s%n",MODE,elapsed,pos.x,TARGET,distance/Math.max(dt,1e-9),held);
        }catch(Throwable ex){failure=ex;done=true;}
    }
    public static void safety(ServerSubLevel ship,Object driveState,Vec3 origin,Vector3d velocity,double seconds,boolean clear){
        if(!ENABLED||!started||done||!MODE.equals("airship"))return;
        var terrain=(FlightTerrain.Snapshot)driveState;
        double x=origin.x+velocity.x*seconds,y=origin.y+velocity.y*seconds,z=origin.z+velocity.z*seconds;
        int radius=AirshipSystem.RADIUS*2,cx=((int)Math.floor(origin.x))>>4,missing=0;
        String reason="clear";
        if(!clear){
            if(!velocity.isFinite()||!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))reason="numeric";
            else if(y<ship.getLevel().getMinBuildHeight()+radius||y>ship.getLevel().getMaxBuildHeight()-radius)reason="height";
            else if(!new FlightTerrain.Snapshot(terrain.checked(),terrain.checked()).sweep(origin.x,origin.z,velocity.x*seconds,velocity.z*seconds))reason="snapshot_bounds";
            else if(!terrain.sweep(origin.x,origin.z,velocity.x*seconds,velocity.z*seconds)){reason="missing_snapshot_chunks";missing=1;}
            else reason="collision_readiness";
        }
        safetyRows.add(String.format(Locale.ROOT,"%.6f,%s,%s,%s,%.3f,%.3f,%.3f,%d,%d",(System.nanoTime()-epoch)/1e9,phase,clear,reason,origin.x,x,velocity.length(),cx,missing));
    }
    private static void finish(ServerPlayer p)throws Exception{
        if(OBSTACLE!=0){
            if(furthestX<OBSTACLE-12||peakBeforeWall<500)throw new IllegalStateException("Wall check never reached collision at speed: x="+furthestX+" peak="+peakBeforeWall);
            Files.writeString(output.resolve("COLLISION_PASS.txt"),"wall="+OBSTACLE+" furthest="+furthestX+" approach_peak_bps="+peakBeforeWall+"\n");
            System.out.println("AIRSHIP_WALL_COLLISION_PASS x="+furthestX+" peak="+peakBeforeWall);
        }
        FlightChunkLatency.finish(output);
        phase="finished";csv.close();recording.stop();recording.dump(output.resolve("profile.jfr"));recording.close();
        var safetyOutput=new ArrayList<String>();safetyOutput.add("elapsed_s,phase,clear,observed_reason,origin_x,predicted_x,predicted_bps,origin_chunk_x,missing_coverage");safetyOutput.addAll(safetyRows);Files.write(output.resolve("safety.csv"),safetyOutput);
        if(ship!=null)AirshipSystem.input(p,new AirshipPayload.Input(0,0,0,true));FlightChunkHints.clear();
        Files.writeString(output.resolve("COMPLETE.txt"),"Measurement complete. Inspect actual speed and holds; target speed is not a guarantee.\n");done=true;System.out.println("FLIGHT_BENCHMARK_PASS");
    }
    @SubscribeEvent public static void chunk(ChunkEvent.Load e){if(ENABLED&&started&&!done&&!e.getLevel().isClientSide()){FlightChunkLatency.mark(e.getChunk().getPos().toLong(),5);loads.incrementAndGet();if(e.isNewChunk())generated.incrementAndGet();}}
}
