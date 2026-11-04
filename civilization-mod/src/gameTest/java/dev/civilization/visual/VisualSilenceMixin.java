package dev.civilization.visual;

import com.mojang.blaze3d.audio.Listener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Listener.class)
public abstract class VisualSilenceMixin {
    // A hidden client plays every sound as usual, so scenes can check them, but OpenAL hears no master gain: nothing
    // reaches the speakers. The listener keeps the gain it was given, since the engine skips sounds while that is zero.
    @ModifyArg(method = "setGain", at = @At(value = "INVOKE", target = "Lorg/lwjgl/openal/AL10;alListenerf(IF)V", remap = false), index = 1)
    private float civilization$silent(float gain) {
        return 0;
    }
}
