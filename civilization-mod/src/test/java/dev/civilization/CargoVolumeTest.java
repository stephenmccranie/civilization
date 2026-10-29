package dev.civilization;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CargoVolumeTest {
    @Test void cuttingAndRecombiningPreserveVolume() {
        var full = CargoVolume.pieces(1, 8);
        assertEquals(full, CargoVolume.pieces(2, 4));
        assertEquals(full, CargoVolume.pieces(4, 2));
        assertEquals(full, CargoVolume.pieces(8, 1));
        assertEquals(full, CargoVolume.pieces(1, 4).plus(CargoVolume.pieces(2, 2)));
        assertEquals(1, full.cubicMeters());
    }

    @Test void partialCapacityNeverRoundsUpOrOverdraws() {
        var occupied = CargoVolume.pieces(3, 1);
        var capacity = CargoVolume.pieces(1, 8);
        assertEquals(1, occupied.fittingPieces(capacity, 4, 20));
        assertEquals(2, occupied.fittingPieces(capacity, 2, 20));
        assertEquals(5, occupied.fittingPieces(capacity, 1, 20));
        assertEquals(0, occupied.fittingPieces(capacity, 8, 20));
        assertEquals(1, occupied.fittingPieces(capacity, 1, 1));
        assertEquals(0, capacity.fittingPieces(capacity, 1, 1));
        assertEquals(0, new CargoVolume(9).fittingPieces(capacity, 1, 1));
        assertEquals(0, occupied.fittingPieces(capacity, 1, 0));
    }

    @Test void largeStocksRemainExact() {
        assertEquals(8_000_000, CargoVolume.pieces(1_000_000, 8).eighths());
        assertEquals(125_000, CargoVolume.pieces(1_000_000, 1).cubicMeters());
    }

    @Test void invalidOrOverflowingQuantitiesFailInsteadOfWrapping() {
        assertThrows(IllegalArgumentException.class, () -> new CargoVolume(-1));
        assertThrows(IllegalArgumentException.class, () -> CargoVolume.pieces(-1, 8));
        assertThrows(IllegalArgumentException.class, () -> CargoVolume.pieces(1, 3));
        assertThrows(ArithmeticException.class, () -> CargoVolume.pieces(Long.MAX_VALUE, 8));
        assertThrows(ArithmeticException.class, () -> new CargoVolume(Long.MAX_VALUE).plus(new CargoVolume(1)));
        assertThrows(IllegalArgumentException.class, () -> CargoVolume.ZERO.fittingPieces(new CargoVolume(8), 0, 1));
    }
}
