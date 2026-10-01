package dev.civilization.client;

import dev.civilization.Civilization;
import dev.civilization.EquipmentGrade;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;

/** Fixed-width grade and durability bar: green available, black used, gray unavailable, cyan bonus. */
@EventBusSubscriber(modid = Civilization.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class GradeBar {
    private GradeBar() {}

    @SubscribeEvent public static void register(RegisterItemDecorationsEvent event) {
        for (var item : BuiltInRegistries.ITEM) {
            if (item.components().getOrDefault(DataComponents.MAX_DAMAGE, 0) <= 0) continue;
            event.register(item, (graphics, font, stack, x, y) -> {
                if (!EquipmentGrade.hasGrade(stack)) return false;
                int vanilla = item.components().getOrDefault(DataComponents.MAX_DAMAGE, 1);
                int current = stack.getMaxDamage();
                int durabilityLeft = Math.max(0, current - stack.getDamageValue());
                int left = x + 2, top = y + 13;
                graphics.fill(RenderType.guiOverlay(), left, top, left + 13, top + 2, 0xFF161412);
                int bonus = Math.max(0, current - vanilla);
                // Reserve at most two of the existing 13 pixels for the first 10% of bonus grade.
                int bonusWidth = bonus == 0 ? 0 : Math.min(2, Math.max(1, Math.round(20f * bonus / vanilla)));
                int ordinaryWidth = 13 - bonusWidth;
                int available = bonus == 0 ? Math.min(13, Math.max(0, Math.round(13f * current / vanilla))) : ordinaryWidth;
                int remaining = Math.min(available, Math.max(0, Math.round(ordinaryWidth * Math.min(durabilityLeft, vanilla) / (float) vanilla)));
                if (remaining > 0) graphics.fill(RenderType.guiOverlay(), left, top, left + remaining, top + 1, 0xFF59C865);
                if (available > remaining) graphics.fill(RenderType.guiOverlay(), left + remaining, top, left + available, top + 1, 0xFF101112);
                // Gray marks capacity this grade cannot regain; black retains vanilla's damage meaning.
                if (available + bonusWidth < 13) graphics.fill(RenderType.guiOverlay(), left + available, top, left + 13 - bonusWidth, top + 1, 0xFF858782);
                if (bonusWidth > 0) {
                    int bonusLeft = Math.min(bonusWidth, Math.max(0, Math.round(bonusWidth * Math.max(0, durabilityLeft - vanilla) / (float) bonus)));
                    int start = left + ordinaryWidth;
                    if (bonusLeft > 0) graphics.fill(RenderType.guiOverlay(), start, top, start + bonusLeft, top + 1, 0xFF55D8E8);
                    if (bonusLeft < bonusWidth) graphics.fill(RenderType.guiOverlay(), start + bonusLeft, top, start + bonusWidth, top + 1, 0xFF101112);
                }
                return false;
            });
        }
    }
}
