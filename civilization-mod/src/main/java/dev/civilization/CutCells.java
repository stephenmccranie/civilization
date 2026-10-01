package dev.civilization;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.*;

/** Eight half-grid cells; null means empty. Arrays passed to model data are immutable snapshots. */
public final class CutCells {
    public static AABB box(int i) { double x=(i&1)*.5,y=((i>>1)&1)*.5,z=((i>>2)&1)*.5;return new AABB(x,y,z,x+.5,y+.5,z+.5); }
    public static int mask(AABB box) {int mask=0;for(int i=0;i<8;i++)if(box.contains(box(i).getCenter()))mask|=1<<i;return mask;}
    public static int mask(BlockState[] cells) {int mask=0;for(int i=0;i<8;i++)if(cells[i]!=null)mask|=1<<i;return mask;}
    public static BlockState[] filled(AABB box,BlockState material) {var cells=new BlockState[8];int mask=mask(box);for(int i=0;i<8;i++)if((mask&(1<<i))!=0)cells[i]=material;return cells;}
    public static BlockState[] read(BlockGetter level,BlockPos pos) {
        var state=level.getBlockState(pos);
        if(level.getBlockEntity(pos) instanceof CutBlockEntity cut)return cut.cells();
        if(state.getBlock() instanceof SlabBlock && state.getValue(SlabBlock.TYPE)!=SlabType.DOUBLE)
            return filled(state.getShape(level,pos).bounds(),SlabIntegration.material(state));
        return new BlockState[8];
    }
    public static VoxelShape shape(BlockState[] cells) {VoxelShape shape=Shapes.empty();for(int i=0;i<8;i++)if(cells[i]!=null)shape=Shapes.or(shape,Shapes.create(box(i)));return shape;}
    /** Lighting workers need this state-only summary; they cannot rely on block-entity cells. */
    public static int opaqueFaces(BlockState[] cells) {
        int mask=0;
        for(var face:Direction.values()){
            boolean full=true;
            for(int i=0;i<8;i++){
                boolean boundary=switch(face){
                    case WEST -> (i&1)==0; case EAST -> (i&1)!=0;
                    case DOWN -> (i&2)==0; case UP -> (i&2)!=0;
                    case NORTH -> (i&4)==0; case SOUTH -> (i&4)!=0;
                };
                if(boundary && (cells[i]==null || !cells[i].canOcclude())){full=false;break;}
            }
            if(full)mask|=1<<face.ordinal();
        }
        return mask;
    }
    public static BlockState material(BlockState[] cells) {for(var cell:cells)if(cell!=null)return cell;return Blocks.COBBLESTONE.defaultBlockState();}
    /** A single rectangle stays compatible with native slabs and existing multiblock guides. */
    public static BlockState canonical(BlockState[] cells) {
        var material=material(cells);for(var cell:cells)if(cell!=null && !cell.equals(material))return null;
        int mask=mask(cells);if(mask==255)return material;
        for(int units:new int[]{2,1,3})for(Direction side:Direction.values())for(int corner=0;corner<4;corner++) {
            var box=CutGeometry.bounds(units,side,corner);if(mask(box)!=mask)continue;
            var state=CutGeometry.state(box);var slab=SlabIntegration.slab(material.getBlock());
            if(units==2 && side.getAxis().isVertical() && slab!=null)return slab.defaultBlockState().setValue(SlabBlock.TYPE,side==Direction.DOWN?SlabType.BOTTOM:SlabType.TOP);
            return state;
        }
        return null;
    }
    /** Greedy lossless decomposition into canonical inventory pieces. */
    public static List<ItemStack> drops(BlockState[] original) {
        var cells=original.clone();var drops=new ArrayList<ItemStack>();
        for(int units:new int[]{2,1,3})for(Direction side:Direction.values())for(int corner=0;corner<4;corner++) {
            int mask=mask(CutGeometry.bounds(units,side,corner));BlockState material=null;boolean fits=true;
            for(int i=0;i<8;i++)if((mask&(1<<i))!=0){if(cells[i]==null || material!=null && !material.equals(cells[i])){fits=false;break;}material=cells[i];}
            if(!fits)continue;drops.add(CuttingContent.stack(material,units,1));for(int i=0;i<8;i++)if((mask&(1<<i))!=0)cells[i]=null;
        }
        return drops;
    }
}
