package dev.civilization;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Server-driven ground carrier pushed by a handler who walks behind it. The carrier pivots toward the
 * handler's facing and W/S drive it along its heading. Its measured body steps at most one block up or
 * down, and the handler walks behind on their own ground, a block at a time. The carriage pivots on the
 * wheel axle between the wheel's ground and its rear support: the parked legs, or the handler's hands.
 * Its tray holds a fixed volume of goods that clients draw one for one.
 */
public final class WheelbarrowEntity extends Entity implements GeoEntity,CargoHolder,net.minecraft.world.MenuProvider {
    public static final float MODEL_SCALE=1.35f;
    public static final float PUSH_TILT_DEGREES=4;
    public static final float WHEEL_RADIUS=4.1411056f/16*MODEL_SCALE;
    /** Degrees per tick the carrier pivots toward its handler's facing: 70 degrees a second. */
    public static final float TURN=3.5f;
    /** The handler's head turns at most this far from the handles. */
    public static final float LOOK=75;
    /** Blocks of solid volume the tray holds: six layers of 6x9, three to the rim and three heaped above it. */
    public static final int CAPACITY=324;
    // Model frame: x right, y up, z rear.
    public static final Vec3 AXLE=new Vec3(0,.3375,-.84375);
    private static final Vec3 GRIP=new Vec3(5.7375/16,.8015,1.4343);
    // The handler stands this far behind the center, steps a block at a time, and lets go beyond arm's reach.
    private static final double HANDLER=1.4*MODEL_SCALE,STEP=1,REACH=4;
    private static final AABB WHEEL=new AABB(-.055,0,-1.195,.055,.1,-.49),LEGS=new AABB(-6.48/16,0,9.72/16,6.48/16,.1,11.205/16);
    private static final EntityDataAccessor<Boolean> HELD=SynchedEntityData.defineId(WheelbarrowEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> TRAVEL=SynchedEntityData.defineId(WheelbarrowEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEADING=SynchedEntityData.defineId(WheelbarrowEntity.class,EntityDataSerializers.FLOAT);
    private final WheelbarrowCollision collision=new WheelbarrowCollision();
    // The whole ledger in its saved form, so clients draw the load one for one.
    private static final EntityDataAccessor<CompoundTag> CARGO=SynchedEntityData.defineId(WheelbarrowEntity.class,EntityDataSerializers.COMPOUND_TAG);
    private final CargoLedger cargo=new CargoLedger(()->CAPACITY,this::cargoChanged);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private int forward,lerpSteps;
    private long lastInput=Long.MIN_VALUE;
    private double targetX,targetY,targetZ;
    private float lastHeading=Float.NaN;
    private int headingSteps;
    private float sampledTravel,rolled;
    public float previousTravel,previousTilt,tilt;
    /** Ground under the wheel and under the rear support, gliding over steps. */
    private double front,frontO,rear,rearO;
    public final WalkAnimationState handlerWalkAnimation=new WalkAnimationState();
    public WheelbarrowEntity(EntityType<? extends WheelbarrowEntity> type,Level level){super(type,level);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(HELD,false);b.define(TRAVEL,0f);b.define(HEADING,0f);b.define(CARGO,new CompoundTag());}
    public VoxelShape playerCollisionShape(){return collision.at(position(),getYRot());}
    private void cargoChanged(){
        if(level().isClientSide)return;
        var tag=new CompoundTag();tag.put("Cargo",cargo.save(registryAccess()));entityData.set(CARGO,tag);
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){
        super.onSyncedDataUpdated(key);
        if(level().isClientSide&&CARGO.equals(key))cargo.load(entityData.get(CARGO).getList("Cargo",10),registryAccess());
    }
    @Override public CargoLedger cargo(){return cargo;}
    @Override public Level cargoLevel(){return level();}
    @Override public Vec3 cargoPosition(){return position();}
    @Override public boolean cargoValid(Player p){return isAlive()&&!isVehicle()&&p.isAlive()&&!p.isSpectator()&&p.distanceToSqr(this)<=36&&accessible(p,getBoundingBox());}
    @Override public Component cargoName(){return Component.translatable("entity.civilization.wheelbarrow");}
    @Override public Component getDisplayName(){return cargoName();}
    @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id,net.minecraft.world.entity.player.Inventory inv,Player p){return new CargoMenu(id,inv,this);}
    @Override public InteractionResult interactAt(Player p,Vec3 hit,InteractionHand hand){
        if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;
        // Transform the actual hit into the model frame: the handles lie at the rear.
        if(p.isShiftKeyDown()||hit.yRot((getYRot()-180)*Mth.DEG_TO_RAD).z>.4*MODEL_SCALE)return interact(p,hand);
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
    @Override protected boolean canAddPassenger(Entity passenger){return passenger instanceof Player&&getPassengers().isEmpty();}
    // Deliberately retain Entity's null controlling-passenger contract: physics
    // stays on the server rather than accepting vanilla client vehicle movement.
    public void input(Player p,WheelbarrowPayload input){
        if(level().isClientSide||getFirstPassenger()!=p)return;
        if(input.release()){p.stopRiding();forward=0;return;}
        forward=Integer.signum(input.forward());lastInput=level().getGameTime();
    }
    @Override public InteractionResult interact(Player p,InteractionHand hand){
        if(isRemoved())return InteractionResult.FAIL;
        if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;
        if(p.isShiftKeyDown()&&!p.getItemInHand(hand).isEmpty())return InteractionResult.PASS;
        if(level().isClientSide)return InteractionResult.SUCCESS;
        if(!accessible(p,getBoundingBox())||!level().mayInteract(p,blockPosition()))return InteractionResult.FAIL;
        if(isVehicle())return InteractionResult.FAIL;
        if(p.isShiftKeyDown()){
            if(!cargo.isEmpty()){p.displayClientMessage(Component.literal("Empty the wheelbarrow before recovering it."),true);return InteractionResult.FAIL;}
            var stack=WheelbarrowContent.ITEM.toStack();if(!p.addItem(stack))spawnAtLocation(stack);
            discard();return InteractionResult.CONSUME;
        }
        if(p.isPassenger()||p.isSpectator()||isInWater()||!handlerFits(p,position(),getYRot()))return InteractionResult.FAIL;
        if(p.startRiding(this)){
            // Take the handles facing along them, so steering starts from rest.
            p.setYRot(getYRot());p.setYHeadRot(getYRot());entityData.set(HEADING,getYRot());
            forward=0;lastInput=Long.MIN_VALUE;
            p.displayClientMessage(Component.literal("Wheelbarrow: W/S push/back up · look or A/D to steer · Sneak to release"),true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.FAIL;
    }
    /** Where the handler's feet stand behind a carrier at `at` facing `yaw`, before finding their ground. */
    public Vec3 handlerPosition(Vec3 at,float yaw){return WheelbarrowCollision.place(at,yaw,new Vec3(0,0,HANDLER));}
    private AABB handlerBox(Player p,Vec3 at,float yaw,double ground){var v=handlerPosition(at,yaw);double r=p.getBbWidth()/2;return new AABB(v.x-r,ground+.02,v.z-r,v.x+r,ground+p.getBbHeight(),v.z+r);}
    /** The handler can step onto their own ground behind this pose and still reach the handles. */
    private boolean handlerFits(Player p,Vec3 at,float yaw){
        double ground=handlerGround(p,at,yaw);if(!(Math.abs(ground-at.y)<=REACH))return false;
        var box=handlerBox(p,at,yaw,ground);
        return level().noCollision(p,box)&&accessible(p,box)&&accessible(p,WheelbarrowCollision.bounds(at,yaw));
    }
    /** The handler walks: their ground is found within a step of where they stand now. */
    private double handlerGround(Player p,Vec3 at,float yaw){return ground(handlerBox(p,at,yaw,rear),rear);}
    /** Top of whatever supports `box` within a block above or below `level`; NaN when nothing does. */
    private double ground(AABB box,double level){
        double depth=2*STEP+1e-4;var probe=box.move(0,level+STEP-box.minY,0);
        double fall=Shapes.collide(Direction.Axis.Y,probe,level().getBlockCollisions(null,probe.expandTowards(0,-depth,0)),-depth);
        return fall>-depth?probe.minY+fall:Double.NaN;
    }
    @Override protected void positionRider(Entity passenger,MoveFunction move){
        if(!hasPassenger(passenger))return;
        var feet=handlerPosition(position(),getYRot());move.accept(passenger,feet.x,rear,feet.z);face(passenger);
    }
    @Override public void onPassengerTurned(Entity passenger){face(passenger);}
    /** The handler's body follows the handles and their head turns at most LOOK away. */
    private void face(Entity handler){
        handler.setYBodyRot(getYRot());
        float offset=Mth.wrapDegrees(handler.getYRot()-getYRot()),limited=Mth.clamp(offset,-LOOK,LOOK);
        handler.yRotO+=limited-offset;handler.setYRot(handler.getYRot()+limited-offset);handler.setYHeadRot(handler.getYRot());
    }
    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger){
        var feet=handlerPosition(position(),getYRot());
        for(double side:new double[]{1,-1,0}){
            Vec3 at=new Vec3(feet.x,passenger.getY(),feet.z).add(new Vec3(side*.95*MODEL_SCALE,0,0).yRot(-getYRot()*Mth.DEG_TO_RAD));
            var box=passenger.getBoundingBox().move(at.subtract(passenger.position()));
            if(level().hasChunkAt(BlockPos.containing(at))&&level().noCollision(passenger,box))return at;
        }
        return super.getDismountLocationForPassenger(passenger);
    }
    @Override public void setYRot(float yaw){super.setYRot(yaw);setBoundingBox(makeBoundingBox());}
    @Override protected AABB makeBoundingBox(){return WheelbarrowCollision.bounds(position(),getYRot());}
    public AABB interactionBounds(){return WheelbarrowCollision.pickBounds(position(),getYRot());}
    // The carriage may hang up to a block below its resting height over a step.
    @Override public AABB getBoundingBoxForCulling(){return interactionBounds().inflate(0,STEP,0);}
    public boolean clearOfPlayers(Vec3 at,float yaw){
        var shape=collision.at(at,yaw);
        return level().getEntitiesOfClass(Player.class,shape.bounds(),p->!p.isSpectator()&&!hasPassenger(p)&&Shapes.joinIsNotEmpty(shape,Shapes.create(p.getBoundingBox()),BooleanOp.AND)).isEmpty();
    }
    /** Whether a new carrier fits here: claimed land, players and solid surfaces all permit it. */
    public boolean placeable(Player p){var body=collision.at(position(),getYRot());return accessible(p,getBoundingBox())&&clearOfPlayers(position(),getYRot())&&clear(body,obstacles(body.bounds()));}
    /** Small bounded footprint query; neither movement nor placement loads chunks. */
    public boolean accessible(Player p,AABB box){
        for(int x=Mth.floor(box.minX);x<=Mth.floor(box.maxX);x++)for(int z=Mth.floor(box.minZ);z<=Mth.floor(box.maxZ);z++){
            var pos=BlockPos.containing(x,box.minY,z);
            if(!level().hasChunkAt(pos)||!CivicAccess.allowed(level(),pos,p))return false;
        }
        return level().getWorldBorder().isWithinBounds(box);
    }
    /** Block surfaces and other carriers' measured bodies within `region`. */
    private List<AABB> obstacles(AABB region){
        var boxes=new ArrayList<AABB>();
        for(var shape:level().getBlockCollisions(this,region))boxes.addAll(shape.toAabbs());
        for(var shape:level().getEntityCollisions(this,region))boxes.addAll(shape.toAabbs());
        return boxes;
    }
    private static boolean clear(VoxelShape body,List<AABB> obstacles){return obstacles.stream().noneMatch(b->Shapes.joinIsNotEmpty(body,Shapes.create(b.deflate(1e-7)),BooleanOp.AND));}
    /** How far the body can sink, up to `limit`, before something holds it. */
    private static double lower(VoxelShape body,List<AABB> obstacles,double limit){for(var b:obstacles)limit=Math.min(limit,body.collide(Direction.Axis.Y,b,limit));return limit;}
    /**
     * Where the measured body comes to rest at `at`, in native step order: on the level path if that is
     * clear, otherwise lifted at most a block (ceilings permitting), then lowered onto what lies within a
     * block below. Null when it fits nowhere or would hang over a deeper drop.
     */
    private Vec3 settle(Vec3 at,float yaw){
        var body=collision.at(at,yaw);var reach=body.bounds();
        var near=obstacles(new AABB(reach.minX,reach.minY-2*STEP,reach.minZ,reach.maxX,reach.maxY+STEP,reach.maxZ));
        // Obstacles already overlapping the body (a step ahead) never limit its climb.
        double room=STEP;for(var b:near)room=Math.min(room,-body.collide(Direction.Axis.Y,b,-room));
        for(double lift:new double[]{0,room}){
            var raised=body.move(0,lift,0);
            if(!clear(raised,near))continue;
            double limit=lift+STEP+1e-4,drop=lower(raised,near,limit);
            return drop<limit?at.add(0,lift-drop,0):null;
        }
        return null;
    }
    /** Moves to a new pose only when the body fits, the wheel keeps its footing, and other players and the handler fit. */
    private void shift(Vec3 at,float yaw,Player p){
        var rest=settle(at,yaw);
        if(rest!=null&&!Double.isNaN(wheelGround(rest,yaw))&&clearOfPlayers(rest,yaw)&&handlerFits(p,rest,yaw)){setYRot(yaw);setPos(rest);}
    }
    private double wheelGround(Vec3 at,float yaw){return ground(WheelbarrowCollision.place(at,yaw,WHEEL),at.y);}
    /** Whatever stops holding the carrier lets it fall. */
    private void fall(){
        var body=collision.at(position(),getYRot());double speed=Math.min(.8,.04-getDeltaMovement().y);
        double drop=lower(body,obstacles(body.bounds().expandTowards(0,-speed,0)),speed);
        setOnGround(drop<speed);setDeltaMovement(0,onGround()?0:-speed,0);
        if(drop>0)setPos(getX(),getY()-drop,getZ());
    }
    @Override public void lerpTo(double x,double y,double z,float yaw,float pitch,int steps){targetX=x;targetY=y;targetZ=z;lerpSteps=Math.max(1,steps);}
    @Override public void onAddedToLevel(){super.onAddedToLevel();front=frontO=rear=rearO=getY();}
    @Override public void tick(){
        super.tick();previousTravel=level().isClientSide?sampledTravel:travel();previousTilt=tilt;tilt=Mth.approach(tilt,held()?1:0,.2f);
        frontO=front;rearO=rear;
        Vec3 feet=handlerPosition(position(),getYRot()).with(Direction.Axis.Y,rear);
        Player p=getFirstPassenger() instanceof Player player?player:null;
        if(level().isClientSide){
            sampledTravel=travel();if(tickCount<=1)previousTravel=sampledTravel;
            float heading=entityData.get(HEADING);if(Float.compare(heading,lastHeading)!=0){lastHeading=heading;headingSteps=3;}
            if(headingSteps>0){setYRot(getYRot()+Mth.wrapDegrees(heading-getYRot())/headingSteps);headingSteps--;}
            if(lerpSteps>0){
                setPos(getX()+(targetX-getX())/lerpSteps,getY()+(targetY-getY())/lerpSteps,getZ()+(targetZ-getZ())/lerpSteps);lerpSteps--;
            }
            support(p);
            double stride=handlerPosition(position(),getYRot()).subtract(feet).horizontalDistance();
            handlerWalkAnimation.update(held()?(float)Math.min(stride*4,1):0,.4f);
            return;
        }
        if(p!=null&&(!p.isAlive()||p.isSpectator()||isInWater()||!handlerFits(p,position(),getYRot()))){p.stopRiding();p=null;}
        entityData.set(HELD,p!=null);
        if(p==null||lastInput==Long.MIN_VALUE||level().getGameTime()-lastInput>10)forward=0;
        Vec3 before=position();
        if(p!=null){
            float turn=Mth.clamp(Mth.wrapDegrees(p.getYRot()-getYRot()),-TURN,TURN);
            if(turn!=0)shift(position(),getYRot()+turn,p);
            double speed=forward>0?PlayerMovement.WALK/20:forward<0?-PlayerMovement.WALK/40:0;
            if(speed!=0&&level().hasChunkAt(blockPosition()))shift(position().add(new Vec3(0,0,speed).yRot(-getYRot()*Mth.DEG_TO_RAD)),getYRot(),p);
        }
        if(level().hasChunkAt(blockPosition()))fall();
        entityData.set(HEADING,getYRot());
        double distance=position().subtract(before).horizontalDistance();
        if(distance>0){entityData.set(TRAVEL,(float)(travel()+Math.copySign(distance,forward==0?1:forward)));roll(distance);}
        support(p);
        if(p!=null){
            Vec3 walked=handlerPosition(position(),getYRot()).with(Direction.Axis.Y,rear).subtract(feet);
            CalorieFoodData.of(p).pushWheelbarrow(p,walked.x,walked.y,walked.z);
        }
    }
    /** The native step sound of the ground, as rolling entities make it. */
    private void roll(double distance){
        rolled+=(float)distance*.6f;if(rolled<1)return;rolled--;
        var on=getOnPos();var state=level().getBlockState(on);if(!state.isAir())playStepSound(on,state);
    }
    /** The wheel and rear support follow their ground: steps of up to a block glide at a third of a block per tick, anything larger snaps. */
    private void support(Player handler){
        Vec3 at=position();float yaw=getYRot();
        front=follow(front,wheelGround(at,yaw));rear=follow(rear,handler!=null?handlerGround(handler,at,yaw):ground(WheelbarrowCollision.place(at,yaw,LEGS),at.y));
    }
    private static double follow(double from,double to){return Double.isNaN(to)?from:Math.abs(to-from)>STEP?to:from+Mth.clamp(to-from,-STEP/3,STEP/3);}
    /** Carriage angle about the wheel axle in degrees; positive lowers the rear. */
    public float carriagePitch(float partial){
        float lift=Mth.lerp(partial,previousTilt,tilt);
        double span=Mth.lerp(lift,LEGS.getCenter().z,HANDLER)-AXLE.z;
        return (float)Math.toDegrees(Math.atan2(Mth.lerp(partial,frontO,front)-Mth.lerp(partial,rearO,rear),span))-PUSH_TILT_DEGREES*lift;
    }
    /** How far the wheel's ground lies below the body's resting height. */
    public double wheelDrop(float partial){return Mth.lerp(partial,frontO,front)-Mth.lerp(partial,yOld,getY());}
    /** A handle grip relative to the handler's feet, in the model frame. */
    public Vec3 gripFromHandler(int side,float partial){
        var grip=new Vec3(side*GRIP.x,GRIP.y,GRIP.z).subtract(AXLE).xRot(-carriagePitch(partial)*Mth.DEG_TO_RAD).add(AXLE);
        return grip.add(0,Mth.lerp(partial,frontO,front)-Mth.lerp(partial,rearO,rear),-HANDLER);
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag){tag.putFloat("Travel",travel());tag.put("Cargo",cargo.save(registryAccess()));}
    @Override protected void readAdditionalSaveData(CompoundTag tag){float value=tag.getFloat("Travel");entityData.set(TRAVEL,Float.isFinite(value)?value:0f);cargo.load(tag.getList("Cargo",10),registryAccess());forward=0;lastInput=Long.MIN_VALUE;}
}
