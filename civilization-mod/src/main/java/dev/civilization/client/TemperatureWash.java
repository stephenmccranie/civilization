package dev.civilization.client;

import dev.civilization.TemperatureWashConfig;
import dev.civilization.ThermalRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** A restrained color wash over the finished frame, including HUD and menus. */
@EventBusSubscriber(modid = "civilization", value = Dist.CLIENT)
public final class TemperatureWash {
    private record Tone(double red, double green, double blue, double opacity) {}

    private static final Tone COLD = new Tone(112, 153, 199, .085);
    private static final Tone COMFORT = new Tone(240, 179, 116, .10);
    private static final Tone HOT = new Tone(215, 72, 68, .13);

    private static Level lastWorld;
    private static long lastFrameNanos;
    private static double red, green, blue, opacity;

    private TemperatureWash() {}

    private static double ease(double amount) {
        double t = Math.clamp(amount, 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static Tone between(Tone a, Tone b, double amount) {
        double t = ease(amount);
        return new Tone(
                a.red + (b.red - a.red) * t,
                a.green + (b.green - a.green) * t,
                a.blue + (b.blue - a.blue) * t,
                a.opacity + (b.opacity - a.opacity) * t);
    }

    private static Tone target(double celsius) {
        double comfort=ThermalRules.comfort(celsius);
        return celsius<ThermalRules.COMFORT_CENTER_C
                ?between(COLD,COMFORT,comfort):between(HOT,COMFORT,comfort);
    }

    private static void draw(GuiGraphics graphics) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (lastWorld != mc.level) {
            lastWorld = mc.level;
            lastFrameNanos = 0;
            opacity = 0;
        }

        var sample = ThermalVision.current();
        boolean active = TemperatureWashConfig.ENABLED.get()
                && sample != null && sample.supported() && Float.isFinite(sample.playerTemperature());
        Tone desired = active ? target(sample.playerTemperature()) : COMFORT;
        double desiredOpacity = active ? desired.opacity * TemperatureWashConfig.INTENSITY.get() : 0;
        long now = System.nanoTime();
        double seconds = lastFrameNanos == 0 ? 0 : Math.clamp((now - lastFrameNanos) / 1_000_000_000.0, 0, .25);
        lastFrameNanos = now;
        double step = 1 - Math.exp(-seconds / 1.5);
        red += (desired.red - red) * step;
        green += (desired.green - green) * step;
        blue += (desired.blue - blue) * step;
        opacity += (desiredOpacity - opacity) * step;

        int alpha = (int) Math.round(Math.clamp(opacity, 0, 1) * 255);
        if (alpha == 0) return;
        int color = alpha << 24
                | (int) Math.round(Math.clamp(red, 0, 255)) << 16
                | (int) Math.round(Math.clamp(green, 0, 255)) << 8
                | (int) Math.round(Math.clamp(blue, 0, 255));
        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 1000);
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), color);
        graphics.flush();
        graphics.pose().popPose();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void afterHud(RenderGuiEvent.Post event) {
        if (Minecraft.getInstance().screen == null) draw(event.getGuiGraphics());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void afterScreen(ScreenEvent.Render.Post event) {
        draw(event.getGuiGraphics());
    }
}
