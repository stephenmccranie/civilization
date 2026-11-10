package dev.civilization;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import static dev.civilization.IndustrialBlock.Kind;
/** Each industrial machine's required parts: a native model's ports, and the Coal Drill's frame of stone and casing. */
public final class IndustrialStructure {
    private static MachineStructure.Part p(int x,int y,int z,String material,int units){return new MachineStructure.Part(x,y,z,material,units,Direction.DOWN);}
    /** A native model's structure is the ports its sections place. */
    private static List<MachineStructure.Part> ports(MachineModel model){return model.ports.stream().map(MachineModel.Port::part).toList();}
    private static final List<MachineStructure.Part> DERRICK=ports(MachineModel.OIL_DERRICK),HEATER=ports(MachineModel.FIRED_HEATER),COLUMN=ports(MachineModel.DISTILLATION_COLUMN),CONDENSER=ports(MachineModel.CONDENSER);
    private static final List<MachineStructure.Part> DRILL=List.of(p(-1,0,0,"stone",4),p(1,0,0,"stone",4),p(-1,1,0,"casing",4),p(1,1,0,"casing",4),p(-1,2,0,"casing",2),p(0,2,0,"casing",2),p(1,2,0,"casing",2),p(0,1,0,"casing",4));
    public static boolean remote(Kind k){return k==Kind.DERRICK||k==Kind.HEATER||k==Kind.COLUMN||k==Kind.CONDENSER;}
    public static List<MachineStructure.Part> parts(Kind kind){return switch(kind){case DERRICK->DERRICK;case HEATER->HEATER;case DRILL->DRILL;case COLUMN->COLUMN;case CONDENSER->CONDENSER;case TANK,COAL_ENGINE->List.of();};}
    public static boolean complete(Level l,BlockPos at,Direction front,Kind kind){
        var modeled=ModeledMachine.of(l.getBlockEntity(at));
        return (modeled==null||ModeledMachine.status(modeled)==MachineStructure.COMPLETE)&&MachineStructure.checkParts(l,at,front,parts(kind)).status()==MachineStructure.COMPLETE;
    }
    public static boolean bind(IndustrialBlockEntity m){
        return bindStatus(m)==MachineStructure.COMPLETE;
    }
    /** Keep unloaded shells distinct from broken ones without validating twice per work step. */
    public static int bindStatus(IndustrialBlockEntity m){
        var l=m.getLevel();if(l==null)return MachineStructure.UNLOADED;
        if(m.model()!=null){int state=ModeledMachine.status(m);if(state!=MachineStructure.COMPLETE)return state;}
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
