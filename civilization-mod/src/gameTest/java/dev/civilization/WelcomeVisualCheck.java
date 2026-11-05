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
            mc.options.chatScale().set(1.0);
            mc.options.chatWidth().set(1.0);
            mc.options.chatHeightUnfocused().set(1.0);
            mc.resizeDisplay();
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        }
        if (ticks == 30) {
            fits(mc);
            shot(mc, "1080p", 3);
        }
        if (ticks == 35) org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 2560, 1440);
        if (ticks == 60) shot(mc, "1440p", 4);
        if (ticks == 65) mc.setScreen(new ChatScreen(""));
        if (ticks == 80) shot(mc, "1440p-open", 4);
        if (ticks == 90) mc.stop();
    }
    /** Every line fits the closed chat unwrapped, at the width the chat wraps to. */
    private static void fits(Minecraft mc) {
        var chat = mc.gui.getChat();
        int width = net.minecraft.util.Mth.floor(chat.getWidth() / chat.getScale());
        if (Welcome.MESSAGE.size() > chat.getLinesPerPage()) throw new IllegalStateException("Welcome has " + Welcome.MESSAGE.size() + " lines; closed chat shows " + chat.getLinesPerPage());
        for (var line : Welcome.MESSAGE) if (mc.font.width(line) > width) throw new IllegalStateException("Welcome line wraps at " + mc.font.width(line) + " of " + width + " px: " + line.getString());
    }
    private static void shot(Minecraft mc, String name, int expectedScale) {
        var window = mc.getWindow();
        if (window.getGuiScale() != expectedScale) throw new IllegalStateException("Auto GUI scale at " + window.getWidth() + "x" + window.getHeight() + " is " + window.getGuiScale() + ", expected " + expectedScale);
        Screenshot.grab(mc.gameDirectory, "welcome-" + name + ".png", mc.getMainRenderTarget(), message -> {});
    }
}
