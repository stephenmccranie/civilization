package dev.civilization;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CoalChippingTest {
    @Test void everyFaceCanBeWorkedThroughWithoutRepeatingStock(){
        for(int axis=0;axis<3;axis++)for(int sign:new int[]{-1,1}){
            long mask=-1L;int paid=0,u=axis==0?1:0,v=axis==2?1:2;
            for(int depth=0;depth<4;depth++)for(int a=0;a<2;a++)for(int b=0;b<2;b++){
                double[] p=new double[3];p[axis]=sign>0?1-depth*.25:depth*.25;p[u]=a*.5+.1;p[v]=b*.5+.1;
                long taken=CoalChipping.chip(mask,p[0],p[1],p[2],axis,sign);assertEquals(4,Long.bitCount(taken));assertEquals(0,taken&~mask);mask&=~taken;paid+=Long.bitCount(taken);
                assertEquals(0,CoalChipping.chip(mask,p[0],p[1],p[2],axis,sign),"Stale contact cannot mine the next layer");
            }
            assertEquals(0,mask);assertEquals(64,paid);
        }
    }
    @Test void partialPatchNeverGrantsAbsentCells(){long first=CoalChipping.chip(-1L,.1,.1,0,2,-1);long sparse=-1L&~(first&-first);assertEquals(3,Long.bitCount(CoalChipping.chip(sparse,.1,.1,0,2,-1)));assertEquals(0,CoalChipping.chip(0,.1,.1,0,2,-1));}
}
