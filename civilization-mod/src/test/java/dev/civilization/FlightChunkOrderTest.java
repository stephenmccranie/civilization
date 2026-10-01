package dev.civilization;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FlightChunkOrderTest {
    @Test void completeSquareWithoutDuplicatesInEveryDirection(){
        for(int radius:new int[]{0,1,2,8,64})for(int heading=0;heading<16;heading++){
            var order=FlightChunkOrder.create(radius,heading);var seen=new HashSet<Long>();long count=(2L*radius+1)*(2L*radius+1);
            while(order.hasNext()){
                assertEquals(count-seen.size(),order.remaining());long p=order.next();
                assertTrue(Math.abs(FlightChunkOrder.x(p))<=radius);assertTrue(Math.abs(FlightChunkOrder.z(p))<=radius);assertTrue(seen.add(p));
            }
            assertEquals(count,seen.size());assertEquals(0,order.remaining());assertThrows(NoSuchElementException.class,order::next);
        }
    }
    @Test void nearbySafetyThenForwardCorridorAndReverse(){
        for(int heading:new int[]{0,8}){
            var order=FlightChunkOrder.create(8,heading);var positions=new ArrayList<Long>();
            while(order.hasNext())positions.add(order.next());
            for(int i=0;i<25;i++){long p=positions.get(i);assertTrue(Math.max(Math.abs(FlightChunkOrder.x(p)),Math.abs(FlightChunkOrder.z(p)))<=2);}
            int front=heading==0?8:-8;
            assertTrue(positions.indexOf(FlightChunkOrder.pack(front,0))<positions.indexOf(FlightChunkOrder.pack(0,3)));
            assertTrue(positions.indexOf(FlightChunkOrder.pack(front,0))<positions.indexOf(FlightChunkOrder.pack(-front,0)));
        }
    }
    @Test void boundsAndIndependentCachedCursors(){
        assertNull(FlightChunkOrder.create(65,0));assertNull(FlightChunkOrder.create(8,-1));
        var a=FlightChunkOrder.create(8,0);var b=FlightChunkOrder.create(8,0);a.next();assertEquals(289,b.remaining());
        assertEquals(-1,FlightChunkOrder.heading(79,0));assertEquals(0,FlightChunkOrder.heading(80,0));assertEquals(8,FlightChunkOrder.heading(-80,0));assertEquals(-1,FlightChunkOrder.heading(Double.NaN,0));
    }
    @Test void worldIsolationSnapshotCopyAndCleanup(){
        Object a=new Object(),b=new Object();var hints=new ArrayList<FlightChunkHints.Hint>();hints.add(new FlightChunkHints.Hint(5,6,0));
        FlightChunkHints.publish(Map.of(a,hints));hints.clear();
        assertEquals(0,FlightChunkHints.heading(a,FlightChunkOrder.pack(5,6)));
        assertEquals(-1,FlightChunkHints.heading(b,FlightChunkOrder.pack(5,6)));
        assertEquals(-1,FlightChunkHints.heading(a,FlightChunkOrder.pack(15,6)));
        FlightChunkHints.clear();assertEquals(-1,FlightChunkHints.heading(a,FlightChunkOrder.pack(5,6)));
    }
}
