package dev.civilization;

/** Stable, bounded detail policy. No dependency on Minecraft or DH. */
public final class DhTravelPolicy {
    public record View(double multiplier) {}
    public static final double WARP_SPEED=80, WARP_MULTIPLIER=16;
    private double speed, recoverySeconds;
    private boolean warp;
    public void reset(){speed=0;warp=false;recoverySeconds=0;}
    public int tier(){return warp?1:0;}
    public double speed(){return speed;}
    public View view(){return new View(warp?WARP_MULTIPLIER:1);}
    public void update(double measuredSpeed,double seconds){
        if(!Double.isFinite(measuredSpeed)||measuredSpeed<0||!Double.isFinite(seconds)||seconds<=0||seconds>.5){reset();return;}
        speed+=(measuredSpeed-speed)*(-Math.expm1(-seconds/.25));
        if(speed>=WARP_SPEED){warp=true;recoverySeconds=0;}
        else if(warp){
            recoverySeconds+=seconds;
            if(recoverySeconds>=1){warp=false;recoverySeconds=0;}
        }
    }
}
