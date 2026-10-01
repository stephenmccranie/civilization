package dev.civilization;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import static dev.civilization.IndustrialBlock.Kind;
/** Fixed hollow vessels, cut corners and accessible flanged connections. */
public final class IndustrialStructure {
    private static MachineStructure.Part p(int x,int y,int z,String material,int units){return new MachineStructure.Part(x,y,z,material,units,Direction.DOWN);}
    private static final List<MachineStructure.Part> PUMP=OilDerrickStructure.PARTS;
    private static final List<MachineStructure.Part> DRILL=List.of(p(-1,0,0,"stone",4),p(1,0,0,"stone",4),p(-1,1,0,"casing",4),p(1,1,0,"casing",4),p(-1,2,0,"casing",2),p(0,2,0,"casing",2),p(1,2,0,"casing",2),p(0,1,0,"casing",4));
    private static final List<MachineStructure.Part> HEATER=heater(),COLUMN=column(),CONDENSER=condenser();
    private static void replace(List<MachineStructure.Part> a,int x,int y,int z,String material,Direction side){a.removeIf(p->p.x()==x&&p.y()==y&&p.depth()==z);a.add(new MachineStructure.Part(x,y,z,material,4,side));}
    private static List<MachineStructure.Part> heater(){
        var a=new ArrayList<MachineStructure.Part>();
        // Broad brick hearth, stepped jacket, recessed service door and a narrow rear flue.
        for(int y=0;y<=3;y++)for(int z=0;z<=4;z++)for(int x=-2;x<=2;x++){
            if(x==0&&y==0&&z==0)continue;
            if(Math.abs(x)==2&&(z==0||z==4)){
                if(y==0)a.add(p(x,y,z,"brick",2));
                else if(y<3)a.add(new MachineStructure.Part(x,y,z,"casing",1,x<0?Direction.EAST:Direction.WEST,z==0?3:2));
                continue;
            }
            boolean inside=Math.abs(x)<2&&z>0&&z<4;
            a.add(p(x,y,z,inside&&y>0&&y<3?"air":y==0?"brick":"casing",y==3?2:4));
        }
        // A small raised crown gives the roof an arch rather than a flat box.
        for(int z=1;z<=3;z++){replace(a,0,3,z,"casing",Direction.DOWN);a.add(p(0,4,z,"casing",2));}
        replace(a,0,3,4,"casing",Direction.DOWN);
        for(int y=4;y<=7;y++)a.add(p(0,y,4,"flue",4));
        replace(a,-2,1,1,"input_port",Direction.WEST);replace(a,2,1,2,"output_port",Direction.EAST);
        return List.copyOf(a);
    }
    private static List<MachineStructure.Part> column(){
        var a=new ArrayList<MachineStructure.Part>();
        // Five-block diameter with clipped corners, hollow cross-shaped core and a tapered crown.
        for(int y=0;y<=10;y++)for(int z=0;z<=4;z++)for(int x=-2;x<=2;x++){
            if(x==0&&y==0&&z==0)continue;
            int dx=Math.abs(x),dz=Math.abs(z-2);
            if(dx==2&&dz==2)continue;
            if(y==0){a.add(p(x,y,z,"casing",4));continue;}
            if(y==10){if(dx<2&&dz<2)a.add(new MachineStructure.Part(x,y,z,"casing",dx+dz==2?3:2,Direction.DOWN,(x<0?1:0)+(z<2?2:0)));continue;}
            boolean core=dx+dz<2;
            if(core){a.add(p(x,y,z,y==9?"casing":"air",4));continue;}
            if(dx==2&&dz==1)a.add(new MachineStructure.Part(x,y,z,"casing",2,z<2?Direction.SOUTH:Direction.NORTH));
            else if(dz==2&&dx==1)a.add(new MachineStructure.Part(x,y,z,"casing",2,x<0?Direction.EAST:Direction.WEST));
            else a.add(p(x,y,z,"casing",4));
        }
        replace(a,-2,1,2,"input_port",Direction.WEST);replace(a,2,9,2,"output_port",Direction.EAST);replace(a,2,1,2,"aux_port",Direction.EAST);
        return List.copyOf(a);
    }
    private static List<MachineStructure.Part> condenser(){
        var a=new ArrayList<MachineStructure.Part>();
        // Long horizontal vessel, rounded half-grid ends and visible cooling grilles.
        for(int x=-3;x<=3;x++)for(int y=1;y<=3;y++)for(int z=1;z<=3;z++){
            boolean end=Math.abs(x)==3,corner=y!=2&&z!=2;
            String material=!end&&y==2&&z==2?"air":!end&&y==2&&z!=2?"cooling":"casing";
            if(corner)a.add(new MachineStructure.Part(x,y,z,material,1,y==1?Direction.UP:Direction.DOWN,z==1?3:2));
            else a.add(p(x,y,z,material,y!=2?2:4));
        }
        // The bottom half belongs at the top of its block to meet the vessel's middle course.
        for(int i=0;i<a.size();i++){var p=a.get(i);if(p.y()==1&&p.units()==2)a.set(i,new MachineStructure.Part(p.x(),p.y(),p.depth(),p.material(),2,Direction.UP));}
        for(int x:new int[]{-2,2})for(int z=1;z<=3;z++)a.add(p(x,0,z,"casing",4));
        // Solid saddles meet the rounded underside instead of stopping half a block short.
        for(int x:new int[]{-2,2})replace(a,x,1,2,"casing",Direction.DOWN);
        a.add(p(0,0,1,"casing",4));a.add(p(0,0,2,"casing",4));
        replace(a,-3,2,2,"input_port",Direction.WEST);replace(a,3,2,2,"output_port",Direction.EAST);
        return List.copyOf(a);
    }
    public static boolean remote(Kind k){return k==Kind.PUMP||k==Kind.REFINERY||k==Kind.COLUMN||k==Kind.CONDENSER;}
    public static List<MachineStructure.Part> parts(Kind kind){return switch(kind){case PUMP->PUMP;case REFINERY->HEATER;case DRILL->DRILL;case COLUMN->COLUMN;case CONDENSER->CONDENSER;case TANK->List.of();};}
    public static boolean complete(Level l,BlockPos at,Direction front,Kind kind){
        return MachineStructure.checkParts(l,at,front,parts(kind)).status()==MachineStructure.COMPLETE;
    }
    public static boolean bind(IndustrialBlockEntity m){
        return bindStatus(m)==MachineStructure.COMPLETE;
    }
    /** Keep unloaded shells distinct from broken ones without validating twice per work step. */
    public static int bindStatus(IndustrialBlockEntity m){
        var l=m.getLevel();if(l==null)return MachineStructure.UNLOADED;
        var structure=MachineStructure.checkParts(l,m.getBlockPos(),m.front(),parts(m.kind));
        if(structure.status()!=MachineStructure.COMPLETE)return structure.status();
        for(var part:parts(m.kind))if(part.material().endsWith("_port")){
            var at=MachineStructure.position(m.getBlockPos(),m.front(),part);
            if(!(l.getBlockEntity(at) instanceof RefineryPortEntity port)||!port.canBind(m))return MachineStructure.INCOMPLETE;
        }
        for(var part:parts(m.kind))if(part.material().endsWith("_port")){
            var port=(RefineryPortEntity)l.getBlockEntity(MachineStructure.position(m.getBlockPos(),m.front(),part));
            port.bind(m,part.material(),MachineStructure.side(part,m.front()));
        }
        return MachineStructure.COMPLETE;
    }
}
