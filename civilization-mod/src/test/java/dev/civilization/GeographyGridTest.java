package dev.civilization;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeographyGridTest {
    @Test void circularBandAndOverlappingWoodland() {
        var grid = new GeographyGrid((x, z) -> new GeographyGrid.Region(x == 0 && z == 0, x <= 0), 48);
        assertTrue(grid.column(48, 0).riverBand());
        assertFalse(grid.column(52, 0).riverBand());
        assertFalse(grid.column(48, 48).riverBand());
        assertTrue(grid.column(0, 0).woodland());
        assertTrue(grid.column(0, 0).riverBand());
    }
    @Test void negativeCoordinatesAndCellConsistency() {
        var grid = new GeographyGrid((x, z) -> new GeographyGrid.Region(x == -1 && z == -1, false), 0);
        assertTrue(grid.column(-1, -1).riverBand());
        assertTrue(grid.column(-4, -4).riverBand());
        assertFalse(grid.column(0, 0).riverBand());
        assertFalse(grid.column(-5, -5).riverBand());
    }
    @Test void verticalBoundaryIncludesTerracesButNotSkyOrDeepCaves() {
        assertTrue(GeographyGrid.heightEligible(58, 62, 4, 8));
        assertTrue(GeographyGrid.heightEligible(70, 62, 4, 8));
        assertFalse(GeographyGrid.heightEligible(57, 62, 4, 8));
        assertFalse(GeographyGrid.heightEligible(71, 62, 4, 8));
        assertFalse(GeographyGrid.heightEligible(200, 62, 4, 8));
    }
    @Test void repeatedAndWoodlandQueriesAvoidAreaResampling() {
        int[] samples = {0};
        var grid = new GeographyGrid((x, z) -> { samples[0]++; return new GeographyGrid.Region(false, true); }, 48);
        assertTrue(grid.woodland(0, 0));
        assertEquals(1, samples[0]);
        var column = grid.column(0, 0);
        int first = samples[0];
        assertTrue(first <= 625);
        assertSame(column, grid.column(3, 3));
        assertEquals(first, samples[0]);
        grid.column(4, 0);
        assertTrue(samples[0] - first <= 25, "Neighboring queries should reuse the sampled interior");
    }
    @Test void evictionIsBoundedAndDoesNotChangeGeography() {
        GeographyGrid.Sampler sampler = (x, z) -> new GeographyGrid.Region(x % 3 == 0, z % 2 == 0);
        var grid = new GeographyGrid(sampler, 8, 32, 4);
        var initial = grid.column(-300, 200);
        for (int x = 0; x < 200; x++) grid.column(x * 100, 0);
        assertTrue(grid.regionCount() <= 32);
        assertTrue(grid.columnCount() <= 4);
        assertEquals(initial, grid.column(-300, 200));
        assertEquals(initial, new GeographyGrid(sampler, 8).column(-300, 200));
    }
}
