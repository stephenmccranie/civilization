package dev.civilization.client;

import dev.civilization.Civilization;
import dev.civilization.EquipmentGrade;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = Civilization.MOD_ID, value = Dist.CLIENT)
public final class GradeTooltip {
    private GradeTooltip() {}

    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (!EquipmentGrade.hasGrade(stack)) return;
        int grade = EquipmentGrade.value(stack);
        var color = grade > 100 ? ChatFormatting.AQUA : grade == 100 ? ChatFormatting.GOLD : grade >= 85 ? ChatFormatting.GREEN
                : grade >= 60 ? ChatFormatting.GRAY : ChatFormatting.DARK_RED;
        event.getToolTip().add(1, Component.literal("Grade: " + grade + "%").withStyle(color));
    }
}
