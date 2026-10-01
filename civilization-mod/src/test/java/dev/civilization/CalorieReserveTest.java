package dev.civilization;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CalorieReserveTest {
    @Test void foodGoesIntoOneCappedReserve() {
        var reserve = new CalorieReserve(2400, 2000);
        assertEquals(400, reserve.eat(700));
        assertEquals(2400, reserve.calories());
        assertFalse(reserve.needsFood());
        reserve.spend(0.1);
        assertTrue(reserve.needsFood());
    }
    @Test void workDirectlySpendsCaloriesWithoutSaturationOrDebt() {
        var reserve = new CalorieReserve(2400, 2400);
        for (int i = 0; i < 20; i++) { reserve.spend(4); reserve.spend(2); }
        assertEquals(2280, reserve.calories());
        assertEquals(2280, reserve.spend(3000));
        assertEquals(0, reserve.calories());
    }
    @Test void repeatedFractionalChargesKeepPrecision() {
        var reserve = new CalorieReserve(2400, 2400);
        for (int i = 0; i < 10000; i++) reserve.spend(0.01);
        assertEquals(2300, reserve.calories(), 1e-7);
    }
    @Test void restoringOrResizingDoesNotRefill() {
        var reserve = new CalorieReserve(2400, 312.345);
        var restored = new CalorieReserve(2400, reserve.calories());
        restored.resize(3000);
        assertEquals(312.345, restored.calories());
        restored.resize(200);
        assertEquals(200, restored.calories());
    }
    @Test void malformedValuesCannotProduceUnlimitedEnergy() {
        assertEquals(0, new CalorieReserve(2400, Double.NaN).calories());
        assertThrows(IllegalArgumentException.class, () -> new CalorieReserve(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new CalorieReserve(2400, 0).eat(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new CalorieReserve(2400, 50).spend(-10));
    }
    @Test void healingRequiresEntireCost() {
        var reserve = new CalorieReserve(2400, 39);
        assertFalse(reserve.canAfford(40));
        reserve.eat(1);
        assertTrue(reserve.canAfford(40));
    }
}
