package dev.civilization;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Transient, invisible attachment in ship-local coordinates. Sable transforms its rider. */
public final class AirshipPilotSeat extends Entity {
    public AirshipPilotSeat(EntityType<? extends AirshipPilotSeat> type,Level level){super(type,level);setNoGravity(true);noPhysics=true;}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){}
    @Override protected void readAdditionalSaveData(CompoundTag tag){}
    @Override protected void addAdditionalSaveData(CompoundTag tag){}
    @Override public void tick(){super.tick();setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);if(!level().isClientSide&&tickCount>20&&getPassengers().isEmpty())discard();}
    @Override protected void positionRider(Entity passenger,MoveFunction move){if(hasPassenger(passenger))move.accept(passenger,getX(),getY(),getZ());}
    @Override public boolean isPickable(){return false;}
    @Override public boolean isPushable(){return false;}
    @Override public boolean shouldRiderSit(){return false;}
}
