package dev.civilization;

/**
 * The one cloud field: cumulus cover over block x/z, drifting with the weather clock. The server shades players with
 * it, and {@code assets/civilization/shaders/cloud_cover.glsl}, its exact mirror in float arithmetic, is what Photon
 * draws. Change both together.
 */
public final class CloudCover {
    /** The field repeats every this many blocks, so shader positions stay small enough for floats. */
    public static final int PERIOD = 65536;
    /** Drift in blocks per weather tick, Photon's default cumulus wind at the 60-minute day. */
    public static final double WIND_X = .036, WIND_Z = .0208;
    /** Photon's cumulus shadow layer at the pack's 2400 m cloud altitude: Y 63 + 1.25 × 2400 / 10. */
    public static final double SHADOW_Y = 363;
    public static final int DISTRICT = RegionalWeather.SIZE;
    private static final int[] SIZES = {512, 256, 128};
    private static final float[] WEIGHTS = {.5f, .3f, .2f};

    private CloudCover() {}

    /** Noise position of a block coordinate at a weather clock, wrapped to the period. */
    public static float shift(double block, double wind, double clock) {
        return (float) (((block + wind * clock) % PERIOD + PERIOD) % PERIOD);
    }

    /** Cover from 0 (clear sky) to 1 (cloud) at a noise position, where the district cloudiness is {@code cloudiness}. */
    public static float cover(float x, float z, float cloudiness) {
        float noise = 0;
        for (int octave = 0; octave < SIZES.length; octave++) noise += WEIGHTS[octave] * value(x, z, SIZES[octave], octave);
        float edge = 1.05f - 1.1f * cloudiness;
        float t = Math.clamp((noise - (edge - .06f)) / .12f, 0, 1);
        return t * t * (3 - 2 * t);
    }

    /**
     * Cloudiness at a block offset from the west and north corner of a three by three district grid, blended between
     * district centres and held at the outer centres beyond them.
     */
    public static float cloudiness(float[] grid, float x, float z) {
        float u = Math.clamp((x - DISTRICT / 2f) / DISTRICT, 0, 2), v = Math.clamp((z - DISTRICT / 2f) / DISTRICT, 0, 2);
        int i = (int) Math.min(Math.floor(u), 1), j = (int) Math.min(Math.floor(v), 1);
        float fu = u - i, fv = v - j;
        float north = grid[j * 3 + i] * (1 - fu) + grid[j * 3 + i + 1] * fu;
        float south = grid[(j + 1) * 3 + i] * (1 - fu) + grid[(j + 1) * 3 + i + 1] * fu;
        return north * (1 - fv) + south * fv;
    }

    private static float value(float x, float z, int size, int octave) {
        float qx = x / size, qz = z / size;
        float ix = (float) Math.floor(qx), iz = (float) Math.floor(qz);
        float fx = qx - ix, fz = qz - iz;
        fx = fx * fx * (3 - 2 * fx);
        fz = fz * fz * (3 - 2 * fz);
        int cells = PERIOD / size;
        // Offset by whole periods first: the shader's integer remainder is undefined for negative numbers.
        int cx = ((int) ix + 4 * cells) % cells, cz = ((int) iz + 4 * cells) % cells;
        int ex = (cx + 1) % cells, ez = (cz + 1) % cells;
        float north = lattice(cx, cz, octave) * (1 - fx) + lattice(ex, cz, octave) * fx;
        float south = lattice(cx, ez, octave) * (1 - fx) + lattice(ex, ez, octave) * fx;
        return north * (1 - fz) + south * fz;
    }

    private static float lattice(int x, int z, int octave) {
        return (hash(x * 0x8da6b343 ^ z * 0xd8163841 ^ octave * 0xcb1ab31f) >>> 8) / 16777216f;
    }

    /** Photon's lowbias32 integer hash, exact in both languages. */
    static int hash(int x) {
        x ^= x >>> 16;
        x *= 0x7feb352d;
        x ^= x >>> 15;
        x *= 0x846ca68b;
        x ^= x >>> 16;
        return x;
    }
}
