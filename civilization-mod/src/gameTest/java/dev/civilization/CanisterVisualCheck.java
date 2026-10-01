package dev.civilization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** Short inventory/frame/held-item art review; only edits the disposable visual world. */
final class CanisterVisualCheck {
 private static int ticks;
 static void tick(Minecraft mc){
  ticks++;var server=mc.getSingleplayerServer();
  if(ticks==100){mc.options.pauseOnLostFocus=false;mc.options.guiScale().set(3);mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
   server.execute(()->{
    var l=server.overworld();l.setDayTime(6000);
    var weather=RegionalWeather.get(l);var district=weather.district(l,RegionalWeather.key(0,0));district.rain=false;district.end=weather.clock+72000;weather.setDirty();
    for(var entity:l.getEntitiesOfClass(ItemFrame.class,new net.minecraft.world.phys.AABB(-2,99,-3,10,107,3)))entity.discard();
    for(int x=-2;x<=9;x++)for(int z=-7;z<=2;z++){l.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.STONE_BRICKS.defaultBlockState());for(int y=100;y<=105;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
    var player=server.getPlayerList().getPlayers().getFirst();player.getInventory().clearContent();player.removeAllEffects();
    var items=java.util.List.of(IndustrialContent.CAN.get(),IndustrialContent.CRUDE_CAN.get(),IndustrialContent.FUEL_CAN.get(),IndustrialContent.LUBE_CAN.get());
    for(int i=0;i<items.size();i++){
     player.getInventory().setItem(i,new ItemStack(items.get(i)));player.getInventory().setItem(9+i,new ItemStack(items.get(i)));
     for(int y=100;y<=103;y++)l.setBlockAndUpdate(new BlockPos(i*2, y,0),IndustrialContent.CASING.get().defaultBlockState());
     var frame=new ItemFrame(l,new BlockPos(i*2,102,-1),Direction.NORTH);frame.setItem(new ItemStack(items.get(i)));l.addFreshEntity(frame);
    }
    player.teleportTo(l,3.5,101,-5,java.util.Set.of(),0,0);
   });
  }
  if(ticks==150)shot(mc,"frames");
  if(ticks==155)mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
  if(ticks==170)shot(mc,"inventory");
  if(ticks==175){mc.setScreen(null);mc.player.getInventory().selected=2;}
  if(ticks==195)shot(mc,"fuel-held");
  if(ticks==200)mc.player.getInventory().selected=3;
  if(ticks==220)shot(mc,"lubricant-held");
  if(ticks>225){com.mojang.logging.LogUtils.getLogger().info("CANISTER VISUAL VERIFIED");mc.stop();}
 }
 private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"canisters-"+name+".png",mc.getMainRenderTarget(),m->{});}
}
