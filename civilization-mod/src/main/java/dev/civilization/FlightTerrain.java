package dev.civilization;

import java.util.*;
import java.util.function.LongPredicate;
import it.unimi.dsi.fastutil.longs.*;

/** Bounded world-coordinate coverage. One immutable publication is consumed for a whole physics step. */
public final class FlightTerrain {
    public static final int MAX_CHUNKS=1024;
    public static final double MAX_DISTANCE=4096, MARGIN=24, SAMPLE=8;
    public static final class Snapshot {
        private final LongSet checked,loaded;
        public Snapshot(Set<Long> checked,Set<Long> loaded){
            // Set.copyOf uses Long.hashCode (x XOR z) without mixing; corridor keys cluster badly.
            this.checked=LongSets.unmodifiable(new LongOpenHashSet(checked));
            this.loaded=LongSets.unmodifiable(new LongOpenHashSet(loaded));
        }
        public LongSet checked(){return checked;}
        public LongSet loaded(){return loaded;}
        public static final Snapshot EMPTY=new Snapshot(Set.of(),Set.of());
        public boolean footprint(double x,double z,double radius){
            if(!Double.isFinite(x)||!Double.isFinite(z)||Math.abs(x)>30_000_000||Math.abs(z)>30_000_000)return false;
            int minX=(int)Math.floor((x-radius)/16),maxX=(int)Math.floor((x+radius)/16);
            int minZ=(int)Math.floor((z-radius)/16),maxZ=(int)Math.floor((z+radius)/16);
            for(int a=minX;a<=maxX;a++)for(int b=minZ;b<=maxZ;b++)if(!loaded.contains(key(a,b)))return false;
            return true;
        }
        /** The extra half-sample margin covers the continuous path between samples, including diagonals. */
        public double distance(double x,double z,double dx,double dz,double requested){
            double length=Math.hypot(dx,dz);
            if(!Double.isFinite(length)||!Double.isFinite(requested)||requested<0||!footprint(x,z,MARGIN))return 0;
            if(length<1e-9)return Math.min(requested,MAX_DISTANCE);
            if(!footprint(x,z,MARGIN+SAMPLE*.5))return 0;
            dx/=length;dz/=length;double limit=Math.min(requested,MAX_DISTANCE),clear=0;
            for(double at=Math.min(SAMPLE,limit);at>0;at=Math.min(at+SAMPLE,limit)){
                if(!footprint(x+dx*at,z+dz*at,MARGIN+SAMPLE*.5)){
                    double blocked=at;
                    for(int i=0;i<5;i++){double mid=(clear+blocked)*.5;if(footprint(x+dx*mid,z+dz*mid,MARGIN+SAMPLE*.5))clear=mid;else blocked=mid;}
                    return clear;
                }
                clear=at;if(at>=limit)break;
            }
            return clear;
        }
        public boolean sweep(double x,double z,double dx,double dz){
            double length=Math.hypot(dx,dz);
            return Double.isFinite(length)&&length<=MAX_DISTANCE&&footprint(x,z,MARGIN)&&distance(x,z,dx,dz,length)>=length;
        }
    }
    public static long key(int x,int z){return (x&0xffffffffL)|((z&0xffffffffL)<<32);}
    public static int x(long key){return (int)key;}
    public static int z(long key){return (int)(key>>>32);}
    public static LongSet corridor(double x,double z,double dx,double dz,double lead){
        if(!Double.isFinite(x)||!Double.isFinite(z)||Math.abs(x)>29_999_000||Math.abs(z)>29_999_000)return LongSets.EMPTY_SET;
        var cells=new LongLinkedOpenHashSet();int cx=(int)Math.floor(x/16),cz=(int)Math.floor(z/16);
        for(int a=cx-4;a<=cx+4;a++)for(int b=cz-4;b<=cz+4;b++)cells.add(key(a,b));
        double length=Math.hypot(dx,dz);if(!Double.isFinite(length)||length<1e-9)return cells;
        dx/=length;dz/=length;lead=Double.isFinite(lead)?Math.clamp(lead,0,MAX_DISTANCE):MAX_DISTANCE;
        for(double at=0;at<=lead;at+=SAMPLE){
            cx=(int)Math.floor((x+dx*at)/16);cz=(int)Math.floor((z+dz*at)/16);
            for(int a=cx-3;a<=cx+3;a++)for(int b=cz-3;b<=cz+3;b++){
                if(cells.size()>=MAX_CHUNKS)return cells;
                cells.add(key(a,b));
            }
        }
        return cells;
    }
    public static Snapshot capture(Set<Long> cells,LongPredicate ready){
        var loaded=new LongOpenHashSet();for(long cell:cells)if(ready.test(cell))loaded.add(cell);
        return new Snapshot(cells,loaded);
    }
    /** Preparation only: prediction never grants physics readiness. Straight motion retains first priority. */
    public static LongSet predictedCorridor(double x,double z,double vx,double vz,double lead,
            double facingX,double facingZ,double yawRate,int turn,double acceleration){
        var cells=new LongLinkedOpenHashSet(corridor(x,z,0,0,0));
        double speed=Math.hypot(vx,vz);
        if(cells.isEmpty()||!Double.isFinite(speed)||speed<1e-6)return cells;
        lead=Double.isFinite(lead)?Math.clamp(lead,0,MAX_DISTANCE):MAX_DISTANCE;
        if(!Double.isFinite(yawRate)||!Double.isFinite(acceleration)||!Double.isFinite(facingX)||!Double.isFinite(facingZ))return corridor(x,z,vx,vz,lead);
        double heading=Math.atan2(facingZ,facingX),dx=vx/speed,dz=vz/speed;
        boolean curved=Math.abs(yawRate)>.02||turn!=0||acceleration>0&&Math.abs(dx*Math.sin(heading)-dz*Math.cos(heading))>.02;
        int straightBudget=curved?640:MAX_CHUNKS;
        for(double at=0;at<=lead;at+=SAMPLE)if(!pad(cells,x+dx*at,z+dz*at,28,straightBudget))break;
        if(!curved)return cells;
        double px=x,pz=z,dt=SAMPLE/speed;
        for(double at=0;at<=lead;at+=SAMPLE){
            if(!pad(cells,px,pz,at<128?28:36,MAX_CHUNKS))break;
            // The hull turns before momentum does. Match the yaw controller's 1/3-second response,
            // then apply only lateral drive acceleration to a constant-speed forecast.
            yawRate+=(-Math.clamp(turn,-1,1)-yawRate)*(-Math.expm1(-3*dt));
            heading-=yawRate*dt;
            double lateral=Math.clamp(acceleration,0,speed/Math.max(dt,1e-6))*(dx*Math.sin(heading)-dz*Math.cos(heading));
            double angle=lateral/speed*dt,c=Math.cos(angle),s=Math.sin(angle),nx=dx*c-dz*s;
            dz=dx*s+dz*c;dx=nx;px+=dx*SAMPLE;pz+=dz*SAMPLE;
        }
        return cells;
    }
    private static boolean pad(LongSet cells,double x,double z,double radius,int budget){
        if(Math.abs(x)>29_999_000||Math.abs(z)>29_999_000)return false;
        for(int a=(int)Math.floor((x-radius)/16);a<=(int)Math.floor((x+radius)/16);a++)
            for(int b=(int)Math.floor((z-radius)/16);b<=(int)Math.floor((z+radius)/16);b++){
                long key=key(a,b);if(cells.contains(key))continue;
                if(cells.size()>=budget)return false;cells.add(key);
            }
        return true;
    }
    /** Finite lookahead, not a propulsion cap. Smooth both recovery and braking before the hard step boundary. */
    public static double permittedSpeed(double current,double proposed,double distance,double dt){
        if(!Double.isFinite(current)||!Double.isFinite(proposed)||!Double.isFinite(distance)||!Double.isFinite(dt)||dt<=0)return 0;
        double target=Math.max(0,distance)/(0.35+2*dt);
        double eased=Math.max(0,current)+(target-Math.max(0,current))*(-Math.expm1(-dt/.20));
        double hard=Math.max(0,distance)/(2*dt+.025);
        return Math.max(0,Math.min(proposed,Math.min(eased,hard)));
    }
    private FlightTerrain(){}
}
