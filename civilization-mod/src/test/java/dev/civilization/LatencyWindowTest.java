package dev.civilization;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LatencyWindowTest {
    @Test void averagesRawRepliesWithinFiveSeconds(){
        var w=new LatencyWindow(1);w.reply(w.probe(0),20);w.reply(w.probe(1000),1040);
        assertEquals(30,w.reading(1040).milliseconds());assertEquals(40,w.reading(5020).milliseconds());assertEquals("-- ms",w.reading(6040).label());
    }
    @Test void noReplyIsNotZeroPing(){
        var w=new LatencyWindow(1);assertEquals("-- ms",w.reading(0).label());w.probe(0);assertEquals("-- ms",w.reading(100).label());
    }
    @Test void stalledReplyShowsALowerBoundInsteadOfAnOldHealthyAverage(){
        var w=new LatencyWindow(1);w.probe(0);w.reply(w.probe(1000),1020);
        assertEquals(new LatencyWindow.Reading(5000,true),w.reading(5000));assertEquals("\u2265 5000 ms",w.reading(5000).label());
    }
    @Test void duplicateAndForeignRepliesCannotInventMeasurements(){
        var w=new LatencyWindow(1);int id=w.probe(100);assertFalse(w.reply(77,101));assertTrue(w.reply(id,140));assertFalse(w.reply(id,1000));assertEquals(40,w.reading(1000).milliseconds());
    }
    @Test void lateRepliesMeasureTheirActualRoundTrip(){
        var w=new LatencyWindow(1);int id=w.probe(0);assertTrue(w.reply(id,7000));assertEquals(new LatencyWindow.Reading(7000,false),w.reading(7000));assertEquals("-- ms",w.reading(12000).label());
    }
    @Test void lostProbesExpireAndPendingHistoryIsBounded(){
        var w=new LatencyWindow(1);int first=w.probe(0),last=0;for(int i=0;i<1000;i++)last=w.probe(0);
        assertFalse(w.reply(first,1));assertTrue(w.reply(last,5));assertEquals("-- ms",w.reading(30000).label());
    }
    @Test void probeIdsCanWrapAndRealZeroMillisecondsIsValid(){
        var w=new LatencyWindow(Integer.MAX_VALUE);int first=w.probe(0),next=w.probe(0);assertNotEquals(first,next);assertTrue(w.reply(first,0));assertTrue(w.reply(next,0));assertEquals("0 ms",w.reading(0).label());
    }
}
