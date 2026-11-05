package dev.civilization;

import java.util.*;
import net.minecraft.core.Direction;
import static dev.civilization.MachineStructure.Part;

/** The Textile Workshop's open workstation, built from blocks: a low bench and an upright loom frame. The Tannery and
 * Smithy are native models. */
public final class WorkshopStructure {
    private static final List<Part> TEXTILE=textile();
    public static List<Part> parts(int kind){return kind==1?TEXTILE:List.of();}
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
