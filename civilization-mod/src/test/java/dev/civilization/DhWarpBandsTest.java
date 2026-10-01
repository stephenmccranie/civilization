package dev.civilization;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DhWarpBandsTest {
    @Test void onlyTwoFixedBands(){
        var b=new DhWarpBands();b.begin(true);
        for(int d:new int[]{0,64,128,256,511,512})assertEquals(2,b.detail(d,0,0,d));
        for(int d:new int[]{513,1024,4096,20000})assertEquals(6,b.detail(d,0,0,d));
    }
    @Test void boundaryHistorySurvivesTraversalAndTurns(){
        var b=new DhWarpBands();b.begin(true);assertEquals(2,b.detail(1,1,0,500));
        b.begin(true);assertEquals(2,b.detail(1,1,0,540));
        b.begin(true);assertEquals(6,b.detail(1,1,0,545));
        b.begin(true);assertEquals(6,b.detail(1,1,0,490));
        b.begin(true);assertEquals(2,b.detail(1,1,0,480));
        b.begin(false);assertEquals(6,b.detail(1,1,0,520));
    }
    @Test void parentIntersectionAndEvictedHistory(){
        var b=new DhWarpBands();b.begin(true);assertEquals(2,b.detail(1,0,256,700));
        assertEquals(6,b.detail(1,0,0,700));
        b.begin(true);b.begin(true);assertEquals(6,b.detail(1,0,256,790));
    }
}
