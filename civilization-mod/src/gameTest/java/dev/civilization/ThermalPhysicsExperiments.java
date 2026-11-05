package dev.civilization;

import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Measured physical invariants; failures are collected so one defect cannot hide later experiments. */
final class ThermalPhysicsExperiments {
    private final ServerLevel level;
    private final BlockPos p=new BlockPos(136,64,136);
    private final StringBuilder csv=new StringBuilder("experiment,value,reference,passed\n");
    private final List<String> failures=new ArrayList<>();
    private int checks;
    private ThermalPhysicsExperiments(ServerLevel l){level=l;}
    static void run(ServerLevel l,Path output) throws Exception {
        var suite=new ThermalPhysicsExperiments(l);suite.run();
        Files.writeString(output.resolve("physics.csv"),suite.csv);
        Files.writeString(output.resolve("physics-result.txt"),"checks="+suite.checks+"\nfailures="+suite.failures.size()+"\n"+String.join("\n",suite.failures));
        System.out.println("THERMAL_PHYSICS_"+(suite.failures.isEmpty()?"PASS":"FAIL")+" checks="+suite.checks+" failures="+suite.failures);
    }
    private void record(String name,double value,double reference,boolean passed){
        checks++;csv.append(String.format(Locale.ROOT,"%s,%.9f,%.9f,%s%n",name,value,reference,passed));
        if(!passed)failures.add(name);
    }
    private double energy(ThermalField f){return f.energy.values().doubleStream().sum();}
    private double ledger(ThermalField f){return energy(f)+f.pending.values().doubleStream().sum()+f.atmosphereExchange;}
    private void clear(){
        for(var q:BlockPos.betweenClosed(p.offset(-7,-5,-7),p.offset(7,9,7))){
            level.getChunk(q);level.setBlock(q,Blocks.AIR.defaultBlockState(),2);
        }
    }
    private void box(BlockState wall,int radius){
        for(var q:BlockPos.betweenClosed(p.offset(-radius,-radius,-radius),p.offset(radius,radius,radius)))
            level.setBlockAndUpdate(q,Math.abs(q.getX()-p.getX())==radius||Math.abs(q.getY()-p.getY())==radius||Math.abs(q.getZ()-p.getZ())==radius?wall:Blocks.AIR.defaultBlockState());
    }
    private void run(){
        // Equal initial temperatures must not produce a hotter ceiling without a heat source.
        clear();box(Blocks.BRICKS.defaultBlockState(),2);var uniform=new ThermalField();
        for(var q:BlockPos.betweenClosed(p.offset(-1,-1,-1),p.offset(1,1,1)))uniform.add(level,q,20);
        double before=ledger(uniform);uniform.quarterStep(level,false);
        double maximum=uniform.energy.keySet().longStream().mapToDouble(k->uniform.excess(level,BlockPos.of(k))).max().orElse(0);
        record("uniform_room_no_new_hotspot",maximum,20,maximum<=20+1e-8);
        record("uniform_room_conservation",ledger(uniform),before,Math.abs(ledger(uniform)-before)<1e-7);
        for(var slab:List.of(Blocks.BRICK_SLAB,Blocks.OAK_SLAB)){
            var material=SlabIntegration.material(slab.defaultBlockState());
            for(var type:net.minecraft.world.level.block.state.properties.SlabType.values()){
                clear();level.setBlockAndUpdate(p,slab.defaultBlockState().setValue(SlabBlock.TYPE,type));
                double capacity=type==net.minecraft.world.level.block.state.properties.SlabType.DOUBLE?ThermalField.capacity(material):(ThermalField.capacity(material)+1)/2;
                var f=new ThermalField();f.add(level,p,capacity*10);
                record(slab.getDescriptionId()+"_"+type+"_capacity",f.excess(level,p),10,Math.abs(f.excess(level,p)-10)<1e-9);
                if(type==net.minecraft.world.level.block.state.properties.SlabType.DOUBLE){
                    f.quarterStep(level,false);double transfer=f.energy.get(p.east().asLong());
                    level.setBlockAndUpdate(p,material);var full=new ThermalField();full.add(level,p,capacity*10);full.quarterStep(level,false);
                    record(slab.getDescriptionId()+"_full_conduction",transfer,full.energy.get(p.east().asLong()),Math.abs(transfer-full.energy.get(p.east().asLong()))<1e-9);
                }
            }
        }

        for(var block:List.of(Blocks.OAK_PLANKS,Blocks.BRICKS,Blocks.GLASS,Blocks.IRON_BLOCK,Blocks.WATER)){
            for(int sign:new int[]{-1,1}){
                clear();level.setBlockAndUpdate(p,block.defaultBlockState());
                var field=new ThermalField();field.add(level,p,sign*100);
                double initial=field.excess(level,p),balance=ledger(field);
                for(int i=0;i<12;i++)field.quarterStep(level,false);
                double min=0,max=0;for(long key:field.energy.keySet()){
                    double t=field.excess(level,BlockPos.of(key));min=Math.min(min,t);max=Math.max(max,t);
                }
                String name=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath()+"_"+sign;
                record(name+"_stable",sign>0?max:min,initial,sign>0?min>=-1e-6&&max<=initial+1e-6:max<=1e-6&&min>=initial-1e-6);
                record(name+"_ledger",ledger(field),balance,Math.abs(ledger(field)-balance)<1e-6);
            }
        }
        // Same pulse, same wall, six coordinate directions: material conduction is isotropic.
        double previous=-1;
        for(var d:Direction.values()){
            clear();for(var q:BlockPos.betweenClosed(p.offset(-3,-3,-3),p.offset(3,3,3)))level.setBlock(q,Blocks.BRICKS.defaultBlockState(),2);
            var field=new ThermalField();field.add(level,p,100);field.quarterStep(level,false);
            double received=field.energy.get(p.relative(d).asLong());
            record("solid_direction_"+d.getName(),received,previous,previous<0||Math.abs(received-previous)<1e-9);previous=received;
        }
        for(var block:List.of(Blocks.OAK_DOOR,Blocks.OAK_TRAPDOOR)){
            double[] moved=new double[2];
            for(int open=0;open<2;open++){
                clear();box(Blocks.BRICKS.defaultBlockState(),3);
                level.setBlockAndUpdate(p.east(),block.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN,open==1));
                var field=new ThermalField();field.add(level,p,60);field.quarterStep(level,false);
                moved[open]=field.energy.get(p.east().asLong());
            }
            record(block==Blocks.OAK_DOOR?"door_open_airflow":"trapdoor_open_airflow",moved[1],moved[0],moved[1]>moved[0]*2);
        }
        clear();level.setBlockAndUpdate(p,Blocks.LAVA.defaultBlockState());
        var reservoir=new ThermalField();reservoir.add(level,p.above(),20);reservoir.quarterStep(level,false);
        record("lava_not_a_cold_sink",reservoir.energy.get(p.asLong()),0,Math.abs(reservoir.energy.get(p.asLong()))<1e-9);
        var hotter=new ThermalField();hotter.add(level,p.above(),1100-ThermalField.ambient(level,p.above()));
        double hotBefore=hotter.energy.get(p.above().asLong());BlockHeatSources.emit(level,hotter);
        record("lava_absorbs_hotter_neighbor",hotBefore-hotter.energy.get(p.above().asLong()),8,
            Math.abs(hotBefore-hotter.energy.get(p.above().asLong())-8)<1e-8);

        for(var block:List.of(Blocks.TORCH,Blocks.SOUL_TORCH,Blocks.CAMPFIRE,Blocks.SOUL_CAMPFIRE,Blocks.FIRE,Blocks.SOUL_FIRE,Blocks.REDSTONE_TORCH)){
            clear();level.setBlockAndUpdate(p.below(),block==Blocks.SOUL_FIRE?Blocks.SOUL_SOIL.defaultBlockState():Blocks.NETHERRACK.defaultBlockState());
            level.setBlockAndUpdate(p,block.defaultBlockState());BlockHeatSources.changed(level,p);
            var field=new ThermalField();BlockHeatSources.emit(level,field);
            double measured=0;for(var q:BlockPos.betweenClosed(p.offset(-1,-1,-1),p.offset(1,1,1)))measured+=field.energy.get(q.asLong());
            double expected=block==Blocks.REDSTONE_TORCH?0:BlockHeatSources.torch(block.defaultBlockState())?ThermalRules.TORCH_HEAT:ThermalRules.CAMPFIRE_HEAT;
            String name=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath();
            record(name+"_source_budget",measured,expected,Math.abs(measured-expected)<1e-8);
            level.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());BlockHeatSources.changed(level,p);
            field=new ThermalField();BlockHeatSources.emit(level,field);
            record(name+"_removed",field.energy.get(p.asLong()),0,field.energy.get(p.asLong())==0);
        }
        double wood=wallLeak(Blocks.OAK_PLANKS,1),brick=wallLeak(Blocks.BRICKS,1),thick=wallLeak(Blocks.BRICKS,2);
        // Short warmup also measures thermal mass: lighter timber can initially
        // pass more heat than cold heavy brick. Compare conductivity at equal
        // surface temperature separately instead of asserting the wrong ordering.
        record("transient_wood_wall_transfer",wood,brick,Double.isFinite(wood)&&wood>0);
        double woodSurface=surfaceTransfer(Blocks.OAK_PLANKS),brickSurface=surfaceTransfer(Blocks.BRICKS);
        record("equal_temperature_wood_insulates",woodSurface,brickSurface,woodSurface<brickSurface);
        record("thicker_wall_leaks_less",thick,brick,thick<brick);
        clear();box(Blocks.BRICKS.defaultBlockState(),3);var field=new ThermalField();field.add(level,p,100);
        for(int i=0;i<40;i++)field.quarterStep(level,false);
        record("heat_rises",field.excess(level,p.above()),field.excess(level,p.below()),field.excess(level,p.above())>field.excess(level,p.below()));
        record("closed_pulse_conservation",ledger(field),100,Math.abs(ledger(field)-100)<1e-6);
        var restored=ThermalField.load(field.save(new net.minecraft.nbt.CompoundTag(),level.registryAccess()),level.registryAccess());
        record("pulse_save_exact",energy(restored),energy(field),restored.energy.equals(field.energy)&&restored.atmosphereExchange==field.atmosphereExchange);
        double closed=room("brick",Blocks.BRICKS,1,"closed");
        double doorway=room("brick_door",Blocks.BRICKS,1,"door");
        double roof=room("brick_roof",Blocks.BRICKS,1,"roof");
        room("wood",Blocks.OAK_PLANKS,1,"closed");
        room("glass",Blocks.GLASS,1,"closed");
        room("brick_thick",Blocks.BRICKS,2,"closed");
        room("brick_shaft",Blocks.BRICKS,1,"shaft");
        record("open_door_releases_room_heat",doorway,closed,doorway<closed);
        record("roof_vent_releases_room_heat",roof,closed,roof<closed);
        double[] workshops=new double[2];
        for(int shaft=0;shaft<2;shaft++){
            clear();ThermalGameTests.buildFoundryWorkshop(level,p,shaft==1);
            var foundry=new ThermalField();double rate=ThermalRules.COAL_WASTE_HEAT*20/ProductionEnergy.COAL;
            for(int second=1;second<=180;second++){
                foundry.pending.put(p.asLong(),rate);foundry.step(level);
                if(second%60==0)record("foundry_"+(shaft==1?"vented":"closed")+"_rise_"+second+"s",foundry.excess(level,p.north(2).above())*1.8,rate*second,
                    Math.abs(ledger(foundry)-rate*second)<1e-5);
            }
            workshops[shaft]=foundry.excess(level,p.north(2).above())*1.8;
        }
        record("foundry_chimney_reduces_occupied_heat",workshops[1],workshops[0],workshops[1]<workshops[0]);
    }
    private double wallLeak(Block block,int thickness){
        clear();box(Blocks.BRICKS.defaultBlockState(),4);
        for(int x=1;x<=thickness;x++)for(int y=-3;y<=3;y++)for(int z=-3;z<=3;z++)level.setBlock(p.offset(x,y,z),block.defaultBlockState(),2);
        var f=new ThermalField();
        for(int i=0;i<80;i++){f.add(level,p,1);f.quarterStep(level,false);}
        double behind=0;for(long key:f.energy.keySet())if(BlockPos.of(key).getX()>p.getX()+thickness)behind+=f.energy.get(key);
        return behind;
    }
    private double surfaceTransfer(Block block){
        clear();level.setBlockAndUpdate(p,block.defaultBlockState());
        var f=new ThermalField();f.add(level,p,100*ThermalField.capacity(block.defaultBlockState()));
        f.quarterStep(level,false);return f.energy.get(p.east().asLong());
    }
    private double room(String name,Block wall,int thickness,String opening){
        clear();
        int outer=1+thickness;
        for(var q:BlockPos.betweenClosed(p.offset(-outer,-outer,-outer),p.offset(outer,outer,outer)))level.setBlock(q,wall.defaultBlockState(),2);
        // Reopen the same 3x3x3 interior after adding outward wall thickness.
        for(var q:BlockPos.betweenClosed(p.offset(-1,-1,-1),p.offset(1,1,1)))level.setBlock(q,Blocks.AIR.defaultBlockState(),2);
        if(opening.equals("door")){
            level.setBlockAndUpdate(p.north(2),Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.OPEN,true));
            level.setBlockAndUpdate(p.north(2).above(),Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.OPEN,true)
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        }
        if(opening.equals("roof")||opening.equals("shaft"))level.setBlockAndUpdate(p.above(2),Blocks.AIR.defaultBlockState());
        if(opening.equals("shaft"))for(int y=3;y<=5;y++)for(var d:Direction.Plane.HORIZONTAL)level.setBlockAndUpdate(p.above(y).relative(d),wall.defaultBlockState());
        var f=new ThermalField();double warm=0;
        for(int second=1;second<=120;second++){
            if(second<=60)f.add(level,p.below(),24);
            f.step(level);
            if(second%15==0){
                double mean=0;for(var q:BlockPos.betweenClosed(p.offset(-1,-1,-1),p.offset(1,1,1)))mean+=f.excess(level,q)/27;
                record(name+"_room_rise_"+second+"s",mean,second<=60?second*24:1440,Double.isFinite(mean)&&mean>=0);
                record(name+"_ledger_"+second+"s",ledger(f),second<=60?second*24:1440,Math.abs(ledger(f)-(second<=60?second*24:1440))<1e-6);
                if(second==60)warm=mean;
            }
        }
        return warm;
    }
}
