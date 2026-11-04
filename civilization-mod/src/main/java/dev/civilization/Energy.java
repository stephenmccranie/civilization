package dev.civilization;

import java.util.Locale;

/** The one energy unit: joules, with real values. A common measure compares energy; it never converts one fuel into another. */
public final class Energy {
    private Energy() {}
    /** A Coal item is a 10 kg scuttle of bituminous coal at 25 MJ/kg. */
    public static final double COAL_KG = 10, COAL = COAL_KG * 25e6;
    public static final double KCAL = 4184;

    /** Three significant figures in J, kJ, MJ or GJ, such as "2.09 MJ" or "250 MJ". */
    public static String text(double joules) { return scaled(joules, new String[]{"J", "kJ", "MJ", "GJ", "TJ"}); }
    /** Power in the same form, such as "837 W" or "69.4 kW". */
    public static String watts(double watts) { return scaled(watts, new String[]{"W", "kW", "MW", "GW", "TW"}); }
    private static String scaled(double amount, String[] units) {
        int unit = 0;
        double value = amount;
        while (Math.abs(value) >= 999.5 && unit < units.length - 1) { value /= 1000; unit++; }
        String digits = Math.abs(value) >= 99.95 ? "%.0f" : Math.abs(value) >= 9.995 ? "%.1f" : "%.2f";
        return String.format(Locale.ROOT, digits, value) + " " + units[unit];
    }
}
