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
 * Server-driven ground carrier pushed by a handler who walks behind it. A/D pivot it and W/S drive it
 * along its heading; the handler looks around freely within reach of the handles. Its measured body steps at most one block up or
 * down, and the handler walks behind on their own ground, a block at a time. The carriage pivots on the
 * wheel axle between the wheel's ground and its rear support: the parked legs, or the handler's hands.
 * Its tray holds a fixed volume of goods that clients draw one for one.
 */
public final class WheelbarrowEntity extends Entity implements GeoEntity,CargoHolder,net.minecraft.world.MenuProvider {
    public static final float PUSH_TILT_DEGREES=4;
    public static final float WHEEL_RADIUS=5.5f/16;
    /** Degrees per tick A/D pivot the carrier: 70 degrees a second. */
    public static final float TURN=3.5f;
    /** The handler's head turns at most this far from the handles. */
    public static final float LOOK=75;
    /** Four 4x4 layers of quarter-size blocks: two below the rim, a third rising past it and a fourth heaped above. */
    public static final CargoGrid GRID=new CargoGrid(4,4,4,CargoGrid.CELL);
    // Model frame: x right, y up, z rear; measured from the model's landmarks.
    public static final Vec3 AXLE=new Vec3(0,5.5/16,-15.5/16);
    /** The right grip's centre, parked. */
    public static final Vec3 GRIP=new Vec3(5.75/16,.6487,1.2199);
    // The handler stands where the hands reach the raised grips, steps a block at a time, and lets go beyond arm's reach.
    private static final double HANDLER=1.44,STEP=1,REACH=4;
    // Where the tyre can touch the ground, and the parked legs' shoes.
    private static final AABB WHEEL=new AABB(-.07,0,-21/16d,.07,.1,-10/16d),LEGS=new AABB(-8.125/16,0,4.625/16,8.125/16,.1,6.375/16);
    private static final EntityDataAccessor<Boolean> HELD=SynchedEntityData.defineId(WheelbarrowEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> TRAVEL=SynchedEntityData.defineId(WheelbarrowEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEADING=SynchedEntityData.defineId(WheelbarrowEntity.class,EntityDataSerializers.FLOAT);
    // The whole ledger in its saved form, so clients draw the load one for one.
    private static final EntityDataAccessor<CompoundTag> CARGO=SynchedEntityData.defineId(WheelbarrowEntity.class,EntityDataSerializers.COMPOUND_TAG);
    private final CargoLedger cargo=new CargoLedger(GRID::capacity,this::cargoChanged);
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private int forward,turn,lerpSteps;
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
    public Solid body(){return body(position(),getYRot());}
    private static Solid body(Vec3 at,float yaw){return WheelbarrowCollision.BODY.place(at,yaw);}
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
    @Override public boolean cargoValid(Player p,double reach){return isAlive()&&!isVehicle()&&p.level()==level()&&p.isAlive()&&!p.isSpectator()&&p.distanceToSqr(this)<=reach*reach&&accessible(p,getBoundingBox());}
    @Override public Component cargoName(){return Component.translatable("entity.civilization.wheelbarrow");}
    @Override public Component getDisplayName(){return cargoName();}
    @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id,net.minecraft.world.entity.player.Inventory inv,Player p){return new CargoMenu(id,inv,this);}
    @Override public InteractionResult interactAt(Player p,Vec3 hit,InteractionHand hand){
        if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;
        // Transform the actual hit into the model frame: the rear wall and the handles behind it take the handles.
        if(p.isShiftKeyDown()||hit.yRot((getYRot()-180)*Mth.DEG_TO_RAD).z>.5)return interact(p,hand);
        if(!level().isClientSide&&cargoValid(p)&&!putHeld(p))p.openMenu(this);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    public float travel(){return entityData.get(TRAVEL);}
    public float renderedTravel(){return level().isClientSide?sampledTravel:travel();}
    public boolean held(){return entityData.get(HELD);}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override public boolean isPickable(){return !isRemoved();}
    @Override public boolean shouldRiderSit(){return false;}
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
        if(!accessible(p,getBoundingBox())||!level().mayInteract(p,blockPosition()))return InteractionResult.FAIL;
        if(isVehicle())return InteractionResult.FAIL;
        if(p.isShiftKeyDown()){
            if(!cargo.isEmpty()){p.displayClientMessage(Component.literal("Empty the wheelbarrow before recovering it."),true);return InteractionResult.FAIL;}
            var stack=WheelbarrowContent.ITEM.toStack();if(!p.addItem(stack))spawnAtLocation(stack);
            discard();return InteractionResult.CONSUME;
        }
        if(p.isPassenger()||p.isSpectator()||isInWater()||!handlerFits(p,position(),getYRot()))return InteractionResult.FAIL;
        if(p.startRiding(this)){
            // Take the handles facing along them.
            p.setYRot(getYRot());p.setYHeadRot(getYRot());entityData.set(HEADING,getYRot());
            forward=turn=0;lastInput=Long.MIN_VALUE;
            p.displayClientMessage(Component.literal("Wheelbarrow: W/S push/back up · A/D steer · Sneak to release"),true);
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
            Vec3 at=new Vec3(feet.x,passenger.getY(),feet.z).add(new Vec3(side,0,0).yRot(-getYRot()*Mth.DEG_TO_RAD));
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
        var body=body(at,yaw);
        return level().getEntitiesOfClass(Player.class,body.bounds(),p->!p.isSpectator()&&!hasPassenger(p)&&body.overlaps(Solid.of(p.getBoundingBox()))).isEmpty();
    }
    /** Whether a new carrier fits here: claimed land, players and solid surfaces all permit it. */
    public boolean placeable(Player p){var body=body();return accessible(p,getBoundingBox())&&clearOfPlayers(position(),getYRot())&&clear(body,obstacles(body.bounds()));}
    /** Small bounded footprint query; neither movement nor placement loads chunks. */
    public boolean accessible(Player p,AABB box){
        for(int x=Mth.floor(box.minX);x<=Mth.floor(box.maxX);x++)for(int z=Mth.floor(box.minZ);z<=Mth.floor(box.maxZ);z++){
            var pos=BlockPos.containing(x,box.minY,z);
            if(!level().hasChunkAt(pos)||!CivicAccess.allowed(level(),pos,p))return false;
        }
        return level().getWorldBorder().isWithinBounds(box);
    }
    /** Block surfaces and other carriers' bodies within `region`. */
    private List<Solid> obstacles(AABB region){
        var solids=new ArrayList<Solid>();
        for(var shape:level().getBlockCollisions(this,region))for(var box:shape.toAabbs())solids.add(Solid.of(box));
        for(var other:level().getEntitiesOfClass(WheelbarrowEntity.class,region,e->e!=this))solids.add(other.body());
        return solids;
    }
    private static boolean clear(Solid body,List<Solid> obstacles){return obstacles.stream().noneMatch(body::overlaps);}
    /** How far the body can sink, up to `limit`, before something holds it. */
    private static double lower(Solid body,List<Solid> obstacles,double limit){for(var o:obstacles)limit=-body.reach(o,-limit);return limit;}
    /**
     * Where the measured body comes to rest at `at`, in native step order: on the level path if that is
     * clear, otherwise lifted at most a block (ceilings permitting), then lowered onto what lies within a
     * block below. Null when it fits nowhere or would hang over a deeper drop.
     */
    private Vec3 settle(Vec3 at,float yaw){
        var body=body(at,yaw);var reach=body.bounds();
        var near=obstacles(new AABB(reach.minX,reach.minY-2*STEP,reach.minZ,reach.maxX,reach.maxY+STEP,reach.maxZ));
        // Obstacles already overlapping the body (a step ahead) never limit its climb.
        double room=STEP;for(var o:near)room=body.reach(o,room);
        for(double lift:new double[]{0,room}){
            var raised=body.rise(lift);
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
        var body=body();double speed=Math.min(.8,.04-getDeltaMovement().y);
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
        if(p==null||lastInput==Long.MIN_VALUE||level().getGameTime()-lastInput>10)forward=turn=0;
        Vec3 before=position();
        if(p!=null){
            if(turn!=0)shift(position(),getYRot()+turn*TURN,p);
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
    @Override protected void readAdditionalSaveData(CompoundTag tag){float value=tag.getFloat("Travel");entityData.set(TRAVEL,Float.isFinite(value)?value:0f);cargo.load(tag.getList("Cargo",10),registryAccess());forward=turn=0;lastInput=Long.MIN_VALUE;}
}
