package dev.civilization.client;

import java.util.List;
import net.minecraft.util.Mth;

/**
 * Pieces afloat in a machine's water, such as hides steeping in a tan pit. Each drifts on a slow eddy that wanders round
 * the pool, turning with it; neighbours nudge each other apart and the walls hold them in. The first floats awash and
 * each further one rides a little deeper, so crowded pieces pass over and under rather than through each other. All in
 * the model's own space, in blocks, stepped by the frame while the machine is drawn.
 */
final class Drift {
    /** A piece's rest centre in the model and its half extents across (x) and along (z). */
    record Piece(float x, float y, float z, float halfX, float halfZ) {}
    /** The water's surface: its extent and height. */
    record Pool(float x0, float x1, float z0, float z1, float y) {}

    private static final float STEP = 1 / 40f, RIDE = .2f / 16, DEEPER = .3f / 16, BOB = .12f / 16;
    private float[] x = {}, z = {}, vx = {}, vz = {}, yaw = {}, spin = {};
    private double clock = Double.NaN;

    /** Advances the drift to `time`, in seconds, for `count` pieces; a new count spreads them afresh along the pool. */
    void step(Pool pool, List<Piece> pieces, int count, double time, long seed) {
        if (count != x.length) place(pool, count, seed);
        double elapsed = time - clock;
        clock = time;
        // Drawn again after a pause, the pieces carry on where they were.
        if (!(elapsed > 0) || elapsed > 1) return;
        for (float t = 0; t < elapsed; t += STEP) move(pool, pieces, (float) Math.min(STEP, elapsed - t), (float) (time - elapsed + t));
    }

    private void place(Pool pool, int count, long seed) {
        x = new float[count]; z = new float[count]; vx = new float[count]; vz = new float[count]; yaw = new float[count]; spin = new float[count];
        var random = new java.util.Random(seed);
        for (int i = 0; i < count; i++) {
            x[i] = Mth.lerp((i + .5f) / count, pool.x0(), pool.x1());
            z[i] = Mth.lerp(random.nextFloat(), pool.z0(), pool.z1());
            yaw[i] = (random.nextFloat() - .5f) * 1.2f;
        }
    }

    private void move(Pool pool, List<Piece> pieces, float dt, float t) {
        float cx = (pool.x0() + pool.x1()) / 2 + (pool.x1() - pool.x0()) * .15f * Mth.sin(t * .07f), cz = (pool.z0() + pool.z1()) / 2 + (pool.z1() - pool.z0()) * .15f * Mth.cos(t * .09f);
        float eddy = .18f + .08f * Mth.sin(t * .05f);
        for (int i = 0; i < x.length; i++) {
            var piece = pieces.get(i);
            // The eddy carries the piece round its wandering centre, with a slow sideways wander of its own.
            float fx = -(z[i] - cz) * eddy + .015f * Mth.sin(t * .13f + i * 2.1f), fz = (x[i] - cx) * eddy + .015f * Mth.cos(t * .11f + i * 1.3f);
            for (int j = 0; j < x.length; j++) {
                if (j == i) continue;
                float dx = x[i] - x[j], dz = z[i] - z[j], d = Mth.sqrt(dx * dx + dz * dz) + 1e-4f;
                float reach = .9f * (piece.halfX() + piece.halfZ() + pieces.get(j).halfX() + pieces.get(j).halfZ()) / 2, overlap = reach - d;
                if (overlap > 0) { fx += dx / d * overlap * 3; fz += dz / d * overlap * 3; }
            }
            float drag = Math.min(1, 1.5f * dt);
            vx[i] += (fx - vx[i]) * drag; vz[i] += (fz - vz[i]) * drag;
            spin[i] += (eddy * .6f + .15f * Mth.sin(t * .3f + i) - spin[i]) * Math.min(1, dt);
            yaw[i] += spin[i] * dt; x[i] += vx[i] * dt; z[i] += vz[i] * dt;
            // The walls stop a piece and take its speed into them; its turned outline sets how near it comes.
            float c = Math.abs(Mth.cos(yaw[i])), s = Math.abs(Mth.sin(yaw[i]));
            float hx = c * piece.halfX() + s * piece.halfZ(), hz = s * piece.halfX() + c * piece.halfZ();
            float x0 = pool.x0() + hx, x1 = Math.max(x0, pool.x1() - hx), z0 = pool.z0() + hz, z1 = Math.max(z0, pool.z1() - hz);
            if (x[i] < x0 || x[i] > x1) { x[i] = Mth.clamp(x[i], x0, x1); vx[i] *= -.2f; }
            if (z[i] < z0 || z[i] > z1) { z[i] = Mth.clamp(z[i], z0, z1); vz[i] *= -.2f; }
        }
    }

    int count() { return x.length; }
    float x(int i) { return x[i]; }
    float z(int i) { return z[i]; }
    float yaw(int i) { return yaw[i]; }
    /** The height of piece `i` at `time`, bobbing on the ripples. */
    float y(Pool pool, int i, double time) { return pool.y() + RIDE - i * DEEPER + BOB * Mth.sin((float) (time * 1.1 + i * 1.7)); }
}
