package dev.civilization;

import java.util.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/**
 * Measured native model surfaces in the model frame (x right, y up, z rear). The same boxes collide,
 * take mouse hits and show as the debug hitbox. Rotation is voxelized once per heading, never per frame.
 */
public final class WheelbarrowCollision {
    public static final double TRAY_TOP=15.525/16;
    public static final List<AABB> BODY=parts();
    // Handles are interaction targets, never solid obstacles; the wide box makes them easy to grab.
    private static final AABB HANDLES=new AABB(-.48,.45,12.69/16,.48,1.05,1.472);
    private float yawA=Float.NaN,yawB=Float.NaN;
    private VoxelShape shapeA,shapeB;
    private static List<AABB> parts(){
        var boxes=new ArrayList<AABB>();
        // A solid tray platform gives a stable rim-height surface to jump onto.
        boxes.add(new AABB(-7.155/16,9.045/16,-7.29/16,7.155/16,TRAY_TOP,12.69/16));
        for(int side:new int[]{-1,1}){
            double x0=side<0?-6.48/16:4.995/16,x1=side<0?-4.995/16:6.48/16;
            boxes.add(new AABB(x0,0,9.72/16,x1,9.045/16,11.205/16));
        }
        boxes.add(new AABB(-.055,0,-1.065,.055,.12,-.625));
        boxes.add(new AABB(-.055,.12,-1.195,.055,.56,-.49));
        boxes.add(new AABB(-.055,.56,-1.065,.055,.69,-.625));
        return List.copyOf(boxes);
    }
    /** World point of a model-frame point on a carrier at `at` facing `yaw`. */
    public static Vec3 place(Vec3 at,float yaw,Vec3 model){return at.add(model.yRot((float)Math.toRadians(180d-yaw)));}
    /** World bounds of a model-frame box on a carrier at `at` facing `yaw`. */
    public static AABB place(Vec3 at,float yaw,AABB model){
        double a=Math.toRadians(180d-yaw),s=Math.abs(Math.sin(a)),c=Math.abs(Math.cos(a));
        var center=place(at,yaw,model.getCenter());double x=c*model.getXsize()/2+s*model.getZsize()/2,z=s*model.getXsize()/2+c*model.getZsize()/2;
        return new AABB(center.x-x,at.y+model.minY,center.z-z,center.x+x,at.y+model.maxY,center.z+z);
    }
    /** Physical body bounds, padded for the voxel grid's rounding. */
    public static AABB bounds(Vec3 at,float yaw){return place(at,yaw,new AABB(-.4471875,0,-1.195,.4471875,TRAY_TOP,12.69/16)).inflate(1d/32,0,1d/32);}
    public static AABB pickBounds(Vec3 at,float yaw){return bounds(at,yaw).minmax(place(at,yaw,HANDLES));}
    /** Nearest point where a ray meets the body or handles, tested in the model frame. */
    public static Optional<Vec3> pick(Vec3 at,float yaw,Vec3 from,Vec3 to){
        float back=(float)Math.toRadians(yaw-180d);
        Vec3 a=from.subtract(at).yRot(back),b=to.subtract(at).yRot(back),best=null;
        for(var box:BODY)best=nearer(a,best,box.clip(a,b));
        best=nearer(a,best,HANDLES.clip(a,b));
        return Optional.ofNullable(best).map(hit->place(at,yaw,hit));
    }
    private static Vec3 nearer(Vec3 from,Vec3 best,Optional<Vec3> hit){return hit.filter(h->best==null||from.distanceToSqr(h)<from.distanceToSqr(best)).orElse(best);}
    public VoxelShape at(Vec3 position,float yaw){return local(yaw).move(position.x,position.y,position.z);}
    private VoxelShape local(float yaw){
        if(Float.compare(yaw,yawA)==0)return shapeA;
        if(Float.compare(yaw,yawB)==0)return shapeB;
        yawB=yawA;shapeB=shapeA;yawA=yaw;shapeA=build(yaw);return shapeA;
    }
    private static VoxelShape build(float yaw){
        // One shared horizontal grid avoids expensive unions of hundreds of
        // independently rotated boxes. Vertical coordinates remain exact.
        double[] ys=BODY.stream().flatMapToDouble(b->java.util.stream.DoubleStream.of(b.minY,b.maxY)).distinct().sorted().toArray();
        int cells=96;double step=1d/32,origin=-1.5;
        double[] horizontal=new double[cells+1];for(int i=0;i<=cells;i++)horizontal[i]=origin+i*step;
        var grid=new BitSetDiscreteVoxelShape(cells,ys.length-1,cells);
        double a=Math.toRadians(180d-yaw),s=Math.sin(a),c=Math.cos(a);
        if(Math.abs(s)<1e-12)s=0;if(Math.abs(c)<1e-12)c=0;
        for(var box:BODY){
            double[] xs=new double[4],zs=new double[4];double minX=Double.POSITIVE_INFINITY,maxX=Double.NEGATIVE_INFINITY;
            for(int i=0;i<4;i++){
                double x=(i==0||i==3)?box.minX:box.maxX,z=i<2?box.minZ:box.maxZ;
                xs[i]=x*c+z*s;zs[i]=z*c-x*s;minX=Math.min(minX,xs[i]);maxX=Math.max(maxX,xs[i]);
            }
            // Clip the rotated rectangle into thin world-X strips. Unlike one
            // rotated bounding box, empty diagonal corners stay walkable.
            for(int ix=Math.max(0,(int)Math.floor((minX-origin)/step));ix<cells&&horizontal[ix]<maxX-1e-9;ix++){
                double lo=horizontal[ix],hi=horizontal[ix+1],minZ=Double.POSITIVE_INFINITY,maxZ=Double.NEGATIVE_INFINITY;
                for(int i=0;i<4;i++){
                    if(xs[i]>=lo-1e-10&&xs[i]<=hi+1e-10){minZ=Math.min(minZ,zs[i]);maxZ=Math.max(maxZ,zs[i]);}
                    int j=(i+1)%4;double dx=xs[j]-xs[i];if(Math.abs(dx)<1e-12)continue;
                    for(double edge:new double[]{lo,hi})if(edge>=Math.min(xs[i],xs[j])-1e-10&&edge<=Math.max(xs[i],xs[j])+1e-10){
                        double z=zs[i]+(edge-xs[i])/dx*(zs[j]-zs[i]);minZ=Math.min(minZ,z);maxZ=Math.max(maxZ,z);
                    }
                }
                if(maxZ>minZ+1e-9){
                    int y0=Arrays.binarySearch(ys,box.minY),y1=Arrays.binarySearch(ys,box.maxY);
                    int z0=Math.max(0,(int)Math.floor((minZ-origin)/step)),z1=Math.min(cells,(int)Math.ceil((maxZ-origin)/step-1e-9));
                    for(int iy=y0;iy<y1;iy++)for(int iz=z0;iz<z1;iz++)grid.fill(ix,iy,iz);
                }
            }
        }
        return new GridShape(grid,horizontal,ys);
    }
    private static final class GridShape extends ArrayVoxelShape {
        GridShape(DiscreteVoxelShape grid,double[] horizontal,double[] vertical){super(grid,horizontal,vertical,horizontal);}
    }
}
