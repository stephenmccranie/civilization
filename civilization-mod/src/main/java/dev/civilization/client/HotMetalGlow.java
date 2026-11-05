package dev.civilization.client;

import dev.civilization.Civilization;
import dev.civilization.HotMetal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;

/** Hot metal glows in its slot, fading as it cools. */
@EventBusSubscriber(modid = Civilization.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class HotMetalGlow {
    private HotMetalGlow() {}

    @SubscribeEvent public static void register(RegisterItemDecorationsEvent event) {
        for (var item : BuiltInRegistries.ITEM) event.register(item, (graphics, font, stack, x, y) -> {
            var level = Minecraft.getInstance().level;
            long left = level == null ? 0 : HotMetal.heatLeft(stack, level.getGameTime());
            if (left == 0) return false;
            int alpha = 0x30 + (int)(0x80 * left / HotMetal.COOL_TICKS);
            graphics.fillGradient(RenderType.guiOverlay(), x, y, x + 16, y + 16, alpha / 2 << 24 | 0xFFB040, alpha << 24 | 0xFF5020, 0);
            return false;
        });
    }
}
