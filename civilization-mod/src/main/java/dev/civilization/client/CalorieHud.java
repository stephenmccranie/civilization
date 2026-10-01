package dev.civilization.client;

import dev.civilization.CalorieFoodData;
import dev.civilization.FoodCalories;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = "civilization", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class CalorieHud {
    private CalorieHud() {}


    @SubscribeEvent
    public static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(dev.civilization.KilnContent.MENU.get(), KilnScreen::new);
        event.register(dev.civilization.KilnContent.RETORT_MENU.get(), KilnScreen::new);
        event.register(dev.civilization.KilnContent.FOUNDRY_MENU.get(), KilnScreen::new);
        event.register(dev.civilization.CookingContent.MENU.get(), KilnScreen::new);
        event.register(dev.civilization.PrototypeStoveContent.MENU.get(), PrototypeStoveScreen::new);
        event.register(dev.civilization.CivicContent.LAND_MENU.get(), LandScreen::new);
        event.register(dev.civilization.CivicContent.SHOP_MENU.get(), TradeScreen::new);
        event.register(dev.civilization.CivicContent.SURVEY_MENU.get(), SurveyScreen::new);
    }

    @SubscribeEvent
    public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                dev.civilization.FarmingContent.FERTILIZED_WHEAT.get(), net.minecraft.client.renderer.RenderType.cutout()));
    }

    @SubscribeEvent
    public static void layers(RegisterGuiLayersEvent event) {
        event.replaceLayer(VanillaGuiLayers.FOOD_LEVEL, (graphics, delta) -> {
            var mc = Minecraft.getInstance();
            if (mc.player == null || mc.options.hideGui || mc.player.isCreative() || mc.player.isSpectator()) return;
            var data = CalorieFoodData.of(mc.player);
            var reserve = data.reserve();
            int x = graphics.guiWidth() / 2 + 10;
            int y = graphics.guiHeight() - mc.gui.rightHeight;
            int width = 81;
            double ratio = reserve.calories() / reserve.capacity();
            boolean critical = data.isDepleted() || ratio <= 0.1;
            boolean low = ratio <= 0.25;
            int color = critical ? 0xFFCC4935 : low ? 0xFFE48C28 : 0xFFDDA43B;
            int highlight = critical ? 0xFFF58B68 : low ? 0xFFFFCB65 : 0xFFFFDE83;
            int shade = critical ? 0xFF78251E : low ? 0xFF914819 : 0xFF8E5B20;

            // Nine-pixel silhouette and ten divisions echo the vanilla survival meters.
            // Keep all geometry and the font on whole GUI pixels for crisp integer scaling.
            graphics.fill(x + 1, y, x + width - 1, y + 9, 0xFF100D09);
            graphics.fill(x, y + 1, x + width, y + 8, 0xFF100D09);
            graphics.fill(x + 1, y + 1, x + width - 1, y + 8, 0xFF30291E);
            graphics.fill(x + 1, y + 1, x + width - 1, y + 2, 0xFF19160F);
            graphics.fill(x + 1, y + 7, x + width - 1, y + 8, 0xFF51432F);

            int innerWidth = width - 2;
            int right = x + width - 1;
            int filled = (int) Math.ceil(innerWidth * Math.clamp(ratio, 0, 1));
            int left = right - filled;
            if (filled > 0) {
                // Anchor energy to the right: depletion advances from left to right.
                graphics.fill(left, y + 2, right, y + 7, color);
                graphics.fill(left, y + 1, right, y + 2, highlight);
                graphics.fill(left, y + 7, right, y + 8, shade);
            }
            for (int division = 1; division < 10; division++) {
                int tick = x + division * 8;
                graphics.fill(tick, y + 1, tick + 1, y + 8, 0xB020160B);
            }
            // Mirror the sprint threshold along with the fill direction.
            int threshold = right - (int) Math.clamp(
                    Math.ceil(innerWidth * (data.isDepleted() ? data.clientRecoveryThreshold : data.clientSprintMinimum)
                            / reserve.capacity()), 1, innerWidth);
            graphics.fill(threshold, y + 9, threshold + 1, y + 10, 0xFFFFB05C);
            String label = String.format(Locale.ROOT, "%,.0f kcal", reserve.calories());
            graphics.drawString(mc.font, label, x + width - mc.font.width(label), y - 10,
                    critical ? 0xFFFF8A75 : low ? 0xFFFFCE76 : 0xFFFFFFFF, true);
            ComfortHud.draw(graphics, graphics.guiWidth()/2, y);
            mc.gui.rightHeight += 21;
            if (data.isDepleted()) {
                var warning = Component.translatable("hud.civilization.depleted");
                graphics.drawString(mc.font, warning, x + width - mc.font.width(warning), y - 20, 0xFFFF8A75, true);
                mc.gui.rightHeight += 10;
            }
        });
    }

    @EventBusSubscriber(modid = "civilization", value = Dist.CLIENT)
    public static final class Tooltips {
        @SubscribeEvent
        public static void tooltip(ItemTooltipEvent event) {
            if (event.getEntity() == null) return;
            if (event.getItemStack().is(net.minecraft.world.item.Items.BONE_MEAL))
                event.getToolTip().add(Component.translatable("message.civilization.bone_meal_disabled").withStyle(ChatFormatting.GRAY));
            if (event.getItemStack().is(dev.civilization.CookingContent.STATION_ITEM.get())) {
                event.getToolTip().add(Component.translatable("tooltip.civilization.cooking_input").withStyle(ChatFormatting.GRAY));
                event.getToolTip().add(Component.translatable("tooltip.civilization.cooking_fuel").withStyle(ChatFormatting.GRAY));
            }
            if (event.getItemStack().is(dev.civilization.CookingContent.BREAD_DOUGH.get())
                    || event.getItemStack().is(dev.civilization.CookingContent.COOKIE_DOUGH.get())
                    || event.getItemStack().is(dev.civilization.CookingContent.UNBAKED_PIE.get())
                    || event.getItemStack().is(dev.civilization.CookingContent.CAKE_BATTER.get()))
                event.getToolTip().add(Component.translatable("tooltip.civilization.bake").withStyle(ChatFormatting.GRAY));
            if (event.getItemStack().is(dev.civilization.KilnContent.RETORT_ITEM.get())) {
                event.getToolTip().add(Component.translatable("tooltip.civilization.preview").withStyle(ChatFormatting.GRAY));
                event.getToolTip().add(Component.translatable("tooltip.civilization.retort_input").withStyle(ChatFormatting.GRAY));
            }
            if (event.getItemStack().is(dev.civilization.FarmingContent.MINERAL_BLEND.get()))
                event.getToolTip().add(Component.translatable("tooltip.civilization.mineral_blend").withStyle(ChatFormatting.GRAY));
            if (event.getItemStack().is(dev.civilization.KilnContent.MINERAL_COAL.get()))
                event.getToolTip().add(Component.translatable("tooltip.civilization.mineral_coal").withStyle(ChatFormatting.GRAY));
            if (event.getItemStack().is(dev.civilization.KilnContent.KILN_ITEM.get())) {
                event.getToolTip().add(Component.translatable("tooltip.civilization.preview").withStyle(ChatFormatting.GRAY));
                event.getToolTip().add(Component.translatable("tooltip.civilization.kiln_input").withStyle(ChatFormatting.GRAY));
            }
            if (event.getItemStack().is(dev.civilization.FarmingContent.FERTILIZER.get()))
                event.getToolTip().add(Component.translatable("tooltip.civilization.fertilizer").withStyle(ChatFormatting.GRAY));
            if (event.getItemStack().is(dev.civilization.FarmingContent.RATION.get()))
                event.getToolTip().add(Component.translatable("tooltip.civilization.ration").withStyle(ChatFormatting.GRAY));
            var food = event.getItemStack().getFoodProperties(event.getEntity());
            if (food != null) event.getToolTip().add(Component.translatable("tooltip.civilization.calories",
                    String.format(Locale.ROOT, "%,.0f", FoodCalories.of(event.getItemStack(), food)))
                    .withStyle(ChatFormatting.GOLD));
        }
    }
}
