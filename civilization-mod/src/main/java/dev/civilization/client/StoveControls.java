package dev.civilization.client;

import dev.civilization.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid="civilization",value=Dist.CLIENT)
public final class StoveControls {
    @SubscribeEvent public static void scroll(InputEvent.MouseScrollingEvent e){var mc=Minecraft.getInstance();if(mc.screen!=null||mc.player==null||!mc.player.isShiftKeyDown()||!(mc.hitResult instanceof BlockHitResult hit)||!(mc.level.getBlockEntity(hit.getBlockPos()) instanceof PrototypeStoveEntity stove))return;
        e.setCanceled(true);double dial=Math.clamp(stove.dial()+e.getScrollDeltaY()*(mc.options.keySprint.isDown()?.003:.025),0,1);PacketDistributor.sendToServer(new StoveDialPayload(hit.getBlockPos(),dial));mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("Stove heat: "+Math.round(dial*100)+"% · crouch + right-click to serve"),true);
    }
}
