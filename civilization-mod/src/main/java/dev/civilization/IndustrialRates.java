package dev.civilization;

/** One extraction line at 20 TPS. Volumes remain integer mB throughout. */
public final class IndustrialRates {
    public static final int STEP_TICKS = 10;
    public static final int PIPE_MB_PER_STEP = 25;
    public static final int PUMP_MB = 125;
    public static final int PUMP_TICKS = 50;
    public static final int DRILL_FUEL_MB = 80;
    private IndustrialRates() {}
}
