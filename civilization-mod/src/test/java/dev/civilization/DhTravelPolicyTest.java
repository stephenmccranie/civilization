package dev.civilization;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DhTravelPolicyTest {
    private static void run(DhTravelPolicy p,double speed,double seconds){for(int i=0;i<(int)(seconds*20);i++)p.update(speed,.05);}
    @Test void oneThresholdAndDelayedRecovery(){var p=new DhTravelPolicy();run(p,79,5);assertEquals(0,p.tier());run(p,81,1);assertEquals(1,p.tier());assertEquals(16,p.view().multiplier());run(p,0,.5);assertEquals(1,p.tier());run(p,0,1);assertEquals(0,p.tier());}
    @Test void thresholdOscillationDoesNotFlap(){var p=new DhTravelPolicy();run(p,100,1);assertEquals(1,p.tier());for(int i=0;i<300;i++)p.update(i%2==0?75:85,.05);assertEquals(1,p.tier());}
    @Test void discontinuityResetsAndExtremeSpeedStaysFinite(){var p=new DhTravelPolicy();run(p,1e100,.5);assertEquals(1,p.tier());assertEquals(16,p.view().multiplier());p.update(100,2);assertEquals(0,p.tier());p.update(Double.NaN,.05);assertEquals(0,p.tier());}
}
