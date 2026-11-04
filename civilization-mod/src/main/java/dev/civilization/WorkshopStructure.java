package dev.civilization;

import java.util.*;
import net.minecraft.core.Direction;
import static dev.civilization.MachineStructure.Part;

/** Open workstations built from blocks: a low tanning vat and an upright textile frame. The Smithy is a native model. */
public final class WorkshopStructure {
    private static final List<List<Part>> PARTS=List.of(tannery(),textile());
    public static List<Part> parts(int kind){return kind<PARTS.size()?PARTS.get(kind):List.of();}
    private static List<Part> tannery(){
        var a=new ArrayList<Part>();
        // Cobblestone bears the vat; two front bricks mark the fired working face.
        for(int z=0;z<3;z++)for(int x=-1;x<=1;x++)if(x!=0||z!=0)
            a.add(new Part(x,0,z,z==0?"brick":"stone",4,Direction.DOWN));
        // Thin oak work rim around a genuinely open vat.
        for(int x=-1;x<=1;x++){
            a.add(new Part(x,1,0,"planks",2,Direction.DOWN));
            var rim=new Part(x,1,2,"planks",2,Direction.DOWN);
            if(x==0)a.add(rim);
            else {
                var support=new Part(x,1,2,"planks",3,Direction.UP,x<0?1:0);
                int mask=CutCells.mask(CutBlock.bounds(MachineStructure.shape(rim,Direction.NORTH)))
                        |CutCells.mask(CutBlock.bounds(MachineStructure.shape(support,Direction.NORTH)));
                a.add(rim.shared(mask));a.add(support.shared(mask));
            }
        }
        for(int x:new int[]{-1,1})a.add(new Part(x,1,1,"planks",2,x<0?Direction.EAST:Direction.WEST));
        a.add(new Part(0,1,1,"air",4,Direction.DOWN));
        // Narrow rear drying rack: two supported posts and a half-grid crossbar.
        for(int x:new int[]{-1,1})
            a.add(new Part(x,2,2,"planks",1,x<0?Direction.EAST:Direction.WEST,2));
        for(int x=-1;x<=1;x++)a.add(new Part(x,3,2,"planks",1,Direction.DOWN,2));
        return List.copyOf(a);
    }
    private static List<Part> textile(){
        var a=new ArrayList<Part>();
        // Thin two-deep workbench. Four eighth-block feet share cells with the upper slab.
        for(int z=0;z<=1;z++)for(int x=-1;x<=1;x++){
            if(x==0&&z==0)continue;
            var top=new Part(x,0,z,"planks",2,Direction.UP);
            if(x==0){a.add(top);continue;}
            var foot=new Part(x,0,z,"planks",3,Direction.DOWN,(x<0?0:1)+(z==0?0:2));
            int mask=CutCells.mask(CutBlock.bounds(MachineStructure.shape(foot,Direction.NORTH)))
                    |CutCells.mask(CutBlock.bounds(MachineStructure.shape(top,Direction.NORTH)));
            a.add(foot.shared(mask));a.add(top.shared(mask));
        }
        // Open rear loom frame. The restrained copper roller spans the clear center cell.
        for(int x:new int[]{-1,1})for(int y=0;y<=2;y++)
            a.add(new Part(x,y,2,"planks",1,x<0?Direction.EAST:Direction.WEST,2));
        for(int x=-1;x<=1;x++)a.add(new Part(x,3,2,"planks",1,Direction.DOWN,2));
        a.add(new Part(0,2,2,"copper",1,Direction.DOWN,2));
        a.add(new Part(0,1,2,"air",4,Direction.DOWN));
        return List.copyOf(a);
    }
}
