package dev.civilization;

import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Opt-in development world. Uses actual source discovery, machine tickers and scheduled diffusion. */
@EventBusSubscriber(modid="civilization")
public final class ThermalLaboratory {
    private static final String PHASE=Fixture.phase("thermal");
    static final int Y=80;
    private static int ticks;
    private static Path output;
    private static double warmEnergy;
    private static double warmLedger;
    private record Station(String name, BlockPos source, boolean room, Block wall, boolean vent, boolean machine) {}
    private static final List<Station> STATIONS=List.of(
        station("torch",0,0,false,Blocks.STONE,false,false),
        station("campfire",24,0,false,Blocks.STONE,false,false),
        station("fire",48,0,false,Blocks.STONE,false,false),
        station("lava",72,0,false,Blocks.STONE,false,false),
        station("redstone_control",96,0,false,Blocks.STONE,false,false),
        station("wood_room",0,24,true,Blocks.OAK_PLANKS,false,false),
        station("brick_room",24,24,true,Blocks.BRICKS,false,false),
        station("vented_room",48,24,true,Blocks.BRICKS,true,false),
        station("foundry_outside",72,24,false,Blocks.STONE,false,true),
        station("foundry_room",96,24,true,Blocks.BRICKS,true,true));
    private static Station station(String name,int x,int z,boolean room,Block wall,boolean vent,boolean machine){
        return new Station(name,new BlockPos(x,Y,z),room,wall,vent,machine);
    }
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    @SubscribeEvent public static void commands(net.neoforged.neoforge.event.RegisterCommandsEvent e){
        if(PHASE.isEmpty())return;
        var root=net.minecraft.commands.Commands.literal("thermal_lab").requires(s->s.hasPermission(2));
        root.then(net.minecraft.commands.Commands.literal("sample").executes(c->{
            try{sample(c.getSource().getLevel(),"manual");}catch(Exception ex){throw new IllegalStateException(ex);}
            c.getSource().sendSuccess(()->Component.literal("Saved laboratory probes to measurements.csv"),false);return 1;
        }));
        root.then(net.minecraft.commands.Commands.literal("off").executes(c->{sourcesOff(c.getSource().getLevel());return 1;}));
        root.then(net.minecraft.commands.Commands.literal("rain").executes(c->{RegionalWeather.get(c.getSource().getLevel()).call(c.getSource().getLevel(),new BlockPos(0,Y,0));return 1;}));
        root.then(net.minecraft.commands.Commands.literal("clear").executes(c->{RegionalWeather.get(c.getSource().getLevel()).clear(c.getSource().getLevel(),new BlockPos(0,Y,0));return 1;}));
        var visit=net.minecraft.commands.Commands.literal("visit");
        for(var s:STATIONS)visit.then(net.minecraft.commands.Commands.literal(s.name()).executes(c->{
            var p=c.getSource().getPlayerOrException();var at=s.source().north(3);
            p.teleportTo(c.getSource().getLevel(),at.getX()+.5,at.getY(),at.getZ()+.5,Set.of(),0,0);return 1;
        }));
        root.then(visit);e.getDispatcher().register(root);
    }
    @SubscribeEvent public static void started(ServerStartedEvent e) throws Exception {
        if(PHASE.isEmpty())return;
        ticks=0;
        var l=e.getServer().overworld();
        require(ThermalConfig.enabled(),"Thermal lab requires enabled heat");
        output=e.getServer().getWorldPath(LevelResource.ROOT);
        var marker=output.resolve("thermal-lab-v1.txt");

        // Only this explicitly selected, isolated run is allowed to load the laboratory campus.
        for(int x=-1;x<=6;x++)for(int z=-1;z<=2;z++)l.setChunkForced(x,z,true);
        if(!Files.exists(marker)) {
            require(!PHASE.equals("reload"),"Missing laboratory persistence marker");
            build(l);
            Files.writeString(marker,"Civilization thermal laboratory v1\nNo automatic rebuild on later starts.\n");
        }
        if(PHASE.equals("check")){
            require(!Files.exists(output.resolve("measurements.csv")),"Check already ran; use a new isolated lab directory for a fresh baseline");
            Files.writeString(output.resolve("measurements.csv"),"tick,phase,station,probe,x,y,z,ambient_c,temperature_c,rise_c,solver_age_s,cells,pending_heat,field_energy,atmosphere,coal_items,work_reserve,output_items\n");
        }
        if(!Files.exists(output.resolve("measurements.csv")))Files.writeString(output.resolve("measurements.csv"),
            "tick,phase,station,probe,x,y,z,ambient_c,temperature_c,rise_c,solver_age_s,cells,pending_heat,field_energy,atmosphere,coal_items,work_reserve,output_items\n");
        if(PHASE.equals("reload")) {
            var tag=net.minecraft.nbt.NbtIo.readCompressed(output.resolve("thermal-check.nbt"),net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            var expected=ThermalField.load(tag,l.registryAccess());var actual=ThermalField.get(l);
            require(actual.energy.equals(expected.energy)&&actual.pending.equals(expected.pending)
                &&actual.timeline.save().equals(expected.timeline.save()),"Heat cells, pending fuel or timed releases changed across server restart");
            require(l.getBlockEntity(STATIONS.getLast().source()) instanceof KilnBlockEntity,"Foundry lost across restart");
            System.out.println("THERMAL_RELOAD_PASS cells="+actual.activeCells());
            e.getServer().halt(false);
        }
        System.out.println("THERMAL_LAB_READY world="+output.toAbsolutePath()+" phase="+PHASE);
    }
    private static void build(ServerLevel l){
        l.setDayTime(6000);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,l.getServer());
        l.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(false,l.getServer());
        l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,l.getServer());
        l.setDefaultSpawnPos(new BlockPos(48,Y,12),0);
        RegionalWeather.get(l).clear(l,new BlockPos(0,Y,0));
        for(var p:BlockPos.betweenClosed(new BlockPos(-7,Y-1,-7),new BlockPos(103,Y+12,32)))
            l.setBlock(p,p.getY()==Y-1?Blocks.STONE_BRICKS.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        for(var s:STATIONS){
            var p=s.source();
            if(s.room())for(var q:BlockPos.betweenClosed(p.offset(-4,0,-4),p.offset(4,8,4))){
                int dx=Math.abs(q.getX()-p.getX()),dz=Math.abs(q.getZ()-p.getZ());
                if(dx==4||dz==4||q.getY()==Y+8)l.setBlockAndUpdate(q,s.wall().defaultBlockState());
            }
            if(s.room()){
                // Real closed door: users can compare the same room with it opened.
                var door=p.north(4);
                l.setBlockAndUpdate(door,Blocks.OAK_DOOR.defaultBlockState());
                l.setBlockAndUpdate(door.above(),Blocks.OAK_DOOR.defaultBlockState().setValue(
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF,
                    net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
                if(s.vent())l.setBlockAndUpdate(p.south().above(8),Blocks.AIR.defaultBlockState());
            }
            if(s.machine()){
                var state=KilnContent.FOUNDRY.get().defaultBlockState().setValue(AbstractFurnaceBlock.FACING,Direction.NORTH);
                l.setBlockAndUpdate(p,state);
                MachineFixture.build(l,p);
                var kiln=(KilnBlockEntity)l.getBlockEntity(p);
                require(kiln.checkStructure().status()==MachineStructure.COMPLETE,"Lab foundry must form");
                kiln.setItem(0,new ItemStack(Items.RAW_IRON,32));
                kiln.setItem(1,KilnContent.MINERAL_COAL.toStack(8));
                // Seed a lit fire with warm masonry so work starts inside the heating phase (warming from cold takes
                // two minutes); supply() still purchases actual coal and tickers spend it normally.
                CoalFireFixture.set(kiln.fire,true,kiln.fire.capacity());
            } else {
                Block block=switch(s.name()){
                    case "torch"->Blocks.TORCH;case "fire"->Blocks.FIRE;
                    case "lava"->Blocks.LAVA;case "redstone_control"->Blocks.REDSTONE_TORCH;
                    default->Blocks.CAMPFIRE;
                };
                if(block==Blocks.FIRE)l.setBlockAndUpdate(p.below(),Blocks.NETHERRACK.defaultBlockState());
                if(block==Blocks.LAVA)for(var d:Direction.Plane.HORIZONTAL)l.setBlockAndUpdate(p.relative(d),Blocks.BRICKS.defaultBlockState());
                l.setBlockAndUpdate(p,block.defaultBlockState());
            }
            var sign=p.offset(-2,0,-6);l.setBlockAndUpdate(sign,Blocks.OAK_SIGN.defaultBlockState());
            if(l.getBlockEntity(sign) instanceof SignBlockEntity be){
                var text=be.getFrontText().setMessage(0,Component.literal(s.name()))
                    .setMessage(1,Component.literal("Probe: 2 north +1"))
                    .setMessage(2,Component.literal("/civilization heat"));
                be.setText(text,true);be.setChanged();
            }
        }
        var chest=new BlockPos(45,Y,12);l.setBlockAndUpdate(chest,Blocks.CHEST.defaultBlockState());
        var inventory=(net.minecraft.world.Container)l.getBlockEntity(chest);
        inventory.setItem(0,ThermalContent.HELMET.toStack());inventory.setItem(1,KilnContent.MINERAL_COAL.toStack(64));
        inventory.setItem(2,new ItemStack(Items.FLINT_AND_STEEL));inventory.setItem(3,new ItemStack(Items.RAW_IRON,64));
    }
    static void sourcesOff(ServerLevel l){
        for(var s:STATIONS)if(s.machine())((KilnBlockEntity)l.getBlockEntity(s.source())).fire.extinguish();
        else l.setBlockAndUpdate(s.source(),Blocks.AIR.defaultBlockState());
    }
    private static double total(ThermalField f){return f.energy.values().doubleStream().sum();}
    private static void sample(ServerLevel l,String phase) throws Exception {
        var f=ThermalField.get(l);var rows=new StringBuilder();
        double total=total(f),pending=f.pending.values().doubleStream().sum()+f.queuedHeat();
        for(var s:STATIONS){
            int coal=-1,reserve=-1,out=-1;
            if(s.machine()){
                var kiln=(KilnBlockEntity)l.getBlockEntity(s.source());
                coal=MachineInventory.count(kiln,MachineInventory.KILN_FUEL);reserve=kiln.fire.heat();
                out=MachineInventory.count(kiln,MachineInventory.KILN_OUTPUT);
            }
            var probes=List.of(s.source().north(2).above(),s.source().above(4),s.source().east(4).above(),s.source().above(10));
            String[] names={"torso","plume","wall","above_roof"};
            for(int i=0;i<probes.size();i++){
                var p=probes.get(i);double ambient=ThermalField.ambient(l,p),temp=ThermalField.temperature(l,p);
                require(Double.isFinite(temp),"Nonfinite temperature at "+p);
                rows.append(String.format(Locale.ROOT,"%d,%s,%s,%s,%d,%d,%d,%.6f,%.6f,%.6f,%.3f,%d,%.6f,%.6f,%.6f,%d,%d,%d%n",
                    phase.equals("manual")?l.getGameTime():ticks,phase,s.name(),names[i],p.getX(),p.getY(),p.getZ(),ambient,temp,temp-ambient,
                    f.simulationAgeSeconds(l),f.activeCells(),pending,total,f.atmosphereExchange,coal,reserve,out));
            }
        }
        Files.writeString(output.resolve("measurements.csv"),rows,StandardOpenOption.APPEND);
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post e) throws Exception {
        if(PHASE.equals("physics")){
            if(++ticks==20){ThermalPhysicsExperiments.run(e.getServer().overworld(),output);e.getServer().halt(false);}
            return;
        }
        if(PHASE.equals("seed")){
            if(++ticks==100){System.out.println("THERMAL_SEED_PASS");e.getServer().halt(false);}
            return;
        }
        if(!PHASE.equals("check"))return;
        var l=e.getServer().overworld();ticks++;
        if(ticks%100==0)sample(l,ticks<=1600?"heating":"cooling");
        if(ticks==1600){
            var f=ThermalField.get(l);warmEnergy=total(f);
            warmLedger=warmEnergy+f.pending.values().doubleStream().sum()+f.queuedHeat()+f.atmosphereExchange;
            for(var s:STATIONS)if(s.machine()){
                var kiln=(KilnBlockEntity)l.getBlockEntity(s.source());
                require(MachineInventory.count(kiln,MachineInventory.KILN_FUEL)<8,"Foundry must spend real coal");
                require(MachineInventory.count(kiln,MachineInventory.KILN_OUTPUT)>0,"Foundry must perform real work");
            }
            require(f.excess(l,STATIONS.get(0).source().above())>0,"Scheduled torch emission must warm air");
            require(f.excess(l,STATIONS.get(1).source().above())>0,"Scheduled campfire emission must warm air");
            require(f.excess(l,STATIONS.get(2).source().above())>0,"Scheduled ordinary fire emission must warm air");
            require(f.excess(l,STATIONS.get(3).source().above())>0,"Scheduled lava emission must warm air");
            System.out.println("THERMAL_LAB_FIRE_SUPPORTED="+BlockHeatSources.source(l.getBlockState(STATIONS.get(2).source())));
            sourcesOff(l);System.out.println("THERMAL_LAB_SOURCES_OFF energy="+warmEnergy);
        }
        if(ticks==2400){
            var f=ThermalField.get(l);
            require(total(f)<warmEnergy,"Stored heat must cool after sources stop");
            require(Math.abs(total(f)+f.pending.values().doubleStream().sum()+f.queuedHeat()+f.atmosphereExchange-warmLedger)<1e-6,
                "Shutdown storage, queued heat and atmosphere must balance without new source energy");
            require(f.completedExchanges>0&&f.simulationAgeSeconds(l)<10,"Scheduled solver must make timely progress");
            net.minecraft.nbt.NbtIo.writeCompressed(f.save(new net.minecraft.nbt.CompoundTag(),l.registryAccess()),output.resolve("thermal-check.nbt"));
            System.out.println("THERMAL_CHECK_PASS energy="+total(f)+" cells="+f.activeCells()+" age="+f.simulationAgeSeconds(l));
            e.getServer().halt(false);
        }
    }
}
