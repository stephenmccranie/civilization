package dev.civilization;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecoveryRulesTest {
    @Test void zeroEntersDepletion() {
        assertTrue(RecoveryRules.depleted(false, 0, 200));
        assertFalse(RecoveryRules.depleted(false, 100, 200));
    }
    @Test void morselsCannotInstantlyClearThePenalty() {
        for (int kcal = 0; kcal < 200; kcal += 25) assertTrue(RecoveryRules.depleted(true, kcal, 200));
        assertFalse(RecoveryRules.depleted(true, 200, 200));
    }
    @Test void starvationNeverCrossesHealthFloor() {
        assertEquals(1, RecoveryRules.starvationDamage(20, 6));
        assertEquals(0.5f, RecoveryRules.starvationDamage(6.5f, 6));
        assertEquals(0, RecoveryRules.starvationDamage(6, 6));
        assertEquals(0, RecoveryRules.starvationDamage(2, 6));
    }
}
