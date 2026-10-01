package dev.civilization;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AirshipFlightTest {
    @Test void thrustBuildsGraduallyAndReversalMustPassThroughNeutral(){
        double throttle=0;
        for(int i=0;i<20;i++)throttle=AirshipFlight.rampThrottle(throttle,1,.05);
        assertEquals(1d/3,throttle,1e-12);
        for(int i=0;i<40;i++)throttle=AirshipFlight.rampThrottle(throttle,1,.05);
        assertEquals(1,throttle,1e-12);
        for(int i=0;i<20;i++)throttle=AirshipFlight.rampThrottle(throttle,-1,.05);
        assertTrue(throttle>0,"Reverse command cannot instantly flip thrust");
        for(int i=0;i<100;i++)throttle=AirshipFlight.rampThrottle(throttle,-1,.05);
        assertEquals(-1,throttle,1e-12);
        double fine=0;for(int i=0;i<100;i++)fine=AirshipFlight.rampThrottle(fine,1,.01);
        assertEquals(AirshipFlight.rampThrottle(0,1,1),fine,1e-12,"Ramp is independent of physics substep size");
    }
    @Test void powerAcceptsExtremeFiniteValuesButRejectsInvalidInput(){
        assertEquals(1e250,AirshipFlight.parsePower("1e250"));
        assertEquals(Double.MAX_VALUE,AirshipFlight.parsePower(Double.toString(Double.MAX_VALUE)));
        assertEquals(0,AirshipFlight.parsePower("0"));
        for(var text:new String[]{"-1","NaN","Infinity","1e999","nope"})assertThrows(IllegalArgumentException.class,()->AirshipFlight.parsePower(text));
    }
    @Test void powerAndDragHaveExpectedPhysicalTrends(){
        assertEquals(0,AirshipFlight.support(0,1000));
        assertEquals(1000,AirshipFlight.support(10000,1000));
        assertTrue(Double.isFinite(AirshipFlight.thrust(Double.MAX_VALUE,0)));
        assertTrue(AirshipFlight.thrust(1000,20)<AirshipFlight.thrust(1000,0));
        assertEquals(4*AirshipFlight.drag(3,10),AirshipFlight.drag(3,20));
        assertEquals(-AirshipFlight.drag(3,10),AirshipFlight.drag(3,-10));
    }
    @Test void increasingPowerKeepsIncreasingDriveBeyondFormerSpeedAndThrustLimits(){
        double lower=AirshipFlight.driveDelta(1e7,10000,100,.05);
        double higher=AirshipFlight.driveDelta(1e9,10000,100,.05);
        assertTrue(higher>lower&&higher>1);
        assertTrue(Double.isFinite(AirshipFlight.driveDelta(Double.MAX_VALUE,10000,100,.05)));
        assertEquals(0,AirshipFlight.driveDelta(0,10000,100,.05));
    }
}
