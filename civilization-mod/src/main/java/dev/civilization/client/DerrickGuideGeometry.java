package dev.civilization.client;

import dev.civilization.ModeledDerrick;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

/** Immutable guide snapshot. Combine bounds, never voxel unions, for preview outlines. */
public record DerrickGuideGeometry(AABB area,List<Cell> cells,int[] materials) {
    public record Cell(int index,BlockPos pos,AABB box) {}
    public static DerrickGuideGeometry build(BlockPos at,Direction front,int built){
        var area=new AABB(at);var cells=new ArrayList<Cell>();int missing=ModeledDerrick.ALL^built;
        for(int i=0;i<ModeledDerrick.CELLS.size();i++){
            var cell=ModeledDerrick.CELLS.get(i);var pos=ModeledDerrick.position(at,front,cell);
            area=area.minmax(new AABB(pos));AABB bounds=null;
            for(var entry:cell.pieces().entrySet())if((missing&(1<<entry.getKey()))!=0){
                var box=entry.getValue()[front.get2DDataValue()].bounds();
                bounds=bounds==null?box:bounds.minmax(box);
            }
            if(bounds!=null)cells.add(new Cell(i,pos,bounds.move(pos)));
        }
        int[] materials=new int[4];
        for(int part=0;part<ModeledDerrick.PARTS;part++)if((missing&(1<<part))!=0)materials[ModeledDerrick.material(part)]+=ModeledDerrick.cost(part);
        return new DerrickGuideGeometry(area,List.copyOf(cells),materials);
    }
}
