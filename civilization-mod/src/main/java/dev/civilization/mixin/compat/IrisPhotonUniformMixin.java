package dev.civilization.mixin.compat;

import dev.civilization.CloudCover;
import dev.civilization.client.LocalWeather;
import dev.civilization.client.PhotonRivers;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL20C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every Iris program that draws the shared cloud field or river water gets those uniforms with its own. Iris keeps
 * one of these per program and updates it while the program is bound, whatever kind of program it is: Photon's
 * prepare and deferred passes, Sodium's terrain and water, entities and Distant Horizons.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.gl.program.ProgramUniforms", remap = false)
public abstract class IrisPhotonUniformMixin {
    @Unique private static final float[] civilization$clear = new float[9];
    @Unique private static final String[] civilization$names = {"civilizationCloudShift", "civilizationCloudGrid",
            "civilizationCloudiness", "civilizationRiverCount", "civilizationRiverLine", "civilizationRiverAlong"};
    // Each name's location in this program, -1 where it lacks one; found on the first update, with the program bound.
    @Unique private int[] civilization$uniforms;

    @Inject(method = "update()V", at = @At("TAIL"), require = 1)
    private void civilization$uniforms(CallbackInfo ci) {
        int[] at = civilization$uniforms;
        if (at == null) {
            int id = GL20C.glGetInteger(GL20C.GL_CURRENT_PROGRAM);
            at = civilization$uniforms = new int[civilization$names.length];
            for (int k = 0; k < at.length; k++) at[k] = GL20C.glGetUniformLocation(id, civilization$names[k]);
        }
        if (at[0] >= 0 || at[1] >= 0 || at[2] >= 0) civilization$clouds(at);
        if (at[3] >= 0 || at[4] >= 0 || at[5] >= 0) PhotonRivers.upload(at[3], at[4], at[5]);
    }

    @Unique
    private static void civilization$clouds(int[] at) {
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
