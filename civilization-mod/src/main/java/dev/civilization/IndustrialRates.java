package dev.civilization;

/** One extraction line at 20 TPS. Volumes remain integer mB throughout. */
public final class IndustrialRates {
    public static final int STEP_TICKS = 10;
    public static final int PIPE_MB_PER_STEP = 25;
    public static final int PUMP_MB = 125;
    /** Full-power ticks of engine work per pumped step, and per block the derrick drills. */
    public static final int PUMP_TICKS = 50, DRILL_TICKS = 100;
    /** Blocks of well that one Iron Plate of casing lines. */
    public static final int CASING_BLOCKS = 4;
    /** The derrick's well lies this many blocks behind its controller. */
    public static final int WELL = 5;
    /** The Coal Engine's share of full power, and the work units of coal heat one full-power tick costs: one Coal
     *  drives five full-power seconds. */
    public static final double COAL_ENGINE_POWER = .5, COAL_WORK_UNITS = 4;
    public static final int DRILL_FUEL_MB = 80;
    private IndustrialRates() {}
}
