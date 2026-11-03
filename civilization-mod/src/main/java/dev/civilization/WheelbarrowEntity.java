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
public final class WheelbarrowEntity extends Entity implements GeoEntity,CargoHolder,net.minecraft.world.MenuProvider {
    public static final float MODEL_SCALE=1.35f;
    public static final float PUSH_TILT_DEGREES=4;
    public static final float WHEEL_RADIUS=4.1411056f/16*MODEL_SCALE;
    private static final EntityDataAccessor<Boolean> HELD=SynchedEntityData.defineId(WheelbarrowEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> TRAVEL=SynchedEntityData.defineId(WheelbarrowEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEADING=SynchedEntityData.defineId(WheelbarrowEntity.class,EntityDataSerializers.FLOAT);
    private final WheelbarrowCollision collision=new WheelbarrowCollision();
    private static final EntityDataAccessor<net.minecraft.world.item.ItemStack> CARGO_ICON=SynchedEntityData.defineId(WheelbarrowEntity.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Float> CARGO_FILL=SynchedEntityData.defineId(WheelbarrowEntity.class,EntityDataSerializers.FLOAT);
    private final MassCargo cargo=new MassCargo(()->CargoConfig.WHEELBARROW.get(),this::cargoChanged);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private int forward,turn,lerpSteps;
    private long lastInput=Long.MIN_VALUE;
    private double targetX,targetY,targetZ;
    private float lastHeading=Float.NaN;
    private int headingSteps;
    private float sampledTravel;
    public float previousTravel,previousTilt,tilt;
    public final WalkAnimationState handlerWalkAnimation=new WalkAnimationState();
    public WheelbarrowEntity(EntityType<? extends WheelbarrowEntity> type,Level level){super(type,level);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(HELD,false);b.define(TRAVEL,0f);b.define(HEADING,0f);b.define(CARGO_ICON,net.minecraft.world.item.ItemStack.EMPTY);b.define(CARGO_FILL,0f);}
    public float steeringYaw(){return level().isClientSide?entityData.get(HEADING):getYRot();}
    public net.minecraft.world.phys.shapes.VoxelShape playerCollisionShape(){return collision.at(position(),getYRot());}
    private void cargoChanged(){
        if(level().isClientSide)return;
        entityData.set(CARGO_ICON,cargo.isEmpty()?net.minecraft.world.item.ItemStack.EMPTY:cargo.entry(0).item());
        entityData.set(CARGO_FILL,(float)Math.min(1,cargo.load()/cargo.capacity()));
    }
    public net.minecraft.world.item.ItemStack cargoIcon(){return entityData.get(CARGO_ICON);}
    public float cargoFill(){return entityData.get(CARGO_FILL);}
    @Override public MassCargo cargo(){return cargo;}
    @Override public Level cargoLevel(){return level();}
    @Override public Vec3 cargoPosition(){return position();}
    @Override public boolean cargoValid(Player p){return isAlive()&&!isVehicle()&&p.isAlive()&&!p.isSpectator()&&p.distanceToSqr(this)<=36&&accessible(p,terrainBounds());}
    @Override public Component cargoName(){return Component.translatable("entity.civilization.wheelbarrow");}
    @Override public Component getDisplayName(){return cargoName();}
    @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id,net.minecraft.world.entity.player.Inventory inv,Player p){return new CargoMenu(id,inv,this);}
    @Override public InteractionResult interactAt(Player p,Vec3 hit,InteractionHand hand){
        if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;
        // Transform the actual hit into the entity's forward/back frame.
        if(p.isShiftKeyDown()||hit.yRot(getYRot()*Mth.DEG_TO_RAD).z<-.4*MODEL_SCALE)return interact(p,hand);
        if(!level().isClientSide&&cargoValid(p))p.openMenu(this);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    public float travel(){return entityData.get(TRAVEL);}
    public float renderedTravel(){return level().isClientSide?sampledTravel:travel();}
    public boolean held(){return entityData.get(HELD);}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override public boolean isPickable(){return !isRemoved();}
    // The collision hook replaces these broad bounds with measured surfaces.
    @Override public boolean canBeCollidedWith(){return isAlive();}
    @Override public boolean canCollideWith(Entity other){return other instanceof WheelbarrowEntity&&!other.isRemoved()&&!isPassengerOfSameVehicle(other);}
    @Override public boolean shouldRiderSit(){return false;}
    @Override public float maxUpStep(){return 1;}
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
        if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;
        if(p.isShiftKeyDown()&&!p.getItemInHand(hand).isEmpty())return InteractionResult.PASS;
        if(level().isClientSide)return InteractionResult.SUCCESS;
        if(!accessible(p,terrainBounds())||!level().mayInteract(p,blockPosition()))return InteractionResult.FAIL;
        if(isVehicle())return InteractionResult.FAIL;
        if(p.isShiftKeyDown()){
            if(!cargo.isEmpty()){p.displayClientMessage(Component.literal("Empty the wheelbarrow before recovering it."),true);return InteractionResult.FAIL;}
            var stack=WheelbarrowContent.ITEM.toStack();if(!p.addItem(stack))spawnAtLocation(stack);
            discard();return InteractionResult.CONSUME;
        }
        if(p.isPassenger()||p.isSpectator()||isInWater()||!level().noCollision(p,riderBox(position(),getYRot(),p)))return InteractionResult.FAIL;
        if(p.startRiding(this)){
            entityData.set(HEADING,getYRot());
            forward=turn=0;lastInput=Long.MIN_VALUE;
            p.displayClientMessage(Component.literal("Wheelbarrow: W/S push/back up · A/D turn · Sneak to release"),true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.FAIL;
    }
    public Vec3 gripPosition(Vec3 center,float yaw){return center.add(new Vec3(0,0,-1.4*MODEL_SCALE).yRot(-yaw*Mth.DEG_TO_RAD));}
    private AABB riderBox(Vec3 at,float yaw,Player p){var v=gripPosition(at,yaw);double r=p.getBbWidth()/2;return new AABB(v.x-r,v.y+.02,v.z-r,v.x+r,v.y+p.getBbHeight(),v.z+r);}
    @Override protected void positionRider(Entity passenger,MoveFunction move){if(hasPassenger(passenger)){var p=gripPosition(position(),getYRot());move.accept(passenger,p.x,p.y,p.z);}}
    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger){
        for(double side:new double[]{1,-1,0}){
            Vec3 at=position().add(new Vec3(side*.95*MODEL_SCALE,0,-1.4*MODEL_SCALE).yRot(-getYRot()*Mth.DEG_TO_RAD));
            var box=passenger.getBoundingBox().move(at.subtract(passenger.position()));
            if(level().hasChunkAt(BlockPos.containing(at))&&level().noCollision(passenger,box))return at;
        }
        return super.getDismountLocationForPassenger(passenger);
    }
    private AABB footprint(Vec3 at,float yaw){
        // Centered main-body envelope keeps native movement's entity origin stable.
        double a=yaw*Mth.DEG_TO_RAD,x=Math.abs(Math.cos(a))*.4471875+Math.abs(Math.sin(a))*1.195+1d/32,z=Math.abs(Math.sin(a))*.4471875+Math.abs(Math.cos(a))*1.195+1d/32;
        return new AABB(at.x-x,at.y,at.z-z,at.x+x,at.y+WheelbarrowCollision.TRAY_TOP,at.z+z);
    }
    /** Centered main-body terrain/access envelope, separate from handle picking. */
    public AABB terrainBounds(){return footprint(position(),getYRot());}
    @Override protected AABB makeBoundingBox(){return WheelbarrowCollision.bounds(position(),getYRot());}
    public AABB interactionBounds(){return WheelbarrowCollision.pickBounds(position(),getYRot());}
    @Override public AABB getBoundingBoxForCulling(){return interactionBounds().inflate(0,.15,0);}
    public boolean clearOfPlayers(Vec3 at,float yaw){
        var shape=collision.at(at,yaw);
        return level().getEntitiesOfClass(Player.class,shape.bounds(),p->!p.isSpectator()&&!hasPassenger(p)&&net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(shape,net.minecraft.world.phys.shapes.Shapes.create(p.getBoundingBox()),net.minecraft.world.phys.shapes.BooleanOp.AND)).isEmpty();
    }
    @Override public void move(MoverType type,Vec3 motion){
        // Native block stepping sees the main body. Restore physical/broad
        // bounds before other entities query the measured collision surfaces.
        setBoundingBox(terrainBounds());
        Vec3 before=position();boolean grounded=onGround();float fall=fallDistance;
        try{
            super.move(type,motion);
            // Native stepping resolves the carrier first. Check the raised
            // handler too, so a low ceiling cannot push their head into blocks.
            if(getY()>before.y&&getFirstPassenger() instanceof Player p
                    &&(!accessible(p,terrainBounds())||!clearOfPlayers(position(),getYRot())
                    ||!accessible(p,riderBox(position(),getYRot(),p))||!level().noCollision(p,riderBox(position(),getYRot(),p)))){
                setPos(before);setOnGround(grounded);fallDistance=fall;setDeltaMovement(Vec3.ZERO);
            }
        }finally{setBoundingBox(makeBoundingBox());}
    }
    /** Small bounded footprint query; neither movement nor placement loads chunks. */
    public boolean accessible(Player p,AABB box){
        for(int x=Mth.floor(box.minX);x<=Mth.floor(box.maxX);x++)for(int z=Mth.floor(box.minZ);z<=Mth.floor(box.maxZ);z++){
            var pos=BlockPos.containing(x,box.minY,z);
            if(!level().hasChunkAt(pos)||!CivicAccess.allowed(level(),pos,p))return false;
        }
        return level().getWorldBorder().isWithinBounds(box);
    }
    @Override public void lerpTo(double x,double y,double z,float yaw,float pitch,int steps){targetX=x;targetY=y;targetZ=z;lerpSteps=Math.max(1,steps);}
    @Override public void tick(){
        super.tick();previousTravel=level().isClientSide?sampledTravel:travel();previousTilt=tilt;tilt=Mth.approach(tilt,held()?1:0,.2f);
        if(level().isClientSide){
            Vec3 oldGrip=gripPosition(position(),getYRot());
            sampledTravel=travel();if(tickCount<=1)previousTravel=sampledTravel;
            float heading=steeringYaw();if(Float.compare(heading,lastHeading)!=0){lastHeading=heading;headingSteps=3;}
            if(headingSteps>0){setYRot(getYRot()+Mth.wrapDegrees(heading-getYRot())/headingSteps);headingSteps--;}
            if(lerpSteps>0){
                setPos(getX()+(targetX-getX())/lerpSteps,getY()+(targetY-getY())/lerpSteps,getZ()+(targetZ-getZ())/lerpSteps);lerpSteps--;
            }
            double distance=gripPosition(position(),getYRot()).subtract(oldGrip).horizontalDistance();
            handlerWalkAnimation.update(held()?(float)Math.min(distance*4,1):0,.4f);
            return;
        }
        Player p=getFirstPassenger() instanceof Player player?player:null;
        if(p!=null&&(!p.isAlive()||p.isSpectator()||isInWater()||!accessible(p,terrainBounds())||!accessible(p,riderBox(position(),getYRot(),p)))){p.stopRiding();p=null;}
        entityData.set(HELD,p!=null);
        if(p==null||lastInput==Long.MIN_VALUE||level().getGameTime()-lastInput>10){forward=turn=0;}
        Vec3 before=position();float oldYaw=getYRot();Vec3 oldGrip=gripPosition(before,oldYaw);
        float nextYaw=oldYaw+turn*3.5f;
        if(p!=null&&turn!=0){
            var rotated=footprint(position(),nextYaw);
            if(accessible(p,rotated)&&level().noCollision(this,rotated)&&clearOfPlayers(position(),nextYaw)&&accessible(p,riderBox(position(),nextYaw,p))&&level().noCollision(p,riderBox(position(),nextYaw,p))){setYRot(nextYaw);setBoundingBox(makeBoundingBox());}
        }
        double speed=forward>0?PlayerMovement.WALK/20:forward<0?-PlayerMovement.WALK/40:0;
        Vec3 motion=new Vec3(0,Math.max(-.8,getDeltaMovement().y-.04),speed).yRot(-getYRot()*Mth.DEG_TO_RAD);
        var destination=terrainBounds().expandTowards(motion);
        if(!accessible(p,destination)||!clearOfPlayers(position().add(motion.x,0,motion.z),getYRot())||(p!=null&&(!accessible(p,riderBox(position().add(motion.x,0,motion.z),getYRot(),p))||!level().noCollision(p,riderBox(position().add(motion.x,0,motion.z),getYRot(),p)))))motion=new Vec3(0,motion.y,0);
        if(level().hasChunkAt(blockPosition())){setDeltaMovement(motion);move(MoverType.SELF,motion);setDeltaMovement(0,onGround()?0:getDeltaMovement().y,0);}
        double distance=position().subtract(before).horizontalDistance();
        if(distance>0)entityData.set(TRAVEL,(float)(travel()+Math.copySign(distance,forward==0?1:forward)));
        if(p!=null){
            Vec3 walked=gripPosition(position(),getYRot()).subtract(oldGrip);
            CalorieFoodData.of(p).pushWheelbarrow(p,walked.x,walked.y,walked.z);
        }
        if(p!=null)p.setYBodyRot(getYRot());
        entityData.set(HEADING,getYRot());
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag){tag.putFloat("Travel",travel());tag.put("Cargo",cargo.save(registryAccess()));}
    @Override protected void readAdditionalSaveData(CompoundTag tag){float value=tag.getFloat("Travel");entityData.set(TRAVEL,Float.isFinite(value)?value:0f);cargo.load(tag.getList("Cargo",10),registryAccess());forward=turn=0;lastInput=Long.MIN_VALUE;}
}
