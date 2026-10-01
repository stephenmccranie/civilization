package dev.civilization;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BoatEngineTest {
 @Test void parkedAndEmptyNeverWearOrBurn(){assertEquals(new BoatEngine.State(100,20,.7,0),BoatEngine.step(100,20,.7,0,.05));assertEquals(new BoatEngine.State(0,20,.7,0),BoatEngine.step(0,20,.7,1,.05));}
 @Test void workConsumesBothProductsAndOilRestores(){var s=BoatEngine.step(100,20,.2,1,1);assertEquals(98,s.fuel());assertEquals(19.9,s.oil(),1e-9);assertEquals(1,s.condition());assertEquals(1,s.power());}
 @Test void dryEngineGraduallySlowsAndCannotConsumeMoreFuelThanStored(){var s=BoatEngine.step(.01,0,.7,1,1);assertEquals(0,s.fuel());assertTrue(s.condition()<.7&&s.condition()>.69);assertTrue(s.power()<.01);assertEquals(.2,BoatEngine.step(100,0,.2,1,1).condition());}
 @Test void accountingDoesNotDependOnSubsteps(){var one=BoatEngine.step(100,20,1,1,1);var many=new BoatEngine.State(100,20,1,0);for(int i=0;i<20;i++)many=BoatEngine.step(many.fuel(),many.oil(),many.condition(),1,.05);assertEquals(one.fuel(),many.fuel(),1e-9);assertEquals(one.oil(),many.oil(),1e-9);}
 @Test void fixedEngineThrustMakesMassMatter(){double baseline=BoatEngine.driveAcceleration(1,BoatEngine.REFERENCE_MASS);assertEquals(11.2,baseline,1e-9);assertEquals(baseline/2,BoatEngine.driveAcceleration(1,BoatEngine.REFERENCE_MASS*2),1e-9);assertEquals(baseline,BoatEngine.driveAcceleration(2,BoatEngine.REFERENCE_MASS*2),1e-9);assertEquals(0,BoatEngine.driveAcceleration(0,100));}
}
