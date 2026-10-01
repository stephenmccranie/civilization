package dev.civilization;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.*;
final class CompactCampusVisual {
 private static int ticks;private static volatile Throwable failure;
 static void camera(Minecraft mc,double x,double y,double z,float yaw,float pitch){mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SPECTATOR);p.teleportTo(mc.getSingleplayerServer().overworld(),x,y,z,java.util.Set.of(),yaw,pitch);});}
 static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"compact-"+name+".png",mc.getMainRenderTarget(),m->{});}
 static void tick(Minecraft mc){if(failure!=null)throw new IllegalStateException("Compact campus failed",failure);ticks++;
  if(ticks==80){mc.options.fov().set(65);mc.options.hideGui=true;mc.getSingleplayerServer().execute(()->{try{
   var l=mc.getSingleplayerServer().overworld();for(int x=-4;x<=5;x++)for(int z=-3;z<=5;z++)l.getChunk(x,z);CompactCampus.verify(l);
   for(int x:new int[]{-8,-5,-2,2,5,8})if(l.getBlockState(new BlockPos(x,65,-3)).getValue(ButtonBlock.FACE)!=AttachFace.WALL)throw new IllegalStateException("Control is not wall-mounted");
   var pump=(IndustrialBlockEntity)l.getBlockEntity(new BlockPos(18,64,26));var drill=(IndustrialBlockEntity)l.getBlockEntity(new BlockPos(-34,64,-22));drill.setItem(1,net.minecraft.world.item.ItemStack.EMPTY);
   var heat=(IndustrialBlockEntity)l.getBlockEntity(new BlockPos(30,64,20));var column=(IndustrialBlockEntity)l.getBlockEntity(new BlockPos(42,64,20));var condenser=(IndustrialBlockEntity)l.getBlockEntity(new BlockPos(56,64,20));var tank=(IndustrialBlockEntity)l.getBlockEntity(new BlockPos(64,66,22));int before=tank.input.getFluidAmount();
   var site=Deposits.at(l,pump.getBlockPos());long oilBefore=oil(l,site);
   // Gameplay suite owns sustained pipeline checks; this visual fixture must not simulate repeated work at one timestamp.
   for(int i=0;i<200;i++){drill.process();pump.process();}
   condenser.input.fill(new net.neoforged.neoforge.fluids.FluidStack(IndustrialContent.VAPOR.get(),200),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);condenser.progress=condenser.duration()-IndustrialRates.STEP_TICKS;condenser.process();condenser.transfer();
   if(drill.getItem(1).isEmpty()||oil(l,site)>=oilBefore||tank.input.getFluidAmount()<=before)throw new IllegalStateException("Local extraction/refinery failed: "+pump.status+","+heat.status+","+column.status+","+condenser.status);
   System.out.println("COMPACT_RELOAD_PIPELINE_PASS coal="+drill.getItem(1).getCount()+" oil_drained="+(oilBefore-oil(l,site))+" new_fuel="+(tank.input.getFluidAmount()-before));
  }catch(Throwable t){failure=t;}});camera(mc,-54,103,-60,-44,29);}
  if(ticks==130)shot(mc,"overview");
  if(ticks==140)camera(mc,0,66,6,180,0);
  if(ticks==175)shot(mc,"controls");
  if(ticks==180)camera(mc,12,78,7,-45,25);
  if(ticks==220)shot(mc,"refinery");
  if(ticks==225)camera(mc,18,30,31,180,0);
  if(ticks==260){shot(mc,"oil-window");System.out.println("COMPACT_VISUAL_PASS");}
  if(ticks>270)mc.stop();
 }
 static long oil(net.minecraft.server.level.ServerLevel l,Deposits.Site s){long amount=0;for(int i=0;i<s.cells();i++){var b=l.getBlockState(s.cell(i));if(b.is(IndustrialContent.RESERVOIR_OIL.get()))amount+=(8-b.getValue(LiquidBlock.LEVEL))*125;}return amount;}
}
