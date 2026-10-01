package dev.civilization;

import java.util.*;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.platform.SableAssemblyPlatform;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

/** Validate the whole destination, copy successfully, then remove the original vessel. */
final class AirshipDisassembly {
    private record BlockCopy(BlockPos source,BlockPos target,BlockState state,CompoundTag tag,BlockState[] cells) {}
    static BlockPos disassemble(ServerSubLevel ship,ServerPlayer player){
        var level=ship.getLevel();var anchor=AirshipSystem.controller(ship);
        if(ship.isRemoved()||!AirshipSystem.access(player,anchor))throw new IllegalStateException("Stand near your controller to disassemble.");
        if(AirshipSystem.hasPilot(ship))throw new IllegalStateException("Leave the pilot controls before disassembling.");
        var body=RigidBodyHandle.of(ship);
        if(body==null||!body.isValid()||!body.getLinearVelocity(new Vector3d()).isFinite()
                ||body.getLinearVelocity(new Vector3d()).length()>.5||!body.getAngularVelocity(new Vector3d()).isFinite()
                ||body.getAngularVelocity(new Vector3d()).length()>.05)
            throw new IllegalStateException("Stop the ship and let it settle before disassembling.");
        var up=ship.logicalPose().transformNormal(new Vector3d(0,1,0),new Vector3d());
        if(!up.isFinite()||up.y<.99)throw new IllegalStateException("Let the ship level out before disassembling.");
        var world=AirshipSystem.world(ship,anchor);
        if(!Double.isFinite(world.x)||!Double.isFinite(world.y)||!Double.isFinite(world.z))throw new IllegalStateException("Invalid ship position.");
        var target=BlockPos.containing(world);
        var axis=ship.logicalPose().transformNormal(new Vector3d(1,0,0),new Vector3d());
        int quarter=Math.floorMod((int)Math.round(Math.atan2(-axis.z,axis.x)/(Math.PI/2)),4);
        var rotation=new Rotation[]{Rotation.NONE,Rotation.COUNTERCLOCKWISE_90,Rotation.CLOCKWISE_180,Rotation.CLOCKWISE_90}[quarter];
        var transform=new SubLevelAssemblyHelper.AssemblyTransform(anchor,target,quarter,rotation,level);
        var copies=new ArrayList<BlockCopy>();var bounds=ship.getPlot().getBoundingBox();
        if((long)(bounds.maxX()-bounds.minX()+1)*(bounds.maxY()-bounds.minY()+1)*(bounds.maxZ()-bounds.minZ()+1)>32768)
            throw new IllegalStateException("Ship bounds exceed the prototype disassembly limit.");
        for(var mutable:BlockPos.betweenClosed(bounds.minX(),bounds.minY(),bounds.minZ(),bounds.maxX(),bounds.maxY(),bounds.maxZ())){
            var source=mutable.immutable();var state=level.getBlockState(source);if(state.isAir())continue;
            if(copies.size()>=AirshipSystem.MAX_BLOCKS)throw new IllegalStateException("Ship exceeds the prototype block limit.");
            var destination=transform.apply(source);
            if(SubLevelContainer.getContainer(level).inBounds(destination)||level.isOutsideBuildHeight(destination)||!level.getWorldBorder().isWithinBounds(destination))
                throw new IllegalStateException("The ship would extend outside the buildable world.");
            if(!level.hasChunkAt(destination))throw new IllegalStateException("Load the whole destination before disassembling.");
            if(!CivicAccess.allowed(level,destination,player))throw new IllegalStateException("The destination crosses protected land.");
            if(!level.getBlockState(destination).isAir())throw new IllegalStateException("Destination obstructed. Move the ship into clear space.");
            var be=level.getBlockEntity(source);CompoundTag tag=be==null?null:be.saveWithFullMetadata(level.registryAccess());
            if(state.hasBlockEntity()&&be==null)throw new IllegalStateException("A ship block entity is not ready.");
            BlockState[] cells=be instanceof CutBlockEntity cut&&cut.hasCells()?rotateCells(cut.cells(),quarter):null;
            copies.add(new BlockCopy(source,destination,state.rotate(rotation),tag,cells));
        }
        if(copies.isEmpty())throw new IllegalStateException("The ship has no blocks to disassemble.");
        // Suppress placement callbacks until every supporting block and inventory has been copied.
        var placed=new ArrayList<BlockCopy>();SableAssemblyPlatform.INSTANCE.setIgnoreOnPlace(level,true);
        try{
            for(var copy:copies){
                placed.add(copy);level.getChunkAt(copy.target).setBlockState(copy.target,copy.state,true);
                if(level.getBlockState(copy.target)!=copy.state)throw new IllegalStateException("Could not place the complete ship.");
                if(copy.tag!=null){
                    var be=level.getBlockEntity(copy.target);if(be==null)throw new IllegalStateException("Could not restore a ship inventory.");
                    var tag=copy.tag.copy();tag.putInt("x",copy.target.getX());tag.putInt("y",copy.target.getY());tag.putInt("z",copy.target.getZ());
                    be.loadWithComponents(tag,level.registryAccess());
                    if(copy.cells!=null)((CutBlockEntity)be).cells(copy.cells);
                    be.setChanged();
                }
            }
        }catch(RuntimeException ex){
            for(var copy:placed){level.removeBlockEntity(copy.target);level.getChunkAt(copy.target).setBlockState(copy.target,Blocks.AIR.defaultBlockState(),true);level.sendBlockUpdated(copy.target,copy.state,Blocks.AIR.defaultBlockState(),3);}
            throw new IllegalStateException("Disassembly failed; the original ship was retained.",ex);
        }finally{SableAssemblyPlatform.INSTANCE.setIgnoreOnPlace(level,false);}
        AirshipSystem.forget(ship);
        SubLevelAssemblyHelper.moveTrackingPoints(level,bounds,null,transform);
        // Sable moves plot-local entities back into world coordinates before removing the plot.
        ship.deleteAllEntities();
        SableAssemblyPlatform.INSTANCE.setIgnoreOnPlace(level,true);
        try{for(var copy:copies){level.removeBlockEntity(copy.source);level.getChunkAt(copy.source).setBlockState(copy.source,Blocks.AIR.defaultBlockState(),true);}}
        finally{SableAssemblyPlatform.INSTANCE.setIgnoreOnPlace(level,false);}
        SubLevelContainer.getContainer(level).removeSubLevel(ship,SubLevelRemovalReason.REMOVED);
        for(var copy:copies)SubLevelAssemblyHelper.markAndNotifyBlock(level,copy.target,level.getChunkAt(copy.target),Blocks.AIR.defaultBlockState(),copy.state,3,512);
        return target;
    }
    static BlockState[] rotateCells(BlockState[] source,int quarter){
        var result=new BlockState[8];
        for(int i=0;i<8;i++){int x=i&1,y=(i>>1)&1,z=(i>>2)&1;for(int q=0;q<quarter;q++){int old=x;x=z;z=1-old;}result[x|(y<<1)|(z<<2)]=source[i];}
        return result;
    }
}
