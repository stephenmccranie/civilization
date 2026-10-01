package dev.civilization;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.*;
final class OilEngineVisualCheck {
 private static int ticks;private static final BlockPos AT=new BlockPos(0,100,0);
 static void tick(Minecraft mc){ticks++;var server=mc.getSingleplayerServer();
  if(ticks==100){mc.options.pauseOnLostFocus=false;mc.options.guiScale().set(3);mc.options.hideGui=true;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);server.execute(()->{var l=server.overworld();l.setDayTime(6000);for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){var weather=RegionalWeather.get(l);var district=weather.district(l,RegionalWeather.key(dx*512,dz*512));district.rain=false;district.end=weather.clock+72000;weather.setDirty();}for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){l.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.STONE_BRICKS.defaultBlockState());for(int y=100;y<=112;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}l.setBlockAndUpdate(AT,OilEngineContent.ENGINE.get().defaultBlockState());for(var part:OilEngineStructure.PARTS)MachineStructure.placePart(l,AT,Direction.NORTH,part);var e=(OilEngineEntity)l.getBlockEntity(AT);e.fuel.fill(new FluidStack(IndustrialContent.FUEL.get(),4000),EXECUTE);e.oil.fill(new FluidStack(IndustrialContent.LUBE.get(),2000),EXECUTE);var p=server.getPlayerList().getPlayers().getFirst();p.removeAllEffects();p.getInventory().clearContent();p.getInventory().setItem(0,OilEngineContent.ENGINE.get().asItem().getDefaultInstance());p.getInventory().setItem(1,OilEngineContent.CYLINDER.get().asItem().getDefaultInstance());p.getInventory().setItem(2,OilEngineContent.WHEEL.get().asItem().getDefaultInstance());p.teleportTo(l,-3,102,-3,java.util.Set.of(),-40,10);});}
  if(ticks==135){shot(mc,"stopped");server.execute(()->((OilEngineEntity)server.overworld().getBlockEntity(AT)).control(0));}
  if(ticks==175){shot(mc,"starting");server.execute(()->server.getPlayerList().getPlayers().getFirst().teleportTo(server.overworld(),-3,102,5,java.util.Set.of(),-140,18));}
  if(ticks==205)shot(mc,"starting-back");
  if(ticks==215)server.execute(()->server.getPlayerList().getPlayers().getFirst().teleportTo(server.overworld(),-3,102,-3,java.util.Set.of(),-40,10));
  if(ticks==245)server.execute(()->((OilEngineEntity)server.overworld().getBlockEntity(AT)).control(1));
  if(ticks==270){if(((OilEngineEntity)mc.level.getBlockEntity(AT)).status!=5)throw new IllegalStateException("Engine running state not synchronized");shot(mc,"running");}
  if(ticks==274)shot(mc,"motion");
  if(ticks==280){mc.options.hideGui=false;server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),.5,100,-2,java.util.Set.of(),0,0);p.openMenu((OilEngineEntity)server.overworld().getBlockEntity(AT));});}
  if(ticks==310){if(!(mc.player.containerMenu instanceof OilEngineMenu m)||m.data.get(0)<=0)throw new IllegalStateException("Engine menu missing fuel");shot(mc,"menu");}
  if(ticks==316){mc.player.closeContainer();mc.options.hideGui=true;server.execute(()->server.getPlayerList().getPlayers().getFirst().teleportTo(server.overworld(),-3,102,5,java.util.Set.of(),-140,18));}
  if(ticks==336)shot(mc,"back");
  if(ticks==337){mc.options.hideGui=false;server.execute(()->{
   var l=server.overworld();for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=100;y<=104;y++)l.removeBlock(new BlockPos(x,y,z),false);
   l.setBlockAndUpdate(AT,OilEngineContent.ENGINE.get().defaultBlockState().setValue(CivicBlock.FACING,Direction.EAST));
   for(var part:OilEngineStructure.PARTS)if(!part.material().startsWith("engine_"))MachineStructure.placePart(l,AT,Direction.EAST,part);
   server.getPlayerList().getPlayers().getFirst().teleportTo(l,3,100,.5,java.util.Set.of(),90,25);
  });}
  if(ticks==357)shot(mc,"guide-items");
  if(ticks==358)mc.player.getInventory().selected=1;
  if(ticks==363)shot(mc,"guide-cylinder-held");
  if(ticks==364)mc.player.getInventory().selected=2;
  if(ticks==369)shot(mc,"guide-wheel-held");
  if(ticks>370){com.mojang.logging.LogUtils.getLogger().info("OIL ENGINE VISUAL VERIFIED");mc.stop();}
 }
 private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"engine-"+name+".png",mc.getMainRenderTarget(),m->{});}
}
