package dev.civilization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ChatScreen;

/** The join welcome as players see it at the pack's GUI scale, closed and open, received from the integrated server's real login. */
final class WelcomeVisualCheck {
    private static int ticks;
    static void tick(Minecraft mc) {
        ticks++;
        if (ticks == 10) {
            mc.options.hideGui = false;
            mc.options.guiScale().set(4);
            mc.resizeDisplay();
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        }
        if (ticks == 40) shot(mc, "closed");
        if (ticks == 45) mc.setScreen(new ChatScreen(""));
        if (ticks == 60) shot(mc, "open");
        if (ticks == 70) mc.stop();
    }
    private static void shot(Minecraft mc, String name) { Screenshot.grab(mc.gameDirectory, "welcome-" + name + ".png", mc.getMainRenderTarget(), message -> {}); }
}
