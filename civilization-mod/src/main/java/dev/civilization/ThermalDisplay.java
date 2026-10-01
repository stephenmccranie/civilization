package dev.civilization;

/** Fixed Fahrenheit-anchored display palette; simulation/save units remain unchanged. */
public final class ThermalDisplay {
    private ThermalDisplay() {}
    // More color steps around human comfort; the legend gives each interval equal width.
    private static final double[] STOPS_F = {0, 40, 60, 70, 80, 100, 130, 180, 300};
    private static final int[] COLORS = {0x384ab8, 0x3795df, 0x76c8c8, 0xd7bb7b, 0xe7c05f,
            0xffa343, 0xf37136, 0xd73831, 0xffe6d7};
    public static double fahrenheit(double celsius) { return celsius * 1.8 + 32; }
    /** Fraction along the labeled, deliberately nonlinear absolute-temperature legend. */
    public static double legendPosition(double celsius) {
        double f = fahrenheit(celsius);
        if (!Double.isFinite(f)) return 0;
        if (f <= STOPS_F[0]) return 0;
        for (int i = 1; i < STOPS_F.length; i++)
            if (f <= STOPS_F[i]) return (i - 1 + (f - STOPS_F[i - 1]) / (STOPS_F[i] - STOPS_F[i - 1])) / (STOPS_F.length - 1);
        return 1;
    }
    public static double legendFahrenheit(double position) {
        double scaled = Math.clamp(position, 0, 1) * (STOPS_F.length - 1);
        int index = Math.min(STOPS_F.length - 2, (int) scaled);
        return STOPS_F[index] + (STOPS_F[index + 1] - STOPS_F[index]) * (scaled - index);
    }
    public static int color(double celsius) {
        if (!Double.isFinite(celsius)) return 0xff888888;
        double f=fahrenheit(celsius);
        if (f <= STOPS_F[0]) return 0xff000000 | COLORS[0];
        for (int i=1;i<STOPS_F.length;i++) if(f<=STOPS_F[i]) {
            double t=(f-STOPS_F[i-1])/(STOPS_F[i]-STOPS_F[i-1]);
            int a=COLORS[i-1],b=COLORS[i],out=0xff000000;
            for(int shift:new int[]{16,8,0})out|=(int)Math.round(((a>>shift)&255)*(1-t)+((b>>shift)&255)*t)<<shift;
            return out;
        }
        return 0xff000000 | COLORS[COLORS.length-1];
    }
}
