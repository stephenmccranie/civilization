package dev.civilization.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StoveSoundMixTest {
    @Test void fryingRemainsAudibleWithoutAnEnergyDipAcrossCooking() {
        for (int work = 0; work <= 1800; work++) {
            double energy = 0;
            assertTrue(StoveSoundMix.gain(work, 0) >= .62, "Frying bed at " + work);
            for (int layer = 0; layer < 3; layer++) {
                double gain = StoveSoundMix.gain(work, layer);
                energy += gain * gain;
                if (work > 0) assertTrue(Math.abs(gain - StoveSoundMix.gain(work - 1, layer)) < .01);
            }
            assertEquals(1, energy, 1e-12);
        }
    }

    @Test void goldenTextureHoldsAcrossThePlateauAndScorchingStartsAfterIt() {
        assertEquals(0, StoveSoundMix.gain(600, 1));
        assertTrue(StoveSoundMix.gain(800, 1) > .65);
        for (int work = 800; work <= 1000; work++) {
            assertEquals(StoveSoundMix.gain(800, 1), StoveSoundMix.gain(work, 1));
            assertEquals(0, StoveSoundMix.gain(work, 2));
        }
        assertEquals(0, StoveSoundMix.gain(1400, 1));
        assertTrue(StoveSoundMix.gain(1400, 2) > StoveSoundMix.gain(1400, 0));
    }
}
