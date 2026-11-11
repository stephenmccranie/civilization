package dev.civilization.client;

import dev.civilization.RiverFlow;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import org.lwjgl.opengl.GL20C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Photon's river waves run downstream. The server sends the lines of the rivers near the player; this keeps their
 * chords, gives the water program the {@link #CHORDS} nearest the camera, and rewrites Photon's translucent program
 * to lay its flowing-water waves along them ({@code river_flow.glsl}). Lakes, the sea, other packs and Photon files
 * this does not recognise keep their own still water.
 */
@net.neoforged.fml.common.EventBusSubscriber(modid = "civilization", value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class PhotonRivers {
    private static final Logger LOG = LoggerFactory.getLogger("Civilization/PhotonRivers");
    /** Chords the water program holds; it must match the arrays in {@code river_flow.glsl}. */
    static final int CHORDS = 64;
    // Floats per chord: start x and z, end x and z, river above the start and the end, half-width at both ends
    private static final int CHORD = 8;
    // The nearest chords are picked again once the camera has moved this far
    private static final double RESELECT = 16;

    private static final String PROGRAM = "/program/gbuffers_all_translucent.fsh";
    private static final String TBN = "#else\n#define tbn_fixed tbn\n#endif";
    private static final String FLOW = "flow_dir = flowing_water ? normalize(flat_normal.xz) : vec2(0.0);\n            }";
    private static final String MAIN = "\nvoid main() {";
    private static final String FIELD = read();
    /** Whether Photon's translucent program was rewritten to run river waves downstream. */
    public static volatile boolean patched;

    private static Level world;
    private static float[] chords = new float[0];
    private static int[] selected = new int[0];
    private static double selectedX = Double.NaN, selectedZ = Double.NaN;
    private static final float[] lineBuffer = new float[CHORDS * 4], alongBuffer = new float[CHORDS * 4];

    private PhotonRivers() {}

    private static String read() {
        try (var in = PhotonRivers.class.getResourceAsStream("/assets/civilization/shaders/river_flow.glsl")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The source Iris reads for a pack file, rewritten if it is Photon's translucent program. */
    public static String patch(String path, String source) {
        if (source == null || !path.endsWith(PROGRAM)) return source;
        int main = source.indexOf(MAIN);
        if (!source.contains(TBN) || !source.contains(FLOW) || main < 0) {
            LOG.warn("River waves inactive: {} does not have the expected Photon v1.3b shape; rivers keep still water", path);
            return source;
        }
        patched = true;
        return (source.substring(0, main) + "\n" + FIELD + source.substring(main))
                .replace(TBN, "#else\nmat3 tbn_fixed = tbn;\n#endif")
                .replace(FLOW, FLOW + "\n            civilization_river_flow(position_scene, flat_normal, coord, flow_dir, flowing_water, tbn_fixed);");
    }

    /** Keeps the river lines the server sent for this level: points of block x, block z, river above, half-width. */
    public static void accept(Level level, float[][] lines) {
        int n = 0;
        for (float[] line : lines) n += line.length / RiverFlow.POINT - 1;
        float[] out = new float[n * CHORD];
        int c = 0;
        for (float[] line : lines) {
            for (int o = 0; o + RiverFlow.POINT < line.length; o += RiverFlow.POINT, c += CHORD) {
                int q = o + RiverFlow.POINT;
                out[c] = line[o];
                out[c + 1] = line[o + 1];
                out[c + 2] = line[q];
                out[c + 3] = line[q + 1];
                out[c + 4] = line[o + 2];
                out[c + 5] = line[q + 2];
                out[c + 6] = line[o + 3];
                out[c + 7] = line[q + 3];
            }
        }
        world = level;
        chords = out;
        selectedX = Double.NaN;
    }

    /** Hands the bound program the chords nearest the camera, each relative to it; locations are -1 where absent. */
    public static void upload(int count, int line, int along) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.level != world) {
            if (count >= 0) GL20C.glUniform1i(count, 0);
            return;
        }
        var camera = mc.gameRenderer.getMainCamera().getPosition();
        if (!(Math.abs(camera.x - selectedX) < RESELECT && Math.abs(camera.z - selectedZ) < RESELECT)) {
            selected = nearest(chords, camera.x, camera.z);
            selectedX = camera.x;
            selectedZ = camera.z;
        }
        for (int k = 0; k < selected.length; k++) {
            int c = selected[k] * CHORD, o = k * 4;
            lineBuffer[o] = (float) (chords[c] - camera.x);
            lineBuffer[o + 1] = (float) (chords[c + 1] - camera.z);
            lineBuffer[o + 2] = (float) (chords[c + 2] - camera.x);
            lineBuffer[o + 3] = (float) (chords[c + 3] - camera.z);
            System.arraycopy(chords, c + 4, alongBuffer, o, 4);
        }
        if (count >= 0) GL20C.glUniform1i(count, selected.length);
        if (line >= 0) GL20C.glUniform4fv(line, lineBuffer);
        if (along >= 0) GL20C.glUniform4fv(along, alongBuffer);
    }

    /** The chords nearest (x, z), at most {@link #CHORDS}, in their original order so a line's chords stay in turn. */
    static int[] nearest(float[] chords, double x, double z) {
        int n = chords.length / CHORD;
        long[] keyed = new long[n];
        for (int k = 0; k < n; k++) {
            int c = k * CHORD;
            double ax = chords[c] - x, az = chords[c + 1] - z, bx = chords[c + 2] - x, bz = chords[c + 3] - z;
            double abx = bx - ax, abz = bz - az, len2 = abx * abx + abz * abz;
            double t = len2 > 0 ? Math.clamp(-(ax * abx + az * abz) / len2, 0, 1) : 0;
            double dx = ax + abx * t, dz = az + abz * t;
            keyed[k] = (long) Float.floatToIntBits((float) (dx * dx + dz * dz)) << 32 | k;
        }
        Arrays.sort(keyed);
        int[] out = new int[Math.min(CHORDS, n)];
        for (int k = 0; k < out.length; k++) out[k] = (int) keyed[k];
        Arrays.sort(out);
        return out;
    }

    /** Forgets the lines when their level unloads. */
    @net.neoforged.bus.api.SubscribeEvent
    public static void unload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) {
        if (event.getLevel() == world) {
            world = null;
            chords = new float[0];
            selected = new int[0];
        }
    }
}
