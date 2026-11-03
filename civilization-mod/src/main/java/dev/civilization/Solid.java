package dev.civilization;

import java.util.Optional;
import net.minecraft.world.phys.*;

/**
 * A convex outline in plan raised between two heights: a carrier's body or handles, or a block box it
 * meets. Every test separates two solids along each edge normal of either outline, so a turned body
 * meets the world exactly at any heading, with no grid steps along its diagonal faces.
 */
public final class Solid {
    private static final double E=1e-7;
    private final double[] x,z,nx,nz;
    public final double bottom,top;
    /** Vertices in order around a convex outline. */
    public Solid(double[] x,double[] z,double bottom,double top){
        this.x=x;this.z=z;this.bottom=bottom;this.top=top;nx=new double[x.length];nz=new double[x.length];
        for(int i=0;i<x.length;i++){
            int j=(i+1)%x.length;double ex=x[j]-x[i],ez=z[j]-z[i],length=Math.hypot(ex,ez);
            nx[i]=ez/length;nz[i]=-ex/length;
        }
    }
    public static Solid of(AABB b){return new Solid(new double[]{b.minX,b.maxX,b.maxX,b.minX},new double[]{b.minZ,b.minZ,b.maxZ,b.maxZ},b.minY,b.maxY);}
    /** This model-frame solid (x right, y up, z rear) on a carrier at `at` facing `yaw`. */
    public Solid place(Vec3 at,float yaw){
        double a=Math.toRadians(180d-yaw),c=Math.cos(a),s=Math.sin(a);double[] px=new double[x.length],pz=new double[x.length];
        for(int i=0;i<x.length;i++){px[i]=at.x+x[i]*c+z[i]*s;pz[i]=at.z+z[i]*c-x[i]*s;}
        return new Solid(px,pz,at.y+bottom,at.y+top);
    }
    public Solid rise(double dy){return new Solid(x,z,bottom+dy,top+dy);}
    public int corners(){return x.length;}
    public double x(int i){return x[i];}
    public double z(int i){return z[i];}
    public AABB bounds(){return new AABB(least(1,0),bottom,least(0,1),-least(-1,0),top,-least(0,-1));}
    /** Whether the two share volume; touching faces do not. */
    public boolean overlaps(Solid o){return bottom<o.top-E&&top>o.bottom+E&&planOverlaps(o);}
    /** How much of the vertical move `dy` this makes before resting on, or bumping into, `o`. Solids already level with each other never stop it. */
    public double reach(Solid o,double dy){
        if(!planOverlaps(o))return dy;
        if(dy<0&&o.top<=bottom+E)return Math.max(dy,o.top-bottom);
        if(dy>0&&o.bottom>=top-E)return Math.min(dy,o.bottom-top);
        return dy;
    }
    /**
     * The horizontal move `m` with whatever would carry this into `o` taken off along the face it meets
     * first, so the rest of the move slides along that face. A solid already inside `o` moves freely, out.
     */
    public Vec3 slide(Solid o,Vec3 m){
        if(!(bottom<o.top-E&&top>o.bottom+E))return m;
        double first=-1,leave=Double.POSITIVE_INFINITY,ux=0,uz=0,push=0;
        for(var s:new Solid[]{this,o})for(int i=0;i<s.nx.length;i++){
            double ax=s.nx[i],az=s.nz[i],above=least(ax,az)+o.least(-ax,-az),below=o.least(ax,az)+least(-ax,-az);
            double side=above>=below?1:-1,gap=Math.max(above,below),closing=-side*(m.x*ax+m.z*az);
            if(gap<-E){if(closing<0)leave=Math.min(leave,gap/closing);continue;}
            gap=Math.max(gap,0);
            if(closing<=gap)return m;
            if(gap/closing>first){first=gap/closing;ux=side*ax;uz=side*az;push=closing-gap;}
        }
        return first<0||first>=leave?m:m.add(ux*push,0,uz*push);
    }
    /** Where the segment from `from` to `to` first meets this solid. */
    public Optional<Vec3> clip(Vec3 from,Vec3 to){
        Vec3 d=to.subtract(from);double[] t={0,1};
        if(!slab(from.y,d.y,bottom,top,t))return Optional.empty();
        for(int i=0;i<nx.length;i++)if(!slab(from.x*nx[i]+from.z*nz[i],d.x*nx[i]+d.z*nz[i],least(nx[i],nz[i]),-least(-nx[i],-nz[i]),t))return Optional.empty();
        return Optional.of(from.add(d.scale(t[0])));
    }
    /** Narrows the segment's span `t` to where its position `p + v t` lies between `lo` and `hi`. */
    private static boolean slab(double p,double v,double lo,double hi,double[] t){
        if(Math.abs(v)<1e-12)return p>=lo&&p<=hi;
        double a=(lo-p)/v,b=(hi-p)/v;
        t[0]=Math.max(t[0],Math.min(a,b));t[1]=Math.min(t[1],Math.max(a,b));
        return t[0]<=t[1];
    }
    private boolean planOverlaps(Solid o){
        for(var s:new Solid[]{this,o})for(int i=0;i<s.nx.length;i++){
            double ax=s.nx[i],az=s.nz[i];
            if(least(ax,az)+o.least(-ax,-az)>=-E||o.least(ax,az)+least(-ax,-az)>=-E)return false;
        }
        return true;
    }
    /** The least projection of the outline onto (ax, az); its negation along the reverse is the greatest. */
    private double least(double ax,double az){double v=Double.POSITIVE_INFINITY;for(int i=0;i<x.length;i++)v=Math.min(v,x[i]*ax+z[i]*az);return v;}
}
