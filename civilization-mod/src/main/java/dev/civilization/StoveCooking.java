package dev.civilization;

/** Shared continuous curves, in full-heat work ticks. No XP or randomized quality. */
public final class StoveCooking {
    public static final double OPTIMUM=800, PLATEAU_END=1000, MAX=1800, EDIBLE=400;
    private StoveCooking(){}
    public static double rate(double dial){return Double.isFinite(dial)?Math.pow(Math.clamp(dial,0,1),.7):0;}
    private static double smooth(double x){x=Math.clamp(x,0,1);return x*x*(3-2*x);}
    public static double quality(double work){return work<OPTIMUM?smooth((work-EDIBLE)/(OPTIMUM-EDIBLE)):1-smooth((work-PLATEAU_END)/(MAX-PLATEAU_END));}
    public static double retained(double quality){return .8+.2*Math.clamp(quality,0,1);}
    public static double discount(double quality){return .10+.05*Math.clamp(quality,0,1);}
    /** Pale vegetables, golden plateau, progressive browning and char. */
    public static int color(double work,boolean carrot){
        int[] colors=carrot?new int[]{0xffdc8846,0xffe8a34b,0xffc87932,0xff3d3028}:new int[]{0xffe4dbad,0xffe5b75c,0xffa16a33,0xff332b24};
        double[] marks={0,OPTIMUM,1200,MAX};int i=0;while(i<2&&work>marks[i+1])i++;
        double t=Math.clamp((work-marks[i])/(marks[i+1]-marks[i]),0,1);int out=0xff000000;
        for(int shift:new int[]{16,8,0})out|=((int)Math.round(((colors[i]>>shift)&255)*(1-t)+((colors[i+1]>>shift)&255)*t))<<shift;
        return out;
    }
}
