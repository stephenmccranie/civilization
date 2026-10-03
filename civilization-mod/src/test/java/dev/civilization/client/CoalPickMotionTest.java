package dev.civilization.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CoalPickMotionTest {
    @Test void aTurnCarriesTheHeadWithTheCameraAndSettlesAfterItStops(){
        var motion=new CoalPickMotion();motion.advance(30,-20,1.0/60);
        assertTrue(motion.yaw()>12&&motion.pitch()<-8,"Head follows the camera turn instead of trailing it");
        double lag=motion.yaw();motion.advance(0,0,1.0/60);
        assertTrue(motion.yaw()>0&&motion.yaw()<lag,"Release settles rather than snapping to the camera");
        for(int i=0;i<180;i++)motion.advance(0,0,1.0/60);
        assertEquals(0,motion.yaw(),.001);assertEquals(0,motion.pitch(),.001);
    }
    private static double turn(int fps){var m=new CoalPickMotion();for(int i=0;i<fps/2;i++)m.advance(60.0/fps,0,1.0/fps);return m.yaw();}
    @Test void responseIsStableAcrossFrameRatesAndFastTurnsStayBounded(){
        assertEquals(turn(30),turn(144),1.0);
        var motion=new CoalPickMotion();for(int i=0;i<600;i++){motion.advance(i%2==0?170:-170,80,1.0/144);assertTrue(Math.abs(motion.yaw())<=28&&Math.abs(motion.pitch())<=24);}
        motion.advance(Double.NaN,0,.01);assertEquals(0,motion.yaw());assertEquals(0,motion.pitch());
    }
    @Test void fullBackswingPrecedesContactAndFollowThroughRecoversContinuously(){
        var overhead=CoalPickMotion.swing(8,12,32);
        assertTrue(overhead.pitch()>=115&&overhead.y()>=.9&&overhead.z()>0,"Raised grip draws the head over the player rather than forward");
        assertTrue(CoalPickMotion.swing(12,12,32).pitch()<-15,"Head drives forward and down at contact");
        assertTrue(CoalPickMotion.swing(12,12,32).z()>-.15,"Grip stays close during the strike instead of reaching far forward");
        assertTrue(CoalPickMotion.swing(15,12,32).pitch()<CoalPickMotion.swing(12,12,32).pitch());
        for(int age=0;age<=32;age++){
            var pose=CoalPickMotion.swing(age,12,32);
            assertEquals(0,pose.yaw(),"Still-camera strikes never spin sideways");
            assertEquals(0,pose.roll(),"Handle stays in the strike plane");
        }
        for(int corner:new int[]{8,12,15,32}){
            var a=CoalPickMotion.swing(corner-.001,12,32);var b=CoalPickMotion.swing(corner+.001,12,32);
            assertEquals(a.pitch(),b.pitch(),.001);assertEquals(a.yaw(),b.yaw(),.001);assertEquals(a.z(),b.z(),.001);
        }
        assertEquals(new CoalPickMotion.Pose(0,0,0,0,0,0),CoalPickMotion.swing(32,12,32));
    }
}
