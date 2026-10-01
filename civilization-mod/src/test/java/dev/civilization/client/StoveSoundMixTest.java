package dev.civilization.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StoveSoundMixTest {
    @Test void soundStillFadesWithDistanceAndStopsAtItsRange() {
        assertEquals(1, StoveSoundMix.distanceGain(0));
        assertEquals(.5, StoveSoundMix.distanceGain(8));
        assertEquals(0, StoveSoundMix.distanceGain(16));
        assertEquals(0, StoveSoundMix.distanceGain(32));
        for (int distance=1;distance<=16;distance++)
            assertTrue(StoveSoundMix.distanceGain(distance)<StoveSoundMix.distanceGain(distance-1));
    }

    @Test void stereoKeepsLeftAndRightButCapsItsAngleEvenBesideThePan() {
        assertEquals(0, StoveSoundMix.panOffset(0, 0));
        assertEquals(.18, StoveSoundMix.panOffset(8, 8));
        assertEquals(-.18, StoveSoundMix.panOffset(-8, 8));
        assertEquals(.018, StoveSoundMix.panOffset(.1, .1), 1e-12);
        for (int degrees=-180;degrees<=180;degrees++) {
            double offset=StoveSoundMix.panOffset(4*Math.sin(Math.toRadians(degrees)),4);
            assertTrue(Math.abs(Math.toDegrees(Math.atan(offset)))<11);
            assertEquals(-offset,StoveSoundMix.panOffset(-4*Math.sin(Math.toRadians(degrees)),4),1e-12);
        }
    }

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
