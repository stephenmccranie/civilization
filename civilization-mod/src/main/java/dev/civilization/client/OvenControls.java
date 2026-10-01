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
public final class OvenControls {
    @SubscribeEvent public static void scroll(InputEvent.MouseScrollingEvent e){var mc=Minecraft.getInstance();if(mc.screen!=null||mc.player==null||mc.level==null||!mc.player.isShiftKeyDown()||!(mc.hitResult instanceof BlockHitResult hit))return;var oven=BakingOvenBlock.owner(mc.level,hit.getBlockPos(),mc.level.getBlockState(hit.getBlockPos()));if(oven==null)return;e.setCanceled(true);double delta=Math.clamp(e.getScrollDeltaY()*(mc.options.keySprint.isDown()?.003:.025),-.1,.1);PacketDistributor.sendToServer(new OvenDialPayload(hit.getBlockPos(),delta));mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("Oven draft: "+Math.round(Math.clamp(oven.dial()+delta,0,1)*100)+"%"),true);}
}
