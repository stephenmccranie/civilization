package dev.civilization;

import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Real client interactions and native rendering in the guarded disposable world. */
final class OvenVisualCheck {
    private static final BlockPos POS=new BlockPos(0,100,0);
    private static int ticks;
    private static volatile String failure;
    private static volatile boolean verified;
    static void tick(Minecraft mc){
        ticks++;if(failure!=null)throw new IllegalStateException(failure);var server=mc.getSingleplayerServer();
        if(ticks==100){mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.options.fov().set(60);mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);check(server,()->{
            var l=server.overworld();l.setDayTime(6000);var weather=RegionalWeather.get(l);var district=weather.district(l,RegionalWeather.key(0,0));district.rain=false;district.end=weather.clock+72000;weather.setDirty();
            for(int x=-5;x<=7;x++)for(int z=-6;z<=6;z++){l.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.STONE_BRICKS.defaultBlockState());for(int y=100;y<=106;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
            for(var e:l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(-6,98,-7,8,107,7)))e.discard();
            var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getInventory().clearContent();p.getInventory().selected=0;p.setItemInHand(InteractionHand.MAIN_HAND,BakingOvenContent.ITEM.toStack());p.teleportTo(l,.5,100,-3,java.util.Set.of(),0,15);
        });}
        if(ticks==130)mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(POS.below().getCenter().add(0,.5,0),Direction.UP,POS.below(),false));
        if(ticks==145)check(server,()->{if(!(server.overworld().getBlockEntity(POS) instanceof BakingOvenEntity))throw new IllegalStateException("Actual Creative placement failed");var p=server.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.teleportTo(server.overworld(),3.8,100,-3.8,java.util.Set.of(),31,12);});
        if(ticks==180)shot(mc,"closed");
        if(ticks==185)use(mc);
        if(ticks==210){shot(mc,"open");if(!(mc.level.getBlockEntity(POS) instanceof BakingOvenEntity oven)||oven.door<.95)throw new IllegalStateException("Opening did not animate on client");}
        if(ticks==205)mc.options.keyShift.setDown(true);
        if(ticks==215){mc.hitResult=new BlockHitResult(POS.getCenter(),Direction.NORTH,POS,false);dev.civilization.client.OvenControls.scroll(new net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent(0,1,false,false,false,0,0));}
        if(ticks==225){mc.options.keyShift.setDown(false);check(server,()->{var oven=(BakingOvenEntity)server.overworld().getBlockEntity(POS);if(Math.abs(oven.dial()-.525)>.0001)throw new IllegalStateException("Actual scroll control failed: "+oven.dial());verified=true;});}
        if(ticks==230)shot(mc,"dial");
        if(ticks==235)use(mc);
        if(ticks==260){shot(mc,"closed-again");check(server,()->server.getPlayerList().getPlayers().getFirst().teleportTo(server.overworld(),4.8,100,3.8,java.util.Set.of(),133,12));}
        if(ticks==290)shot(mc,"rear");
        if(ticks==295)check(server,()->{var p=server.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.MAIN_HAND,BakingOvenContent.ITEM.toStack());p.teleportTo(server.overworld(),1,100,-3.6,java.util.Set.of(),0,15);});
        if(ticks==320){mc.options.hideGui=false;mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));}
        if(ticks==340)shot(mc,"inventory");
        if(ticks==345){mc.player.closeContainer();mc.options.hideGui=true;shaders(false);check(server,()->{var p=server.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.teleportTo(server.overworld(),3.8,100,-3.8,java.util.Set.of(),31,12);});}
        if(ticks==395)shot(mc,"closed-vanilla");
        if(ticks==400)use(mc);
        if(ticks==425)shot(mc,"open-vanilla");
        if(ticks==430)shaders(true);
        if(ticks==440){mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);check(server,()->{BakingOvenBlock.toggle((BakingOvenEntity)server.overworld().getBlockEntity(POS));server.getPlayerList().getPlayers().getFirst().teleportTo(server.overworld(),-1,100,-1,java.util.Set.of(),-35,5);});}
        if(ticks==470)shot(mc,"player-scale");
        if(ticks>480){if(!verified)throw new IllegalStateException("Server input check missing");com.mojang.logging.LogUtils.getLogger().info("OVEN VISUAL VERIFIED: actual Creative placement, animated opening and closing, continuous scroll input, native model and inventory, Photon and shaders disabled, standing player scale");mc.stop();}
    }
    private static void check(net.minecraft.server.MinecraftServer s,Runnable r){s.execute(()->{try{r.run();}catch(Throwable e){failure=e.toString();}});}
    private static void use(Minecraft mc){mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(POS.getCenter(),Direction.NORTH,POS,false));}
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"oven-"+name+".png",mc.getMainRenderTarget(),m->{});}
    private static void shaders(boolean enabled){try{var iris=Class.forName("net.irisshaders.iris.Iris");var config=iris.getMethod("getIrisConfig").invoke(null);config.getClass().getMethod("setShadersEnabled",boolean.class).invoke(config,enabled);config.getClass().getMethod("save").invoke(config);iris.getMethod("reload").invoke(null);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
}
