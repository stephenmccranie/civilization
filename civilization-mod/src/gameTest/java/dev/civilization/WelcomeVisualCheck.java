package dev.civilization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ChatScreen;

/**
 * The join welcome as players see it with the pack's Auto GUI scale and chat settings, received from the integrated
 * server's real login, at 1080p and then 1440p to show the interface keeping its share of the screen.
 */
final class WelcomeVisualCheck {
    private static int ticks;
    static void tick(Minecraft mc) {
        ticks++;
        if (ticks == 10) {
            mc.options.hideGui = false;
            mc.options.guiScale().set(0);
            mc.options.chatScale().set(0.75);
            mc.options.chatWidth().set(0.8);
            mc.options.chatHeightUnfocused().set(1.0);
            mc.resizeDisplay();
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        }
        if (ticks == 30) shot(mc, "1080p", 3);
        if (ticks == 35) org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 2560, 1440);
        if (ticks == 60) shot(mc, "1440p", 4);
        if (ticks == 65) mc.setScreen(new ChatScreen(""));
        if (ticks == 80) shot(mc, "1440p-open", 4);
        if (ticks == 90) mc.stop();
    }
    private static void shot(Minecraft mc, String name, int expectedScale) {
        var window = mc.getWindow();
        if (window.getGuiScale() != expectedScale) throw new IllegalStateException("Auto GUI scale at " + window.getWidth() + "x" + window.getHeight() + " is " + window.getGuiScale() + ", expected " + expectedScale);
        Screenshot.grab(mc.gameDirectory, "welcome-" + name + ".png", mc.getMainRenderTarget(), message -> {});
    }
}
