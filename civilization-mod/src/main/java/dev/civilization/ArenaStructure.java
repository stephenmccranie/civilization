package dev.civilization;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;

public final class ArenaStructure {
    public static final List<MachineStructure.Part> PARTS;
    static {
        var parts=new ArrayList<MachineStructure.Part>(); var occupied=new HashSet<BlockPos>();
        for(var p:ArenaLayout.PIECES) {
            parts.add(new MachineStructure.Part(p.x(),p.y(),p.z(),p.gate()?"arena_gate":"arena_stone",p.slab()?2:4,p.gate()?Direction.EAST:Direction.DOWN));
            occupied.add(new BlockPos(p.x(),p.y(),p.z()));
        }
        for(var c:ArenaLayout.FLOOR) for(int y=1;y<=3;y++)
            if(!occupied.contains(new BlockPos(c.x(),y,c.z()))) parts.add(new MachineStructure.Part(c.x(),y,c.z(),"air",4,Direction.DOWN));
        PARTS=List.copyOf(parts);
    }
    public static BlockPos position(BlockPos at,Direction front,int x,int y,int z) {
        return MachineStructure.position(at,front,new MachineStructure.Part(x,y,z,"air",4,Direction.DOWN));
    }
    public static ArenaLayout.Cell cell(BlockPos at,Direction front,Vec3 point) {
        var right=front.getClockWise();var back=front.getOpposite();double dx=point.x-at.getX()-.5,dz=point.z-at.getZ()-.5;
        return new ArenaLayout.Cell((int)Math.floor(dx*right.getStepX()+dz*right.getStepZ()+.5),
                (int)Math.floor(dx*back.getStepX()+dz*back.getStepZ()+.5));
    }
    public static boolean inside(ArenaData.Pit pit,Vec3 point,boolean fightingOnly) {
        var c=cell(pit.at.pos(),pit.front,point);
        return point.y>=pit.at.pos().getY()+1 && point.y<pit.at.pos().getY()+5
                && (fightingOnly?ArenaLayout.FIGHTING:ArenaLayout.FLOOR).contains(c);
    }
    public static String problem(Level level,ArenaData.Pit pit) {
        if(level instanceof net.minecraft.server.level.ServerLevel server&&(AirshipSystem.at(server,pit.at.pos())!=null||BoatSystem.at(server,pit.at.pos())!=null))return "A gladiator pit must be built on land";
        for(var c:ArenaLayout.FLOOR) {
            var pos=position(pit.at.pos(),pit.front,c.x(),0,c.z());
            if(!level.hasChunkAt(pos)) return "Arena terrain is unloaded";
            var state=level.getBlockState(pos);
            if(!state.isCollisionShapeFullBlock(level,pos) || !level.getFluidState(pos).isEmpty() || state.is(Blocks.MAGMA_BLOCK)) return "Prepare a solid, level floor";
        }
        for(var part:PARTS) {
            var pos=MachineStructure.position(pit.at.pos(),pit.front,part);
            if(!level.hasChunkAt(pos))return "Arena structure is unloaded";
            if(!MachineStructure.matches(level,pos,part,pit.front))return part.material().equals("air")?"Clear the arena and prep rooms":"Finish or repair the arena walls, rooms and gates";
        }
        return "";
    }
    public static void gates(Level level,ArenaData.Pit pit,boolean open) {
        for(var p:ArenaLayout.GATES) {
            var at=position(pit.at.pos(),pit.front,p.x(),p.y(),p.z());
            if(level.hasChunkAt(at)&&level.getBlockState(at).is(Blocks.OAK_FENCE_GATE)
                    &&(level.getBlockState(at).getValue(FenceGateBlock.OPEN)!=open||level.getBlockState(at).getValue(FenceGateBlock.POWERED)))
                level.setBlockAndUpdate(at,level.getBlockState(at).setValue(FenceGateBlock.OPEN,open).setValue(FenceGateBlock.POWERED,false));
        }
    }
    private ArenaStructure() {}
}
