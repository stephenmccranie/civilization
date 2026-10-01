package dev.civilization;

/** Prototype tuning in engine units. Input power has no gameplay ceiling. */
public final class AirshipFlight {
    public static final double DEFAULT_POWER = 1e7;
    public static final double VERTICAL_SPEED = 9;
    public static final double HORIZONTAL_INERTIA = 8;
    public static final double THRUST_RAMP_SECONDS = 3;
    /** Signed drive response: full forward to full reverse takes twice the startup ramp. */
    public static double rampThrottle(double current,double target,double dt){
        double step=Math.max(0,dt)/THRUST_RAMP_SECONDS;
        return current+Math.clamp(target-current,-step,step);
    }
    public static boolean validPower(double value) { return Double.isFinite(value) && value >= 0; }
    public static double parsePower(String text) {
        double value = Double.parseDouble(text.trim());
        if (!validPower(value)) throw new IllegalArgumentException("Use a finite, non-negative number.");
        return value;
    }
    /** Field support costs 10 prototype watts per newton; reserve this before propulsion. */
    public static double support(double power, double weight) { return Math.min(power / 10, weight); }
    public static double thrust(double power, double speed) { return power / (10 + Math.abs(speed)); }
    /** Implicit drive step: spend finite power without a zero-speed singularity or a force/speed cap. */
    public static double driveDelta(double power,double mass,double speed,double dt) {
        double work=power*(dt/mass), half=(10+Math.abs(speed))*.5;
        return work/(Math.sqrt(work+half*half)+half);
    }
    public static double drag(double area, double speed) { return -.6 * area * speed * Math.abs(speed); }
    private AirshipFlight() {}
}
