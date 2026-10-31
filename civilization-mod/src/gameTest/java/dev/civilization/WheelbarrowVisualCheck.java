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
    private static float cameraBeforeTurn,headingBeforeTurn,leftStartYaw;
    private static volatile boolean cargoVerified;
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
        if(ticks==190)dev.civilization.client.WheelbarrowRenderCheck.gaitCheck=true;
        if(ticks==210)shot(mc,"pushing-first-person");
        if(ticks==200)mc.player.setYRot(barrow(mc).getYRot());
        if(ticks==215)mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        if(ticks==230)shot(mc,"pushing-player");
        if(ticks==235)mc.options.setCameraType(CameraType.FIRST_PERSON);
        if(ticks==250){mc.options.keyUp.setDown(false);check(server,()->{var e=barrow(server);if(e.getZ()<3.5||e.getBoundingBox().maxZ>5.01)throw new IllegalStateException("Wall collision or push failed: "+e.position()+" / "+e.getBoundingBox());stopZ=e.getZ();});}
        if(ticks==260){check(server,()->{if(Math.abs(barrow(server).getZ()-stopZ)>.03)throw new IllegalStateException("Released forward key kept driving");});mc.options.keyDown.setDown(true);}
        if(ticks==264){mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);lookOffset(mc,55);}
        if(ticks==269||ticks==272||ticks==275||ticks==278)shot(mc,"walking-side-"+ticks);
        if(ticks==280){mc.options.keyDown.setDown(false);lookOffset(mc,-55);mc.options.setCameraType(CameraType.FIRST_PERSON);}
        if(ticks==295){dev.civilization.client.WheelbarrowRenderCheck.cameraCheck=true;cameraBeforeTurn=mc.player.getYRot();headingBeforeTurn=barrow(mc).getYRot();mc.options.keyRight.setDown(true);}
        if(ticks==304){mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);mc.player.setYRot(mc.player.getYRot()+37);mc.player.yRotO+=37;mc.player.setYHeadRot(mc.player.getYHeadRot()+37);mc.player.yHeadRotO+=37;}
        if(ticks==310){mc.player.setYRot(mc.player.getYRot()-37);mc.player.yRotO-=37;mc.player.setYHeadRot(mc.player.getYHeadRot()-37);mc.player.yHeadRotO-=37;mc.options.setCameraType(CameraType.FIRST_PERSON);}
        if(ticks==315){mc.options.keyRight.setDown(false);check(server,()->{if(barrow(server).getYRot()<40)throw new IllegalStateException("Actual steering input did not turn");startX=barrow(server).getX();});}
        if(ticks==322){
            dev.civilization.client.WheelbarrowRenderCheck.assertHeading(barrow(mc));
            float expected=cameraBeforeTurn+net.minecraft.util.Mth.wrapDegrees(barrow(mc).getYRot()-headingBeforeTurn);
            if(Math.abs(net.minecraft.util.Mth.wrapDegrees(mc.player.getYRot()-expected))>1)throw new IllegalStateException("Handler camera did not follow actual synchronized steering: expected="+expected+" player="+mc.player.getYRot()+" barrow="+barrow(mc).getYRot());
            if(Math.abs(net.minecraft.util.Mth.wrapDegrees(mc.player.getYRot()-barrow(mc).getYRot()))>5)throw new IllegalStateException("Handler view and turned carrier heading diverged");
            shot(mc,"turned-heading");
        }
        if(ticks==325)mc.options.keyUp.setDown(true);
        if(ticks==345){mc.options.keyUp.setDown(false);dev.civilization.client.WheelbarrowRenderCheck.assertHeading(barrow(mc));check(server,()->{if(barrow(server).getX()>startX-.7)throw new IllegalStateException("Turned carrier did not move along its heading");});}
        if(ticks==346){leftStartYaw=barrow(mc).getYRot();mc.options.keyLeft.setDown(true);}
        if(ticks==354){
            mc.options.keyLeft.setDown(false);
            if(net.minecraft.util.Mth.wrapDegrees(barrow(mc).getYRot()-leftStartYaw)>-10)throw new IllegalStateException("A did not turn the carrier back to the left");
            dev.civilization.client.WheelbarrowRenderCheck.assertHeading(barrow(mc));shot(mc,"left-turn");
        }
        if(ticks==355){
            dev.civilization.client.WheelbarrowRenderCheck.cameraCheck=false;
            dev.civilization.client.WheelbarrowRenderCheck.gaitCheck=false;
            if(dev.civilization.client.WheelbarrowRenderCheck.gaitSamples<150)throw new IllegalStateException("Insufficient real walking pose samples");
            if(dev.civilization.client.WheelbarrowRenderCheck.rightCameraSamples<5||dev.civilization.client.WheelbarrowRenderCheck.leftCameraSamples<3)throw new IllegalStateException("Insufficient left/right sub-tick camera samples");
            mc.options.keyShift.setDown(true);
        }
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
        if(ticks==560){check(server,()->{
            var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.teleportTo(server.overworld(),2.5,100,-1,java.util.Set.of(),0,20);
            p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(CargoContent.STOCKPILE.get()));p.inventoryMenu.broadcastFullState();
        });}
        if(ticks==575)mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(2.5,100,.5),Direction.UP,new BlockPos(2,99,0),false));
        if(ticks==580)check(server,()->{
            var s=(StockpileEntity)server.overworld().getBlockEntity(new BlockPos(2,100,0));if(s==null)throw new IllegalStateException("Real stockpile placement failed");
            s.cargo().insert(CuttingContent.stack(Blocks.STONE.defaultBlockState(),3,1),400);
            var p=server.getPlayerList().getPlayers().getFirst();p.getInventory().setItem(9,new ItemStack(Blocks.STONE,32));p.inventoryMenu.broadcastFullState();
        });
        if(ticks==588){var e=barrow(mc);mc.gameMode.interactAt(mc.player,e,new EntityHitResult(e,e.position().add(0,.8,0)),InteractionHand.MAIN_HAND);}
        if(ticks==600){if(!(mc.player.containerMenu instanceof CargoMenu))throw new IllegalStateException("Tray hit did not open cargo");mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId,0);}
        if(ticks==612){var m=(CargoMenu)mc.player.containerMenu;if(m.count(0)!=256||Math.abs(m.mass()-64000)>1)throw new IllegalStateException("Cargo quantity or mass did not synchronize beyond native stack counts");shot(mc,"cargo-menu");}
        if(ticks==620)mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId,0,1,net.minecraft.world.inventory.ClickType.PICKUP,mc.player);
        if(ticks==625){if(mc.player.containerMenu.getCarried().getCount()!=16)throw new IllegalStateException("Real cargo right-click cursor did not synchronize");mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId,0,0,net.minecraft.world.inventory.ClickType.PICKUP,mc.player);}
        if(ticks==630)mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId,27,0,net.minecraft.world.inventory.ClickType.QUICK_MOVE,mc.player);
        if(ticks==635)check(server,()->{var p=server.getPlayerList().getPlayers().getFirst();if(p.getInventory().getItem(9).getCount()!=32||barrow(server).cargo().entry(0).count()!=256)throw new IllegalStateException("Full cargo accepted an oversized transfer");});
        if(ticks==640)mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId,1);
        if(ticks==644){if(((CargoMenu)mc.player.containerMenu).count(0)!=0)throw new IllegalStateException("Unload failed to clear synchronized cargo");mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId,0);}
        if(ticks==650){mc.player.closeContainer();check(server,()->{var e=barrow(server);var p=server.getPlayerList().getPlayers().getFirst();var grip=e.gripPosition(e.position(),0);p.teleportTo(server.overworld(),grip.x,grip.y,grip.z,java.util.Set.of(),0,20);});}
        if(ticks==660){var e=barrow(mc);mc.gameMode.interactAt(mc.player,e,new EntityHitResult(e,e.position().add(0,.95,-.83)),InteractionHand.MAIN_HAND);}
        if(ticks==665){if(mc.player.getVehicle()!=barrow(mc))throw new IllegalStateException("Rear/handle hit did not take loaded handles");mc.options.keyUp.setDown(true);mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);lookOffset(mc,35);}
        if(ticks==672)shot(mc,"loaded-pushing");
        if(ticks==675){mc.options.keyUp.setDown(false);lookOffset(mc,-35);mc.options.keyShift.setDown(true);}
        if(ticks==680){mc.options.keyShift.setDown(false);mc.options.setCameraType(CameraType.FIRST_PERSON);check(server,()->{
            var e=barrow(server);e.setYRot(0);e.setPos(.5,100,.5);var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),2.5,100,-1,java.util.Set.of(),0,20);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.inventoryMenu.broadcastFullState();
        });}
        if(ticks==695)mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(2.5,100.125,.5),Direction.UP,new BlockPos(2,100,0),false));
        if(ticks==710){if(!(mc.player.containerMenu instanceof CargoMenu m)||m.count(0)!=144)throw new IllegalStateException("Stockpile remainder/menu failed to synchronize");shot(mc,"stockpile-menu");cargoVerified=true;}
        if(ticks==715){mc.player.closeContainer();check(server,()->{var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SPECTATOR);p.teleportTo(server.overworld(),3.7,102,4.2,java.util.Set.of(),140,18);});}
        if(ticks==735)shot(mc,"loaded-world-photon");
        if(ticks==745)shaders(false);
        if(ticks==785)shot(mc,"loaded-world-no-shaders");
        if(ticks==795)shaders(true);
        if(ticks>815){if(!verified||!cargoVerified)throw new IllegalStateException("Wheelbarrow server/cargo acceptance missing");com.mojang.logging.LogUtils.getLogger().info("WHEELBARROW VISUAL VERIFIED: actual movement and camera/gait, tray/handle hit packets, stockpile placement, mass-limited loading, 256-item synchronization, cursor/shift-click conservation, direct unload/reload, loaded pushing, cargo menus and Photon/shaders-disabled views");mc.stop();}
    }
    private static WheelbarrowEntity barrow(Minecraft mc){if(entityId!=0&&mc.level.getEntity(entityId) instanceof WheelbarrowEntity e)return e;return mc.level.getEntitiesOfClass(WheelbarrowEntity.class,new AABB(-10,98,-10,10,108,10)).getFirst();}
    private static WheelbarrowEntity barrow(net.minecraft.server.MinecraftServer s){return (WheelbarrowEntity)s.overworld().getEntity(entityId);}
    private static void check(net.minecraft.server.MinecraftServer s,Runnable action){s.execute(()->{try{action.run();}catch(Throwable e){failure=e.toString();}});}
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"wheelbarrow-"+name+".png",mc.getMainRenderTarget(),m->{});}
    private static void lookOffset(Minecraft mc,float degrees){mc.player.setYRot(mc.player.getYRot()+degrees);mc.player.yRotO+=degrees;mc.player.setYHeadRot(mc.player.getYHeadRot()+degrees);mc.player.yHeadRotO+=degrees;}
    private static void shaders(boolean enabled){try{var iris=Class.forName("net.irisshaders.iris.Iris");var config=iris.getMethod("getIrisConfig").invoke(null);config.getClass().getMethod("setShadersEnabled",boolean.class).invoke(config,enabled);config.getClass().getMethod("save").invoke(config);iris.getMethod("reload").invoke(null);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
}
