package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Server-driven ground carrier. A standing passenger supplies input at the handles. */
public final class WheelbarrowEntity extends Entity implements GeoEntity {
    private static final EntityDataAccessor<Boolean> HELD=SynchedEntityData.defineId(WheelbarrowEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> TRAVEL=SynchedEntityData.defineId(WheelbarrowEntity.class,EntityDataSerializers.FLOAT);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private int forward,turn,lerpSteps;
    private long lastInput=Long.MIN_VALUE;
    private double targetX,targetY,targetZ;
    private float targetYaw;
    private float sampledTravel;
    public float previousTravel,previousTilt,tilt;
    public WheelbarrowEntity(EntityType<? extends WheelbarrowEntity> type,Level level){super(type,level);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(HELD,false);b.define(TRAVEL,0f);}
    public float travel(){return entityData.get(TRAVEL);}
    public float renderedTravel(){return level().isClientSide?sampledTravel:travel();}
    public boolean held(){return entityData.get(HELD);}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override public boolean isPickable(){return !isRemoved();}
    @Override public boolean canBeCollidedWith(){return isAlive();}
    @Override public boolean shouldRiderSit(){return false;}
    @Override public float maxUpStep(){return .6f;}
    @Override protected boolean canAddPassenger(Entity passenger){return passenger instanceof Player&&getPassengers().isEmpty();}
    // Deliberately retain Entity's null controlling-passenger contract: physics
    // stays on the server rather than accepting vanilla client vehicle movement.
    public void input(Player p,WheelbarrowPayload input){
        if(level().isClientSide||getFirstPassenger()!=p)return;
        if(input.release()){p.stopRiding();forward=turn=0;return;}
        forward=Integer.signum(input.forward());turn=Integer.signum(input.turn());lastInput=level().getGameTime();
    }
    @Override public InteractionResult interact(Player p,InteractionHand hand){
        if(isRemoved())return InteractionResult.FAIL;
        if(hand!=InteractionHand.MAIN_HAND||!p.getItemInHand(hand).isEmpty())return InteractionResult.PASS;
        if(level().isClientSide)return InteractionResult.SUCCESS;
        if(!accessible(p,getBoundingBox())||!level().mayInteract(p,blockPosition()))return InteractionResult.FAIL;
        if(isVehicle())return InteractionResult.FAIL;
        if(p.isShiftKeyDown()){
            var stack=WheelbarrowContent.ITEM.toStack();if(!p.addItem(stack))spawnAtLocation(stack);
            discard();return InteractionResult.CONSUME;
        }
        if(p.isPassenger()||p.isSpectator()||isInWater()||!level().noCollision(p,riderBox(position(),getYRot(),p)))return InteractionResult.FAIL;
        if(p.startRiding(this)){
            forward=turn=0;lastInput=Long.MIN_VALUE;
            p.displayClientMessage(Component.literal("Wheelbarrow: W/S push/back up · A/D turn · Sneak to release"),true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.FAIL;
    }
    public Vec3 gripPosition(Vec3 center,float yaw){return center.add(new Vec3(0,0,-1.4).yRot(-yaw*Mth.DEG_TO_RAD));}
    private AABB riderBox(Vec3 at,float yaw,Player p){var v=gripPosition(at,yaw);double r=p.getBbWidth()/2;return new AABB(v.x-r,v.y+.02,v.z-r,v.x+r,v.y+p.getBbHeight(),v.z+r);}
    @Override protected void positionRider(Entity passenger,MoveFunction move){if(hasPassenger(passenger)){var p=gripPosition(position(),getYRot());move.accept(passenger,p.x,p.y,p.z);}}
    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger){
        for(double side:new double[]{1,-1,0}){
            Vec3 at=position().add(new Vec3(side*.95,0,-1.4).yRot(-getYRot()*Mth.DEG_TO_RAD));
            var box=passenger.getBoundingBox().move(at.subtract(passenger.position()));
            if(level().hasChunkAt(BlockPos.containing(at))&&level().noCollision(passenger,box))return at;
        }
        return super.getDismountLocationForPassenger(passenger);
    }
    private AABB footprint(Vec3 at,float yaw){
        double a=yaw*Mth.DEG_TO_RAD,x=Math.abs(Math.cos(a))*.4+Math.abs(Math.sin(a))*1.08,z=Math.abs(Math.sin(a))*.4+Math.abs(Math.cos(a))*1.08;
        return new AABB(at.x-x,at.y,at.z-z,at.x+x,at.y+.95,at.z+z);
    }
    @Override protected AABB makeBoundingBox(){return footprint(position(),getYRot());}
    /** Small bounded footprint query; neither movement nor placement loads chunks. */
    public boolean accessible(Player p,AABB box){
        for(int x=Mth.floor(box.minX);x<=Mth.floor(box.maxX);x++)for(int z=Mth.floor(box.minZ);z<=Mth.floor(box.maxZ);z++){
            var pos=BlockPos.containing(x,box.minY,z);
            if(!level().hasChunkAt(pos)||!CivicAccess.allowed(level(),pos,p))return false;
        }
        return level().getWorldBorder().isWithinBounds(box);
    }
    @Override public void lerpTo(double x,double y,double z,float yaw,float pitch,int steps){targetX=x;targetY=y;targetZ=z;targetYaw=yaw;lerpSteps=Math.max(1,steps);}
    @Override public void tick(){
        super.tick();previousTravel=level().isClientSide?sampledTravel:travel();previousTilt=tilt;tilt=Mth.approach(tilt,held()?1:0,.2f);
        if(level().isClientSide){
            sampledTravel=travel();if(tickCount<=1)previousTravel=sampledTravel;
            if(lerpSteps>0){setPos(getX()+(targetX-getX())/lerpSteps,getY()+(targetY-getY())/lerpSteps,getZ()+(targetZ-getZ())/lerpSteps);setYRot(getYRot()+Mth.wrapDegrees(targetYaw-getYRot())/lerpSteps);lerpSteps--;}
            return;
        }
        Player p=getFirstPassenger() instanceof Player player?player:null;
        if(p!=null&&(!p.isAlive()||p.isSpectator()||isInWater()||!accessible(p,getBoundingBox())||!accessible(p,riderBox(position(),getYRot(),p)))){p.stopRiding();p=null;}
        entityData.set(HELD,p!=null);
        if(p==null||lastInput==Long.MIN_VALUE||level().getGameTime()-lastInput>10){forward=turn=0;}
        Vec3 before=position();float oldYaw=getYRot();
        float nextYaw=oldYaw+turn*3.5f;
        if(p!=null&&turn!=0){
            var rotated=footprint(position(),nextYaw);
            if(accessible(p,rotated)&&level().noCollision(this,rotated)&&accessible(p,riderBox(position(),nextYaw,p))&&level().noCollision(p,riderBox(position(),nextYaw,p))){setYRot(nextYaw);setBoundingBox(rotated);}
        }
        double speed=forward>0?.105:forward<0?-.055:0;
        Vec3 motion=new Vec3(0,Math.max(-.8,getDeltaMovement().y-.04),speed).yRot(-getYRot()*Mth.DEG_TO_RAD);
        var destination=getBoundingBox().expandTowards(motion);
        if(!accessible(p,destination)||(p!=null&&(!accessible(p,riderBox(position().add(motion.x,0,motion.z),getYRot(),p))||!level().noCollision(p,riderBox(position().add(motion.x,0,motion.z),getYRot(),p)))))motion=new Vec3(0,motion.y,0);
        if(level().hasChunkAt(blockPosition())){setDeltaMovement(motion);move(MoverType.SELF,motion);setDeltaMovement(0,onGround()?0:getDeltaMovement().y,0);}
        double distance=position().subtract(before).horizontalDistance();
        if(distance>0)entityData.set(TRAVEL,(float)(travel()+Math.copySign(distance,forward==0?1:forward)));
        if(p!=null)p.setYBodyRot(getYRot());
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag){tag.putFloat("Travel",travel());}
    @Override protected void readAdditionalSaveData(CompoundTag tag){float value=tag.getFloat("Travel");entityData.set(TRAVEL,Float.isFinite(value)?value:0f);forward=turn=0;lastInput=Long.MIN_VALUE;}
}
