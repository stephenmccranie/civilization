package dev.civilization;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CloudCoverTest {
    private static float share(float cloudiness){
        int covered=0,n=0;
        for(int x=0;x<8192;x+=37)for(int z=0;z<8192;z+=41){n++;if(CloudCover.shade(CloudCover.density(x,z,cloudiness))>.5f)covered++;}
        return covered/(float)n;
    }
    @Test void cloudinessSetsHowMuchSkyIsCovered(){
        assertTrue(share(0)<.01,"A clear district has next to no cloud: "+share(0));
        assertTrue(share(1)>.99,"An overcast district is covered: "+share(1));
        float half=share(.5f);
        assertTrue(half>.3&&half<.7,"Half cloudiness covers about half the sky: "+half);
        assertTrue(share(.3f)<half&&half<share(.7f),"More cloudiness, more cover");
    }
    @Test void theFieldRepeatsAndIsTheSameEverywhereItIsEvaluated(){
        // Whole blocks, so a position and the same one a period on are both exact in float.
        for(int x=-3000;x<3000;x+=413){
            int z=x*7/10;
            assertEquals(CloudCover.density(x+CloudCover.PERIOD,z,.5f),CloudCover.density(x,z,.5f),"Periodic in x at "+x);
            assertEquals(CloudCover.density(x,z+CloudCover.PERIOD,.5f),CloudCover.density(x,z,.5f),"Periodic in z at "+x);
        }
        assertEquals(CloudCover.shift(-10,CloudCover.WIND_X,0),CloudCover.PERIOD-10,1e-3,"Negative coordinates wrap into the period");
        assertEquals(CloudCover.shift(100,CloudCover.WIND_X,1000),100+CloudCover.WIND_X*1000,1e-3,"The field drifts with the weather clock");
    }
    @Test void districtCloudinessBlendsBetweenCentres(){
        var grid=new float[]{0,0,0, 0,1,0, 0,0,0};
        assertEquals(1,CloudCover.cloudiness(grid,768,768),1e-6,"The centre district's own value at its centre");
        assertEquals(.5,CloudCover.cloudiness(grid,1024,768),1e-6,"Halfway to the eastern centre");
        assertEquals(0,CloudCover.cloudiness(grid,5000,-5000),1e-6,"Held at the outer centres beyond the grid");
    }
    @Test void hashIsPhotonsLowbias32(){
        assertEquals(0,CloudCover.hash(0));
        assertEquals(0x688990c0,CloudCover.hash(1),"Same value as the GLSL hash for 1u");
    }
}
