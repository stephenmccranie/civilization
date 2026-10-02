package dev.civilization;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class BuilderLineContent {
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(Civilization.MOD_ID);
    public static final DeferredItem<Item> LINE=ITEMS.register("builders_line",()->new Item(new Item.Properties().stacksTo(1)) {
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag) {
            lines.add(Component.translatable("tooltip.civilization.builders_line").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    });
    public static void register(IEventBus bus){ITEMS.register(bus);bus.addListener(BuilderLinePayload::register);}
}
