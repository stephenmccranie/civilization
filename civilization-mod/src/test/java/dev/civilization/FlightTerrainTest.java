package dev.civilization;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightTerrainTest {
    @Test void narrowPreparationReachesFartherWithoutIncreasingTickets(){
        var old=FlightTerrain.capture(FlightTerrain.corridor(.5,.5,1,0,4096),k->true);
        var cells=FlightTerrain.predictedCorridor(.5,.5,5000,0,4096,1,0,0,0,1000);
        var prepared=FlightTerrain.capture(cells,k->true);
        assertTrue(cells.size()<=FlightTerrain.MAX_CHUNKS);
        assertTrue(prepared.distance(.5,.5,1,0,4096)>old.distance(.5,.5,1,0,4096)+800);
        assertTrue(prepared.sweep(.5,.5,2500,0));
    }
    @Test void turnForecastPreservesMomentumCoverageAndPreparesBothTurnDirections(){
        for(int turn:new int[]{-1,1}){
            var cells=FlightTerrain.predictedCorridor(.5,.5,1000,0,1000,1,0,0,turn,1500);
            var prepared=FlightTerrain.capture(cells,k->true);
            assertTrue(cells.size()<=FlightTerrain.MAX_CHUNKS);
            assertTrue(prepared.sweep(.5,.5,800,0),"Current momentum must remain covered");
            assertTrue(cells.longStream().anyMatch(k->FlightTerrain.x(k)>20&&turn*FlightTerrain.z(k)>6),"Turn branch missing");
            assertFalse(FlightTerrain.capture(cells,k->false).sweep(.5,.5,1,0),"Prediction is not readiness");
        }
    }
    @Test void predictedCoverageIsBoundedForDiagonalsAndInvalidPositions(){
        assertTrue(FlightTerrain.predictedCorridor(Double.NaN,0,5000,0,4000,1,0,0,0,1).isEmpty());
        for(int heading=0;heading<16;heading++){
            double a=heading*Math.PI/8,dx=Math.cos(a),dz=Math.sin(a);
            var cells=FlightTerrain.predictedCorridor(-.5,-.5,dx*5000,dz*5000,4096,dx,dz,.5,1,1000);
            assertTrue(cells.size()<=1024);
            assertTrue(FlightTerrain.capture(cells,k->true).sweep(-.5,-.5,dx*500,dz*500));
        }
    }
    @Test void immutableCoverageHandlesNegativeCoordinatesAndMissingCells(){
        var cells=FlightTerrain.corridor(-200,-300,1,0,600);
        var snapshot=FlightTerrain.capture(cells,key->true);cells.clear();
        assertThrows(UnsupportedOperationException.class,()->snapshot.loaded().clear());
        assertThrows(UnsupportedOperationException.class,()->snapshot.checked().add(FlightTerrain.key(500,500)));
        assertTrue(snapshot.sweep(-200,-300,400,0));
        long gap=FlightTerrain.key(4,-19);
        var missing=new HashSet<>(snapshot.loaded());missing.remove(gap);
        var broken=new FlightTerrain.Snapshot(snapshot.checked(),missing);
        assertFalse(broken.sweep(-200,-300,400,0));
        missing.add(gap);assertFalse(broken.sweep(-200,-300,400,0),"Publication must not change after construction");
        assertFalse(snapshot.sweep(-200,-300,1e200,0));
        assertFalse(snapshot.sweep(Double.NaN,0,0,0));
    }
    @Test void corridorsStayBoundedAndCoverDiagonalSweeps(){
        for(int heading=0;heading<16;heading++){
            double a=heading*Math.PI/8,dx=Math.cos(a),dz=Math.sin(a);
            var cells=FlightTerrain.corridor(-.5,-.5,dx,dz,Double.MAX_VALUE);
            assertTrue(cells.size()<=FlightTerrain.MAX_CHUNKS);
            var snapshot=FlightTerrain.capture(cells,key->true);
            assertTrue(snapshot.sweep(-.5,-.5,dx*500,dz*500),"Heading "+heading);
            assertFalse(snapshot.sweep(-.5,-.5,-dx*500,-dz*500));
        }
    }
    @Test void brakingStopsBeforeMissingTerrainWithoutAHighSpeedDiscontinuity(){
        var cells=new HashSet<Long>();for(int x=-8;x<100;x++)for(int z=-8;z<=8;z++)cells.add(FlightTerrain.key(x,z));
        var snapshot=FlightTerrain.capture(cells,key->true);
        double x=0,speed=1000,maxDrop=0,dt=.025;
        for(int i=0;i<400;i++){
            double distance=snapshot.distance(x,0,1,0,FlightTerrain.MAX_DISTANCE);
            double next=FlightTerrain.permittedSpeed(speed,1000,distance,dt);
            assertTrue(snapshot.sweep(x,0,next*.05,0),"Brake must preserve the next full tick's clearance");
            maxDrop=Math.max(maxDrop,speed-next);x+=next*dt;speed=next;
        }
        assertTrue(x<1576&&x>1400);assertTrue(speed<1);assertTrue(maxDrop<100,"No 1000-to-zero step");
    }
    @Test void readyDistanceScalesSpeedWithoutAConfiguredCruisingCeiling(){
        assertTrue(FlightTerrain.permittedSpeed(5000,1e200,2400,.025)>5000);
        assertTrue(FlightTerrain.permittedSpeed(1000,2000,500,.025)>1000);
        assertTrue(FlightTerrain.permittedSpeed(1000,2000,100,.025)<1000);
        assertEquals(0,FlightTerrain.permittedSpeed(1000,2000,0,.025));
        assertEquals(0,FlightTerrain.permittedSpeed(1000,Double.NaN,100,.025));
    }
}
