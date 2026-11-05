package dev.civilization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ChatScreen;

/**
 * The join welcome as players see it with the pack's Auto GUI scale and chat settings, received from the integrated
 * server's real login: at 1080p, with GUI scale 2 there, in a 768p window and at 1440p. Chat text keeps 1080p's size
 * whatever the GUI scale and on the smaller window, and grows with the window above it.
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
        if (ticks == 30) {
            fits(mc);
            shot(mc, "1080p", 3, 2.25);
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
        }
        if (ticks == 40) {
            fits(mc);
            shot(mc, "1080p-gui2", 2, 2.25);
            mc.options.guiScale().set(0);
            org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1366, 768);
        }
        if (ticks == 65) {
            fits(mc);
            shot(mc, "768p", 2, 2.25);
            org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 2560, 1440);
        }
        if (ticks == 90) shot(mc, "1440p", 4, 3);
        if (ticks == 95) mc.setScreen(new ChatScreen(""));
        if (ticks == 110) shot(mc, "1440p-open", 4, 3);
        if (ticks == 120) mc.stop();
    }
    /** Every line fits the closed chat unwrapped, at the width the chat wraps to. */
    private static void fits(Minecraft mc) {
        var chat = mc.gui.getChat();
        int width = net.minecraft.util.Mth.floor(chat.getWidth() / chat.getScale());
        if (Welcome.MESSAGE.size() > chat.getLinesPerPage()) throw new IllegalStateException("Welcome has " + Welcome.MESSAGE.size() + " lines; closed chat shows " + chat.getLinesPerPage());
        for (var line : Welcome.MESSAGE) if (mc.font.width(line) > width) throw new IllegalStateException("Welcome line wraps at " + mc.font.width(line) + " of " + width + " px: " + line.getString());
    }
    /** Checks the GUI scale and the chat's screen pixels per font pixel, then takes the screenshot. */
    private static void shot(Minecraft mc, String name, int expectedScale, double textPixels) {
        var window = mc.getWindow();
        if (window.getGuiScale() != expectedScale) throw new IllegalStateException("GUI scale at " + window.getWidth() + "x" + window.getHeight() + " is " + window.getGuiScale() + ", expected " + expectedScale);
        double drawn = window.getGuiScale() * mc.gui.getChat().getScale();
        if (Math.abs(drawn - textPixels) > 1e-9) throw new IllegalStateException("Chat text at " + name + " draws " + drawn + " screen pixels per font pixel, expected " + textPixels);
        Screenshot.grab(mc.gameDirectory, "welcome-" + name + ".png", mc.getMainRenderTarget(), message -> {});
    }
}
