package dev.civilization;

import java.util.HashMap;
import java.util.Map;

/** Two fixed horizontal resolutions; history lasts only while a node is visited. */
public final class DhWarpBands {
    public static final byte NEAR=2,FAR=6; // 4- and 64-block horizontal cells.
    public static final double RADIUS=512,BUFFER=32;
    private record Node(int x,int z,double radius){}
    private Map<Node,Boolean> previous=new HashMap<>(),current=new HashMap<>();
    public void begin(boolean warp){
        var reuse=previous;previous=current;current=reuse;current.clear();
        if(!warp)previous.clear();
    }
    public byte detail(int x,int z,double radius,double centerDistance){
        var key=new Node(x,z,radius);
        Boolean before=current.get(key);if(before==null)before=previous.get(key);
        // Intersecting parent sections must subdivide so they cannot hide the near band.
        double distance=Math.max(0,centerDistance-radius);
        double boundary=before==null?RADIUS:before?RADIUS+BUFFER:RADIUS-BUFFER;
        boolean near=distance<=boundary;
        if(current.size()<16384)current.put(key,near);
        return near?NEAR:FAR;
    }
}
