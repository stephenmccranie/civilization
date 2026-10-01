package dev.civilization;

import java.util.*;
import net.minecraft.core.Direction;
import static dev.civilization.MachineStructure.Part;

/** Open workstations: a low tanning vat, upright textile frame and hearth with side bench. */
public final class WorkshopStructure {
    public static final int SMITHY_ANVIL_X=3;
    private static final List<List<Part>> PARTS=List.of(build(0),build(1),build(2));
    public static List<Part> parts(int kind){return PARTS.get(kind);}
    private static List<Part> build(int kind){
        if(kind==2)return smithy();
        return kind==0?tannery():textile();
    }
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
    private static List<Part> smithy(){
        var a=new ArrayList<Part>();
        // Two-by-two hearth with a recessed, framed rear facade.
        for(int x=-1;x<=2;x++)for(int z=0;z<2;z++)if(x!=0||z!=0)
            a.add(new Part(x,0,z,"brick",z==0&&(x==-1||x==2)?2:4,x==-1?Direction.EAST:Direction.WEST));
        for(int x=0;x<2;x++)for(int z=0;z<2;z++)a.add(new Part(x,1,z,"air",4,Direction.DOWN));
        for(int x=0;x<2;x++){
            a.add(new Part(x,0,2,"brick",4,Direction.DOWN));
            a.add(new Part(x,1,2,"brick",2,Direction.NORTH));
            a.add(new Part(x,2,2,"brick",2,Direction.DOWN));
        }
        for(int x:new int[]{-1,2}){
            // Half-width, half-depth piers; the hood overhang is only half a block.
            a.add(new Part(x,1,0,"brick",1,x<0?Direction.EAST:Direction.WEST,3));
            a.add(new Part(x,2,0,"brick",3,Direction.DOWN,x<0?3:2));
            a.add(new Part(x,2,1,"brick",1,Direction.DOWN,x<0?1:0));
            a.add(new Part(x,1,1,"brick",2,x<0?Direction.EAST:Direction.WEST));
            a.add(new Part(x,0,2,"brick",2,x<0?Direction.EAST:Direction.WEST));
            a.add(new Part(x,1,2,"brick",1,x<0?Direction.EAST:Direction.WEST,2));
            a.add(new Part(x,2,2,"brick",3,Direction.DOWN,x<0?1:0));
        }
        for(int x=0;x<2;x++){
            a.add(new Part(x,2,0,"brick",2,Direction.DOWN));
            a.add(new Part(x,2,1,"brick",4,Direction.DOWN));
            // Paired vertical slabs center a one-block-wide chimney over a two-block hearth.
            a.add(new Part(x,3,1,"brick",2,x==0?Direction.EAST:Direction.WEST));
            a.add(new Part(x,4,1,"brick",1,Direction.DOWN,x==0?1:0));
        }
        // One block high, two wide and 1.5 deep. Feet share cells with the upper tabletop.
        for(int x=3;x<=4;x++)for(int z=1;z<=2;z++){
            var foot=new Part(x,0,z,"planks",3,Direction.DOWN,x==3?0:1);
            var top=new Part(x,0,z,"planks",z==1?2:1,Direction.UP,2);
            int mask=CutCells.mask(CutBlock.bounds(MachineStructure.shape(foot,Direction.NORTH)))
                    |CutCells.mask(CutBlock.bounds(MachineStructure.shape(top,Direction.NORTH)));
            a.add(foot.shared(mask));a.add(top.shared(mask));
        }
        a.add(new Part(SMITHY_ANVIL_X,-1,0,"stone",4,Direction.DOWN));
        a.add(new Part(SMITHY_ANVIL_X,0,0,"anvil",4,Direction.DOWN));
        a.add(new Part(SMITHY_ANVIL_X,1,0,"air",4,Direction.DOWN));
        return List.copyOf(a);
    }
}
