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
    @EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
    public static final class Keys {@SubscribeEvent public static void register(RegisterKeyMappingsEvent e){e.register(RELOAD);}}
    public static boolean holding(){var p=Minecraft.getInstance().player;return p!=null&&p.getMainHandItem().is(FirearmContent.PATERSON.get());}
    @SubscribeEvent public static void input(InputEvent.InteractionKeyMappingTriggered e){
        var mc=Minecraft.getInstance();if(!e.isAttack()||mc.screen!=null||!holding())return;
        e.setCanceled(true);e.setSwingHand(false);
        if(!fired){PacketDistributor.sendToServer(new FirearmPayload(0));fired=true;}
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        var mc=Minecraft.getInstance();if(mc.player==null){fired=false;return;}
        if(!mc.options.keyAttack.isDown()||mc.screen!=null||!holding())fired=false;
        while(RELOAD.consumeClick())if(mc.screen==null&&holding())PacketDistributor.sendToServer(new FirearmPayload(1));
        int recoil=mc.player.getPersistentData().getInt("patersonRecoil");
        if(recoil>0){if(holding()&&mc.screen==null)mc.player.setXRot(Math.max(-90,mc.player.getXRot()-.3f));mc.player.getPersistentData().putInt("patersonRecoil",recoil-1);}
    }
}
