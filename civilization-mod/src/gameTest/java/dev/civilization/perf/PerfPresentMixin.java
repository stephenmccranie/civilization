package dev.civilization.perf;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.civilization.PerfPresent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The hidden benchmark client paces frames with {@link PerfPresent} instead of swapping a window nobody sees. */
@Mixin(RenderSystem.class)
public abstract class PerfPresentMixin {
    @Redirect(method = "flipFrame", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V", remap = false))
    private static void civilization$present(long window) {
        PerfPresent.present();
    }
}
