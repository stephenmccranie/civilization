package dev.civilization.lab;
import dev.civilization.VisualTestGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.minecraft.world.level.GameType;
import java.util.Set;

@EventBusSubscriber(modid="civilization",value=Dist.CLIENT)
public final class SableVisual {
    private static boolean opened;
    private static int ticks;
    private static double beforeX,beforeZ;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(!Boolean.getBoolean("civilization.sableVisual"))return;
        var mc=Minecraft.getInstance();
        if(!Boolean.getBoolean("civilization.visualHeadless")||!VisualTestGuard.windowPrepared)throw new IllegalStateException("Hidden mouse guards required");
        long window=mc.getWindow().getWindow();if(org.lwjgl.glfw.GLFW.glfwGetWindowAttrib(window,org.lwjgl.glfw.GLFW.GLFW_VISIBLE)!=0||org.lwjgl.glfw.GLFW.glfwGetWindowAttrib(window,org.lwjgl.glfw.GLFW.GLFW_FOCUSED)!=0||mc.mouseHandler.isMouseGrabbed())throw new IllegalStateException("Lab client captured desktop input");
        if(!opened&&mc.screen instanceof TitleScreen){opened=true;mc.createWorldOpenFlows().openWorld("preview-compatibility",mc::stop);}
        if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null)return;
        ticks++;var server=mc.getSingleplayerServer();
        if(ticks==80){mc.options.pauseOnLostFocus=false;mc.options.guiScale().set(3);mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);server.execute(()->{
            var level=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();
            for(int x=-2;x<=5;x++)for(int z=-2;z<=2;z++)level.setChunkForced(x,z,true);
            var ships=SableAdapter.platforms(level);if(ships.isEmpty())throw new IllegalStateException("No saved platform loaded");var ship=ships.getFirst();SableAdapter.assertCargo(ship);
            // Keep the photographic fixture below Photon's volumetric cloud layer.
            ship.logicalPose().position().y=150;ship.getUserDataTag().putDouble("hoverY",150);
            var body=dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle.of(ship);body.teleport(ship.logicalPose().position(),ship.logicalPose().orientation());
            p.setGameMode(GameType.SURVIVAL);var at=SableAdapter.deck(ship);p.teleportTo(level,at.x,at.y,at.z,Set.of(),-90,15);p.getAbilities().flying=false;p.onUpdateAbilities();
            level.setDayTime(6000);level.setWeatherParameters(100000,0,false,false);
        });}
        if(ticks==120)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();beforeX=p.getX();beforeZ=p.getZ();dev.civilization.CalorieFoodData.of(p).resetCounters();SableAdapter.control(SableAdapter.platforms(server.overworld()).getFirst(),1,0,false);});
        if(ticks==200)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();double carried=Math.hypot(p.getX()-beforeX,p.getZ()-beforeZ);if(carried<3)throw new IllegalStateException("Passenger not carried by platform: "+carried);System.out.println("CIV_SABLE_PASSENGER_PASS distance="+carried);System.out.println("CIV_SABLE_RIDING_CALORIES travel="+dev.civilization.CalorieFoodData.of(p).travelSpent+" walking="+dev.civilization.CalorieFoodData.of(p).walkDistance);dev.civilization.CalorieFoodData.of(p).resetCounters();SableAdapter.control(SableAdapter.platforms(server.overworld()).getFirst(),0,0,true);});
        if(ticks==202)mc.options.keyUp.setDown(true);
        if(ticks==207)mc.options.keyUp.setDown(false);
        if(ticks==209)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();System.out.println("CIV_SABLE_WALK_CALORIES travel="+dev.civilization.CalorieFoodData.of(p).travelSpent+" walking="+dev.civilization.CalorieFoodData.of(p).walkDistance);});
        if(ticks==210)server.execute(()->{var ship=SableAdapter.platforms(server.overworld()).getFirst();var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)server.overworld().getBlockEntity(net.minecraft.core.BlockPos.of(ship.getUserDataTag().getLong("chest")));server.getPlayerList().getPlayers().getFirst().openMenu(chest);});
        if(ticks==225){if(!(mc.player.containerMenu instanceof net.minecraft.world.inventory.ChestMenu m)||m.getSlot(0).getItem().getCount()!=23)throw new IllegalStateException("Moving chest menu did not synchronize");shot(mc,"cargo");}
        if(ticks==235)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();var ship=SableAdapter.platforms(server.overworld()).getFirst();var at=ship.logicalPose().position();p.setGameMode(GameType.SPECTATOR);p.teleportTo(server.overworld(),at.x()+8,at.y()+6,at.z()-9,Set.of(),42,26);});
        if(ticks==245)mc.options.hideGui=true;
        if(ticks==275){shot(mc,"platform");System.out.println("CIV_SABLE_VISUAL_PASS");}
        if(ticks>290)mc.stop();
        if(ticks>800)throw new IllegalStateException("Visual lab timed out");
    }
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"sable-"+name+".png",mc.getMainRenderTarget(),message->System.out.println(message.getString()));}
}
