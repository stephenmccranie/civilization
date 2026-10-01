package dev.civilization.visual;

import net.minecraft.client.MouseHandler;
import dev.civilization.VisualTestGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class VisualMouseMixin {
    // Both methods can warp the OS cursor. Releasing it after a grab is already too late.
    @Inject(method = {"grabMouse", "releaseMouse"}, at = @At("HEAD"), cancellable = true)
    private void civilization$noCursorCapture(CallbackInfo ci) {
        VisualTestGuard.preventedMouseCalls++;
        ci.cancel();
    }
}
