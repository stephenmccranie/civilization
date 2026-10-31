package dev.civilization;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DepositDistributionTest {
    @Test void townRadiusHasComparableRareCoalAndOilOpportunities(){
        double area=Math.PI*2048*2048;
        assertEquals(Math.PI/2,area/((double)DepositDistribution.COAL_CELL*DepositDistribution.COAL_CELL*DepositDistribution.COAL_RARITY),1e-9);
        assertEquals(Math.PI/2,area/((double)DepositDistribution.OIL_REGION*DepositDistribution.OIL_REGION*DepositDistribution.OIL_RARITY),1e-9);
    }
}
