package dev.civilization;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DepositDistributionTest {
    @Test void townRadiusHasComparableRareOpportunitiesForEachResource(){
        double area=Math.PI*2048*2048;
        assertEquals(Math.PI/2,area/((double)DepositDistribution.COAL_CELL*DepositDistribution.COAL_CELL*DepositDistribution.COAL_RARITY),1e-9);
        assertEquals(Math.PI/2,area/((double)DepositDistribution.OIL_REGION*DepositDistribution.OIL_REGION*DepositDistribution.OIL_RARITY),1e-9);
        assertEquals(Math.PI/2,area/((double)DepositDistribution.URANIUM_DISTRICT*DepositDistribution.URANIUM_DISTRICT*DepositDistribution.URANIUM_RARITY),1e-9);
    }
    @Test void uraniumDistrictsAreStableAcrossChunksAndNegativeCoordinates(){
        boolean selected=false,rejected=false,changedSeed=false;
        for(long seed=0;seed<128;seed++){
            boolean a=DepositDistribution.uranium(seed,-1,-1);
            assertEquals(a,DepositDistribution.uranium(seed,-512,-512));
            assertEquals(a,DepositDistribution.uranium(seed,-200,-400));
            selected|=a;rejected|=!a;
            changedSeed|=a!=DepositDistribution.uranium(seed+1,-1,-1);
        }
        assertTrue(selected&&rejected&&changedSeed,"District selection is rare, deterministic and seed-dependent");
    }
}
