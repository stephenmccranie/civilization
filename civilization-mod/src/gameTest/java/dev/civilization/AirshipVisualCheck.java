package dev.civilization;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

/** Actual controller packets and keyboard inputs in the isolated hidden Photon client. */
final class AirshipVisualCheck {
    private static int ticks, menuWaitTicks;private static volatile Throwable failure;private static volatile boolean ready;
    private static final BlockPos ROOT=new BlockPos(136,170,10);
    private static volatile ServerSubLevel ship;private static Vector3d start,reverseVelocity;private static double height,maxSpeed;private static Vec3 hoverStart;
    static void tick(Minecraft mc){
        if(failure!=null)throw new IllegalStateException("Airship visual check failed",failure);
        ticks++;DhTravelVisualCheck.check(ticks);var server=mc.getSingleplayerServer();if(ship!=null)maxSpeed=Math.max(maxSpeed,AirshipSystem.speed(ship));
        if(ticks>=180&&ticks<280&&(ticks<195||ticks>205)&&!(mc.player.getVehicle() instanceof AirshipPilotSeat))throw new IllegalStateException("Pilot detached during high-speed flight at tick "+ticks);
        if(ticks==40){mc.options.pauseOnLostFocus=false;mc.options.guiScale().set(3);server.execute(()->{try{
            var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();
            var container=dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(l);
            for(var old:java.util.List.copyOf(AirshipSystem.ships(l)))container.removeSubLevel(old,dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason.REMOVED);
            for(int x=0;x<=16;x++)for(int z=-8;z<=8;z++){l.setChunkForced(x,z,true);l.getChunk(x,z);}
            for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)for(int y=-3;y<=5;y++)l.setBlockAndUpdate(ROOT.offset(x,y,z),Blocks.AIR.defaultBlockState());
            for(int x=-2;x<=2;x++)for(int z=-3;z<=3;z++)l.setBlockAndUpdate(ROOT.offset(x,-1,z),Blocks.OAK_PLANKS.defaultBlockState());
            l.setBlockAndUpdate(ROOT,AirshipContent.CONTROLLER.get().defaultBlockState());
            l.setBlockAndUpdate(ROOT.offset(1,0,2),Blocks.CHEST.defaultBlockState());((net.minecraft.world.level.block.entity.ChestBlockEntity)l.getBlockEntity(ROOT.offset(1,0,2))).setItem(0,KilnContent.STEEL.toStack(23));
            p.stopRiding();p.setDeltaMovement(Vec3.ZERO);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().clearContent();p.teleportTo(l,135.5,170.05,10.5,java.util.Set.of(),-90,25);l.setDayTime(6000);l.setWeatherParameters(100000,0,false,false);AirshipMenu.open(p,ROOT);ready=true;
        }catch(Throwable t){failure=t;}});}
        if(ticks>40&&!ready){ticks=41;return;}
        if(ticks==75){if(!(mc.screen instanceof dev.civilization.client.AirshipScreen)){
            if(++menuWaitTicks>100)throw new IllegalStateException("Controller menu did not open");
            if(menuWaitTicks%10==0)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.stopRiding();p.setDeltaMovement(Vec3.ZERO);p.teleportTo(server.overworld(),135.5,170.05,10.5,java.util.Set.of(),-90,25);AirshipMenu.open(p,ROOT);});
            ticks--;return;
        }edit(mc,"1e250");press(mc,"Apply");}
        if(ticks==85)server.execute(()->{try{if(((AirshipBlockEntity)server.overworld().getBlockEntity(ROOT)).power()!=1e250)throw new IllegalStateException("Typed extreme power not applied");System.out.println("AIRSHIP_EXTREME_POWER_PASS");}catch(Throwable t){failure=t;}});
        if(ticks==90){edit(mc,"NaN");press(mc,"Apply");}
        if(ticks==100){edit(mc,"1e9");press(mc,"Apply");}
        if(ticks==110)press(mc,"Inspect");
        if(ticks==120)Screenshot.grab(mc.gameDirectory,"airship-controller.png",mc.getMainRenderTarget(),m->{});
        if(ticks==125)press(mc,"Assemble");
        if(ticks==145)server.execute(()->{try{
            var l=server.overworld();ship=AirshipSystem.ships(l).getFirst();var p=server.getPlayerList().getPlayers().getFirst();var pos=AirshipSystem.controller(ship);var standing=ship.logicalPose().transformPosition(Vec3.atLowerCornerOf(pos).add(-.5,.05,.5));p.teleportTo(l,standing.x,standing.y,standing.z,java.util.Set.of(),180,15);height=AirshipSystem.world(ship,pos).y;AirshipMenu.open(p,pos);
        }catch(Throwable t){failure=t;}});
        if(ticks==165)press(mc,"Pilot");
        if(ticks==180){start=new Vector3d(ship.logicalPose().position());mc.options.keyUp.setDown(true);}
        // Reproduce the former heartbeat and proximity release paths without loading user terrain.
        if(ticks==190)mc.player.getPersistentData().putLong("civ_airship_tick",mc.player.tickCount-200);
        if(ticks==195)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();var at=AirshipSystem.world(ship,AirshipSystem.controller(ship));p.teleportTo(server.overworld(),at.x+40,at.y+2,at.z,java.util.Set.of(),180,15);});
        if(ticks==210){
            if(!mc.player.getPersistentData().getBoolean("civ_airship_pilot"))throw new IllegalStateException("Streaming separation released pilot");
            if(!(mc.player.getVehicle() instanceof AirshipPilotSeat))throw new IllegalStateException("Pilot attachment did not recover after separation");
            System.out.println("AIRSHIP_PILOT_STREAMING_GAP_PASS");
        }
        if(ticks==215){
            reverseVelocity=dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle.of(ship).getLinearVelocity(new Vector3d());
            mc.options.keyUp.setDown(false);mc.options.keyDown.setDown(true);
        }
        if(ticks==235){
            var velocity=dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle.of(ship).getLinearVelocity(new Vector3d());
            double along=velocity.dot(new Vector3d(reverseVelocity).normalize());
            if(reverseVelocity.length()<10||along<5)throw new IllegalStateException("Reverse erased momentum too quickly: "+reverseVelocity.length()+" -> "+along);
            System.out.println("AIRSHIP_INERTIA_REVERSAL_PASS before="+reverseVelocity.length()+" afterOneSecond="+along);
            mc.options.keyDown.setDown(false);mc.options.keyJump.setDown(true);
        }
        if(ticks==260){mc.player.setXRot(65);mc.options.keyJump.setDown(false);mc.options.keyRight.setDown(true);}
        if(ticks==275){
            if(net.neoforged.fml.ModList.get().isLoaded("c2me_notickvd")){
                if(FlightChunkHints.reordered.get()==0)throw new IllegalStateException("No actual C2ME flight source was reordered");
                System.out.println("C2ME_FLIGHT_ORDER_PASS iterators="+FlightChunkHints.reordered.get());
            }else System.out.println("C2ME_FLIGHT_ABSENT_PASS");
        }
        if(ticks==280){mc.player.setXRot(25);mc.options.keyRight.setDown(false);mc.options.keyShift.setDown(true);}
        if(ticks==275){
            if(!(mc.player.getVehicle() instanceof AirshipPilotSeat))throw new IllegalStateException("Turning detached pilot");
            System.out.println("AIRSHIP_PILOT_ATTACHMENT_PASS peak="+maxSpeed);
        }
        if(ticks==285)mc.options.keyShift.setDown(false);
        if(ticks==295&&mc.player.getVehicle() instanceof AirshipPilotSeat)throw new IllegalStateException("Shift failed to release pilot attachment");
        if(ticks==725)server.execute(()->{try{
            var l=server.overworld();double distance=ship.logicalPose().position().distance(start);double rise=AirshipSystem.world(ship,AirshipSystem.controller(ship)).y-height;
            if(distance<5||rise<1)throw new IllegalStateException("Flight failed distance="+distance+" rise="+rise);
            if(AirshipSystem.speed(ship)>3)throw new IllegalStateException("Hover braking failed: "+AirshipSystem.speed(ship));
            var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)l.getBlockEntity(AirshipSystem.controller(ship).offset(1,0,2));if(chest.getItem(0).getCount()!=23)throw new IllegalStateException("Cargo changed");
            if(Math.abs(ship.logicalPose().orientation().y())<.01)throw new IllegalStateException("Yaw failed");
            if(maxSpeed<=48)throw new IllegalStateException("Flight never exceeded former speed cap: "+maxSpeed);
            System.out.println("AIRSHIP_FLIGHT_PASS distance="+distance+" rise="+rise+" speed="+AirshipSystem.speed(ship)+" peak="+maxSpeed);
            var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);var target=AirshipSystem.world(ship,AirshipSystem.controller(ship));var eye=target.add(8,7,10);var delta=target.subtract(eye);p.teleportTo(l,eye.x,eye.y-p.getEyeHeight(),eye.z,java.util.Set.of(),(float)Math.toDegrees(Math.atan2(-delta.x,delta.z)),(float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z))));
        }catch(Throwable t){failure=t;}});
        if(ticks==740){mc.options.hideGui=true;Screenshot.grab(mc.gameDirectory,"airship-flight.png",mc.getMainRenderTarget(),m->{});}
        if(ticks==750)server.execute(()->{try{((AirshipBlockEntity)server.overworld().getBlockEntity(AirshipSystem.controller(ship))).power(1e250);}catch(Throwable t){failure=t;}});
        if(ticks==770)server.execute(()->{try{if(!Double.isFinite(AirshipSystem.speed(ship)))throw new IllegalStateException("Extreme-power stability failed");((AirshipBlockEntity)server.overworld().getBlockEntity(AirshipSystem.controller(ship))).power(1e5);}catch(Throwable t){failure=t;}});
        if(ticks==1100)server.execute(()->hoverStart=AirshipSystem.world(ship,AirshipSystem.controller(ship)));
        if(ticks==1160)server.execute(()->{try{var now=AirshipSystem.world(ship,AirshipSystem.controller(ship));double drift=Math.hypot(now.x-hoverStart.x,now.z-hoverStart.z);if(drift>.02)throw new IllegalStateException("Unpiloted hover drift: "+drift);System.out.println("AIRSHIP_HOVER_HOLD_PASS drift="+drift+" over=3s power=1e5");height=now.y;((AirshipBlockEntity)server.overworld().getBlockEntity(AirshipSystem.controller(ship))).power(0);}catch(Throwable t){failure=t;}});
        if(ticks==1185)server.execute(()->{try{double fall=height-AirshipSystem.world(ship,AirshipSystem.controller(ship)).y;if(fall<1)throw new IllegalStateException("Zero power still supports vessel: "+fall);System.out.println("AIRSHIP_ZERO_POWER_PASS fall="+fall);((AirshipBlockEntity)server.overworld().getBlockEntity(AirshipSystem.controller(ship))).power(1e7);System.out.println("AIRSHIP VISUAL VERIFIED");}catch(Throwable t){failure=t;}});
        if(ticks==1200)server.execute(()->{try{
            var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();var root=ROOT.offset(24,0,0);
            p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();
            for(var pos:BlockPos.betweenClosed(root.offset(-3,-1,-3),root.offset(3,3,3)))l.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            l.setBlockAndUpdate(root,AirshipContent.CONTROLLER.get().defaultBlockState());
            l.setBlockAndUpdate(root.east(),Blocks.CHEST.defaultBlockState());((net.minecraft.world.level.block.entity.ChestBlockEntity)l.getBlockEntity(root.east())).setItem(0,KilnContent.STEEL.toStack(23));
            p.teleportTo(l,root.getX()+.5,root.getY()+2,root.getZ()+.5,java.util.Set.of(),0,60);
            ship=AirshipSystem.launch(l,root,p,AirshipSystem.scan(l,root,p));
            AirshipMenu.open(p,AirshipSystem.controller(ship));
        }catch(Throwable t){failure=t;}});
        if(ticks==1230){mc.options.hideGui=false;Screenshot.grab(mc.gameDirectory,"airship-disassemble.png",mc.getMainRenderTarget(),m->{});}
        if(ticks==1240)press(mc,"Disassemble");
        if(ticks==1260)server.execute(()->{try{
            var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();var root=ROOT.offset(24,0,0);
            if(!ship.isRemoved()||!l.getBlockState(root).is(AirshipContent.CONTROLLER.get()))throw new IllegalStateException("Disassemble button did not restore world blocks");
            if(((net.minecraft.world.level.block.entity.ChestBlockEntity)l.getBlockEntity(root.east())).getItem(0).getCount()!=23)throw new IllegalStateException("Disassembly cargo mismatch");
            AirshipMenu.open(p,root);System.out.println("AIRSHIP_DISASSEMBLY_BUTTON_PASS");
        }catch(Throwable t){failure=t;}});
        if(ticks==1270)Screenshot.grab(mc.gameDirectory,"airship-disassembled.png",mc.getMainRenderTarget(),m->{});
        if(ticks>1280)mc.stop();
    }
    private static void edit(Minecraft mc,String text){for(var child:mc.screen.children())if(child instanceof EditBox box){box.setValue(text);return;}throw new IllegalStateException("No power field");}
    private static void press(Minecraft mc,String label){for(var child:mc.screen.children())if(child instanceof Button button&&button.getMessage().getString().equals(label)){button.onPress();return;}throw new IllegalStateException("No button "+label);}
}
