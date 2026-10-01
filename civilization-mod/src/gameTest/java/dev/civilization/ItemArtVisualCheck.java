package dev.civilization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** Short inventory/frame/held-item art review; only edits the disposable visual world. */
final class ItemArtVisualCheck {
 private static int ticks;
 private static String prefix;
 static void tick(Minecraft mc,String scene){
  prefix=scene;
  ticks++;var server=mc.getSingleplayerServer();
  if(ticks==100){mc.options.pauseOnLostFocus=false;mc.options.guiScale().set(3);mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
   server.execute(()->{
    var l=server.overworld();l.setDayTime(6000);
    var weather=RegionalWeather.get(l);var district=weather.district(l,RegionalWeather.key(0,0));district.rain=false;district.end=weather.clock+72000;weather.setDirty();
    for(var entity:l.getEntitiesOfClass(ItemFrame.class,new net.minecraft.world.phys.AABB(-2,99,-3,20,107,3)))entity.discard();
    for(int x=-2;x<=19;x++)for(int z=-7;z<=2;z++){l.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.STONE_BRICKS.defaultBlockState());for(int y=100;y<=105;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
    var player=server.getPlayerList().getPlayers().getFirst();player.getInventory().clearContent();player.removeAllEffects();
    var items=items(scene);
    for(int i=0;i<items.size();i++){
     player.getInventory().setItem(i,new ItemStack(items.get(i)));player.getInventory().setItem(9+i,new ItemStack(items.get(i)));
     for(int y=100;y<=103;y++)l.setBlockAndUpdate(new BlockPos(i*2, y,0),IndustrialContent.CASING.get().defaultBlockState());
     var frame=new ItemFrame(l,new BlockPos(i*2,102,-1),Direction.NORTH);frame.setItem(new ItemStack(items.get(i)));l.addFreshEntity(frame);
    }
    player.teleportTo(l,items.size()-0.5,101,-(items.size()>4?items.size()+2:5),java.util.Set.of(),0,0);
   });
  }
  if(ticks==150)shot(mc,"frames");
  if(ticks==155)mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
  if(ticks==170)shot(mc,"inventory");
  if(ticks==175){mc.setScreen(null);mc.player.getInventory().selected=1;}
  if(ticks==195)shot(mc,"held");
  if(ticks>200){com.mojang.logging.LogUtils.getLogger().info("ITEM ART VISUAL VERIFIED: "+scene);mc.stop();}
 }
 private static java.util.List<net.minecraft.world.item.Item> items(String scene){
  return switch(scene){
   case "manufactured" -> java.util.List.of(KilnContent.STEEL.get(),IndustrialContent.PROBE.get(),KilnContent.MACHINE_PARTS.get(),net.minecraft.world.item.Items.IRON_INGOT,net.minecraft.world.item.Items.COPPER_INGOT);
   case "foods" -> java.util.List.of(CookingContent.BREAD_DOUGH.get(),CookingContent.COOKIE_DOUGH.get(),CookingContent.CAKE_BATTER.get(),CookingContent.UNBAKED_PIE.get(),FarmingContent.RATION.get(),RecoveryItems.MORSEL.get(),WorkshopContent.CLOTH.get(),FarmingContent.FERTILIZER.get());
   case "supplies" -> java.util.List.of(WorkshopContent.HIDE.get(),WorkshopContent.CLOTH.get(),FarmingContent.MINERAL_BLEND.get(),IndustrialContent.ENRICHED_BLEND.get(),FarmingContent.FERTILIZER.get(),IndustrialContent.SULFUR.get(),KilnContent.MACHINE_PARTS.get());
   case "parts" -> java.util.List.of(KilnContent.STEEL.get(),KilnContent.MACHINE_PARTS.get(),IndustrialContent.SULFUR.get(),WorkshopContent.CLOTH.get());
   case "sulfur" -> java.util.List.of(KilnContent.MINERAL_COAL.get(),IndustrialContent.SULFUR.get(),net.minecraft.world.item.Items.GOLD_INGOT,WorkshopContent.CLOTH.get());
   default -> java.util.List.of(net.minecraft.world.item.Items.WHITE_WOOL,WorkshopContent.CLOTH.get(),net.minecraft.world.item.Items.LEATHER,KilnContent.MINERAL_COAL.get());
  };
 }
 private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,prefix+"-"+name+".png",mc.getMainRenderTarget(),m->{});}
}
