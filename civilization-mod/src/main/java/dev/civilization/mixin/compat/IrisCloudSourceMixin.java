package dev.civilization.mixin.compat;

import dev.civilization.client.PhotonClouds;
import java.nio.file.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Every shader-pack file passes here as Iris reads it, before includes and the preprocessor. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.shaderpack.include.IncludeGraph", remap = false)
public abstract class IrisCloudSourceMixin {
    @Inject(method = "readFile(Ljava/nio/file/Path;)Ljava/lang/String;", at = @At("RETURN"), cancellable = true, require = 1)
    private static void civilization$clouds(Path path, CallbackInfoReturnable<String> source) {
        source.setReturnValue(PhotonClouds.patch(path.toString().replace('\\', '/'), source.getReturnValue()));
    }
}
