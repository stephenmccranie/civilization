package dev.civilization.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.civilization.*;
import net.minecraft.client.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid="civilization",value=Dist.CLIENT)
public final class FirearmClient {
    public static final KeyMapping RELOAD=new KeyMapping("key.civilization.reload",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_R,"key.categories.civilization");
    private static boolean fired;
    private static int reloadHeld;private static boolean reloadPending,reloadConsumed;
    public static void shotSound(){Minecraft.getInstance().getSoundManager().play(new PatersonShotSound());}
    @EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
    public static final class Keys {@SubscribeEvent public static void register(RegisterKeyMappingsEvent e){e.register(RELOAD);}}
    public static boolean holding(){var p=Minecraft.getInstance().player;return p!=null&&p.getMainHandItem().is(FirearmContent.PATERSON.get());}
    @SubscribeEvent public static void input(InputEvent.InteractionKeyMappingTriggered e){
        var mc=Minecraft.getInstance();if(!e.isAttack()||mc.screen!=null||!holding())return;
        e.setCanceled(true);e.setSwingHand(false);
        if(!fired){PacketDistributor.sendToServer(new FirearmPayload(0));fired=true;}
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        var mc=Minecraft.getInstance();if(mc.player==null){fired=false;reloadPending=false;reloadConsumed=false;reloadHeld=0;return;}
        if(!mc.options.keyAttack.isDown()||mc.screen!=null||!holding())fired=false;
        if(mc.screen!=null||!holding()){reloadPending=false;reloadHeld=0;while(RELOAD.consumeClick()){};}
        else {
            if(!RELOAD.isDown())reloadConsumed=false;
            while(RELOAD.consumeClick())if(!reloadPending&&!reloadConsumed){
                reloadConsumed=true;
                if(PatersonItem.rounds(mc.player.getMainHandItem())==0)PacketDistributor.sendToServer(new FirearmPayload(1));
                else {reloadPending=true;reloadHeld=0;}
            }
            if(reloadPending){
                if(RELOAD.isDown()){if(++reloadHeld>=12){PacketDistributor.sendToServer(new FirearmPayload(1));reloadPending=false;}}
                else {PacketDistributor.sendToServer(new FirearmPayload(3));reloadPending=false;}
            }
        }
        int recoil=mc.player.getPersistentData().getInt("patersonRecoil");
        if(recoil>0){if(holding()&&mc.screen==null)mc.player.setXRot(Math.max(-90,mc.player.getXRot()-.3f));mc.player.getPersistentData().putInt("patersonRecoil",recoil-1);}
    }
}
