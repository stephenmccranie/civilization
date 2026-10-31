package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class ThermalConsistencyGameTests {
    @GameTest(template="industrial") public static void openSkyAdvectionCannotCreateHotterAir(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(6,3,6));
        for(var q:BlockPos.betweenClosed(p.offset(-2,0,-2),p.offset(2,9,2)))l.setBlockAndUpdate(q,Blocks.AIR.defaultBlockState());
        var f=new ThermalField();double maximum=Double.NEGATIVE_INFINITY;
        for(int y=0;y<7;y++){f.add(l,p.above(y),20);maximum=Math.max(maximum,f.localTemperature(l,p.above(y)));}
        for(int step=0;step<20;step++){
            f.quarterStep(l,false);
            for(long key:f.energy.keySet())h.assertTrue(f.localTemperature(l,BlockPos.of(key))<=maximum+1e-8,"Open-column flow cannot create a hotter parcel than the source column");
        }
        h.assertTrue(Math.abs(f.energy.values().doubleStream().sum()+f.atmosphereExchange-140)<1e-7,"Open-column transport conserves heat including atmosphere loss");h.succeed();
    }
    @GameTest(template="industrial") public static void doubleSlabsHaveFullMaterialHeatCapacityAndConduction(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(6,4,6));
        for(var slab:java.util.List.of(Blocks.BRICK_SLAB,Blocks.OAK_SLAB)){
            for(var q:BlockPos.betweenClosed(p.offset(-2,-2,-2),p.offset(2,2,2)))l.setBlockAndUpdate(q,Blocks.AIR.defaultBlockState());
            var state=slab.defaultBlockState().setValue(SlabBlock.TYPE,net.minecraft.world.level.block.state.properties.SlabType.DOUBLE);
            var material=SlabIntegration.material(state);double heat=ThermalField.capacity(material)*10;
            l.setBlockAndUpdate(p,state);var joined=new ThermalField();joined.add(l,p,heat);
            h.assertTrue(Math.abs(joined.excess(l,p)-10)<1e-9,"Two joined slabs must store heat like the full material, not empty air");
            joined.quarterStep(l,false);double transfer=joined.energy.get(p.east().asLong());
            l.setBlockAndUpdate(p,material);var full=new ThermalField();full.add(l,p,heat);full.quarterStep(l,false);
            h.assertTrue(Math.abs(transfer-full.energy.get(p.east().asLong()))<1e-9,"Joined slab face conduction must match its full material");
        }h.succeed();
    }
    @GameTest(template="industrial") public static void uniformWarmRoomCannotCreateCeilingHotspot(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(6,4,6));
        for(var q:BlockPos.betweenClosed(p.offset(-2,-2,-2),p.offset(2,2,2)))l.setBlockAndUpdate(q,Blocks.BRICKS.defaultBlockState());
        var f=new ThermalField();double max=Double.NEGATIVE_INFINITY;
        for(var q:BlockPos.betweenClosed(p.offset(-1,-1,-1),p.offset(1,1,1))){l.setBlockAndUpdate(q,Blocks.AIR.defaultBlockState());f.add(l,q,20);max=Math.max(max,f.localTemperature(l,q));}
        f.quarterStep(l,false);
        for(long key:f.energy.keySet())h.assertTrue(f.localTemperature(l,BlockPos.of(key))<=max+1e-8,"Passive transfer cannot exceed the original hottest temperature");
        h.assertTrue(Math.abs(f.energy.values().doubleStream().sum()+f.atmosphereExchange-540)<1e-7,"Ceiling stability preserves all heat");h.succeed();
    }
    @GameTest(template="industrial") public static void openTrapdoorPassesAirHeat(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(6,4,6));double[] heat=new double[2];
        for(int open=0;open<2;open++){
            for(var q:BlockPos.betweenClosed(p.offset(-2,-2,-2),p.offset(2,2,2)))l.setBlockAndUpdate(q,Blocks.AIR.defaultBlockState());
            l.setBlockAndUpdate(p.east(),Blocks.OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN,open==1));
            var f=new ThermalField();f.add(l,p,60);f.quarterStep(l,false);heat[open]=f.energy.get(p.east().asLong());
        }
        h.assertTrue(heat[1]>heat[0]*2,"Opening the trapdoor opens its thermal airflow interface");h.succeed();
    }
    @GameTest(template="industrial") public static void lavaHasOnlyOneReservoirExchangePath(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(6,4,6));
        for(var q:BlockPos.betweenClosed(p.offset(-2,-2,-2),p.offset(2,2,2)))l.setBlockAndUpdate(q,Blocks.AIR.defaultBlockState());
        l.setBlockAndUpdate(p,Blocks.LAVA.defaultBlockState());var f=new ThermalField();f.add(l,p.above(),20);f.quarterStep(l,false);
        h.assertTrue(!f.energy.containsKey(p.asLong()),"A maintained hot lava reservoir cannot act as cold storage in the diffusion path");
        double before=f.energy.get(p.above().asLong());BlockHeatSources.emit(l,f);
        h.assertTrue(f.energy.get(p.above().asLong())>before,"The bounded reservoir surface still warms adjacent air");
        var hot=new ThermalField();hot.add(l,p.above(),1100-ThermalField.ambient(l,p.above()));
        before=hot.energy.get(p.above().asLong());BlockHeatSources.emit(l,hot);
        h.assertTrue(Math.abs(before-hot.energy.get(p.above().asLong())-8)<1e-8,"A 1000C reservoir absorbs bounded heat from 1100C air instead of acting as a one-way source");h.succeed();
    }
    @GameTest(template="industrial") public static void ordinaryAndSoulFireEmitOnlyWhilePresent(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(6,4,6));
        for(var fire:java.util.List.of(Blocks.FIRE,Blocks.SOUL_FIRE)){
            for(var q:BlockPos.betweenClosed(p.offset(-2,-2,-2),p.offset(2,2,2)))l.setBlockAndUpdate(q,Blocks.AIR.defaultBlockState());
            l.setBlockAndUpdate(p.below(),fire==Blocks.FIRE?Blocks.NETHERRACK.defaultBlockState():Blocks.SOUL_SOIL.defaultBlockState());
            l.setBlockAndUpdate(p,fire.defaultBlockState());var f=new ThermalField();BlockHeatSources.emit(l,f);
            double heat=0;for(var q:BlockPos.betweenClosed(p.offset(-1,-1,-1),p.offset(1,1,1)))heat+=f.energy.get(q.asLong());
            h.assertTrue(Math.abs(heat-ThermalRules.FIRE_HEAT)<1e-8,"Exactly one flame heat budget enters the shared field");
            l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());f=new ThermalField();BlockHeatSources.emit(l,f);
            h.assertTrue(!f.energy.containsKey(p.asLong()),"Removing flame stops new heat");
        }h.succeed();
    }
}
