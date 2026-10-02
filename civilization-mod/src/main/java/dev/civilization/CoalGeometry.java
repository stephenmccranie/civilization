package dev.civilization;

import java.util.*;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.*;

/** Quarter-block cells. Stock and collision share one immutable mask. */
public final class CoalGeometry {
    public static final long FULL=-1L;
    private static final Map<Long,VoxelShape> SHAPES=Collections.synchronizedMap(new LinkedHashMap<>(128,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<Long,VoxelShape> e){return size()>512;}
    });
    public static int index(int x,int y,int z){return x|(y<<2)|(z<<4);}
    public static boolean occupied(long mask,int x,int y,int z){return x>=0&&x<4&&y>=0&&y<4&&z>=0&&z<4&&(mask&(1L<<index(x,y,z)))!=0;}
    public static AABB box(int i){int x=i&3,y=(i>>2)&3,z=(i>>4)&3;return new AABB(x*.25,y*.25,z*.25,(x+1)*.25,(y+1)*.25,(z+1)*.25);}
    public static VoxelShape shape(long mask){
        return SHAPES.computeIfAbsent(mask,m->{var shape=Shapes.empty();for(int i=0;i<64;i++)if((m&(1L<<i))!=0)shape=Shapes.or(shape,Shapes.create(box(i)));return shape.optimize();});
    }
    /** One exposed 2x2 patch, one cell deep. Never remove coal behind a hole. */
    public static long chip(long mask,Vec3 contact,Direction face){
        return CoalChipping.chip(mask,contact.x,contact.y,contact.z,face.getAxis().ordinal(),face.getAxisDirection()==Direction.AxisDirection.POSITIVE?1:-1);
    }
    private CoalGeometry(){}
}
