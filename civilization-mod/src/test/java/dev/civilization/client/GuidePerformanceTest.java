package dev.civilization.client;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuidePerformanceTest {
    @Test void changesRefreshImmediatelyAndStableSelectionPollsAtTenTicks(){
        var refresh=new GuidePerformance.Refresh();assertTrue(refresh.due("first"));
        for(int i=0;i<9;i++)assertFalse(refresh.due("first"));assertTrue(refresh.due("first"));
        assertTrue(refresh.due("changed"));refresh.reset();assertTrue(refresh.due("changed"));
    }
    @Test void cullThenCapAndKeepAimedPartInsideBudget(){
        var all=IntStream.range(0,200).boxed().toList();
        var result=GuidePerformance.outlines(all,n->n%2==0,n->n,198);
        assertEquals(64,result.size());assertTrue(result.contains(198));assertTrue(result.contains(0));assertTrue(result.stream().allMatch(n->n%2==0));
        assertFalse(GuidePerformance.outlines(all,n->n<4,n->n,198).contains(198));
    }
}
