package dev.civilization.client;

import dev.civilization.*;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid="civilization", value=Dist.CLIENT, bus=EventBusSubscriber.Bus.MOD)
public final class CuttingClient {
    @SubscribeEvent public static void models(ModelEvent.ModifyBakingResult event) {
        event.getModels().replaceAll((id, model) -> id.id().getNamespace().equals("civilization") && id.id().getPath().equals("cut_block") ? new CutModel(model) : model);
    }
    @SubscribeEvent public static void blockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> level != null && pos != null && level.getBlockEntity(pos) instanceof CutBlockEntity cut
                ? Minecraft.getInstance().getBlockColors().getColor(tint>=256 && cut.cells()[tint/256-1]!=null?cut.cells()[tint/256-1]:cut.material(), level, pos, tint>=256?tint%256:tint) : -1, CuttingContent.PIECE.get());
    }
    @SubscribeEvent public static void itemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> Minecraft.getInstance().getItemColors().getColor(new net.minecraft.world.item.ItemStack(CuttingContent.material(stack).getBlock()), tint), CuttingContent.ITEM.get());
    }
}
