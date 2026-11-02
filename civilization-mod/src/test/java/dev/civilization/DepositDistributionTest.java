package dev.civilization;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DepositDistributionTest {
    @Test void townRadiusRetainsRareFavoredOpportunitiesWithHalfAsManyInTemperate(){
        double area=Math.PI*2048*2048;
        double districtArea=(double)DepositDistribution.REGION*DepositDistribution.REGION;
        assertEquals(Math.PI/2,area/(districtArea*DepositDistribution.FAVORED_RARITY),1e-9);
        assertEquals(Math.PI/4,area/(districtArea*DepositDistribution.TEMPERATE_RARITY),1e-9);
    }
    @Test void climateBoundariesAndModdedTemperatureValuesAreExplicit(){
        assertEquals(DepositDistribution.Climate.COLD,DepositDistribution.climate(-.5f));
        assertEquals(DepositDistribution.Climate.COLD,DepositDistribution.climate(.29999f));
        assertEquals(DepositDistribution.Climate.TEMPERATE,DepositDistribution.climate(.3f));
        assertEquals(DepositDistribution.Climate.TEMPERATE,DepositDistribution.climate(.89999f));
        assertEquals(DepositDistribution.Climate.HOT,DepositDistribution.climate(.9f));
        assertEquals(DepositDistribution.Climate.HOT,DepositDistribution.climate(2));
    }
    @Test void excludedClimatesNeverReceiveSitesAndFavoredFrequencyIsDoubleTemperate(){
        for(boolean coal:new boolean[]{false,true}){
            int favored=0,temperate=0;
            for(int x=-256;x<256;x++)for(int z=-128;z<128;z++){
                long order=DepositDistribution.district(73291,x,z,coal);
                assertFalse(DepositDistribution.allows(order,coal,coal?DepositDistribution.Climate.HOT:DepositDistribution.Climate.COLD));
                boolean yes=DepositDistribution.allows(order,coal,coal?DepositDistribution.Climate.COLD:DepositDistribution.Climate.HOT);
                assertEquals(yes,DepositDistribution.selected(order));
                if(yes)favored++;
                if(DepositDistribution.allows(order,coal,DepositDistribution.Climate.TEMPERATE)){temperate++;assertTrue(yes);}
            }
            assertTrue(favored>15000&&favored<18000,"Favored districts stay near one in eight");
            assertTrue(favored/(double)temperate>1.95&&favored/(double)temperate<2.05,"Climate preference is 2:1 across equal area");
        }
    }
    @Test void districtSearchCanFindSuitableGroundInTheLastCell(){
        for(long order:new long[]{0,1,-1,Long.MIN_VALUE,73291}){
            var visited=new java.util.ArrayList<Integer>();
            Integer result=DepositDistribution.search(order,index->{visited.add(index);return visited.size()==16?index:null;});
            assertEquals(16,new java.util.HashSet<>(visited).size());
            assertEquals(visited.getLast(),result);
            var repeat=new java.util.ArrayList<Integer>();
            assertNull(DepositDistribution.search(order,index->{repeat.add(index);return null;}));
            assertEquals(visited,repeat,"Cache misses cannot change search order");
            assertEquals(visited.getFirst(),DepositDistribution.search(order,index->index),"Stop after the first suitable cell");
        }
    }
    @Test void coalAndOilHaveIndependentStableDistricts(){
        long coal=DepositDistribution.district(-8010344558293493527L,-3,7,true);
        assertEquals(coal,DepositDistribution.district(-8010344558293493527L,-3,7,true));
        assertNotEquals(coal,DepositDistribution.district(-8010344558293493527L,-3,7,false));
        assertNotEquals(coal,DepositDistribution.district(-8010344558293493527L,3,7,true));
    }
}
