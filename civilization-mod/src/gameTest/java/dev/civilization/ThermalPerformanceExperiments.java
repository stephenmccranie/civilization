package dev.civilization;

import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;

/** Fixed-work benchmark with warmed rounds, allocation accounting and saved numerical references. */
final class ThermalPerformanceExperiments {
    static void run(ServerLevel level,Path directory) throws Exception {
        directory=directory.toAbsolutePath().normalize();
        if(Files.exists(directory.resolve("benchmark.csv")))throw new IllegalStateException("Preserve benchmark evidence; choose a fresh RunName");
        var reference=directory.getParent().getParent().resolve("bench-before/thermal-laboratory");
        boolean baseline=directory.equals(reference);
        if(!baseline&&!Files.exists(reference.resolve("benchmark.csv")))throw new IllegalStateException("Run Bench -RunName bench-before before modifying the solver");
        var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        if(!bean.isCurrentThreadCpuTimeSupported()||!bean.isThreadAllocatedMemorySupported())
            throw new IllegalStateException("Benchmark requires supported per-thread CPU and allocation counters");
        if(bean.isThreadCpuTimeSupported()&&!bean.isThreadCpuTimeEnabled())bean.setThreadCpuTimeEnabled(true);
        if(bean.isThreadAllocatedMemorySupported()&&!bean.isThreadAllocatedMemoryEnabled())bean.setThreadAllocatedMemoryEnabled(true);
        var rows=new StringBuilder("scene,round,initial_cells,final_cells,quarter_steps,wall_ms,cpu_ms,allocated_bytes,max_reference_error\n");
        for(String name:List.of("dense","sparse","partial")){
            var origin=new BlockPos(name.equals("sparse")?192:128,64,name.equals("partial")?192:128);
            int nx=name.equals("dense")?32:16,ny=name.equals("dense")?16:8,nz=nx,stride=name.equals("sparse")?3:1;
            var field=new ThermalField();
            for(var p:BlockPos.betweenClosed(origin.offset(-6,-6,-6),origin.offset(nx*stride+6,ny*stride+6,nz*stride+6))){level.getChunk(p);level.setBlock(p,Blocks.AIR.defaultBlockState(),2);}
            for(int x=0;x<nx;x++)for(int y=0;y<ny;y++)for(int z=0;z<nz;z++){
                var p=origin.offset(x*stride,y*stride,z*stride);
                if(name.equals("partial"))level.setBlockAndUpdate(p,((x+y+z)%2==0?Blocks.BRICK_SLAB:Blocks.OAK_SLAB).defaultBlockState());
                field.add(level,p,16);
            }
            var seed=field.save(new CompoundTag(),level.registryAccess());
            ThermalField expected=baseline?null:ThermalField.load(NbtIo.readCompressed(reference.resolve(name+"-state.nbt"),NbtAccounter.unlimitedHeap()),level.registryAccess());
            for(int round=-2;round<3;round++){
                var candidate=ThermalField.load(seed,level.registryAccess());
                long allocated=bean.getCurrentThreadAllocatedBytes(),cpu=bean.getCurrentThreadCpuTime(),wall=System.nanoTime();
                for(int step=0;step<8;step++)candidate.quarterStep(level,false);
                double wallMs=(System.nanoTime()-wall)/1e6,cpuMs=(bean.getCurrentThreadCpuTime()-cpu)/1e6;
                allocated=bean.getCurrentThreadAllocatedBytes()-allocated;
                double error=0;
                if(expected!=null){
                    if(!expected.energy.keySet().equals(candidate.energy.keySet()))throw new IllegalStateException(name+" changed reference cell membership");
                    for(long key:expected.energy.keySet())error=Math.max(error,Math.abs(expected.energy.get(key)-candidate.energy.get(key)));
                    error=Math.max(error,Math.abs(expected.atmosphereExchange-candidate.atmosphereExchange));
                    if(error>1e-8)throw new IllegalStateException(name+" changed physical output: "+error);
                }
                if(round>=0){
                    rows.append(String.format(Locale.ROOT,"%s,%d,%d,%d,8,%.6f,%.6f,%d,%.12g%n",name,round,field.activeCells(),candidate.activeCells(),wallMs,cpuMs,allocated,error));
                    if(round==0)NbtIo.writeCompressed(candidate.save(new CompoundTag(),level.registryAccess()),directory.resolve(name+"-state.nbt"));
                }
            }
        }
        Files.writeString(directory.resolve("benchmark.csv"),rows);
        System.out.println("THERMAL_BENCH_PASS baseline="+baseline+" file="+directory.resolve("benchmark.csv"));
    }
}
