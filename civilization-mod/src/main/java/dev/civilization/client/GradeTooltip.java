package dev.civilization.client;

import dev.civilization.Civilization;
import dev.civilization.EquipmentGrade;
import dev.civilization.Grade;
import dev.civilization.HotMetal;
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
        if (!Grade.graded(stack) && !EquipmentGrade.hasGrade(stack)) return;
        int grade = Grade.value(stack);
        var units = Grade.of(stack).units();
        var color = grade > 1000 ? ChatFormatting.AQUA : grade == 1000 ? ChatFormatting.GOLD : grade >= 850 ? ChatFormatting.GREEN
                : grade >= 600 ? ChatFormatting.GRAY : ChatFormatting.DARK_RED;
        event.getToolTip().add(1, Component.literal("Grade: " + Grade.percent(grade)
                + (units.size() > 1 ? " average, " + Grade.percent(units.getLast()) + "–" + Grade.percent(units.getFirst()) : "")).withStyle(color));
        long heat = event.getEntity() == null ? 0 : HotMetal.heatLeft(stack, event.getEntity().level().getGameTime());
        if (heat > HotMetal.DULL_TICKS) event.getToolTip().add(2, Component.literal("Glowing: hammer it at an anvil within " + (heat - HotMetal.DULL_TICKS + 19) / 20 + " s").withStyle(ChatFormatting.GOLD));
        else if (heat > 0) event.getToolTip().add(2, Component.literal("Cooling: quench it in water or wait " + (heat + 19) / 20 + " s").withStyle(ChatFormatting.RED));
    }
}
