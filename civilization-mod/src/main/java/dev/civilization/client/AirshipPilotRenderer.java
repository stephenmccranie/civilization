package dev.civilization.client;

import dev.civilization.AirshipContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid="civilization",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class AirshipPilotRenderer {
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(AirshipContent.PILOT_SEAT.get(),net.minecraft.client.renderer.entity.NoopRenderer::new);}
}
