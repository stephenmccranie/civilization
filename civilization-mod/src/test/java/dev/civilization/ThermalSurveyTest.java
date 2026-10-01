package dev.civilization;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ThermalSurveyTest {
    @Test void absoluteColorsDoNotShiftWhenAHotSourceAppears() {
        double comfortable = (70 - 32) / 1.8;
        double hot = (130 - 32) / 1.8;
        assertEquals(0xffd7bb7b, ThermalDisplay.color(comfortable));
        assertNotEquals(ThermalDisplay.color(comfortable), ThermalDisplay.color(hot));
        assertEquals(3.0 / 8, ThermalDisplay.legendPosition(comfortable), 1e-9);
        assertEquals(130, ThermalDisplay.legendFahrenheit(ThermalDisplay.legendPosition(hot)), 1e-9);
        assertEquals(ThermalDisplay.color(1000), ThermalDisplay.color(200));
    }

}
