package dev.civilization.client;

import dev.civilization.StoveCooking;

/** Equal-power texture blend; the frying bed remains audible at every cooking stage. */
final class StoveSoundMix {
    private StoveSoundMix() {}

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
