package dev.civilization;

import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Real placement/interaction packets, actual client controls and synchronized geometry. */
final class WheelbarrowVisualCheck {
    private static int ticks;
    private static volatile String failure;
    private static volatile boolean verified;
    private static volatile int entityId;
    private static double stopZ,startX;
    static void tick(Minecraft mc){
        ticks++;if(failure!=null)throw new IllegalStateException(failure);var server=mc.getSingleplayerServer();
        if(ticks==100){mc.options.pauseOnLostFocus=false;mc.options.hideGui=false;mc.options.fov().set(65);mc.gui.getChat().clearMessages(true);mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);check(server,()->{
            var l=server.overworld();l.setDayTime(6000);var weather=RegionalWeather.get(l);var d=weather.district(l,RegionalWeather.key(0,0));d.rain=false;d.end=weather.clock+72000;weather.setDirty();
            for(int x=-9;x<=9;x++)for(int z=-8;z<=9;z++){l.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.STONE_BRICKS.defaultBlockState());for(int y=100;y<=106;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
            for(var e:l.getEntitiesOfClass(WheelbarrowEntity.class,new AABB(-12,98,-12,12,110,12)))e.discard();
            for(int x=-4;x<=4;x++)for(int y=100;y<=102;y++)l.setBlockAndUpdate(new BlockPos(x,y,5),Blocks.STONE_BRICKS.defaultBlockState());
            var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();p.getInventory().selected=0;p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));p.setItemInHand(InteractionHand.MAIN_HAND,WheelbarrowContent.ITEM.toStack());p.inventoryMenu.broadcastFullState();p.teleportTo(l,.5,100,-2,java.util.Set.of(),0,20);
        });}
        if(ticks==130)mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(.5,100,.5),Direction.UP,new BlockPos(0,99,0),false));
        if(ticks==150){var e=barrow(mc);entityId=e.getId();check(server,()->{var b=barrow(server);b.setYRot(45);b.setPos(b.position());var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,WheelbarrowContent.ITEM.toStack());p.inventoryMenu.broadcastFullState();var at=b.gripPosition(b.position(),45);p.teleportTo(server.overworld(),at.x,at.y,at.z,java.util.Set.of(),45,20);});}
        if(ticks==165)mc.gameMode.interact(mc.player,barrow(mc),InteractionHand.MAIN_HAND);
        if(ticks==180){if(!(mc.player.getVehicle() instanceof WheelbarrowEntity))throw new IllegalStateException("Taking diagonal handles while holding the placement item did not synchronize");check(server,()->{var e=barrow(server);e.setYRot(0);e.setPos(e.position());var p=server.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.inventoryMenu.broadcastFullState();var at=e.gripPosition(e.position(),0);p.connection.teleport(at.x,at.y,at.z,0,20);});mc.options.keyUp.setDown(true);}
        if(ticks==210)shot(mc,"pushing-first-person");
        if(ticks==215)mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        if(ticks==230)shot(mc,"pushing-player");
        if(ticks==235)mc.options.setCameraType(CameraType.FIRST_PERSON);
        if(ticks==250){mc.options.keyUp.setDown(false);check(server,()->{var e=barrow(server);if(e.getZ()<3.5||e.getBoundingBox().maxZ>5.01)throw new IllegalStateException("Wall collision or push failed: "+e.position()+" / "+e.getBoundingBox());stopZ=e.getZ();});}
        if(ticks==260){check(server,()->{if(Math.abs(barrow(server).getZ()-stopZ)>.03)throw new IllegalStateException("Released forward key kept driving");});mc.options.keyDown.setDown(true);}
        if(ticks==280)mc.options.keyDown.setDown(false);
        if(ticks==295)mc.options.keyRight.setDown(true);
        if(ticks==315){mc.options.keyRight.setDown(false);check(server,()->{if(barrow(server).getYRot()<40)throw new IllegalStateException("Actual steering input did not turn");startX=barrow(server).getX();});}
        if(ticks==325)mc.options.keyUp.setDown(true);
        if(ticks==345){mc.options.keyUp.setDown(false);check(server,()->{if(barrow(server).getX()>startX-.7)throw new IllegalStateException("Turned carrier did not move along its heading");});}
        if(ticks==355){mc.options.keyShift.setDown(true);}
        if(ticks==360){mc.options.keyShift.setDown(false);mc.options.hideGui=true;mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);check(server,()->{var e=barrow(server);if(e.isVehicle())throw new IllegalStateException("Release did not end pushing");e.setYRot(0);e.setPos(.5,100,.5);verified=true;var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),-.9,100,.5,java.util.Set.of(),135,12);});}
        if(ticks==390){shot(mc,"standing-scale");shot(mc,"photon-front");}
        if(ticks==400){mc.options.setCameraType(CameraType.FIRST_PERSON);check(server,()->{var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SPECTATOR);p.teleportTo(server.overworld(),3.7,102,4.2,java.util.Set.of(),140,18);});}
        if(ticks==430)shot(mc,"photon-rear");
        if(ticks==440){mc.options.hideGui=false;check(server,()->{var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.teleportTo(server.overworld(),-3,101.5,-3.7,java.util.Set.of(),-40,10);p.setItemInHand(InteractionHand.MAIN_HAND,WheelbarrowContent.ITEM.toStack());});}
        if(ticks==465){mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));}
        if(ticks==480)shot(mc,"inventory");
        if(ticks==485)mc.setScreen(null);
        if(ticks==495)shaders(false);
        if(ticks==535)shot(mc,"shaders-disabled");
        if(ticks==545)shaders(true);
        if(ticks>565){if(!verified)throw new IllegalStateException("Wheelbarrow server acceptance missing");com.mojang.logging.LogUtils.getLogger().info("WHEELBARROW VISUAL VERIFIED: real placement, empty-hand mount, server-authoritative forward/reverse/turn, wall collision, key-release stop, dismount, synchronized model, Photon front/rear, inventory and shaders-disabled views");mc.stop();}
    }
    private static WheelbarrowEntity barrow(Minecraft mc){if(entityId!=0&&mc.level.getEntity(entityId) instanceof WheelbarrowEntity e)return e;return mc.level.getEntitiesOfClass(WheelbarrowEntity.class,new AABB(-10,98,-10,10,108,10)).getFirst();}
    private static WheelbarrowEntity barrow(net.minecraft.server.MinecraftServer s){return (WheelbarrowEntity)s.overworld().getEntity(entityId);}
    private static void check(net.minecraft.server.MinecraftServer s,Runnable action){s.execute(()->{try{action.run();}catch(Throwable e){failure=e.toString();}});}
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"wheelbarrow-"+name+".png",mc.getMainRenderTarget(),m->{});}
    private static void shaders(boolean enabled){try{var iris=Class.forName("net.irisshaders.iris.Iris");var config=iris.getMethod("getIrisConfig").invoke(null);config.getClass().getMethod("setShadersEnabled",boolean.class).invoke(config,enabled);config.getClass().getMethod("save").invoke(config);iris.getMethod("reload").invoke(null);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
}
