package dev.civilization.client;

import dev.civilization.BlockCarry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/**
 * A held right-click keeps to what the main hand started, such as eating: the offhand's building blocks wait for a
 * fresh click instead of being placed when the main hand stops. Each use tries the main hand, then the offhand, so a
 * use whose last attempt was the main hand ended there.
 */
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT)
public final class BlockCarryClient {
    private static boolean mainLast,mainActed;
    private BlockCarryClient(){}
    @SubscribeEvent public static void input(InputEvent.InteractionKeyMappingTriggered e){
        if(!e.isUseItem())return;
        mainLast=e.getHand()==InteractionHand.MAIN_HAND;
        var player=Minecraft.getInstance().player;
        if(!mainLast&&mainActed&&player!=null&&BlockCarry.carried(player.getOffhandItem())){e.setCanceled(true);e.setSwingHand(false);}
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        mainActed=Minecraft.getInstance().options.keyUse.isDown()&&(mainActed||mainLast);
        mainLast=false;
    }
}
