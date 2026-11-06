package dev.civilization.mixin.compat;

import dev.civilization.CloudCover;
import dev.civilization.client.LocalWeather;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL20C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Each Iris pass that draws the shared cloud field gets its uniforms when bound: Photon's prepare and deferred passes. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.gl.program.Program", remap = false)
public abstract class IrisCloudUniformMixin {
    @Unique private static final float[] civilization$clear = new float[9];
    // Cloud shift, grid offset and cloudiness locations, -1 where this program lacks one.
    @Unique private int[] civilization$cloudUniforms;

    @Shadow public abstract int getProgramId();

    @Inject(method = "use()V", at = @At("TAIL"), require = 1)
    private void civilization$clouds(CallbackInfo ci) {
        int[] at = civilization$cloudUniforms;
        if (at == null) {
            int id = getProgramId();
            at = civilization$cloudUniforms = new int[]{GL20C.glGetUniformLocation(id, "civilizationCloudShift"),
                    GL20C.glGetUniformLocation(id, "civilizationCloudGrid"), GL20C.glGetUniformLocation(id, "civilizationCloudiness")};
        }
        if (at[0] < 0 && at[1] < 0 && at[2] < 0) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        var camera = mc.gameRenderer.getMainCamera().getPosition();
        var snapshot = LocalWeather.snapshot(mc.level);
        double clock = LocalWeather.clock(mc.level, mc.getTimer().getGameTimeDeltaPartialTick(false));
        // Before the first snapshot the camera sits in a clear centre district.
        double cornerX = snapshot == null ? camera.x - 1.5 * CloudCover.DISTRICT : (snapshot.x() - 1) * (double) CloudCover.DISTRICT;
        double cornerZ = snapshot == null ? camera.z - 1.5 * CloudCover.DISTRICT : (snapshot.z() - 1) * (double) CloudCover.DISTRICT;
        if (at[0] >= 0) GL20C.glUniform2f(at[0], CloudCover.shift(camera.x, CloudCover.WIND_X, clock), CloudCover.shift(camera.z, CloudCover.WIND_Z, clock));
        if (at[1] >= 0) GL20C.glUniform2f(at[1], (float) (camera.x - cornerX), (float) (camera.z - cornerZ));
        if (at[2] >= 0) GL20C.glUniform1fv(at[2], snapshot == null ? civilization$clear : snapshot.cloudiness());
    }
}
