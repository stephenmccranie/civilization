package dev.civilization;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GrassWearTest {
    @Test void negativeChunksAndTallSurfacesKeepDistinctCells(){
        assertEquals(GrassWear.key(-1,4096,-1),GrassWear.key(15,4096,15));
        assertNotEquals(GrassWear.key(1,0,2),GrassWear.key(1,4096,2));
        for(int y:new int[]{-4096,-1,0,2048,8192})assertEquals(y,GrassWear.y(GrassWear.key(-20,y,31)));
    }
    @Test void heightChangesOnlyAtThresholdsAndStopsAtShortGrass(){
        var wear=new GrassWear();long key=GrassWear.key(3,80,7);
        for(int n=1;n<=32;n++){assertTrue(wear.step(key));assertEquals(n/4,GrassWear.stage(wear.steps(key)));}
        assertFalse(wear.step(key));assertEquals(32,wear.steps(key));
        assertEquals(1,GrassWear.heightScale(0));assertEquals(.2f,GrassWear.heightScale(8),.0001);
        float last=1;for(int stage=0;stage<=8;stage++){float scale=GrassWear.heightScale(stage);assertTrue(scale<=last);last=scale;}
    }
    @Test void saveReloadRetainsPartialCountsAndPermanentWear(){
        var wear=new GrassWear();long a=GrassWear.key(-1,-40,-2),b=GrassWear.key(3,4080,4);
        for(int n=0;n<3;n++)wear.step(a);for(int n=0;n<32;n++)wear.step(b);
        var copy=new GrassWear();copy.load(wear.save());assertArrayEquals(wear.save(),copy.save());
        assertEquals(3,copy.steps(a));assertEquals(1,copy.visible().length);
        for(int n=0;n<100;n++){var next=new GrassWear();next.load(copy.save());copy=next;}
        assertEquals(32,copy.steps(b));assertEquals(3,copy.steps(a)); // No clock-based decay or regeneration path.
    }
    @Test void storageIsBoundedAndRejectsInvalidSavedCounts(){
        var wear=new GrassWear();for(int i=0;i<GrassWear.MAX_CELLS;i++)assertTrue(wear.step(GrassWear.key(i&15,i>>8,(i>>4)&15)));
        assertEquals(GrassWear.MAX_CELLS,wear.size());assertFalse(wear.step(GrassWear.key(0,100000,0)));
        wear.load(new long[]{(GrassWear.key(1,80,1)<<7)|64});assertEquals(32,wear.steps(GrassWear.key(1,80,1)));
        wear.load(new long[]{0,127,Long.MAX_VALUE});assertEquals(0,wear.size());
    }
}
