package dev.civilization;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.*;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
import com.mojang.authlib.GameProfile;
import net.neoforged.neoforge.common.util.FakePlayer;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class BoatGameTests {
    private static FakePlayer player(GameTestHelper h){var p=new FakePlayer(h.getLevel(),new GameProfile(java.util.UUID.randomUUID(),"boat-test"));p.setGameMode(GameType.SURVIVAL);return p;}
    @GameTest(template="empty",templateNamespace="civilization") public static void releasingHelmKeepsThrottleAndClearsSteering(GameTestHelper h){
        for(int gear=-1;gear<=2;gear++){var d=new BoatSystem.Drive();d.pilot=java.util.UUID.randomUUID();d.throttle=gear;d.steer=1;d.power=.75;BoatSystem.release(d,null);h.assertTrue(d.throttle==gear&&d.pilot==null&&d.steer==0&&d.power==.75,"Leaving retains throttle and propulsion");}h.succeed();
    }
    private static void water(net.minecraft.server.level.ServerLevel l,BlockPos pos){for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++){l.setBlockAndUpdate(pos.offset(x,-4,z),Blocks.STONE.defaultBlockState());for(int y=-3;y<0;y++)l.setBlockAndUpdate(pos.offset(x,y,z),Blocks.WATER.defaultBlockState());}}
    private static ServerSubLevel launch(GameTestHelper h,BlockPos pos){var l=h.getLevel();water(l,pos);l.setBlockAndUpdate(pos,BoatContent.HELM.get().defaultBlockState());for(var part:BoatSystem.parts())MachineStructure.placePart(l,pos,Direction.NORTH,part);return BoatSystem.launch(l,pos,player(h));}
    private static OilEngineEntity engine(ServerSubLevel ship,int x){var l=ship.getLevel();var at=BoatSystem.helm(ship).offset(x,0,1);l.setBlockAndUpdate(at,OilEngineContent.ENGINE.get().defaultBlockState().setValue(CivicBlock.FACING,Direction.NORTH));for(var part:OilEngineStructure.PARTS)MachineStructure.placePart(l,at,Direction.NORTH,part);var engine=(OilEngineEntity)l.getBlockEntity(at);engine.fuel.fill(new FluidStack(IndustrialContent.FUEL.get(),1000),EXECUTE);engine.oil.fill(new FluidStack(IndustrialContent.LUBE.get(),1000),EXECUTE);engine.enabled=true;engine.warmup=100;return engine;}
    @GameTest(template="empty",templateNamespace="civilization") public static void freeformHullHasNoOldEnvelope(GameTestHelper h){
        var pos=new BlockPos(4000,100,4000);for(int x=249;x<=250;x++)for(int z=249;z<=250;z++){h.getLevel().setChunkForced(x,z,true);h.getLevel().getChunk(x,z);}water(h.getLevel(),pos);h.getLevel().setBlockAndUpdate(pos,BoatContent.HELM.get().defaultBlockState());for(var part:BoatSystem.parts())MachineStructure.placePart(h.getLevel(),pos,Direction.NORTH,part);for(int x=3;x<=9;x++)h.getLevel().setBlockAndUpdate(pos.offset(x,-1,0),Blocks.OAK_PLANKS.defaultBlockState());for(int y=0;y<=6;y++)h.getLevel().setBlockAndUpdate(pos.offset(9,y,0),Blocks.OAK_PLANKS.defaultBlockState());
        ServerSubLevel ship=null;try{ship=BoatSystem.launch(h.getLevel(),pos,player(h));var helm=BoatSystem.helm(ship);h.assertTrue(ship.getLevel().getBlockState(helm.offset(9,6,0)).is(Blocks.OAK_PLANKS),"Freeform launch keeps blocks beyond old width and height");h.assertTrue(BoatSystem.attached(ship,helm.offset(10,6,0)),"Connected edge can expand without an envelope");}finally{if(ship!=null&&!ship.isRemoved())SubLevelContainer.getContainer(h.getLevel()).removeSubLevel(ship,SubLevelRemovalReason.REMOVED);for(int x=249;x<=250;x++)for(int z=249;z<=250;z++)h.getLevel().setChunkForced(x,z,false);}h.succeed();
    }
    @GameTest(template="empty",templateNamespace="civilization") public static void unoccupiedBoatUsesPhysicalEngine(GameTestHelper h){
        var pos=new BlockPos(4032,100,4032);for(int x=251;x<=253;x++)for(int z=251;z<=253;z++){h.getLevel().setChunkForced(x,z,true);h.getLevel().getChunk(x,z);}var ship=launch(h,pos);var first=engine(ship,0);var second=engine(ship,4);var helm=BoatSystem.helm(ship);var state=ship.getLevel().getBlockState(helm);ship.getLevel().setBlockAndUpdate(helm,state.setValue(BoatHelmBlock.GEAR,3));
        h.runAfterDelay(10,()->{try{h.assertTrue(ship.getLevel().getBlockState(helm).getValue(BoatHelmBlock.GEAR)==3,"Unoccupied boat retains gear");h.assertTrue(first.fuel.getFluidAmount()<1000&&first.oil.getFluidAmount()<1000&&second.fuel.getFluidAmount()<1000&&second.oil.getFluidAmount()<1000,"Every physical running engine supplies propulsion and consumes its fluids");h.assertTrue(BoatSystem.engines(ship).size()==2,"Vessel discovers both physical engines");}finally{if(!ship.isRemoved())SubLevelContainer.getContainer(h.getLevel()).removeSubLevel(ship,SubLevelRemovalReason.REMOVED);for(int x=251;x<=253;x++)for(int z=251;z<=253;z++)h.getLevel().setChunkForced(x,z,false);}h.succeed();});
    }
    @GameTest(template="empty",templateNamespace="civilization") public static void untaggedPhysicsObjectsDoNotBecomeBoats(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(1,2,1));level.setBlockAndUpdate(pos,Blocks.OAK_PLANKS.defaultBlockState());var fragment=SubLevelAssemblyHelper.assembleBlocks(level,pos,java.util.List.of(pos),BoundingBox3i.from(java.util.List.of(pos)));var local=fragment.getPlot().getCenterBlock();
        try{fragment.getLevel().setBlockAndUpdate(local,Blocks.OAK_PLANKS.defaultBlockState());h.assertTrue(fragment.getUserDataTag()==null&&!BoatSystem.boats(level).contains(fragment)&&BoatSystem.at(level,local)==null,"Absent metadata remains unrelated");fragment.setUserDataTag(new CompoundTag());h.assertTrue(!BoatSystem.boats(level).contains(fragment),"Unrelated custom data remains unrelated");var tag=new CompoundTag();tag.putBoolean(BoatSystem.TAG,true);tag.putLong("helm",local.asLong());fragment.setUserDataTag(tag);h.assertTrue(!BoatSystem.boats(level).contains(fragment),"Tagged fragment without helm is ignored");fragment.getLevel().setBlockAndUpdate(local,BoatContent.HELM.get().defaultBlockState());h.assertTrue(BoatSystem.boats(level).contains(fragment)&&BoatSystem.at(level,local)==fragment,"Tagged vessel with physical helm is recognized");}finally{SubLevelContainer.getContainer(level).removeSubLevel(fragment,SubLevelRemovalReason.REMOVED);}h.succeed();
    }
}
