package dev.civilization;
/** Pure work accounting, shared by production and tests. Amounts are millibuckets. */
public final class BoatEngine {
    public static final double REFERENCE_MASS=33;
    public record State(double fuel,double oil,double condition,double power) {}
    public static State step(double fuel,double oil,double condition,double demand,double dt) {
        demand=Math.clamp(demand,0,1);dt=Math.clamp(dt,0,1);condition=Math.clamp(condition,.2,1);
        if(demand==0||dt==0||fuel<=0)return new State(Math.max(0,fuel),Math.max(0,oil),condition,0);
        double work=Math.min(demand*dt,fuel/2);
        fuel=Math.max(0,fuel-2*work);
        if(oil>=.1*work){oil-=.1*work;condition=1;}else condition=Math.max(.2,condition-.001*work);
        return new State(fuel,Math.max(0,oil),condition,condition*work/dt);
    }
    public static double driveAcceleration(double enginePower,double mass){return !Double.isFinite(enginePower)||!Double.isFinite(mass)||enginePower<=0||mass<=0?0:11.2*REFERENCE_MASS*enginePower/mass;}
}
