package dev.civilization.client;

import dev.civilization.StoveCooking;

/** Equal-power texture blend; the frying bed remains audible at every cooking stage. */
final class StoveSoundMix {
    private StoveSoundMix() {}

    /** Preserve the former 16-block linear reach independently of stereo placement. */
    static double distanceGain(double distance) {
        return Math.clamp(1 - distance / 16, 0, 1);
    }

    /** A source one unit forward moves at most .5 sideways (about 27 degrees). */
    static double panOffset(double rightward, double distance) {
        return .5 * Math.clamp(rightward / Math.max(1, distance), -1, 1);
    }

    static double gain(double work, int layer) {
        double scorched = smooth((work - StoveCooking.PLATEAU_END) / 400);
        double golden = .9 * smooth((work - (StoveCooking.OPTIMUM - 200)) / 200) * (1 - scorched);
        double charred = 1.25 * scorched;
        double norm = Math.sqrt(1 + golden * golden + charred * charred);
        return (layer == 0 ? 1 : layer == 1 ? golden : charred) / norm;
    }

    private static double smooth(double value) {
        double t = Math.clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }
}
