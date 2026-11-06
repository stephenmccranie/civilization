package dev.civilization.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.civilization.TemperatureWashConfig;
import dev.civilization.ThermalRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Felt temperature on screen, all driven by one signed strain from cold (-1) to hot (1):
 * a color wash over the finished frame, edge glow and frost over the world view, and a
 * camera shiver or heat sway that follows the vanilla distortion-effects setting.
 */
@EventBusSubscriber(modid = "civilization", value = Dist.CLIENT)
public final class TemperatureWash {
    private record Tone(double red, double green, double blue, double opacity) {
        int argb(double strength) {
            return (int) Math.round(Math.clamp(opacity * strength, 0, 1) * 255) << 24
                    | (int) Math.round(Math.clamp(red, 0, 255)) << 16
                    | (int) Math.round(Math.clamp(green, 0, 255)) << 8
                    | (int) Math.round(Math.clamp(blue, 0, 255));
        }
    }

    private static final Tone COLD = new Tone(78, 138, 236, .23);
    private static final Tone COMFORT = new Tone(240, 179, 116, .10);
    private static final Tone HOT = new Tone(215, 72, 68, .13);
    private static final Tone ICE_EDGE = new Tone(214, 236, 255, .40);
    private static final Tone EMBER_EDGE = new Tone(255, 104, 24, .55);
    private static final ResourceLocation FROST = ResourceLocation.withDefaultNamespace("textures/misc/powder_snow_outline.png");
    /** Every effect is at full strength this far from comfort: 36°F and 104°F felt. */
    private static final double FULL_STRAIN_C = 19;

    private static Level lastWorld;
    private static long lastFrameNanos;
    private static double strain, presence, seconds, beat;

    private TemperatureWash() {}

    private static double ease(double amount) {
        double t = Math.clamp(amount, 0, 1);
        return t * t * (3 - 2 * t);
    }

    /** Eased share of the strain toward one side beyond the point where an effect begins. */
    private static double past(double sideStrain, double start) {
        return ease((sideStrain - start) / (1 - start));
    }

    private static Tone between(Tone a, Tone b, double amount) {
        double t = ease(amount);
        return new Tone(
                a.red + (b.red - a.red) * t,
                a.green + (b.green - a.green) * t,
                a.blue + (b.blue - a.blue) * t,
                a.opacity + (b.opacity - a.opacity) * t);
    }

    /** A lub-dub heartbeat, 0 between beats and 1 at the first. */
    private static double pulse() {
        double phase = beat - Math.floor(beat);
        double lub = (phase - .1) / .07, dub = (phase - .34) / .07;
        return Math.exp(-lub * lub) + .55 * Math.exp(-dub * dub);
    }

    private static double motion() {
        return presence * Minecraft.getInstance().options.screenEffectScale().get();
    }

    private static void advance() {
        var mc = Minecraft.getInstance();
        if (lastWorld != mc.level) {
            lastWorld = mc.level;
            lastFrameNanos = 0;
            strain = presence = 0;
        }
        var sample = ThermalVision.current();
        boolean active = TemperatureWashConfig.ENABLED.get()
                && sample != null && sample.supported() && Float.isFinite(sample.playerTemperature());
        double desired = active ? Math.clamp((sample.playerTemperature() - ThermalRules.COMFORT_CENTER_C) / FULL_STRAIN_C, -1, 1) : 0;
        long now = System.nanoTime();
        double elapsed = lastFrameNanos == 0 ? 0 : Math.clamp((now - lastFrameNanos) / 1_000_000_000.0, 0, .25);
        lastFrameNanos = now;
        double step = 1 - Math.exp(-elapsed / 1.5);
        strain += (desired - strain) * step;
        presence += ((active ? TemperatureWashConfig.INTENSITY.get() : 0) - presence) * step;
        seconds += elapsed;
        beat += elapsed * (70 + 60 * Math.max(0, strain)) / 60;
    }

    private static void wash(GuiGraphics graphics) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        advance();
        int color = between(COMFORT, strain < 0 ? COLD : HOT, 2 * Math.abs(strain)).argb(presence);
        if (color >>> 24 == 0) return;
        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 1000);
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), color);
        graphics.flush();
        graphics.pose().popPose();
    }

    /** Transparent inside an ellipse of {@code inner} half-screens, full tone from the screen edges outward. */
    private static void vignette(GuiGraphics graphics, Tone tone, double strength, double inner) {
        double[] radius = {inner, (inner + 1) / 2, 1, 1.5};
        int[] color = {tone.argb(0), tone.argb(.35 * strength), tone.argb(strength), tone.argb(strength)};
        float halfWidth = graphics.guiWidth() / 2f, halfHeight = graphics.guiHeight() / 2f;
        var pose = graphics.pose().last().pose();
        var buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        int segments = 48;
        for (int i = 0; i < segments; i++) {
            double a = Math.TAU * i / segments, b = Math.TAU * (i + 1) / segments;
            for (int ring = 0; ring < radius.length - 1; ring++) {
                // Counter-clockwise on screen, like GuiGraphics.fill, so the culling GUI type keeps it.
                for (int corner = 0; corner < 4; corner++) {
                    double angle = corner == 1 || corner == 2 ? b : a;
                    int at = ring + (corner < 2 ? 0 : 1);
                    buffer.addVertex(pose,
                                    (float) (halfWidth + Math.cos(angle) * halfWidth * radius[at]),
                                    (float) (halfHeight + Math.sin(angle) * halfHeight * radius[at]), -90)
                            .setColor(color[at]);
                }
            }
        }
        graphics.flush();
    }

    private static void overlay(GuiGraphics graphics) {
        double cold = past(-strain, .25), heat = past(strain, .25);
        if (presence <= 0 || cold + heat == 0) return;
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        if (cold > 0) {
            vignette(graphics, ICE_EDGE, cold * presence, .6);
            double frost = past(-strain, .45) * .8 * presence;
            if (frost > 0) {
                RenderSystem.enableBlend();
                graphics.setColor(1, 1, 1, (float) Math.min(frost, 1));
                graphics.blit(FROST, 0, 0, -90, 0, 0, graphics.guiWidth(), graphics.guiHeight(), graphics.guiWidth(), graphics.guiHeight());
                graphics.setColor(1, 1, 1, 1);
                RenderSystem.disableBlend();
            }
        }
        if (heat > 0) {
            double beatNow = pulse();
            vignette(graphics, EMBER_EDGE, heat * (.7 + .3 * beatNow) * presence, .5 - .08 * beatNow);
        }
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles event) {
        double shiver = past(-strain, .5), sway = past(strain, .5);
        double burst = Math.pow(Math.max(0, Math.sin(seconds * 1.4)), 2);
        double roll = shiver * burst * (.6 * Math.sin(seconds * 43) + .4 * Math.sin(seconds * 71)) * .9
                + sway * Math.sin(seconds * .8) * 1.6;
        event.setRoll(event.getRoll() + (float) (roll * motion()));
    }

    @SubscribeEvent
    public static void fov(ViewportEvent.ComputeFov event) {
        if (event.usedConfiguredFov()) event.setFOV(event.getFOV() * (1 - .02 * past(strain, .5) * pulse() * motion()));
    }

    @EventBusSubscriber(modid = "civilization", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Layers {
        @SubscribeEvent
        public static void register(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, ResourceLocation.fromNamespaceAndPath("civilization", "temperature"),
                    (graphics, delta) -> overlay(graphics));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void afterHud(RenderGuiEvent.Post event) {
        if (Minecraft.getInstance().screen == null) wash(event.getGuiGraphics());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void afterScreen(ScreenEvent.Render.Post event) {
        wash(event.getGuiGraphics());
    }
}
