package dev.civilization.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuideFocusTest {
    @Test void edgeMissesDoNotFlickerButLookingAwayReleases(){
        var focus=new GuideFocus();
        assertFalse(focus.update(false,0));
        assertTrue(focus.update(true,10));
        assertTrue(focus.update(false,60));
        assertTrue(focus.update(true,90));
        assertTrue(focus.update(false,239));
        assertFalse(focus.update(false,240));
        assertFalse(focus.update(false,400));
    }
    @Test void completionAndContextChangesCancelImmediately(){
        var focus=new GuideFocus();focus.update(true,100);
        focus.reset();assertFalse(focus.visible());assertFalse(focus.update(false,101));
        assertTrue(focus.update(true,102));
    }
    @Test void repeatedRenderQueriesDoNotExtendTheReleaseDelay(){
        var focus=new GuideFocus();focus.update(true,100);
        for(int now=101;now<250;now++)assertTrue(focus.update(false,now));
        assertFalse(focus.update(false,250));
    }
}
