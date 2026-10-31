package dev.civilization;

import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** Development-only comparison of reference, skipped-step and bounded coarse integration. */
final class ThermalBatchingExperiments {
    private record Mode(String name,int quarters,boolean burst,int passes,boolean cooling,boolean conduction) {
        Mode(String name,int quarters,boolean burst){this(name,quarters,burst,burst?quarters:1,false,false);}
    }
    private record Result(double warm,double cold,double centerWarm,double centerCold,double airWarm,double airCold,int cells,int steps,
                          double error,double wall,double cpu,long allocated,ThermalField field) {}
    private static final List<Mode> MODES=List.of(new Mode("quarter",1,false),new Mode("burst-four",4,true),
        new Mode("half-second",2,false),new Mode("one-second",4,false),new Mode("two-second",8,false),
        new Mode("one-cooled",4,false,1,true,false),new Mode("one-conductive",4,false,1,true,true),
        new Mode("one-two-pass",4,false,2,true,true));
    private static final BlockPos SOURCE=new BlockPos(136,65,136);
    private static double energy(ThermalField field){return field.energy.values().doubleStream().sum();}
    private static double air(ServerLevel level,ThermalField field){
        double sum=0;int cells=0;
        for(var p:BlockPos.betweenClosed(SOURCE.offset(-2,0,-2),SOURCE.offset(2,2,2))){sum+=field.excess(level,p);cells++;}
        return sum/cells;
    }
    private static void fixture(ServerLevel level,String scene){
        for(var p:BlockPos.betweenClosed(SOURCE.offset(-9,-5,-9),SOURCE.offset(9,12,9))){level.getChunk(p);level.setBlock(p,Blocks.AIR.defaultBlockState(),2);}
        for(var p:BlockPos.betweenClosed(SOURCE.offset(-8,-2,-8),SOURCE.offset(8,-2,8)))level.setBlock(p,Blocks.STONE.defaultBlockState(),2);
        if(scene.equals("outdoor"))return;
        var wall=scene.equals("wood")?Blocks.OAK_PLANKS.defaultBlockState():Blocks.BRICKS.defaultBlockState();
        for(var p:BlockPos.betweenClosed(SOURCE.offset(-3,-1,-3),SOURCE.offset(3,3,3)))
            if(Math.abs(p.getX()-SOURCE.getX())==3||Math.abs(p.getZ()-SOURCE.getZ())==3||p.getY()==SOURCE.getY()-1||p.getY()==SOURCE.getY()+3)
                level.setBlock(p,wall,2);
        if(scene.equals("vented"))level.setBlock(SOURCE.above(3),Blocks.AIR.defaultBlockState(),2);
    }
    private static Result integrate(ServerLevel level,Mode mode,int heatQuarters,int totalQuarters,com.sun.management.ThreadMXBean bean){
        var field=new ThermalField();double warm=0,centerWarm=0,airWarm=0,error=0;int steps=0;
        long bytes=bean.getCurrentThreadAllocatedBytes(),cpu=bean.getCurrentThreadCpuTime(),start=System.nanoTime();
        for(int tick=0;tick<totalQuarters;tick+=mode.quarters){
            int passes=mode.passes;
            for(int sub=0;sub<passes;sub++){
                int duration=mode.quarters/passes,at=tick+sub*duration;
                if(at<heatQuarters)field.add(level,SOURCE,6d*Math.min(duration,heatQuarters-at));
                if(mode.cooling)field.coarseStep(level,duration,mode.conduction);
                else field.quarterStep(level,false);
                steps++;
            }
            if(tick+mode.quarters==heatQuarters){warm=energy(field);centerWarm=field.excess(level,SOURCE);airWarm=air(level,field);}
        }
        double wall=(System.nanoTime()-start)/1e6,cpuMs=(bean.getCurrentThreadCpuTime()-cpu)/1e6;
        bytes=bean.getCurrentThreadAllocatedBytes()-bytes;
        error=Math.abs(energy(field)+field.atmosphereExchange-heatQuarters*6d);
        return new Result(warm,energy(field),centerWarm,field.excess(level,SOURCE),airWarm,air(level,field),field.activeCells(),steps,error,wall,cpuMs,bytes,field);
    }
    static void run(ServerLevel level,Path output) throws Exception {
        if(Files.exists(output.resolve("batching.csv")))throw new IllegalStateException("Preserve batching evidence; choose a fresh RunName");
        var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        if(!bean.isCurrentThreadCpuTimeSupported()||!bean.isThreadAllocatedMemorySupported())throw new IllegalStateException("JVM counters unavailable");
        bean.setThreadCpuTimeEnabled(true);bean.setThreadAllocatedMemoryEnabled(true);
        var csv=new StringBuilder("scene,mode,round,repeats,steps,supplied,warm_energy,cold_energy,warm_source_rise,cold_source_rise,warm_air_rise,cold_air_rise,cells,ledger_error,wall_ms,cpu_ms,allocated_bytes\n");
        var failures=new ArrayList<String>();int checks=0;
        for(String scene:List.of("outdoor","brick","wood","vented")){
            fixture(level,scene);
            for(int warm=0;warm<2;warm++)for(var mode:MODES)integrate(level,mode,96,160,bean);
            var reference=integrate(level,MODES.getFirst(),96,160,bean).field;
            for(int round=0;round<3;round++)for(int index=0;index<MODES.size();index++){
                    var mode=MODES.get((index+round)%MODES.size());
                    Result r=null;double elapsed=0,cpu=0;long allocated=0;
                    // Aggregate eight complete histories to reduce timer/counter noise.
                    for(int repeat=0;repeat<8;repeat++){
                        r=integrate(level,mode,96,160,bean);elapsed+=r.wall;cpu+=r.cpu;allocated+=r.allocated;
                        if(r.error>1e-6||!Double.isFinite(r.warm)||!Double.isFinite(r.cold)||r.warm<=0||r.cold>=r.warm)
                            failures.add(scene+"/"+mode.name+"/"+round+"/"+repeat+": ledger, warming or source-off cooling");
                        checks++;
                    }
                    if(mode.burst){
                        double difference=0;for(long k:reference.energy.keySet())difference=Math.max(difference,Math.abs(reference.energy.get(k)-r.field.energy.get(k)));
                        checks++;if(!reference.energy.keySet().equals(r.field.energy.keySet())||difference>1e-10||Math.abs(reference.atmosphereExchange-r.field.atmosphereExchange)>1e-8)
                            failures.add(scene+"/burst: changed unchanged-step reference");
                    }
                    csv.append(String.format(Locale.ROOT,"%s,%s,%d,8,%d,576,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%d,%.12g,%.6f,%.6f,%d%n",
                        scene,mode.name,round,r.steps,r.warm,r.cold,r.centerWarm,r.centerCold,r.airWarm,r.airCold,r.cells,r.error,elapsed,cpu,allocated));
            }
        }
        Files.writeString(output.resolve("batching.csv"),csv);
        Files.writeString(output.resolve("batching-result.txt"),"checks="+checks+"\nfailures="+failures.size()+"\n"+String.join("\n",failures));
        System.out.println("THERMAL_BATCHING_"+(failures.isEmpty()?"PASS":"FAIL")+" checks="+checks+" failures="+failures);
    }
}
