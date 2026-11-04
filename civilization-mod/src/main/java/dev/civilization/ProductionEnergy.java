package dev.civilization;

/** Shared production coal budget. Claim upkeep is a separate real-time policy. */
public final class ProductionEnergy {
    private ProductionEnergy() {}
    /** Work units one Coal buys: its 250 MJ in units of 625 kJ. */
    /** Work units one Coal buys: its 250 MJ in units of 625 kJ. */
    public static final int COAL = 400;
    /** Kilns and standard workshop jobs use one unit per work/heat tick; tanning and weaving are gentler. */
    public static final int HEAT_TICKS = COAL;
    /** Pump/heater work costs eight energy units per five mB. */
    public static final int OIL_MB_PER_COAL = COAL * 5 / 8;
}
