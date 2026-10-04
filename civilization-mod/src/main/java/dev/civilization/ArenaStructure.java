package dev.civilization;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;

public final class ArenaStructure {
    public static final List<MachineStructure.Part> PARTS;
    static {
        var parts=new ArrayList<MachineStructure.Part>();
        for(var p:ArenaLayout.PIECES) {
            parts.add(new MachineStructure.Part(p.x(),p.y(),p.z(),p.gate()?"arena_gate":"arena_stone",p.slab()?2:4,p.gate()?Direction.EAST:Direction.DOWN));
        }
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
        return (fightingOnly?ArenaLayout.FIGHTING:ArenaLayout.FLOOR).contains(c);
    }
    public static String problem(Level level,ArenaData.Pit pit) {
        if(level instanceof net.minecraft.server.level.ServerLevel server&&(AirshipSystem.at(server,pit.at.pos())!=null||BoatSystem.at(server,pit.at.pos())!=null))return "A gladiator pit must be built on land";
        if(!level.hasChunkAt(pit.at.pos()))return "Arena controller is unloaded";
        if(!level.getBlockState(pit.at.pos()).is(ArenaContent.PIT.get()))return "Arena controller is missing";
        for(var part:PARTS) {
            var pos=MachineStructure.position(pit.at.pos(),pit.front,part);
            if(!level.hasChunkAt(pos))return "Arena structure is unloaded";
            if(!MachineStructure.matches(level,pos,part,pit.front))return "Finish or repair the arena walls, rooms and gates";
        }
        return "";
    }
    /** Choose a clear, dry standing position below the prep roof, nearest the original floor. */
    public static Vec3 prepPosition(net.minecraft.server.level.ServerPlayer player,ArenaData.Pit pit,int side) {
        var level=player.serverLevel();int sign=side==0?-1:1;
        for(int distance=0;distance<pit.at.pos().getY()+2-level.getMinBuildHeight();distance++)
            for(int dy:distance==0?new int[]{0}:new int[]{distance,-distance}) {
                int y=pit.at.pos().getY()+1+dy;if(y>pit.at.pos().getY()+2||y<=level.getMinBuildHeight())continue;
                for(int dx:new int[]{17,16,18,15,19})for(int z:new int[]{0,-1,1,-2,2}) {
                    var feet=position(pit.at.pos(),pit.front,sign*dx,y-pit.at.pos().getY(),z);
                    if(!level.hasChunkAt(feet))continue;
                    var support=level.getBlockState(feet.below());
                    var target=Vec3.atBottomCenterOf(feet);
                    var box=player.getBoundingBox().move(target.subtract(player.position()));
                    if(support.isFaceSturdy(level,feet.below(),Direction.UP)&&!support.is(Blocks.MAGMA_BLOCK)
                            &&level.noCollision(player,box)&&!level.containsAnyLiquid(box)
                            &&!level.getBlockState(feet).is(Blocks.FIRE)&&!level.getBlockState(feet).is(Blocks.SOUL_FIRE))return target;
                }
            }
        return null;
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
