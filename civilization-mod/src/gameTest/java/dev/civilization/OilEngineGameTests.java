package dev.civilization;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.fluids.FluidStack;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.*;
@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public class OilEngineGameTests {
 /** Fixture engine: the controller and every section's cells placed directly, as a finished build would leave them. */
 static OilEngineEntity place(ServerLevel l,BlockPos at,Direction facing){
  l.setBlockAndUpdate(at,OilEngineContent.ENGINE.get().defaultBlockState().setValue(CivicBlock.FACING,facing));
  var e=(OilEngineEntity)l.getBlockEntity(at);var model=e.model();e.build().sections=model.all;
  for(int i=0;i<model.cells.size();i++)l.setBlockAndUpdate(ModeledMachine.position(at,facing,model.cells.get(i)),ModeledMachine.cellState(model,i,facing));
  e.setChanged();l.sendBlockUpdated(at,e.getBlockState(),e.getBlockState(),3);return e;
 }
 static OilEngineEntity build(GameTestHelper h,Direction facing){return place(h.getLevel(),h.absolutePos(new BlockPos(4,1,3)),facing);}
 @GameTest(template="industrial") public static void engineBuildsInSectionsFromEverySide(GameTestHelper h){
  var l=h.getLevel();var player=new net.neoforged.neoforge.common.util.FakePlayer(l,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"EngineBuilder"));
  var at=h.absolutePos(new BlockPos(4,1,3));var model=MachineModel.OIL_ENGINE;
  for(var front:Direction.Plane.HORIZONTAL){
   for(var cell:model.cells)l.removeBlock(ModeledMachine.position(at,front,cell),false);
   l.setBlockAndUpdate(at,OilEngineContent.ENGINE.get().defaultBlockState().setValue(CivicBlock.FACING,front));
   var e=(OilEngineEntity)l.getBlockEntity(at);player.setPos(at.getX()+.5,at.getY()+1,at.getZ()+.5);
   for(int part=0;part<model.sections.size();part++){var stack=model.section(part).sample();h.assertTrue(ModeledMachine.build(e,player,stack)&&stack.isEmpty(),"Section "+model.section(part).name()+" built from its materials facing "+front);}
   h.assertTrue(e.build().sections==model.all&&ModeledMachine.status(e)==MachineStructure.COMPLETE,"Every section completes the engine facing "+front);
   var silencer=model.cells.stream().filter(c->c.pieces().size()==1&&c.pieces().containsKey(5)).findFirst().orElseThrow();
   l.destroyBlock(ModeledMachine.position(at,front,silencer),false);
   h.assertTrue(!e.build().has(5)&&Integer.bitCount(e.build().sections)==model.sections.size()-1,"Breaking the silencer removes only the exhaust section facing "+front);
   h.assertTrue(e.requestWork(at,1)==0,"An engine missing a section supplies no work");
   l.removeBlock(at,false);h.killAllEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class);
  }h.succeed();
 }
 @GameTest(template="industrial") public static void engineStartWorkAndConservation(GameTestHelper h){
  for(var d:Direction.Plane.HORIZONTAL){var e=build(h,d);h.assertTrue(ModeledMachine.status(e)==MachineStructure.COMPLETE,"Rotated engine build "+d);}
  var e=build(h,Direction.NORTH);e.fuel.fill(new FluidStack(IndustrialContent.FUEL.get(),1000),EXECUTE);e.oil.fill(new FluidStack(IndustrialContent.LUBE.get(),1000),EXECUTE);e.toggle();
  e.tick();h.assertTrue(e.lit()&&e.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT),"Lamps light the controller while the bulbs heat");
  for(int i=1;i<100;i++)e.tick();h.assertTrue(e.warmup==100&&e.fuel.getFluidAmount()==990,"One 10mB cold start");
  for(int i=0;i<30;i++)e.tick();h.assertTrue(e.fuel.getFluidAmount()==990&&e.working()&&!e.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT),"Once the bulbs are hot the engine runs, its lamps out, using no fuel off load");
  double power=e.requestWork(e.getBlockPos(),1);h.assertTrue(power>.99,"Engine supplies work");h.assertTrue(e.requestWork(e.getBlockPos(),1)==0,"Repeated request cannot double-spend or double-output");
  for(int i=0;i<5;i++)e.tick();h.assertTrue(e.status==4&&e.working()&&!e.lit(),"Off load it keeps running with its lamps out");
  h.assertTrue(Math.abs(e.fuel.getFluidAmount()+e.fuelCredit-989.9)<.00001,"Fractional fuel is conserved");
  h.assertTrue(Math.abs(e.oil.getFluidAmount()+e.oilCredit-999.995)<.00001,"Fractional lubricant is conserved");
  var saved=e.saveWithoutMetadata(h.getLevel().registryAccess());var restored=new OilEngineEntity(e.getBlockPos(),e.getBlockState());restored.loadWithComponents(saved,h.getLevel().registryAccess());
  h.assertTrue(Math.abs(restored.fuelCredit-e.fuelCredit)<.00001&&restored.warmup==100&&restored.build().sections==e.build().sections,"Saved build, warmup and fractional stock");
  h.getLevel().removeBlock(ModeledMachine.position(e.getBlockPos(),e.front(),e.model().cells.getFirst()),false);h.assertTrue(e.requestWork(e.getBlockPos(),1)==0,"Broken structure cannot work");h.succeed();
 }
 @GameTest(template="industrial") public static void enginePipesReachUnobstructedServicePorts(GameTestHelper h){
  for(var facing:Direction.Plane.HORIZONTAL){
   var e=build(h,facing);var l=h.getLevel();e.fuel.setFluid(FluidStack.EMPTY);e.oil.setFluid(FluidStack.EMPTY);
   for(var side:java.util.List.of(facing,Direction.DOWN)){
    boolean fuel=side==facing;var pipe=e.getBlockPos().relative(side);l.setBlockAndUpdate(pipe,IndustrialContent.PIPE.get().defaultBlockState());
    var at=pipe.relative(side);l.setBlockAndUpdate(at,IndustrialContent.TANK.get().defaultBlockState());var tank=(IndustrialBlockEntity)l.getBlockEntity(at);
    var fluid=fuel?IndustrialContent.FUEL.get():IndustrialContent.LUBE.get();var target=fuel?e.fuel:e.oil;tank.input.fill(new FluidStack(fluid,1000),EXECUTE);
    var cap=l.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,e.getBlockPos(),side);
    h.assertTrue(cap==target,"Correct sided capability");
    int n=PipeRouting.transfer(l,e.getBlockPos(),side,target,false);h.assertTrue(n==25&&target.getFluidAmount()==25&&tank.input.getFluidAmount()==975,"Pipe transfer conserves fluid "+facing+" "+side+" n="+n+" engine="+target.getFluidAmount()+" tank="+tank.input.getFluidAmount());
    h.assertTrue(ModeledMachine.status(e)==MachineStructure.COMPLETE,"Service pipes leave the engine complete");
    l.removeBlock(pipe,false);l.removeBlock(at,false);
   }
  }h.succeed();
 }
 @GameTest(template="industrial") public static void engineFluidAndFailureBoundaries(GameTestHelper h){var e=build(h,Direction.NORTH);
  h.assertTrue(e.fuel.fill(new FluidStack(IndustrialContent.CRUDE.get(),1000),EXECUTE)==0,"Crude cannot bypass refining");h.assertTrue(e.oil.fill(new FluidStack(IndustrialContent.FUEL.get(),1000),EXECUTE)==0,"No fluid mixing");
  e.fuel.fill(new FluidStack(IndustrialContent.FUEL.get(),9),EXECUTE);e.toggle();e.tick();h.assertTrue(e.warmup==0&&e.fuel.getFluidAmount()==9,"Insufficient starting fuel stays intact");
  e.fuel.fill(new FluidStack(IndustrialContent.FUEL.get(),1000),EXECUTE);for(int i=0;i<100;i++)e.tick();double power=e.requestWork(e.getBlockPos(),1);h.assertTrue(power>0&&e.condition<1,"Dry engine gradually loses efficiency");
  e.toggle();h.assertTrue(e.requestWork(e.getBlockPos(),1)==0,"Stopped engine supplies no work");h.succeed();
 }
}
