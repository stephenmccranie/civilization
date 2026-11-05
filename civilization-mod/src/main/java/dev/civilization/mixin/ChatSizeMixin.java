package dev.civilization.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Chat is sized from the window, not the GUI scale: its text size and width options apply as if the GUI scale were
 * the window height over 360 (3 at 1080p, 4 at 1440p, 6 at 4K) without rounding, never below 1080p's 3 and never
 * wider than the window allows. Every GUI scale setting and window size then shows chat at the same size.
 */
@Mixin(ChatComponent.class)
public abstract class ChatSizeMixin {
    @Shadow @Final private Minecraft minecraft;
    @Inject(method = "getScale", at = @At("RETURN"), cancellable = true)
    private void civilization$scale(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(cir.getReturnValue() * civilization$chatScale());
    }
    @Inject(method = "getWidth()I", at = @At("RETURN"), cancellable = true)
    private void civilization$width(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue((int) Math.round(cir.getReturnValue() * civilization$chatScale()));
    }
    /** The chat's own GUI scale, relative to the window's. */
    private double civilization$chatScale() {
        var window = minecraft.getWindow();
        return Math.min(Math.max(3, window.getHeight() / 360.0), window.getWidth() / 320.0) / window.getGuiScale();
    }
}
