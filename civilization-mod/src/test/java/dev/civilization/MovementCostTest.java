package dev.civilization;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MovementCostTest {
    @Test void gaitRatesStayConstantAcrossMovementStepSizes() {
        assertEquals(10, MovementCost.calories(100, false, 0.1, 3), 1e-9);
        assertEquals(10, 10 * MovementCost.calories(10, false, 0.1, 3), 1e-9);
        assertEquals(30, MovementCost.calories(100, true, 0.1, 3), 1e-9);
        assertEquals(30, 10 * MovementCost.calories(10, true, 0.1, 3), 1e-9);
    }
    @Test void diagonalMovementUsesActualDistance() {
        assertEquals(5, MovementCost.distance(3, 0, 4, false, false));
    }
    @Test void splittingMovementIntoPacketsDoesNotChangeCost() {
        double combined = MovementCost.distance(1, 0, 1, false, false);
        double split = 0;
        for (int i = 0; i < 10; i++) split += MovementCost.distance(0.1, 0, 0.1, false, false);
        assertEquals(combined, split, 1e-9);
    }
    @Test void sprintJumpHorizontalDistanceStillCountsButFreefallDoesNot() {
        assertEquals(1, MovementCost.distance(1, 0.5, 0, false, false));
        assertEquals(0, MovementCost.distance(0, -5, 0, false, false));
    }
    @Test void swimmingAndClimbingIncludeVerticalEffort() {
        assertEquals(5, MovementCost.distance(0, 3, 4, true, false));
        assertEquals(0, MovementCost.distance(0, 3, 0, false, true));
        assertEquals(3, MovementCost.ascent(0, 3, 0));
        assertEquals(0, MovementCost.distance(0, -3, 0, false, true));
    }
    @Test void ascentChargesOnlyGainAndIsIndependentOfPacketSize() {
        assertEquals(1, MovementCost.ascent(1, 1, 0));
        assertEquals(1, 2 * MovementCost.ascent(.5, .5, 0));
        assertEquals(0, MovementCost.ascent(0, -1, 0));
        assertEquals(0, MovementCost.ascent(0, 1000, 0));
        assertEquals(0, MovementCost.ascent(Double.NaN, 1, 0));
    }
    @Test void displacementsAndNonFiniteValuesDoNotBecomeHugeBills() {
        assertEquals(0, MovementCost.distance(1000, 0, 0, false, false));
        assertEquals(0, MovementCost.distance(Double.NaN, 0, 0, false, false));
    }
}
