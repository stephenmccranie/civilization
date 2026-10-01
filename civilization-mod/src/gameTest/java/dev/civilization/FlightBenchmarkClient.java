package dev.civilization;

import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

@EventBusSubscriber(modid="civilization",value=Dist.CLIENT)
public final class FlightBenchmarkClient {
    private static boolean requested,finished;private static int ticks;private static long lastFrame,frameStart;private static double interval;private static net.minecraft.world.phys.Vec3 previousCamera;
    private static final List<String> frames=new ArrayList<>();
    static void tick(Minecraft mc){
        mc.options.pauseOnLostFocus=false;
        if(FlightBenchmark.failure!=null)throw new IllegalStateException("Flight benchmark failed",FlightBenchmark.failure);
        if(!requested&&++ticks>=60){requested=true;mc.getSingleplayerServer().execute(()->{try{FlightBenchmark.start(mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst());}catch(Throwable ex){FlightBenchmark.failure=ex;}});}
        mc.options.keyUp.setDown(FlightBenchmark.started&&!FlightBenchmark.done&&FlightBenchmark.MODE.equals("airship")&&!FlightBenchmark.phase.equals("settle"));
        if(FlightBenchmark.done&&!finished){finished=true;try{
            mc.options.keyUp.setDown(false);Screenshot.grab(mc.gameDirectory,"flight-benchmark.png",mc.getMainRenderTarget(),m->{});
            var all=new ArrayList<String>();all.add("elapsed_s,phase,frame_interval_ms,render_ms,warp,client_near_loaded_pct,camera_x,camera_z,camera_wall_bps");all.addAll(frames);Files.write(FlightBenchmark.output.resolve("frames.csv"),all);mc.stop();
        }catch(Exception ex){throw new IllegalStateException(ex);}}
    }
    @SubscribeEvent public static void pre(RenderFrameEvent.Pre e){if(!FlightBenchmark.ENABLED||!FlightBenchmark.started||FlightBenchmark.done)return;frameStart=System.nanoTime();interval=lastFrame==0?0:(frameStart-lastFrame)/1e6;lastFrame=frameStart;}
    @SubscribeEvent public static void post(RenderFrameEvent.Post e){if(!FlightBenchmark.ENABLED||!FlightBenchmark.started||FlightBenchmark.done||frameStart==0)return;var mc=Minecraft.getInstance();var pos=mc.gameRenderer.getMainCamera().getPosition();int cx=((int)Math.floor(pos.x))>>4,cz=((int)Math.floor(pos.z))>>4,loaded=0;
        for(int x=cx-1;x<=cx+1;x++)for(int z=cz-1;z<=cz+1;z++)if(mc.level!=null&&mc.level.getChunkSource().getChunk(x,z,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false)!=null)loaded++;
        double cameraSpeed=previousCamera==null||interval<=0?0:pos.distanceTo(previousCamera)*1000/interval;previousCamera=pos;
        frames.add(String.format(Locale.ROOT,"%.6f,%s,%.4f,%.4f,%s,%.3f,%.5f,%.5f,%.4f",(System.nanoTime()-FlightBenchmark.epoch)/1e9,FlightBenchmark.phase,interval,(System.nanoTime()-frameStart)/1e6,dev.civilization.client.DhWarpTerrain.wanted(),loaded*100.0/9,pos.x,pos.z,cameraSpeed));}
}
