package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class ThermalClockGameTests {
    @GameTest(template="empty") public static void v2HeatUpgradePreservesEnergyAndStartsClockAtLoad(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(2,2,2));var old=new ThermalField();
        old.energy.put(p.asLong(),25);old.pending.put(p.asLong(),80);old.atmosphereExchange=7;
        var tag=old.save(new net.minecraft.nbt.CompoundTag(),l.registryAccess());tag.putInt("version",2);tag.remove("timeline");
        var upgraded=ThermalField.load(tag,l.registryAccess());upgraded.beginTick(1000);
        h.assertTrue(upgraded.energy.equals(old.energy)&&upgraded.pending.equals(old.pending)&&upgraded.atmosphereExchange==7,
            "V2 upgrade must retain stored heat, already-paid pending fuel and atmosphere accounting");
        h.assertTrue(upgraded.timeline.committed==1000&&upgraded.queuedHeat()==0,
            "V2 upgrade begins at current loaded time without inventing release history or offline heat");h.succeed();
    }
    private static double difference(ThermalField a,ThermalField b){
        if(!a.energy.keySet().equals(b.energy.keySet()))return Double.POSITIVE_INFINITY;
        double maximum=0;for(long key:a.energy.keySet())maximum=Math.max(maximum,Math.abs(a.energy.get(key)-b.energy.get(key)));return maximum;
    }
    @GameTest(template="industrial",timeoutTicks=200) public static void workPartitionDoesNotChangePhysicalHistory(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(6,4,6));
        for(var q:BlockPos.betweenClosed(p.offset(-3,-3,-3),p.offset(3,3,3)))l.setBlockAndUpdate(q,Blocks.AIR.defaultBlockState());
        var fast=new ThermalField();var slow=new ThermalField();fast.beginTick(0);slow.beginTick(0);
        ThermalField restored=null;
        for(int tick=0;tick<=120;tick++){
            fast.beginTick(tick);slow.beginTick(tick);if(restored!=null)restored.beginTick(tick);
            if(tick%20==0){fast.add(l,p,24);slow.add(l,p,24);if(restored!=null)restored.add(l,p,24);}
            fast.advanceTimed(l,tick,65536);slow.advanceTimed(l,tick,64);
            if(restored!=null)restored.advanceTimed(l,tick,128);
            if(tick==40){
                h.assertTrue(slow.exchanging(),"Slow worker must yield in the middle of a physical step");
                restored=ThermalField.load(slow.save(new net.minecraft.nbt.CompoundTag(),l.registryAccess()),l.registryAccess());restored.beginTick(tick);
            }
        }
        h.assertTrue(slow.timeline.committed<fast.timeline.committed,"Restricted work must create a real simulation-time backlog in this comparison");
        int loops=0;
        while((fast.timeline.committed<120||slow.timeline.committed<120||restored.timeline.committed<120)&&loops++<100000){
            fast.advanceTimed(l,120,65536);slow.advanceTimed(l,120,64);restored.advanceTimed(l,120,128);
        }
        h.assertTrue(loops<100000,"All bounded workers finish the same timestamped history");
        // Restoring a hash map can change summation order. Require identical cell
        // membership and numerical equivalence well below any physical/UI scale.
        double error=Math.max(difference(fast,slow),difference(fast,restored));
        h.assertTrue(error<1e-10,"Worker slicing/restart must preserve the physical history to floating-point precision; maximum energy error="+error);
        h.assertTrue(Math.abs(fast.atmosphereExchange-slow.atmosphereExchange)<1e-8&&Math.abs(fast.atmosphereExchange-restored.atmosphereExchange)<1e-8,"Atmospheric loss is independent of worker slicing");
        for(var f:java.util.List.of(fast,slow,restored))h.assertTrue(Math.abs(f.energy.values().doubleStream().sum()+f.queuedHeat()+f.atmosphereExchange-168)<1e-7,"Every release is either simulated, queued or accounted to the atmosphere");
        System.out.println("THERMAL_CLOCK_PASS slices=64,128,65536 steps="+fast.completedExchanges+" drainCalls="+loops+" maxError="+error);h.succeed();
    }
    @GameTest(template="empty") public static void timelineIsBoundedAndRebasesWithoutOfflineSimulation(GameTestHelper h){
        var clock=new ThermalTimeline(8);clock.resume(0);
        for(int i=0;i<8;i++)h.assertTrue(clock.add(i,2,0),"Finite queue admits its declared bound");
        h.assertTrue(!clock.add(9,2,0)&&clock.entries()==8,"Queue saturation rejects additional entries");
        var saved=clock.save();var loaded=new ThermalTimeline(8);loaded.load(saved);loaded.resume(1000);
        h.assertTrue(loaded.committed==1000&&loaded.heat()==16&&loaded.through(1000).isEmpty(),"Restart shifts the clock without consuming heat or simulating offline time");
        loaded.commit(1005);h.assertTrue(loaded.empty()&&loaded.headroom()==8,"Only the due physical step retires releases");h.succeed();
    }
}
