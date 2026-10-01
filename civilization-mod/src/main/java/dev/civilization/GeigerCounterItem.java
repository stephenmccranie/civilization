package dev.civilization;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class GeigerCounterItem extends AccessoryItem {
    public GeigerCounterItem(Properties properties) { super(properties); }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context,
                                          List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.civilization.geiger_counter")
                .withStyle(ChatFormatting.GRAY));
    }
}
