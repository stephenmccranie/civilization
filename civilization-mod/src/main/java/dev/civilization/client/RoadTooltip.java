package dev.civilization.client;

import dev.civilization.CuttingContent;
import dev.civilization.RoadSurface;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = "civilization", value = Dist.CLIENT)
public final class RoadTooltip {
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        if (CuttingContent.material(event.getItemStack()).is(RoadSurface.BLOCKS))
            event.getToolTip().add(Component.translatable("tooltip.civilization.road_paving"));
    }
}
