package dev.civilization;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.fluids.FluidStack;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.*;
@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public class OilEngineGameTests {
 @GameTest(template="industrial") public static void componentsAlignWhenPlacedFromEverySide(GameTestHelper h){
  var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"EngineBuilder"));
  var hand=net.minecraft.world.InteractionHand.MAIN_HAND;
  for(var front:Direction.Plane.HORIZONTAL){
   var e=build(h,front);player.setPos(e.getBlockPos().getX()+4,e.getBlockPos().getY()+3,e.getBlockPos().getZ()+4);
   for(var part:OilEngineStructure.PARTS){
    if(!part.material().startsWith("engine_"))continue;
    var target=MachineStructure.position(e.getBlockPos(),front,part);
    var item=MachineStructure.materialState(part.material()).getBlock().asItem();
    for(var approach:Direction.Plane.HORIZONTAL){
     h.getLevel().removeBlock(target,false);player.setYRot(approach.toYRot());
     player.setItemInHand(hand,new net.minecraft.world.item.ItemStack(item,2));
     var hit=new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(target.getX()+.5,target.getY()-.5,target.getZ()+.5),Direction.UP,target.below(),false);
     player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player,hand,hit));
     h.assertTrue(MachineStructure.matches(h.getLevel(),target,part,front),"Placed component matches controller from "+approach+" for "+front);
     h.assertTrue(player.getMainHandItem().getCount()==1,"One component consumed");
    }
   }
   h.assertTrue(OilEngineStructure.complete(h.getLevel(),e.getBlockPos(),front),"Actual item placement completes rotated engine");
   h.getLevel().removeBlock(e.getBlockPos(),false);
   for(var preferred:Direction.Plane.HORIZONTAL)h.assertTrue(OilEngineStructure.placementFacing(h.getLevel(),e.getBlockPos().above(),OilEngineContent.CYLINDER.get(),preferred)==preferred,"Standalone orientation retains player facing");
  }h.succeed();
 }
 @GameTest(template="industrial") public static void heldComponentsDoNotRotateGuides(GameTestHelper h){
  for(var front:Direction.Plane.HORIZONTAL)for(var part:OilEngineStructure.PARTS){
   if(!part.material().startsWith("engine_"))continue;
   var expected=MachineStructure.shape(part,front);
   for(var held:new net.minecraft.world.level.block.state.BlockState[]{Blocks.AIR.defaultBlockState(),OilEngineContent.CYLINDER.get().defaultBlockState(),OilEngineContent.WHEEL.get().defaultBlockState()})
    h.assertTrue(MachineStructure.previewState(part,front,held).equals(expected),"Held item leaves oriented guide intact "+front+" "+part.material());
  }
  var wall=new MachineStructure.Part(0,0,0,"stone",4,Direction.NORTH);
  h.assertTrue(MachineStructure.previewState(wall,Direction.EAST,Blocks.STONE_BRICKS.defaultBlockState()).is(Blocks.STONE_BRICKS),"Accepted material variants still preview");
  h.succeed();
 }
 @GameTest(template="industrial") public static void wheelRecoversStaticModelAfterControllerRemoval(GameTestHelper h){
  for(var facing:Direction.Plane.HORIZONTAL){
   var e=build(h,facing);var l=h.getLevel();var wheel=e.getBlockPos().relative(facing.getCounterClockWise()).relative(facing.getOpposite()).above();
   EngineFlywheelBlock.refresh(l,wheel);h.assertTrue(l.getBlockState(wheel).getValue(EngineFlywheelBlock.LINKED),"Animated wheel linked "+facing);
   l.removeBlock(e.getBlockPos(),false);EngineFlywheelBlock.refresh(l,wheel);
   h.assertTrue(!l.getBlockState(wheel).getValue(EngineFlywheelBlock.LINKED),"Static wheel restored "+facing);
  }h.succeed();
 }
 static OilEngineEntity build(GameTestHelper h,Direction facing){var at=h.absolutePos(new BlockPos(4,1,3));h.getLevel().setBlockAndUpdate(at,OilEngineContent.ENGINE.get().defaultBlockState().setValue(CivicBlock.FACING,facing));for(var part:OilEngineStructure.PARTS)MachineStructure.placePart(h.getLevel(),at,facing,part);return (OilEngineEntity)h.getLevel().getBlockEntity(at);}
 @GameTest(template="industrial") public static void engineStartWorkAndConservation(GameTestHelper h){
  for(var d:Direction.Plane.HORIZONTAL){var e=build(h,d);h.assertTrue(OilEngineStructure.complete(h.getLevel(),e.getBlockPos(),d),"Rotated engine build "+d);}
  var e=build(h,Direction.NORTH);e.fuel.fill(new FluidStack(IndustrialContent.FUEL.get(),1000),EXECUTE);e.oil.fill(new FluidStack(IndustrialContent.LUBE.get(),1000),EXECUTE);e.control(0);
  for(int i=0;i<100;i++)e.tick();h.assertTrue(e.warmup==100&&e.fuel.getFluidAmount()==990,"One 10mB cold start");
  for(int i=0;i<30;i++)e.tick();h.assertTrue(e.fuel.getFluidAmount()==990,"Ready engine uses no idle fuel");
  double power=e.requestWork(e.getBlockPos(),1);h.assertTrue(power>.99,"Engine supplies work");h.assertTrue(e.requestWork(e.getBlockPos(),1)==0,"Repeated request cannot double-spend or double-output");
  h.assertTrue(Math.abs(e.fuel.getFluidAmount()+e.fuelCredit-989.9)<.00001,"Fractional fuel is conserved");
  h.assertTrue(Math.abs(e.oil.getFluidAmount()+e.oilCredit-999.995)<.00001,"Fractional lubricant is conserved");
  var saved=e.saveWithoutMetadata(h.getLevel().registryAccess());var restored=new OilEngineEntity(e.getBlockPos(),e.getBlockState());restored.loadWithComponents(saved,h.getLevel().registryAccess());h.assertTrue(Math.abs(restored.fuelCredit-e.fuelCredit)<.00001&&restored.warmup==100,"Saved warmup and fractional stock");
  h.getLevel().removeBlock(MachineStructure.position(e.getBlockPos(),e.front(),OilEngineStructure.PARTS.getFirst()),false);h.assertTrue(e.requestWork(e.getBlockPos(),1)==0,"Broken structure cannot work");h.succeed();
 }
 @GameTest(template="industrial") public static void enginePipesReachUnobstructedServicePorts(GameTestHelper h){
  for(var facing:Direction.Plane.HORIZONTAL){
   var e=build(h,facing);var l=h.getLevel();e.fuel.setFluid(FluidStack.EMPTY);e.oil.setFluid(FluidStack.EMPTY);
   h.assertTrue(OilEngineStructure.PARTS.stream().filter(p->p.material().equals("casing")).count()==8,"Complete 3x3 bed including controller");
   for(var side:java.util.List.of(facing,Direction.DOWN)){
    boolean fuel=side==facing;var pipe=e.getBlockPos().relative(side);l.setBlockAndUpdate(pipe,IndustrialContent.PIPE.get().defaultBlockState());
    var at=pipe.relative(side);l.setBlockAndUpdate(at,IndustrialContent.TANK.get().defaultBlockState());var tank=(IndustrialBlockEntity)l.getBlockEntity(at);
    var fluid=fuel?IndustrialContent.FUEL.get():IndustrialContent.LUBE.get();var target=fuel?e.fuel:e.oil;tank.input.fill(new FluidStack(fluid,1000),EXECUTE);
    var cap=l.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,e.getBlockPos(),side);
    h.assertTrue(cap==target,"Correct sided capability");
    int n=PipeRouting.transfer(l,e.getBlockPos(),side,target,false);h.assertTrue(n==25&&target.getFluidAmount()==25&&tank.input.getFluidAmount()==975,"Pipe transfer conserves fluid "+facing+" "+side+" n="+n+" engine="+target.getFluidAmount()+" tank="+tank.input.getFluidAmount());
    h.assertTrue(OilEngineStructure.complete(l,e.getBlockPos(),facing),"Ports leave base complete");
    l.removeBlock(pipe,false);l.removeBlock(at,false);
   }
  }h.succeed();
 }
 @GameTest(template="industrial") public static void engineFluidAndFailureBoundaries(GameTestHelper h){var e=build(h,Direction.NORTH);
  h.assertTrue(e.fuel.fill(new FluidStack(IndustrialContent.CRUDE.get(),1000),EXECUTE)==0,"Crude cannot bypass refining");h.assertTrue(e.oil.fill(new FluidStack(IndustrialContent.FUEL.get(),1000),EXECUTE)==0,"No fluid mixing");
  e.fuel.fill(new FluidStack(IndustrialContent.FUEL.get(),9),EXECUTE);e.control(0);e.tick();h.assertTrue(e.warmup==0&&e.fuel.getFluidAmount()==9,"Insufficient starting fuel stays intact");
  e.fuel.fill(new FluidStack(IndustrialContent.FUEL.get(),1000),EXECUTE);for(int i=0;i<100;i++)e.tick();double power=e.requestWork(e.getBlockPos(),1);h.assertTrue(power>0&&e.condition<1,"Dry engine gradually loses efficiency");
  e.control(0);h.assertTrue(e.requestWork(e.getBlockPos(),1)==0,"Stopped engine supplies no work");h.succeed();
 }
}
