package dev.civilization;

import java.util.*;

/** Bounded cached permutations of the unchanged square no-tick footprint. */
public final class FlightChunkOrder {
    public static final int MAX_RADIUS=64;
    private static final Map<Integer,long[]> CACHE=new LinkedHashMap<>(32,.75f,true);
    private FlightChunkOrder(){}
    public static int heading(double x,double z){
        if(!Double.isFinite(x)||!Double.isFinite(z)||Math.hypot(x,z)<80)return -1;
        return Math.floorMod((int)Math.round(Math.atan2(z,x)*8/Math.PI),16);
    }
    public static final class Cursor {
        private final long[] order;
        private int index;
        private Cursor(long[] order){this.order=order;}
        public boolean hasNext(){return index<order.length;}
        public long remaining(){return order.length-index;}
        public long next(){if(!hasNext())throw new NoSuchElementException();return order[index++];}
    }
    public static synchronized Cursor create(int radius,int heading){
        if(radius<0||radius>MAX_RADIUS||heading<0||heading>=16)return null;
        int key=radius*16+heading;
        long[] order=CACHE.get(key);
        if(order==null){
            double dx=Math.cos(heading*Math.PI/8),dz=Math.sin(heading*Math.PI/8);
            Long[] offsets=new Long[(2*radius+1)*(2*radius+1)];int i=0;
            for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)offsets[i++]=pack(x,z);
            Arrays.sort(offsets,Comparator.comparingDouble((Long p)->score(x(p),z(p),dx,dz)).thenComparingLong(Long::longValue));
            order=new long[offsets.length];for(i=0;i<order.length;i++)order[i]=offsets[i];
            CACHE.put(key,order);if(CACHE.size()>32)CACHE.remove(CACHE.keySet().iterator().next());
        }
        return new Cursor(order);
    }
    private static double score(int x,int z,double dx,double dz){
        int ring=Math.max(Math.abs(x),Math.abs(z));
        double ahead=x*dx+z*dz,side=Math.abs(x*dz-z*dx);
        if(ring<=2)return ring*10+Math.hypot(x,z);
        if(ahead>0&&side<=2.5)return 100+ahead+side*.01;
        return 1000+ring*10-ahead*.01;
    }
    public static long pack(int x,int z){return (x&0xffffffffL)|((long)z<<32);}
    public static int x(long p){return (int)p;}
    public static int z(long p){return (int)(p>>>32);}
}
