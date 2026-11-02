package dev.civilization;

/** A smooth daily air-temperature envelope, with the same mean and extrema as the old cycle. */
public final class DailyTemperatureCycle {
    private static final int DAY=24_000,AFTERNOON_PEAK=9_000;
    private DailyTemperatureCycle(){}
    /** Minecraft time 0 is dawn (06:00); 9000 is mid-afternoon (15:00). */
    public static double phase(long dayTime){
        int time=(int)Math.floorMod(dayTime,(long)DAY);
        return time<=AFTERNOON_PEAK
                ? -Math.cos(Math.PI*time/AFTERNOON_PEAK)
                : Math.cos(Math.PI*(time-AFTERNOON_PEAK)/(DAY-AFTERNOON_PEAK));
    }
}
