package dev.civilization;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DailyTemperatureCycleTest {
    @Test void hottestInMidAfternoonAndColdestAtDawn(){
        assertEquals(-1,DailyTemperatureCycle.phase(0),1e-12);
        assertEquals(1,DailyTemperatureCycle.phase(9000),1e-12);
        assertTrue(DailyTemperatureCycle.phase(9000)>DailyTemperatureCycle.phase(6000));
        assertTrue(DailyTemperatureCycle.phase(18000)>DailyTemperatureCycle.phase(0));
    }
    @Test void eveningRetainsDaytimeWarmth(){
        assertTrue(DailyTemperatureCycle.phase(12000)>.8,"Sunset retains most of the peak-to-mean warmth");
        assertTrue(DailyTemperatureCycle.phase(12000)>DailyTemperatureCycle.phase(6000),"Sunset is warmer than noon");
        assertTrue(DailyTemperatureCycle.phase(15000)>0,"Even 21:00 remains above the daily mean");
    }
    @Test void fullDayHasOneSmoothWarmupAndOneGradualCooldown(){
        double last=DailyTemperatureCycle.phase(0);
        for(int time=1;time<=24000;time++){
            double value=DailyTemperatureCycle.phase(time);
            assertTrue(Double.isFinite(value)&&value>=-1&&value<=1);
            assertTrue(Math.abs(value-last)<.00036,"No step change at the peak or dawn wrap");
            if(time<=9000)assertTrue(value>=last);else assertTrue(value<=last);
            last=value;
        }
    }
    @Test void dailyMeanAndAmplitudeArePreserved(){
        double sum=0,min=1,max=-1;
        for(int time=0;time<24000;time++){double value=DailyTemperatureCycle.phase(time);sum+=value;min=Math.min(min,value);max=Math.max(max,value);}
        assertEquals(0,sum/24000,1e-12);assertEquals(-1,min);assertEquals(1,max);
    }
    @Test void wrapsAcrossWorldDaysAndNegativeTime(){
        for(long time:new long[]{0,1,6000,9000,12000,18000,23999}){
            assertEquals(DailyTemperatureCycle.phase(time),DailyTemperatureCycle.phase(time+24000));
            assertEquals(DailyTemperatureCycle.phase(time),DailyTemperatureCycle.phase(time-24000));
        }
        assertTrue(Double.isFinite(DailyTemperatureCycle.phase(Long.MIN_VALUE)));
        assertTrue(Double.isFinite(DailyTemperatureCycle.phase(Long.MAX_VALUE)));
    }
}
