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
    for(int x=-2;x<=19;x++)for(int z=-14;z<=2;z++){l.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.STONE_BRICKS.defaultBlockState());for(int y=100;y<=105;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
    // Survival shows every inventory slot; Creative would replace the inventory with its item tabs.
    var player=server.getPlayerList().getPlayers().getFirst();player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.getInventory().clearContent();player.removeAllEffects();
    var items=items(scene);
    var frames=scene.equals("components")?java.util.List.of(items.get(0),items.get(1),items.get(2),items.get(3),items.get(4),items.get(11),items.get(18),items.get(24)):items;
    for(int i=0;i<items.size();i++){
     player.getInventory().setItem(i,new ItemStack(items.get(i)));if(!scene.equals("components")){var worn=new ItemStack(items.get(i));if(EquipmentGrade.eligible(worn))EquipmentGrade.apply(worn,400,7001L+i);player.getInventory().setItem(9+i,worn);}
    }
    for(int i=0;i<frames.size();i++){
     for(int y=100;y<=103;y++)l.setBlockAndUpdate(new BlockPos(i*2, y,0),IndustrialContent.CASING.get().defaultBlockState());
     var frame=new ItemFrame(l,new BlockPos(i*2,102,-1),Direction.NORTH);frame.setItem(new ItemStack(frames.get(i)));l.addFreshEntity(frame);
    }
    player.teleportTo(l,frames.size()-0.5,101,-(frames.size()>4?frames.size()+2:5),java.util.Set.of(),0,0);
   });
  }
  if(ticks==150)shot(mc,"frames");
  if(ticks==155)mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
  if(ticks==170)shot(mc,"inventory");
  if(ticks==175){mc.setScreen(null);mc.player.getInventory().selected=1;}
  if(ticks==195)shot(mc,"held");
  if(scene.equals("components")){
   if(ticks==205)mc.player.getInventory().selected=0;
   if(ticks==220)shot(mc,"held-handle");
   if(ticks==225)mc.player.getInventory().selected=4;
   if(ticks==240)shot(mc,"held-rod");
   if(ticks==245)server.execute(()->server.getPlayerList().getPlayers().getFirst().getInventory().setItem(0,new ItemStack(ManufacturingContent.head("iron","pickaxe"))));
   if(ticks==250)mc.player.getInventory().selected=0;
   if(ticks==265)shot(mc,"held-pick-head");
  }
  if(ticks>(scene.equals("components")?275:200)){com.mojang.logging.LogUtils.getLogger().info("ITEM ART VISUAL VERIFIED: "+scene);mc.stop();}
 }
 private static java.util.List<net.minecraft.world.item.Item> items(String scene){
  if(scene.equals("components")){
   var items=new java.util.ArrayList<net.minecraft.world.item.Item>(java.util.List.of(ManufacturingContent.HANDLE.get(),ManufacturingContent.FASTENERS.get(),ManufacturingContent.WHEEL.get()));
   for(String metal:java.util.List.of("iron","copper","steel","gold"))for(String form:java.util.List.of("plate","rod"))items.add(ManufacturingContent.part(metal,form));
   for(String metal:java.util.List.of("iron","gold","diamond"))for(String shape:ManufacturingContent.SHAPES)if(!metal.equals("gold")||!shape.equals("saw"))items.add(ManufacturingContent.head(metal,shape));
   return java.util.List.copyOf(items);
  }
  return switch(scene){
   case "tools" -> java.util.List.of(FrontierContent.GEIGER_COUNTER.get(),CuttingContent.IRON_SAW.get(),CuttingContent.STONE_SAW.get(),CuttingContent.DIAMOND_SAW.get(),IndustrialContent.PROBE.get(),BuilderLineContent.LINE.get(),ManufacturingContent.head("iron","saw"),ManufacturingContent.HANDLE.get());
   case "minerals" -> java.util.List.of(KilnContent.MINERAL_COAL.get(),FrontierContent.RAW_URANIUM.get(),IndustrialContent.SULFUR.get(),IndustrialContent.ENRICHED_BLEND.get(),net.minecraft.world.item.Items.COAL,net.minecraft.world.item.Items.RAW_IRON,net.minecraft.world.item.Items.GRAVEL,KilnContent.STEEL.get());
   case "manufactured" -> java.util.List.of(KilnContent.STEEL.get(),IndustrialContent.PROBE.get(),KilnContent.MACHINE_PARTS.get(),net.minecraft.world.item.Items.IRON_INGOT,net.minecraft.world.item.Items.COPPER_INGOT);
   case "foods" -> java.util.List.of(CookingContent.BREAD_DOUGH.get(),CookingContent.COOKIE_DOUGH.get(),CookingContent.CAKE_BATTER.get(),CookingContent.UNBAKED_PIE.get(),FarmingContent.RATION.get(),RecoveryItems.MORSEL.get(),PrototypeStoveContent.MEAL.get(),WorkshopContent.CLOTH.get(),FarmingContent.FERTILIZER.get());
   case "supplies" -> java.util.List.of(WorkshopContent.HIDE.get(),WorkshopContent.CLOTH.get(),FarmingContent.MINERAL_BLEND.get(),IndustrialContent.ENRICHED_BLEND.get(),FarmingContent.FERTILIZER.get(),IndustrialContent.SULFUR.get(),KilnContent.MACHINE_PARTS.get());
   case "parts" -> java.util.List.of(KilnContent.STEEL.get(),KilnContent.MACHINE_PARTS.get(),IndustrialContent.SULFUR.get(),WorkshopContent.CLOTH.get());
   case "sulfur" -> java.util.List.of(KilnContent.MINERAL_COAL.get(),IndustrialContent.SULFUR.get(),net.minecraft.world.item.Items.GOLD_INGOT,WorkshopContent.CLOTH.get());
   default -> java.util.List.of(net.minecraft.world.item.Items.WHITE_WOOL,WorkshopContent.CLOTH.get(),net.minecraft.world.item.Items.LEATHER,KilnContent.MINERAL_COAL.get());
  };
 }
 private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,prefix+"-"+name+".png",mc.getMainRenderTarget(),m->{});}
}
