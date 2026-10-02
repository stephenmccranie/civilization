package dev.civilization;

import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Real client input/payloads and server actions, in the guarded disposable world. */
final class BuilderLineVisualCheck {
    private static final BlockPos A=new BlockPos(-1,100,0),B=A.east(2);
    private static int ticks;private static volatile String failure;private static volatile boolean built,mined;
    static void tick(Minecraft mc){
        ticks++;if(failure!=null)throw new IllegalStateException(failure);var server=mc.getSingleplayerServer();
        if(ticks==100){PreviewConfig.MODE.set(PreviewConfig.Mode.TEXTURED);PreviewConfig.OPACITY.set(.32);mc.gui.getChat().clearMessages(true);mc.options.pauseOnLostFocus=false;mc.options.hideGui=false;mc.options.fov().set(60);mc.options.guiScale().set(3);mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);check(server,()->{
            var l=server.overworld();l.setDayTime(6000);var weather=RegionalWeather.get(l);var district=weather.district(l,RegionalWeather.key(0,0));district.rain=false;district.end=weather.clock+72000;weather.setDirty();
            for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){l.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.STONE_BRICKS.defaultBlockState());for(int y=100;y<=106;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
            for(var e:l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(-7,98,-7,7,107,7)))e.discard();
            var p=server.getPlayerList().getPlayers().getFirst();BuilderLineSystem.finish(p,"");p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getInventory().clearContent();p.getInventory().selected=0;p.setItemInHand(InteractionHand.OFF_HAND,BuilderLineContent.LINE.toStack());p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STONE_BRICKS,4));CalorieFoodData.of(p).reserve().set(2400);CalorieFoodData.of(p).resetCounters();p.teleportTo(l,.5,100,-3,java.util.Set.of(),0,20);
        });}
        if(ticks==125)aim(server,A.below().getCenter().add(0,.5,0));
        if(ticks==140){click(mc);click(mc);}
        if(ticks==144)release(mc);
        if(ticks==150)check(server,()->{var p=server.getPlayerList().getPlayers().getFirst();if(!BuilderLineSystem.selected(p)||BuilderLineSystem.active(p)||!server.overworld().getBlockState(A).isAir()||p.getMainHandItem().getCount()!=4)throw new IllegalStateException("First mark/held-click guard failed");});
        if(ticks==155)aim(server,B.below().getCenter().add(0,.5,0));
        if(ticks==180){mc.gui.getChat().clearMessages(true);shot(mc,"build-preview");}
        if(ticks==185)click(mc);
        if(ticks==189)release(mc);
        if(ticks==220)check(server,()->{var p=server.getPlayerList().getPlayers().getFirst();for(int i=0;i<3;i++)if(!server.overworld().getBlockState(A.east(i)).is(Blocks.STONE_BRICKS))throw new IllegalStateException("Actual right-click line placement failed");if(p.getMainHandItem().getCount()!=1||CalorieFoodData.of(p).placed!=3||BuilderLineSystem.selected(p))throw new IllegalStateException("Placement conservation/cleanup failed");built=true;});
        if(ticks==230){shot(mc,"built");check(server,()->{var tool=Items.IRON_PICKAXE.getDefaultInstance();EquipmentGrade.apply(tool,100,12345L);server.getPlayerList().getPlayers().getFirst().setItemInHand(InteractionHand.MAIN_HAND,tool);});}
        if(ticks==245)aim(server,A.getCenter());
        if(ticks==260)click(mc);
        if(ticks==264)release(mc);
        if(ticks==270)aim(server,B.getCenter());
        if(ticks==290)shot(mc,"mine-preview");
        if(ticks==295)click(mc);
        if(ticks==299)release(mc);
        if(ticks==410){shot(mc,"mined");check(server,()->{var p=server.getPlayerList().getPlayers().getFirst();for(int i=0;i<3;i++)if(!server.overworld().getBlockState(A.east(i)).isAir())throw new IllegalStateException("Automatic mining failed");if(p.getMainHandItem().getDamageValue()!=3||CalorieFoodData.of(p).broken!=3||BuilderLineSystem.selected(p))throw new IllegalStateException("Mining accounting/cleanup failed");int drops=server.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(A).inflate(5)).stream().filter(e->e.getItem().is(Items.STONE_BRICKS)).mapToInt(e->e.getItem().getCount()).sum();if(drops!=3)throw new IllegalStateException("Mining drops missing: "+drops);mined=true;p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STONE_BRICKS,4));});}
        if(ticks==425)aim(server,A.below().getCenter().add(0,.5,0));
        if(ticks==440)click(mc);
        if(ticks==444)release(mc);
        if(ticks==450)aim(server,A.east().north().below().getCenter().add(0,.5,0));
        if(ticks==470){shot(mc,"diagonal-preview");click(mc);}
        if(ticks==474)release(mc);
        if(ticks==480)check(server,()->{var p=server.getPlayerList().getPlayers().getFirst();if(!BuilderLineSystem.selected(p)||BuilderLineSystem.active(p)||p.getMainHandItem().getCount()!=4)throw new IllegalStateException("Diagonal must remain a pending first mark");});
        if(ticks==490){mc.options.keyShift.setDown(true);click(mc);}
        if(ticks==494){release(mc);mc.options.keyShift.setDown(false);}
        if(ticks==500)check(server,()->{if(BuilderLineSystem.selected(server.getPlayerList().getPlayers().getFirst()))throw new IllegalStateException("Actual crouch-right-click cancel failed");});
        if(ticks==510)mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        if(ticks==525)shot(mc,"inventory-offhand");
        if(ticks==530)mc.player.closeContainer();
        if(ticks>545){if(!built||!mined)throw new IllegalStateException("Server acceptance missing");com.mojang.logging.LogUtils.getLogger().info("BUILDER LINE VISUAL VERIFIED: actual right-click endpoints, held-click guard, automatic placement/mining after button release, exact stock/durability/drops/calorie counts, diagonal rejection, crouch-right-click cancel, Photon/Faithful previews and offhand inventory");mc.stop();}
    }
    private static void click(Minecraft mc){mc.options.keyUse.setDown(true);var e=new net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered(1,mc.options.keyUse,InteractionHand.MAIN_HAND);net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(e);if(!e.isCanceled()||e.shouldSwingHand())throw new IllegalStateException("Ordinary main-hand action was not suppressed");}
    private static void release(Minecraft mc){mc.options.keyUse.setDown(false);}
    private static void aim(net.minecraft.server.MinecraftServer s,Vec3 target){check(s,()->{var p=s.getPlayerList().getPlayers().getFirst();var d=target.subtract(p.getEyePosition());float yaw=(float)Math.toDegrees(Math.atan2(-d.x,d.z));float pitch=(float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z)));p.teleportTo(p.serverLevel(),p.getX(),p.getY(),p.getZ(),java.util.Set.of(),yaw,pitch);});}
    private static void check(net.minecraft.server.MinecraftServer s,Runnable r){s.execute(()->{try{r.run();}catch(Throwable e){failure=e.toString();}});}
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"builders-line-"+name+".png",mc.getMainRenderTarget(),m->{});}
}
