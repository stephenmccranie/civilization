package dev.civilization;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** All geometry lives on the half-block grid: six slabs, twelve beams, eight cubes. */
public final class CutGeometry {
    private CutGeometry(){}
    private static int axis(Direction d){return d.getAxis()==Direction.Axis.X?0:d.getAxis()==Direction.Axis.Y?1:2;}
    public static AABB bounds(int units, Direction side, int corner) {
        double[] low={0,0,0},high={1,1,1};int a=axis(side);
        half(low,high,a,side.getAxisDirection()==Direction.AxisDirection.POSITIVE);
        int[] other=new int[2];int n=0;for(int i=0;i<3;i++)if(i!=a)other[n++]=i;
        if(units==1)half(low,high,other[corner/2],corner%2==1);
        if(units==3){half(low,high,other[0],(corner&1)!=0);half(low,high,other[1],(corner&2)!=0);}
        return new AABB(low[0],low[1],low[2],high[0],high[1],high[2]);
    }
    private static void half(double[] low,double[] high,int axis,boolean upper){if(upper)low[axis]=.5;else high[axis]=.5;}
    public static int corner(int units,Direction side,double x,double y,double z) {
        double[] p={x,y,z};int[] other=new int[2];int n=0;for(int i=0;i<3;i++)if(i!=axis(side))other[n++]=i;
        if(units==3)return(p[other[0]]>=.5?1:0)+(p[other[1]]>=.5?2:0);
        int nearest=Math.min(p[other[1]],1-p[other[1]])<Math.min(p[other[0]],1-p[other[0]])?1:0;
        return nearest*2+(p[other[nearest]]>=.5?1:0);
    }
    public static AABB joined(AABB box,Direction face) {
        double[] low={box.minX,box.minY,box.minZ},high={box.maxX,box.maxY,box.maxZ};int a=axis(face);
        if(high[a]-low[a]!=.5)return null;
        if(face.getAxisDirection()==Direction.AxisDirection.POSITIVE && high[a]!=.5)return null;
        if(face.getAxisDirection()==Direction.AxisDirection.NEGATIVE && low[a]!=.5)return null;
        low[a]=0;high[a]=1;return new AABB(low[0],low[1],low[2],high[0],high[1],high[2]);
    }
    public static BlockState state(AABB box) {
        for(int units:new int[]{2,1,3})for(Direction side:Direction.values())for(int corner=0;corner<4;corner++)
            if(bounds(units,side,corner).equals(box))return CuttingContent.PIECE.get().defaultBlockState().setValue(CutBlock.UNITS,units).setValue(CutBlock.SIDE,side).setValue(CutBlock.CORNER,corner);
        return null;
    }
    public static AABB rotate(AABB box,net.minecraft.world.level.block.Rotation rotation) {
        return switch(rotation){case NONE->box;case CLOCKWISE_90->new AABB(1-box.maxZ,box.minY,box.minX,1-box.minZ,box.maxY,box.maxX);case CLOCKWISE_180->new AABB(1-box.maxX,box.minY,1-box.maxZ,1-box.minX,box.maxY,1-box.minZ);case COUNTERCLOCKWISE_90->new AABB(box.minZ,box.minY,1-box.maxX,box.maxZ,box.maxY,1-box.minX);};
    }
}
