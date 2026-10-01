package dev.civilization;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightCollisionTest {
    @Test void thinWallsCannotBeSkippedInEitherDirection(){
        var hull=new FlightCollision.Box(-2,10,-3,2,11,3);
        assertEquals(.098,hull.sweep(new FlightCollision.Box(100,0,-10,100.5,20,10),1000,0,0),1e-9);
        assertEquals(.098,hull.sweep(new FlightCollision.Box(-100.5,0,-10,-100,20,10),-1000,0,0),1e-9);
        assertEquals(1,hull.sweep(new FlightCollision.Box(100,0,10,101,20,20),1000,0,0));
    }
    @Test void verticalDiagonalAndSeparatingMotion(){
        var hull=new FlightCollision.Box(0,0,0,1,1,1);
        assertEquals(.49,hull.sweep(new FlightCollision.Box(50,50,50,51,51,51),100,100,100),1e-9);
        assertEquals(.49,hull.sweep(new FlightCollision.Box(0,50,0,1,51,1),0,100,0),1e-9);
        var wall=new FlightCollision.Box(1,0,0,2,1,1);
        assertEquals(0,hull.sweep(wall,100,0,0));
        assertEquals(1,hull.sweep(wall,-100,0,0));
        assertEquals(1,hull.sweep(wall,0,100,0));
    }
}
