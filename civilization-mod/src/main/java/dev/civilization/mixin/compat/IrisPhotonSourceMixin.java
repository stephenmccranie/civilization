package dev.civilization.mixin.compat;

import dev.civilization.client.PhotonClouds;
import dev.civilization.client.PhotonRivers;
import java.nio.file.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Every shader-pack file passes here as Iris reads it, before includes and the preprocessor. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.shaderpack.include.IncludeGraph", remap = false)
public abstract class IrisPhotonSourceMixin {
    @Inject(method = "readFile(Ljava/nio/file/Path;)Ljava/lang/String;", at = @At("RETURN"), cancellable = true, require = 1)
    private static void civilization$photon(Path path, CallbackInfoReturnable<String> source) {
        String name = path.toString().replace('\\', '/');
        source.setReturnValue(PhotonRivers.patch(name, PhotonClouds.patch(name, source.getReturnValue())));
    }
}
