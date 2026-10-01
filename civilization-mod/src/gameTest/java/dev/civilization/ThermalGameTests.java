package dev.civilization;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class ThermalGameTests {
    private static double productiveHeatPerSecond(){return ThermalRules.COAL_WASTE_HEAT*20/ProductionEnergy.HEAT_TICKS;}
    @GameTest(template="empty") public static void surveyPacketPreservesPositionsAndTemperatures(GameTestHelper h){
        var origin=new BlockPos(100,70,-50);
        var packet=new ThermalPayload(21,20,.8f,2,true,java.util.List.of(
                new ThermalPayload.Sample(origin,-4.26f),
                new ThermalPayload.Sample(origin.offset(16,12,16),1537.24f)));
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try{
            ThermalPayload.write(buffer,packet);
            h.assertTrue(buffer.readableBytes()<40,"Compact survey stores one origin and short readings");
            var restored=ThermalPayload.read(buffer);
            h.assertTrue(restored.samples().get(1).pos().equals(origin.offset(16,12,16)),"Relative samples retain world positions");
            h.assertTrue(Math.abs(restored.samples().get(0).temperature()+4.3f)<.001f
                    &&Math.abs(restored.samples().get(1).temperature()-1537.2f)<.001f,"Survey keeps useful tenth-degree precision");
        }finally{buffer.release();}
        h.succeed();
    }
    @GameTest(template="empty") public static void risingAirKeepsAStableExchangeBudget(GameTestHelper h){
        double indoor=4*ThermalRules.AIR_MIXING+ThermalRules.AIR_MIXING
                +ThermalRules.UPWARD_MIXING+ThermalRules.BUOYANT_RISE;
        double exposed=4*ThermalRules.EXPOSED_AIR_MIXING+ThermalRules.AIR_MIXING
                +ThermalRules.UPWARD_MIXING+ThermalRules.EXPOSED_BUOYANT_RISE;
        h.assertTrue(indoor<1&&exposed<1,"A hot air cell cannot move more than its available heat in one exchange");
        h.assertTrue(ThermalRules.EXPOSED_BUOYANT_RISE>ThermalRules.BUOYANT_RISE,
                "Open-to-sky columns give hot air a stronger upward path than enclosed rooms");
        h.succeed();
    }
    @GameTest(template="empty") public static void sourceQueueIsFairAndDoesNotCatchUp(GameTestHelper h){
        var queue=new HeatSourceQueue();var sources=new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        for(int i=0;i<40;i++)sources.add(i);
        var seen=new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        for(int tick=0;tick<1000&&seen.size()<40;tick++){
            int before=seen.size();
            queue.tick(tick,sources,key->h.assertTrue(seen.add(key),"No source repeats within a round"));
            h.assertTrue(seen.size()-before<=2,"Spread sources over twenty ticks");
        }
        h.assertTrue(seen.size()==40,"No source starves");seen.clear();
        queue.tick(100000,sources,seen::add);
        h.assertTrue(seen.size()<=2,"A long pause creates no catch-up burst");
        var removing=new HeatSourceQueue();seen.clear();
        for(int tick=0;tick<1000&&!sources.isEmpty();tick++)removing.tick(tick,sources,key->{seen.add(key);sources.remove(key);});
        h.assertTrue(seen.size()==40&&sources.isEmpty(),"Callbacks may remove sources without invalidating traversal");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=200) public static void sourceDiscoveryAndChunkRemoval(GameTestHelper h){
        var l=h.getLevel();var p=new BlockPos(5122,181,5122);l.getChunk(p);
        l.setBlockAndUpdate(p.below(),Blocks.STONE.defaultBlockState());l.setBlockAndUpdate(p,Blocks.TORCH.defaultBlockState());
        long chunk=new net.minecraft.world.level.ChunkPos(p).toLong();
        BlockHeatSources.forget(l,chunk);
        var cold=new ThermalField();BlockHeatSources.emit(l,cold);
        h.assertTrue(!cold.energy.containsKey(p.asLong()),"Chunk removal removes its sources");
        BlockHeatSources.enqueue(l,chunk);
        var warm=new ThermalField();int batches=0;
        while(!warm.energy.containsKey(p.asLong())&&batches++<10000){BlockHeatSources.discover(l);BlockHeatSources.emit(l,warm);}
        h.assertTrue(warm.energy.containsKey(p.asLong()),"Queued discovery finds existing torches");
        BlockHeatSources.forget(l,chunk);var removed=new ThermalField();BlockHeatSources.emit(l,removed);
        h.assertTrue(!removed.energy.containsKey(p.asLong()),"Removing the chunk also removes discovered sources");
        l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=200) public static void inFlightHeatIsAtomicAndSaveable(GameTestHelper h){
        var l=h.getLevel();var origin=new BlockPos(4864,180,4864);var field=new ThermalField();
        for(int x=0;x<32;x++)for(int z=0;z<32;z++){
            l.getChunk(origin.offset(x,0,z));
            for(int y=0;y<20;y++)field.add(l,origin.offset(x,y,z),.25);
        }
        var fresh=origin.west(2);l.getChunk(fresh);
        var initial=field.energy.clone();
        var expected=ThermalField.load(field.save(new CompoundTag(),l.registryAccess()),l.registryAccess());
        expected.quarterStep(l,false);
        field.scheduledStep(l,0);
        h.assertTrue(field.exchanging(),"Large exchange yields before finishing");
        h.assertTrue(field.energy.equals(initial),"No partial exchange is published");
        field.add(l,origin,17);field.add(l,fresh,11);
        expected.add(l,origin,17);expected.add(l,fresh,11);
        var restored=ThermalField.load(field.save(new CompoundTag(),l.registryAccess()),l.registryAccess());
        h.assertTrue(restored.energy.equals(field.energy),"Mid-exchange save includes incoming heat exactly once");
        int ticks=0;
        while(field.exchanging()&&ticks++<1000){
            field.add(l,origin,.01);expected.add(l,origin,.01);
            field.scheduledStep(l,1);
        }
        h.assertTrue(!field.exchanging()&&field.completedExchanges==1,"Bounded job completes without restart or catch-up");
        h.assertTrue(expected.energy.keySet().equals(field.energy.keySet()),"Atomic publication preserves cell membership");
        for(long key:expected.energy.keySet())h.assertTrue(Math.abs(expected.energy.get(key)-field.energy.get(key))<1e-9,"Incoming heat does not corrupt pairwise accounting");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=200) public static void sparseHeatFieldHasBoundedWorkAndProgress(GameTestHelper h){
        var l=h.getLevel();var origin=new BlockPos(5376,160,5376);var field=new ThermalField();
        for(int x=0;x<32;x++)for(int z=0;z<32;z++){
            l.getChunk(origin.offset(2*x,0,2*z));
            for(int y=0;y<32;y++)field.add(l,origin.offset(2*x,2*y,2*z),.25);
        }
        int ticks=0;double peak=0,total=0;
        while(field.completedExchanges<4&&ticks<1000){
            long begin=System.nanoTime();field.scheduledStep(l,ticks++%5);
            double ms=(System.nanoTime()-begin)/1e6;peak=Math.max(peak,ms);total+=ms;
        }
        h.assertTrue(field.completedExchanges==4&&field.energy.size()<=ThermalRules.MAX_CELLS,"Dispersed sources make progress within the cell bound");
        h.assertTrue(field.energy.values().stream().allMatch(Double::isFinite),"Dispersed heat stays finite");
        System.out.printf(java.util.Locale.ROOT,"THERMAL_SPARSE_FIELD tickPeak=%.3f tickTotal=%.3f ticks=%d exchanges=%d%n",peak,total,ticks,field.completedExchanges);h.succeed();
    }
    @GameTest(template="empty") public static void disabledHeatPreservesStateAndSkipsWork(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(2,3,2));var field=new ThermalField();
        field.energy.put(p.asLong(),50);field.pending.put(p.asLong(),80);boolean enabled=ThermalConfig.enabled();
        try{
            ThermalConfig.ENABLED.set(false);ThermalConfig.ENABLED.clearCache();
            field.step(l);field.scheduledStep(l,0);BlockHeatSources.emit(l,field);ThermalField.fuel(l,p,100);
            h.assertTrue(field.energy.get(p.asLong())==50&&field.pending.get(p.asLong())==80,"Disabled heat preserves saved energy/source budgets");
            h.assertTrue(!ThermalField.supported(l,p)&&ThermalField.efficiency(l,p)==1&&ThermalField.temperature(l,p)==18,"Disabled heat has neutral readings and machine efficiency");
        }finally{ThermalConfig.ENABLED.set(enabled);ThermalConfig.ENABLED.clearCache();}
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=200) public static void fullHeatFieldStaysBounded(GameTestHelper h){
        var l=h.getLevel();var origin=new BlockPos(4608,180,4608);var field=new ThermalField(32768);
        for(int x=0;x<32;x++)for(int z=0;z<32;z++){l.getChunk(origin.offset(x,0,z));for(int y=0;y<32;y++)field.add(l,origin.offset(x,y,z),.25);}
        long begin=System.nanoTime();field.step(l);double elapsed=(System.nanoTime()-begin)/1e6;
        h.assertTrue(field.energy.size()<=field.maxCells,"Full field must stay bounded");
        h.assertTrue(field.energy.values().stream().allMatch(Double::isFinite),"All heat values remain finite");
        double peak=0,total=0;
        int ticks=0;
        while(field.completedExchanges<4&&ticks<1000){begin=System.nanoTime();field.scheduledStep(l,ticks++%5);double ms=(System.nanoTime()-begin)/1e6;peak=Math.max(peak,ms);total+=ms;}
        h.assertTrue(field.completedExchanges==4,"A full field makes bounded forward progress");
        System.out.printf(java.util.Locale.ROOT,"THERMAL_FULL_FIELD_MS %.3f cells=%d tickPeak=%.3f tickTotal=%.3f ticks=%d exchanges=%d%n",elapsed,field.energy.size(),peak,total,ticks,field.completedExchanges);h.succeed();
    }
    @GameTest(template="empty") public static void quarterHeatStepsPreserveEnergyAndSourceBudget(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(2,3,2));l.setBlockAndUpdate(p,Blocks.BRICKS.defaultBlockState());
        var whole=new ThermalField();whole.energy.put(p.asLong(),50);whole.pending.put(p.asLong(),800);
        var quarters=ThermalField.load(whole.save(new CompoundTag(),l.registryAccess()),l.registryAccess());
        whole.step(l);quarters.releaseSources(l);for(int i=0;i<20;i++)quarters.scheduledStep(l,i%5);
        h.assertTrue(whole.energy.keySet().equals(quarters.energy.keySet()),"Quarter scheduling preserves tracked cells");
        for(long key:whole.energy.keySet())h.assertTrue(Math.abs(whole.energy.get(key)-quarters.energy.get(key))<1e-9,"Quarter scheduling preserves pairwise exchange");
        h.assertTrue(quarters.pending.isEmpty()&&quarters.pending.equals(whole.pending),"Release source heat once, not every quarter");
        var restored=ThermalField.load(quarters.save(new CompoundTag(),l.registryAccess()),l.registryAccess());
        h.assertTrue(restored.energy.equals(quarters.energy),"Versioned heat cells survive a save round-trip");h.succeed();
    }
    @GameTest(template="industrial") public static void campfiresHeatOnlyWhileLit(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(6,4,6));
        for(var q:BlockPos.betweenClosed(p.offset(-3,-1,-3),p.offset(3,3,3)))l.setBlockAndUpdate(q,Blocks.AIR.defaultBlockState());
        for(var block:java.util.List.of(Blocks.CAMPFIRE,Blocks.SOUL_CAMPFIRE)){
            var lit=block.defaultBlockState();l.setBlockAndUpdate(p,lit);
            var field=new ThermalField();BlockHeatSources.emit(l,field);
            h.assertTrue(field.energy.getOrDefault(p.above().asLong(),0d)>0,"Campfire warms air");
            h.assertTrue(field.excess(l,p.above())>0,"Campfire raises physical air temperature");
            l.setBlockAndUpdate(p,lit.setValue(CampfireBlock.LIT,false));
            h.assertTrue(field.excess(l,p.above())>0,"Air retains warmth after the fire goes out");
            field=new ThermalField();BlockHeatSources.emit(l,field);
            h.assertTrue(!field.energy.containsKey(p.above().asLong()),"Extinguished campfire stops adding heat");
            l.setBlockAndUpdate(p,lit);field=new ThermalField();BlockHeatSources.emit(l,field);
            h.assertTrue(field.energy.containsKey(p.above().asLong()),"Relighting restores heat");
            h.assertTrue(!BlockHeatSources.campfire(lit.setValue(CampfireBlock.WATERLOGGED,true)),"Waterlogged campfire cannot heat");
            l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());
            field=new ThermalField();BlockHeatSources.emit(l,field);
            h.assertTrue(!field.energy.containsKey(p.above().asLong()),"Removing campfire stops emission");
        }
        h.succeed();
    }
    @GameTest(template="industrial") public static void playerTemperatureUsesOnlyTorsoAir(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(6,4,6));
        for(var q:BlockPos.betweenClosed(p.offset(-2,-1,-2),p.offset(2,3,2)))l.setBlockAndUpdate(q,Blocks.AIR.defaultBlockState());
        l.setBlockAndUpdate(p.below(),Blocks.STONE.defaultBlockState());
        var feet=net.minecraft.world.phys.Vec3.atCenterOf(p);
        double before=ThermalSystem.airTemperature(l,feet);
        l.setBlockAndUpdate(p,Blocks.TORCH.defaultBlockState());
        h.assertTrue(Math.abs(ThermalSystem.airTemperature(l,feet)-before)<.001,
                "A newly lit torch gives no separate instant player warmth");
        l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());
        var field=new ThermalField();double cool=field.localTemperature(l,p.above());
        field.add(l,p.above(),30);
        double warm=field.localTemperature(l,p.above());
        h.assertTrue(warm>cool+20,"Stored heat raises the torso air cell temperature");
        l.setBlockAndUpdate(p,Blocks.TORCH.defaultBlockState());
        l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());
        h.assertTrue(Math.abs(field.localTemperature(l,p.above())-warm)<.001,
                "Removing a source does not erase already-warm air");
        h.succeed();
    }
    @GameTest(template="industrial") public static void torchesWarmAndStopWhenRemoved(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(6,4,6));
        for(var q:BlockPos.betweenClosed(p.offset(-2,-1,-2),p.offset(2,2,2)))l.setBlockAndUpdate(q,Blocks.AIR.defaultBlockState());
        l.setBlockAndUpdate(p.below(),Blocks.STONE.defaultBlockState());l.setBlockAndUpdate(p.south(),Blocks.STONE.defaultBlockState());
        for(var block:java.util.List.of(Blocks.TORCH,Blocks.WALL_TORCH,Blocks.SOUL_TORCH,Blocks.SOUL_WALL_TORCH)){
            l.setBlockAndUpdate(p,block.defaultBlockState());var field=new ThermalField();BlockHeatSources.emit(l,field);
            h.assertTrue(field.energy.getOrDefault(p.above().asLong(),0d)>0,"Standing/wall flame heats neighboring air: "+block);
            field.step(l);h.assertTrue(field.excess(l,p.above())>0,"Torch heat diffuses through the normal field");
            l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());var cold=new ThermalField();BlockHeatSources.emit(l,cold);
            h.assertTrue(!cold.energy.containsKey(p.above().asLong()),"Removing torch stops new emission");
            h.assertTrue(field.excess(l,p.above())>0,"Removing torch retains residual warmth");
        }
        h.assertTrue(!BlockHeatSources.source(Blocks.REDSTONE_TORCH.defaultBlockState()),"Redstone lamps are not combustion heat sources");h.succeed();
    }
    @GameTest(template="industrial",timeoutTicks=200) public static void workingMachinesWarmOpenAirAtNight(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(6,3,6));
        for(var q:BlockPos.betweenClosed(p.offset(-4,0,-5),p.offset(6,6,4)))l.setBlockAndUpdate(q,Blocks.AIR.defaultBlockState());
        for(var at:java.util.List.of(p,p.east(4))){
            var state=(at.equals(p)?KilnContent.FOUNDRY.get():KilnContent.KILN.get()).defaultBlockState().setValue(AbstractFurnaceBlock.FACING,Direction.NORTH);
            l.setBlockAndUpdate(at,state);
            for(var part:MachineStructure.parts(state))MachineStructure.placePart(l,at,Direction.NORTH,part);
        }
        var field=new ThermalField();
        for(int second=0;second<45;second++){
            field.pending.merge(p.asLong(),productiveHeatPerSecond(),Double::sum);field.pending.merge(p.east(4).asLong(),productiveHeatPerSecond(),Double::sum);field.step(l);
        }
        double near=field.excess(l,p.north().above())*1.8,far=field.excess(l,p.north(3).above())*1.8;
        System.out.printf(java.util.Locale.ROOT,"Open-air machine warmth after 45s: adjacent +%.2f F, three blocks +%.2f F%n",near,far);
        h.assertTrue(near>1&&far>.2&&near>far,"Working machines warm nearby outdoor air with distance falloff: near="+near+", far="+far);
        double controllerRise=field.excess(l,p)*1.8;
        System.out.printf(java.util.Locale.ROOT,"Open-air foundry casing after 45s: controller +%.2f F%n",controllerRise);
        h.assertTrue(controllerRise>3,"The running foundry casing retains some fuel heat even outdoors");
        var foundryBody=ThermalField.body(l,p);
        int top=foundryBody.stream().mapToInt(BlockPos::getY).max().orElse(p.getY());
        h.assertTrue(foundryBody.stream().filter(at->at.getY()==top)
                .anyMatch(at->field.excess(l,at)*1.8>2),"Foundry chimney shell warms from rising exhaust");
        h.assertTrue(field.energy.values().stream().mapToDouble(Double::doubleValue).sum()<=90*productiveHeatPerSecond()+.001,"Stronger air heating redistributes the same fuel-derived budget");h.succeed();
    }
    @GameTest(template="industrial",timeoutTicks=200) public static void foundryWarmsSmallRoomWithRoofFlue(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(6,3,6));
        for(var q:BlockPos.betweenClosed(p.offset(-3,-1,-3),p.offset(3,6,3))){
            int dx=Math.abs(q.getX()-p.getX()),dz=Math.abs(q.getZ()-p.getZ()),dy=q.getY()-p.getY();
            boolean shell=dy==-1||dy==6||dx==3||dz==3;
            l.setBlockAndUpdate(q,shell?Blocks.OAK_PLANKS.defaultBlockState():Blocks.AIR.defaultBlockState());
        }
        var state=KilnContent.FOUNDRY.get().defaultBlockState().setValue(AbstractFurnaceBlock.FACING,Direction.NORTH);
        l.setBlockAndUpdate(p,state);
        for(var part:MachineStructure.parts(state))MachineStructure.placePart(l,p,Direction.NORTH,part);
        var outlet=p.south(2).above(5);
        l.setBlockAndUpdate(outlet.above(),Blocks.AIR.defaultBlockState());
        var room=p.north(2).above();var field=new ThermalField();
        for(int second=0;second<120;second++){
            field.pending.merge(p.asLong(),productiveHeatPerSecond(),Double::sum);field.step(l);
            if(second==59)System.out.printf(java.util.Locale.ROOT,"FOUNDRY_OPEN_ROOM_F after=60s rise=%.2f%n",field.excess(l,room)*1.8);
        }
        double indoorRise=field.excess(l,room)*1.8;
        var body=ThermalField.body(l,p);
        double otherBodyRise=body.stream().filter(at->!at.equals(p))
                .mapToDouble(at->field.excess(l,at)*1.8).average().orElse(0);
        System.out.printf(java.util.Locale.ROOT,"FOUNDRY_OPEN_ROOM_F after=120s rise=%.2f controller=%.2f%n",
                indoorRise,field.excess(l,p)*1.8);
        System.out.printf(java.util.Locale.ROOT,"FOUNDRY_BODY_F after=120s controller=%.2f otherAverage=%.2f%n",
                field.excess(l,p)*1.8,otherBodyRise);
        h.assertTrue(indoorRise>20&&indoorRise<45,"A roof opening lets some exhaust escape while the short unlined flue still warms the room: +"+indoorRise+"°F");
        h.assertTrue(field.excess(l,outlet.above())>0,"Exhaust still rises through the open roof flue");
        l.setBlockAndUpdate(outlet.above(),Blocks.OAK_PLANKS.defaultBlockState());
        var closed=new ThermalField();
        for(int second=0;second<120;second++){
            closed.pending.merge(p.asLong(),productiveHeatPerSecond(),Double::sum);closed.step(l);
            if(second==59)System.out.printf(java.util.Locale.ROOT,"FOUNDRY_CLOSED_ROOM_F after=60s rise=%.2f%n",closed.excess(l,room)*1.8);
        }
        double closedRise=closed.excess(l,room)*1.8;
        System.out.printf(java.util.Locale.ROOT,"FOUNDRY_CLOSED_ROOM_F after=120s rise=%.2f%n",closedRise);
        h.assertTrue(closedRise>indoorRise+8,
                "Capping the roof traps significantly more of the same fuel heat inside the room");
        h.succeed();
    }
    private static void buildFoundryWorkshop(net.minecraft.server.level.ServerLevel l,BlockPos p,boolean shaft){
        for(var q:BlockPos.betweenClosed(p.offset(-3,-1,-3),p.offset(3,9,3))){
            int dx=Math.abs(q.getX()-p.getX()),dz=Math.abs(q.getZ()-p.getZ()),dy=q.getY()-p.getY();
            boolean shell=dy==-1||dy==8||dx==3||dz==3;
            l.setBlockAndUpdate(q,shell?Blocks.OAK_PLANKS.defaultBlockState():Blocks.AIR.defaultBlockState());
        }
        var state=KilnContent.FOUNDRY.get().defaultBlockState().setValue(AbstractFurnaceBlock.FACING,Direction.NORTH);
        l.setBlockAndUpdate(p,state);
        for(var part:MachineStructure.parts(state))MachineStructure.placePart(l,p,Direction.NORTH,part);
        if(shaft){
            var outlet=p.south(2).above(5);
            var body=ThermalField.body(l,p);
            for(int y=0;y<3;y++)for(var direction:Direction.Plane.HORIZONTAL){
                var wall=outlet.above(y).relative(direction);
                if(!body.contains(wall)&&l.getBlockState(wall).isAir())l.setBlockAndUpdate(wall,Blocks.BRICKS.defaultBlockState());
            }
            l.setBlockAndUpdate(outlet.above(3),Blocks.AIR.defaultBlockState());
        }
    }
    @GameTest(template="industrial",timeoutTicks=200) public static void foundryChimneyChangesWorkshopClimate(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(6,3,6));var room=p.north(2).above();
        buildFoundryWorkshop(l,p,true);
        var vented=new ThermalField();
        for(int second=0;second<180;second++){vented.pending.put(p.asLong(),productiveHeatPerSecond());vented.step(l);}
        double open=vented.excess(l,room)*1.8;
        var outlet=p.south(2).above(5);
        System.out.printf(java.util.Locale.ROOT,"FOUNDRY_SHAFT_F after=180s outlet=%.1f middle=%.1f mouth=%.1f above=%.1f%n",
                vented.localTemperature(l,outlet)*1.8+32,
                vented.localTemperature(l,outlet.above(2))*1.8+32,
                vented.localTemperature(l,outlet.above(3))*1.8+32,
                vented.localTemperature(l,outlet.above(4))*1.8+32);
        for(int second=180;second<480;second++){vented.pending.put(p.asLong(),productiveHeatPerSecond());vented.step(l);}
        System.out.printf(java.util.Locale.ROOT,"FOUNDRY_SHAFT_F after=480s outlet=%.1f middle=%.1f mouth=%.1f above=%.1f cells=%d%n",
                vented.localTemperature(l,outlet)*1.8+32,
                vented.localTemperature(l,outlet.above(2))*1.8+32,
                vented.localTemperature(l,outlet.above(3))*1.8+32,
                vented.localTemperature(l,outlet.above(4))*1.8+32,vented.energy.size());
        h.assertTrue(vented.localTemperature(l,outlet.above(2))*1.8+32<500,
                "A long-running open chimney moves exhaust without superheating a single air cell");
        buildFoundryWorkshop(l,p,false);
        var unvented=new ThermalField();
        for(int second=0;second<180;second++){unvented.pending.put(p.asLong(),productiveHeatPerSecond());unvented.step(l);}
        double closed=unvented.excess(l,room)*1.8;
        System.out.printf(java.util.Locale.ROOT,"FOUNDRY_WORKSHOP_F after=180s vented=%.2f unvented=%.2f%n",open,closed);
        h.assertTrue(open>25&&open<45&&closed>55&&closed>open+20,
                "An enclosed chimney keeps the workshop warm while an unvented room grows hot from the same fuel");
        h.succeed();
    }
    @GameTest(template="industrial",timeoutTicks=200) public static void chimneyWarmsApproachingAirWithoutExtraFuel(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(6,3,6));
        for(var q:BlockPos.betweenClosed(p.offset(-5,0,-5),p.offset(6,8,6)))l.setBlockAndUpdate(q,Blocks.AIR.defaultBlockState());
        var state=KilnContent.FOUNDRY.get().defaultBlockState().setValue(AbstractFurnaceBlock.FACING,Direction.NORTH);
        l.setBlockAndUpdate(p,state);
        for(var part:MachineStructure.parts(state))MachineStructure.placePart(l,p,Direction.NORTH,part);
        var outlet=p.south(2).above(5);
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
            var roof=outlet.offset(x,0,z);
            if(!roof.equals(outlet))l.setBlockAndUpdate(roof,Blocks.OAK_PLANKS.defaultBlockState());
        }
        var field=new ThermalField();
        for(int second=0;second<60;second++){field.pending.put(p.asLong(),productiveHeatPerSecond());field.step(l);}
        double mouth=field.excess(l,outlet.above())*1.8;
        double near=field.excess(l,outlet.above().east())*1.8;
        double middle=field.excess(l,outlet.above().east(2))*1.8;
        double far=field.excess(l,outlet.above().east(3))*1.8;
        System.out.printf(java.util.Locale.ROOT,"CHIMNEY_PLUME_F mouth=%.2f near=%.2f middle=%.2f far=%.2f cells=%d%n",
                mouth,near,middle,far,field.energy.size());
        h.assertTrue(mouth>near&&near>middle&&middle>far&&far>0,
                "Open exhaust creates a real descending air-temperature gradient beside the chimney");
        h.assertTrue(near>2,"A player standing beside the outlet notices warmth in torso air");
        h.assertTrue(field.energy.values().stream().mapToDouble(Double::doubleValue).sum()<=60*productiveHeatPerSecond()+.001,
                "The plume only redistributes fuel-derived heat");
        double stored=field.energy.values().stream().mapToDouble(Double::doubleValue).sum();
        h.assertTrue(Math.abs(stored+field.atmosphereExchange-60*productiveHeatPerSecond())<.01,
                "Fuel heat is either retained in cells or passed to the ambient atmosphere");h.succeed();
    }
    @GameTest(template="industrial") public static void machineExhaustLeavesMostlyFromTheTop(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(6,3,6));
        for(var q:BlockPos.betweenClosed(p.offset(-3,0,-3),p.offset(3,5,3)))l.setBlockAndUpdate(q,Blocks.AIR.defaultBlockState());
        var state=KilnContent.FOUNDRY.get().defaultBlockState().setValue(AbstractFurnaceBlock.FACING,Direction.NORTH);
        l.setBlockAndUpdate(p,state);
        for(var part:MachineStructure.parts(state))MachineStructure.placePart(l,p,Direction.NORTH,part);
        var field=new ThermalField();field.pending.put(p.asLong(),80d);field.releaseSources(l);
        double total=field.energy.values().stream().mapToDouble(Double::doubleValue).sum();
        var outlet=p.south(2).above(5);
        double above=0;for(int i=0;i<ThermalRules.EXHAUST_RISE_CELLS;i++)
            above+=field.energy.getOrDefault(outlet.above(i).asLong(),0d);
        h.assertTrue(Math.abs(total-80)<.001,"Exhaust distribution conserves purchased coal heat");
        h.assertTrue(above>60&&field.energy.get(outlet.asLong())<20,
                "Most exhaust moves through an open air column instead of overheating one cell");
        var body=ThermalField.body(l,p);
        int top=body.stream().mapToInt(BlockPos::getY).max().orElse(p.getY());
        double upperBody=body.stream().filter(at->at.getY()==top)
                .mapToDouble(at->field.energy.getOrDefault(at.asLong(),0d)).sum();
        h.assertTrue(field.energy.getOrDefault(p.asLong(),0d)>0,
                "Controller receives its normal share of stored casing heat");
        h.assertTrue(upperBody>0,"Upper shell/chimney also receives stored casing heat");
        h.assertTrue(body.stream().allMatch(at->field.energy.getOrDefault(at.asLong(),0d)>0),
                "Heat enters every actual block of the multiblock, not only its controller");h.succeed();
    }
    @GameTest(template="industrial",timeoutTicks=200) public static void openChimneyWarmsAirAboveRoofEvenAtFieldLimit(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(6,3,6));
        for(var q:BlockPos.betweenClosed(p.offset(-3,0,-3),p.offset(3,8,4)))l.setBlockAndUpdate(q,Blocks.AIR.defaultBlockState());
        var state=KilnContent.FOUNDRY.get().defaultBlockState().setValue(AbstractFurnaceBlock.FACING,Direction.NORTH);
        l.setBlockAndUpdate(p,state);
        for(var part:MachineStructure.parts(state))MachineStructure.placePart(l,p,Direction.NORTH,part);
        var outlet=p.south(2).above(5);
        for(int y=0;y<=2;y++)for(var direction:Direction.Plane.HORIZONTAL)
            l.setBlockAndUpdate(outlet.above(y).relative(direction),Blocks.OAK_PLANKS.defaultBlockState());
        var field=new ThermalField();field.pending.put(p.asLong(),80d);field.releaseSources(l);
        h.assertTrue(field.energy.getOrDefault(outlet.asLong(),0d)>0
                &&field.energy.getOrDefault(outlet.above(2).asLong(),0d)>0,
                "The Foundry carries exhaust through consecutive open air cells");
        for(int i=0;i<20;i++){field.pending.put(p.asLong(),80d);field.step(l);}
        h.assertTrue(field.excess(l,outlet.above())>2,
                "Air rises through the roof shaft and warms the upper chimney");
        h.assertTrue(field.excess(l,outlet.above().east())>0,
                "The upper chimney wall absorbs heat from the rising air");
        var full=new ThermalField(32768);
        for(int i=0;i<full.maxCells;i++)full.energy.put(new BlockPos(1000+i%256,20,1000+i/256).asLong(),1d);
        full.pending.put(p.asLong(),80d);full.releaseSources(l);
        h.assertTrue(full.energy.size()<=full.maxCells&&full.energy.getOrDefault(outlet.asLong(),0d)>0,
                "A saturated field admits new hot exhaust instead of silently losing it");
        full.quarterStep(l,false);
        h.assertTrue(full.excess(l,outlet.above())>0,
                "Hot exhaust continues into the upper shaft when the field was full");
        h.succeed();
    }
    @GameTest(template="industrial") public static void openMachineOutletsTakePriorityOverCoveredTop(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(6,3,6));
        for(var q:BlockPos.betweenClosed(p.offset(-3,0,-3),p.offset(3,5,5)))l.setBlockAndUpdate(q,Blocks.AIR.defaultBlockState());
        var state=KilnContent.KILN.get().defaultBlockState().setValue(AbstractFurnaceBlock.FACING,Direction.NORTH);
        l.setBlockAndUpdate(p,state);
        for(var part:MachineStructure.parts(state))MachineStructure.placePart(l,p,Direction.NORTH,part);
        var outlet=p.south().above(3);
        for(int x=-1;x<=1;x++)for(int z=0;z<=2;z++){
            var roof=p.offset(x,3,z);
            if(!roof.equals(outlet))l.setBlockAndUpdate(roof,Blocks.OAK_PLANKS.defaultBlockState());
        }
        var field=new ThermalField();field.pending.put(p.asLong(),80d);field.releaseSources(l);
        double plume=0;for(int i=0;i<ThermalRules.EXHAUST_RISE_CELLS;i++)
            plume+=field.energy.getOrDefault(outlet.above(i).asLong(),0d);
        h.assertTrue(plume>60&&field.energy.getOrDefault(outlet.asLong(),0d)<20,
                "The only open outlet receives the full top exhaust share across its air column");
        h.assertTrue(Math.abs(field.energy.values().stream().mapToDouble(Double::doubleValue).sum()-80)<.001,
                "Prioritizing the outlet does not create extra heat");
        l.setBlockAndUpdate(outlet,Blocks.OAK_PLANKS.defaultBlockState());
        var covered=new ThermalField();covered.pending.put(p.asLong(),80d);covered.releaseSources(l);
        h.assertTrue(covered.energy.getOrDefault(outlet.asLong(),0d)==0,
                "A blocked top cannot absorb exhaust that should remain in the machine casing");
        h.assertTrue(Math.abs(covered.energy.values().stream().mapToDouble(Double::doubleValue).sum()-80)<.001,
                "Blocking the outlet retains rather than creates or destroys the coal heat");
        h.succeed();
    }
    @GameTest(template="industrial") public static void cutBlockShaftRetainsRisingAirHeat(GameTestHelper h){
        var l=h.getLevel();var shaft=h.absolutePos(new BlockPos(4,4,6));var open=shaft.east(8);
        for(var q:BlockPos.betweenClosed(shaft.offset(-2,-1,-2),open.offset(2,5,2)))
            l.setBlockAndUpdate(q,Blocks.AIR.defaultBlockState());
        for(int y=0;y<3;y++)for(var direction:Direction.Plane.HORIZONTAL){
            var wall=shaft.above(y).relative(direction);
            var cut=CutGeometry.state(CutGeometry.bounds(2,direction.getOpposite(),0));
            l.setBlockAndUpdate(wall,cut);
            ((CutBlockEntity)l.getBlockEntity(wall)).material(Blocks.BRICKS.defaultBlockState());
        }
        var enclosed=new ThermalField();var exposed=new ThermalField();
        for(int second=0;second<20;second++){
            enclosed.add(l,shaft,44);exposed.add(l,open,44);
            enclosed.step(l);exposed.step(l);
        }
        double enclosedTop=enclosed.excess(l,shaft.above(2));
        double openTop=exposed.excess(l,open.above(2));
        h.assertTrue(enclosedTop>openTop*1.15,
                "Cut-block walls retain enough warm air for it to rise higher than an open plume: "+enclosedTop+" vs "+openTop);
        h.assertTrue(enclosed.excess(l,shaft.above(2).east())>0,
                "The wall itself warms by ordinary air-to-block transfer");
        h.succeed();
    }
    @GameTest(template="industrial") public static void lavaHeatsSurroundingsAndStopsWhenRemoved(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(5,4,5));
        for(var q:BlockPos.betweenClosed(p.offset(-2,-2,-2),p.offset(2,2,2)))l.setBlockAndUpdate(q,Blocks.AIR.defaultBlockState());
        l.setBlockAndUpdate(p,Blocks.LAVA.defaultBlockState());
        var full=new ThermalField();BlockHeatSources.emit(l,full);
        double sourceHeat=full.energy.getOrDefault(p.above().asLong(),0d);
        h.assertTrue(sourceHeat>36,"Placed source lava supplies substantially more heat than a campfire");
        h.assertTrue(ThermalField.temperature(l,p)==BlockHeatSources.TEMPERATURE,"Lava itself reads hot");
        full.step(l);h.assertTrue(full.excess(l,p.above())>0,"Lava heat joins the shared thermal field");
        l.setBlockAndUpdate(p,Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL,5));
        var flowing=new ThermalField();BlockHeatSources.emit(l,flowing);
        h.assertTrue(flowing.energy.getOrDefault(p.above().asLong(),0d)>0&&flowing.energy.get(p.above().asLong())<sourceHeat,"Thin flowing lava emits less heat");
        l.setBlockAndUpdate(p,Blocks.OBSIDIAN.defaultBlockState());
        var cooled=new ThermalField();BlockHeatSources.emit(l,cooled);
        h.assertTrue(!cooled.energy.containsKey(p.above().asLong()),"Solidified lava stops emitting");
        h.assertTrue(full.excess(l,p.above())>0,"Existing surrounding warmth is retained");
        h.succeed();
    }
    @GameTest(template="industrial") public static void wallsRetainHeatAndOpeningsLeak(GameTestHelper h){
        var l=h.getLevel();var a=h.absolutePos(new BlockPos(4,4,4));var b=h.absolutePos(new BlockPos(12,4,4));
        l.setBlockAndUpdate(a,Blocks.AIR.defaultBlockState());l.setBlockAndUpdate(b,Blocks.AIR.defaultBlockState());
        for(var d:Direction.values()){l.setBlockAndUpdate(a.relative(d),Blocks.BRICKS.defaultBlockState());l.setBlockAndUpdate(b.relative(d),Blocks.AIR.defaultBlockState());}
        var f=new ThermalField();f.energy.put(a.asLong(),100d);f.energy.put(b.asLong(),100d);
        for(int i=0;i<8;i++)f.step(l);
        h.assertTrue(f.excess(l,a)>f.excess(l,b)*2,"Brick shell retains more heat than open air");
        double total=f.energy.values().stream().mapToDouble(Double::doubleValue).sum();h.assertTrue(total<=200.00001&&total>0,"Transport cannot create energy");
        var door=Blocks.OAK_DOOR.defaultBlockState();h.assertTrue(ThermalField.conductance(door.setValue(DoorBlock.OPEN,true))>ThermalField.conductance(door),"Open door leaks more");
        h.assertTrue(ThermalField.conductance(Blocks.GLASS.defaultBlockState())>ThermalField.conductance(Blocks.BRICKS.defaultBlockState()),"Glass leaks more than masonry");h.succeed();
    }
    @GameTest(template="industrial") public static void heatPersistsAndSourcesAreFinite(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(5,4,5));l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());
        var f=new ThermalField();f.energy.put(p.asLong(),60d);f.pending.put(p.asLong(),160d);
        var copy=ThermalField.load(f.save(new CompoundTag(),l.registryAccess()),l.registryAccess());
        h.assertTrue(copy.energy.equals(f.energy)&&copy.pending.equals(f.pending),"Saved heat and pending emission preserved exactly");
        copy.step(l);copy.step(l);h.assertTrue(copy.pending.isEmpty(),"Finite source fully discharges without replenishing");
        h.assertTrue(copy.energy.values().stream().mapToDouble(Double::doubleValue).sum()<=220.0001,"Waste heat budget conserved");
        var far=new BlockPos(25000000,100,25000000);h.assertTrue(!l.hasChunkAt(far),"Fixture chunk unloaded");copy.energy.put(far.asLong(),25d);copy.step(l);h.assertTrue(!l.hasChunkAt(far)&&copy.energy.get(far.asLong())==25,"No forced loads or offline simulation");h.succeed();
    }
    @GameTest(template="industrial") public static void oldClimateHistoryIsNotMigratedAsHeat(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(5,4,5));var old=new ThermalField();
        old.energy.put(p.asLong(),-200d);old.pending.put(p.asLong(),80d);
        var tag=old.save(new CompoundTag(),l.registryAccess());tag.remove("version");
        var fresh=ThermalField.load(tag,l.registryAccess());
        h.assertTrue(fresh.energy.isEmpty()&&fresh.pending.isEmpty(),
                "Legacy absolute-temperature history resets rather than filling the new climate-relative field");h.succeed();
    }
    @GameTest(template="industrial") public static void comfortAndMachineEfficiencyHaveLimits(GameTestHelper h){
        double rising=ThermalRules.exchangeRate(true,true,Direction.UP,30,20,1,1);
        double sideways=ThermalRules.exchangeRate(true,true,Direction.EAST,30,20,1,1);
        double throughStone=ThermalRules.exchangeRate(false,false,Direction.EAST,30,20,.08,.08);
        double stoneSurface=ThermalRules.exchangeRate(false,true,Direction.EAST,30,20,.08,1);
        h.assertTrue(rising>sideways&&sideways>throughStone,"Survey flow uses the solver's upward air and slower solid rates");
        h.assertTrue(stoneSurface>throughStone*4&&stoneSurface<sideways,"Hot masonry transfers heat to touching air faster than it conducts through a wall");
        h.assertTrue(Direction.WEST.getStepX()*(20-30)*sideways>0,"A hot western neighbor sends heat eastward");
        h.assertTrue(Math.abs(ThermalRules.comfort(ThermalRules.COMFORT_CENTER_C)-1)<1e-9,
                "Comfort peaks at 70°F");
        h.assertTrue(ThermalRules.comfort((48-32)/1.8)<.03&&ThermalRules.comfort((90-32)/1.8)<.05,
                "Cold 48°F air and hot 90°F air grant almost no comfort");
        h.assertTrue(Math.abs(ThermalRules.comfort((60-32)/1.8)-ThermalRules.comfort((80-32)/1.8))<1e-9,
                "Comfort falls symmetrically around 70°F");
        var river=ThermalRules.profile(Biomes.RIVER,.5);
        double riverNoon=ThermalRules.climate(river,64,ThermalRules.daylightPhase(6000),0);
        double riverMidnight=ThermalRules.climate(river,64,ThermalRules.daylightPhase(18000),0);
        h.assertTrue(Math.abs(riverNoon*1.8+32-62.6)<.01&&Math.abs(riverMidnight*1.8+32-44.6)<.01,
                "A temperate river is mildly cold by day and distinctly colder at night");
        double forestNoon=ThermalRules.climate(ThermalRules.profile(Biomes.FOREST,.7),64,1,0);
        double plainsNoon=ThermalRules.climate(ThermalRules.profile(Biomes.PLAINS,.8),64,1,0);
        h.assertTrue(ThermalRules.comfort(forestNoon)<.7&&ThermalRules.comfort(riverNoon)<.7
                        &&ThermalRules.comfort(plainsNoon)<.7,
                "Common clear-weather daytime biomes reward building for comfort instead of granting a full outdoor bonus");
        h.assertTrue(forestNoon<riverNoon&&plainsNoon>riverNoon
                        &&ThermalRules.climate(ThermalRules.profile(Biomes.DESERT,2),64,1,0)>plainsNoon+20,
                "Woodland, riverside and plains are mildly cool; arid land remains distinctly hot");
        h.assertTrue(ThermalRules.climate(ThermalRules.profile(Biomes.SNOWY_PLAINS,0),64,1,0)<0
                &&ThermalRules.climate(ThermalRules.profile(Biomes.DESERT,2),64,1,0)>30,
                "Frozen and hot biomes remain distinct from a temperate river");
        h.assertTrue(ThermalRules.biomeBaseline(-.7)<ThermalRules.biomeBaseline(0),
                "Frozen peaks remain colder than snowy plains instead of sharing the same clamped index");
        h.assertTrue(ThermalRules.climate(ThermalRules.profile(Biomes.DEEP_FROZEN_OCEAN,.5),64,1,0)<0,
                "Deep frozen ocean is cold despite its temperate vanilla gameplay index");
        h.assertTrue(ThermalRules.climate(ThermalRules.profile(Biomes.WARM_OCEAN,.5),64,1,0)
                        >ThermalRules.climate(ThermalRules.profile(Biomes.COLD_OCEAN,.5),64,1,0)+15,
                "Ocean variants have distinct climates despite identical vanilla temperatures");
        h.assertTrue(ThermalRules.profile(Biomes.DESERT,2).dailySwingC()
                        >ThermalRules.profile(Biomes.OCEAN,.5).dailySwingC(),
                "Dry land has a wider day and night range than the open ocean");
        h.assertTrue(ThermalRules.climate(ThermalRules.profile(Biomes.TAIGA,.25),64,1,0)*1.8+32>55
                        &&ThermalRules.climate(ThermalRules.profile(Biomes.SAVANNA,2),64,1,0)*1.8+32<100,
                "Green taiga is not frozen by day and savanna is less hot than the desert");
        var cave=ThermalRules.profile(Biomes.DEEP_DARK,.8);
        h.assertTrue(ThermalRules.climate(cave,-40,1,1)==ThermalRules.climate(cave,-40,-1,0),
                "Deep caves ignore the surface day cycle and regional rain");
        h.assertTrue(ThermalRules.climate(ThermalRules.profile(Biomes.STONY_PEAKS,1),128,1,0)<20,
                "Stony peaks do not inherit temperate lowland warmth");
        double first=ThermalRules.playerTemperature(10,30,1);
        h.assertTrue(first>10&&first<30&&ThermalRules.playerTemperature(first,30,6)>28,
                "Player temperature approaches warmer air over several seconds");
        h.assertTrue(ThermalRules.playerTemperature(30,10,1)>10&&ThermalRules.playerTemperature(30,10,6)<12,
                "Stored personal warmth also fades gradually in colder air");
        h.assertTrue(ThermalRules.coldKcalPerSecond(18,.2)==0&&Math.abs(ThermalRules.coldKcalPerSecond(10,.2)-.05)<1e-9,"Cold drain begins below the comfort threshold and rises gently");
        h.assertTrue(ThermalRules.coldKcalPerSecond(2,.2)==.2&&ThermalRules.coldKcalPerSecond(-20,.2)==.2&&ThermalRules.coldKcalPerSecond(30,.2)==0,"Severe cold is capped and heat is not charged as cold");
        h.assertTrue(ThermalRules.calorieFactor(1)==.8,"Maximum twenty percent work calorie saving");
        h.assertTrue(ThermalRules.efficiency(-20,true)>ThermalRules.efficiency(-20,false),"Shelter reduces cold penalty");
        h.assertTrue(ThermalRules.efficiency(100,true)==1.1&&ThermalRules.efficiency(-100,false)==.7,"Machine gain/loss bounded");
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(5,4,5));var kiln=new KilnBlockEntity(p,KilnContent.KILN.get().defaultBlockState());kiln.setLevel(l);
        var f=ThermalField.get(l);for(var d:Direction.values()){var q=p.relative(d);l.setBlockAndUpdate(q,Blocks.AIR.defaultBlockState());f.energy.put(q.asLong(),100d);}
        h.assertTrue(kiln.getBurnDuration(KilnContent.MINERAL_COAL.toStack())==440,"Warm surroundings affect actual thermal fuel budget");h.succeed();
    }

    @GameTest(template="empty") public static void vanillaOverworldClimateAudit(GameTestHelper h){
        var registry=h.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        var elsewhere=java.util.Set.of(Biomes.NETHER_WASTES,Biomes.WARPED_FOREST,Biomes.CRIMSON_FOREST,
                Biomes.SOUL_SAND_VALLEY,Biomes.BASALT_DELTAS,Biomes.THE_END,Biomes.END_HIGHLANDS,
                Biomes.END_MIDLANDS,Biomes.SMALL_END_ISLANDS,Biomes.END_BARRENS);
        int count=0;
        for(var entry:registry.entrySet()){
            var key=entry.getKey();
            if(!key.location().getNamespace().equals("minecraft")||elsewhere.contains(key))continue;
            var climate=ThermalRules.profile(key,entry.getValue().getBaseTemperature());
            double noon=ThermalRules.climate(climate,64,1,0);
            double midnight=ThermalRules.climate(climate,64,-1,0);
            h.assertTrue(Double.isFinite(noon)&&Double.isFinite(midnight)&&noon>=-45&&noon<=50
                            &&midnight>=-45&&midnight<=50&&noon>=midnight,
                    "Vanilla biome climate is finite and plausible: "+key.location());
            if(System.getenv("CIVILIZATION_CLIMATE_AUDIT")!=null)
                System.out.printf(java.util.Locale.ROOT,"BIOME_CLIMATE %s %.1f %.1f %.1f%n",
                        key.location(),entry.getValue().getBaseTemperature(),noon*1.8+32,midnight*1.8+32);
            count++;
        }
        h.assertTrue(count>=50,"Every vanilla Overworld biome is present in the climate audit");h.succeed();
    }

    @GameTest(template="industrial") public static void machineBodyHeatsBothSides(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(7,3,6));
        var state=KilnContent.FOUNDRY.get().defaultBlockState().setValue(AbstractFurnaceBlock.FACING,Direction.NORTH);
        for(var q:BlockPos.betweenClosed(p.offset(-3,-1,-2),p.offset(3,5,4)))l.setBlockAndUpdate(q,Blocks.AIR.defaultBlockState());
        l.setBlockAndUpdate(p,state);
        for(var part:MachineStructure.parts(state))l.setBlockAndUpdate(MachineStructure.position(p,Direction.NORTH,part),MachineStructure.shape(part,Direction.NORTH));
        var f=new ThermalField();
        for(int second=0;second<30;second++){f.pending.put(p.asLong(),80d);f.step(l);}
        h.assertTrue(f.excess(l,p.north())>0&&f.excess(l,p.south(3))>0,"Sustained casing heat reaches both front and rear room air");
        h.assertTrue(f.pending.isEmpty(),"Surface distribution does not multiply the fuel budget");
        h.assertTrue(f.energy.values().stream().mapToDouble(Double::doubleValue).sum()<=2400.001,"Distributed source conserves its total heat");h.succeed();
    }
    @GameTest(template="industrial") public static void heatAnomalySurvivesReload(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(5,4,5));l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());
        for(var d:Direction.values())l.setBlockAndUpdate(p.relative(d),Blocks.OAK_PLANKS.defaultBlockState());
        var f=new ThermalField();f.energy.put(p.asLong(),15d);
        h.assertTrue(Math.abs(f.excess(l,p)-15)<.00001,"Stored warmth is measured above the shared climate background");
        var copy=ThermalField.load(f.save(new CompoundTag(),l.registryAccess()),l.registryAccess());
        h.assertTrue(Math.abs(copy.excess(l,p)-15)<.00001,"Excess heat persists across reload without a per-cell climate reference");
        h.assertTrue(copy.atmosphereExchange==f.atmosphereExchange,"Atmosphere accounting survives reload");
        copy.step(l);h.assertTrue(copy.excess(l,p)>5,"Enclosed air cools gradually through its walls");
        h.assertTrue(ThermalField.capacity(Blocks.BRICKS.defaultBlockState())>ThermalField.capacity(Blocks.OAK_PLANKS.defaultBlockState()),"Masonry stores more heat than timber");
        h.assertTrue(ThermalField.conductance(Blocks.OAK_PLANKS.defaultBlockState())<ThermalField.conductance(Blocks.BRICKS.defaultBlockState()),"Timber insulates better than masonry");h.succeed();
    }
    @GameTest(template="industrial") public static void airMixesWithoutDestroyingEnclosedHeat(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(5,4,5));
        for(var q:BlockPos.betweenClosed(p.offset(-1,-1,-1),p.offset(3,1,1)))l.setBlockAndUpdate(q,Blocks.OAK_PLANKS.defaultBlockState());
        for(int i=0;i<3;i++)l.setBlockAndUpdate(p.east(i),Blocks.AIR.defaultBlockState());
        var f=new ThermalField();f.energy.put(p.asLong(),60d);
        for(int i=0;i<4;i++)f.step(l);
        h.assertTrue(f.excess(l,p.east(2))>5,"Air carries warmth across the enclosure within seconds");
        h.assertTrue(f.excess(l,p)<35,"Source hotspot mixes into surrounding air");
        double total=f.energy.values().stream().mapToDouble(Double::doubleValue).sum();
        h.assertTrue(total<=60.001&&total>59,"Sheltered heat moves into air and walls instead of vanishing: "+total);h.succeed();
    }
}
