package dev.civilization;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3d;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid="civilization")
public final class AirshipSystem {
    public static final String TAG="civilization_airship";
    public static final int RADIUS=12, MAX_BLOCKS=1024;
    static final class Drive {
        volatile UUID pilot;
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
        AirshipPilotSeat seat;
        Vec3 pilotAnchor;
        volatile int forward,turn,vertical;
        volatile double throttle;
        volatile long last;
        volatile double power,windX,windZ;
        volatile FlightTerrain.Snapshot terrain=FlightTerrain.Snapshot.EMPTY;
        ServerLevel ticketLevel;UUID ticketId;it.unimi.dsi.fastutil.longs.LongSet tickets=it.unimi.dsi.fastutil.longs.LongSets.EMPTY_SET;
        final it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap preparing=new it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap();
        double preparationSeconds=.3;
        volatile boolean streamingBrake;
        volatile String hold="";
        double altitude=Double.NaN;
        double hoverX=Double.NaN,hoverZ=Double.NaN;
    }
    private static final Map<UUID,Drive> drives=new ConcurrentHashMap<>();
    static boolean hasPilot(ServerSubLevel ship){var d=drives.get(ship.getUniqueId());return d!=null&&d.pilot!=null;}
    static void forget(ServerSubLevel ship){var d=drives.remove(ship.getUniqueId());if(d!=null)release(d,null);}
    public static boolean isAirship(ServerSubLevel s){var t=s.getUserDataTag();return t!=null&&t.getBoolean(TAG);}
    public static List<ServerSubLevel> ships(ServerLevel l){var c=SubLevelContainer.getContainer(l);return c==null?List.of():c.getAllSubLevels().stream().filter(AirshipSystem::isAirship).toList();}
    public static ServerSubLevel at(ServerLevel l,BlockPos p){var c=SubLevelContainer.getContainer(l);if(c==null||!c.inBounds(p))return null;var plot=c.getPlot(new ChunkPos(p));return plot!=null&&plot.getSubLevel() instanceof ServerSubLevel s&&isAirship(s)?s:null;}
    public static BlockPos controller(ServerSubLevel s){return BlockPos.of(s.getUserDataTag().getLong("controller"));}
    public static Vec3 world(ServerSubLevel s,BlockPos p){return s.logicalPose().transformPosition(Vec3.atCenterOf(p));}
    public static boolean owner(ServerSubLevel s,Player p){var t=s.getUserDataTag();return p!=null&&(p.hasPermissions(2)||t.hasUUID("owner")&&t.getUUID("owner").equals(p.getUUID()));}
    public static boolean access(ServerPlayer p,BlockPos pos){
        if(!p.isAlive()||!(p.isCreative()||p.hasPermissions(2))||!p.level().getBlockState(pos).is(AirshipContent.CONTROLLER.get()))return false;
        var s=at(p.serverLevel(),pos);var w=s==null?Vec3.atCenterOf(pos):world(s,pos);
        return p.position().distanceToSqr(w)<=64&&(s==null?CivicAccess.allowed(p.level(),pos,p):owner(s,p));
    }
    private static boolean supported(BlockState s){
        if(s.getBlock() instanceof CivicBlock||s.getBlock() instanceof SurveyBlock||s.is(BoatContent.HELM.get())||!s.getFluidState().isEmpty()||s.getBlock() instanceof FallingBlock||s.getBlock() instanceof TntBlock||s.getBlock() instanceof PistonBaseBlock||s.is(Blocks.BEDROCK)||s.is(net.minecraft.tags.BlockTags.PORTALS))return false;
        return !s.hasBlockEntity()||s.is(AirshipContent.CONTROLLER.get())||s.is(Blocks.CHEST)||s.is(Blocks.BARREL)||s.is(CuttingContent.PIECE.get());
    }
    public static List<BlockPos> scan(ServerLevel l,BlockPos root,ServerPlayer player){
        var c=SubLevelContainer.getContainer(l);
        if(c!=null&&c.inBounds(root))throw new IllegalStateException("Assemble in the world, not aboard another vessel.");
        var found=new LinkedHashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();queue.add(root);found.add(root);
        while(!queue.isEmpty()){
            var pos=queue.remove();var state=l.getBlockState(pos);
            if(!CivicAccess.allowed(l,pos,player))throw new IllegalStateException("Structure crosses protected land.");
            if(!supported(state)||!pos.equals(root)&&state.is(AirshipContent.CONTROLLER.get()))throw new IllegalStateException("Unsupported block or second controller in structure.");
            for(var direction:Direction.values()){
                var next=pos.relative(direction);if(found.contains(next))continue;
                if(!l.hasChunkAt(next))throw new IllegalStateException("Load the complete build and its surroundings first.");
                if(l.getBlockState(next).isAir())continue;
                if(Math.abs(next.getX()-root.getX())>RADIUS||Math.abs(next.getY()-root.getY())>RADIUS||Math.abs(next.getZ()-root.getZ())>RADIUS||found.size()>=MAX_BLOCKS)throw new IllegalStateException("Build must be separate from terrain: max 1024 blocks, 12 from controller.");
                found.add(next.immutable());queue.add(next.immutable());
            }
        }
        return List.copyOf(found);
    }
    public static ServerSubLevel launch(ServerLevel l,BlockPos root,ServerPlayer p,List<BlockPos> blocks){
        if(!access(p,root))throw new IllegalStateException("Stand near your controller in Creative mode.");
        var current=scan(l,root,p);
        if(!new HashSet<>(current).equals(new HashSet<>(blocks)))throw new IllegalStateException("Structure changed. Inspect again.");
        // Project occupied columns, not the bounding box or the sum of interior faces.
        var x=new HashSet<String>();var y=new HashSet<String>();var z=new HashSet<String>();
        for(var b:blocks){x.add(b.getY()+":"+b.getZ());y.add(b.getX()+":"+b.getZ());z.add(b.getX()+":"+b.getY());}
        var ship=SubLevelAssemblyHelper.assembleBlocks(l,root,blocks,BoundingBox3i.from(blocks));
        var tag=new CompoundTag();tag.putBoolean(TAG,true);tag.putUUID("owner",p.getUUID());tag.putLong("controller",ship.getPlot().getCenterBlock().asLong());tag.putDouble("areaX",x.size());tag.putDouble("areaY",y.size());tag.putDouble("areaZ",z.size());tag.putInt("blocks",blocks.size());
        ship.setUserDataTag(tag);ship.setName("Prototype Airship");return ship;
    }
    public static void pilot(ServerSubLevel s,ServerPlayer p){
        if(!access(p,controller(s)))throw new IllegalStateException("Stand near your controller.");
        var d=drives.computeIfAbsent(s.getUniqueId(),key->new Drive());
        if(d.pilot!=null&&!p.getUUID().equals(d.pilot))throw new IllegalStateException("Controller is in use.");
        // A player may own several ships, but may only control one at once.
        for(var other:drives.values())if(p.getUUID().equals(other.pilot))release(other,p);
        d.pilotAnchor=s.logicalPose().transformPositionInverse(p.position());
        d.dimension=p.level().dimension();d.pilot=p.getUUID();d.last=p.serverLevel().getGameTime();
        attachPilot(d,p);p.closeContainer();PacketDistributor.sendToPlayer(p,new AirshipPayload.State(true));
    }
    public static void input(ServerPlayer p,AirshipPayload.Input input){
        for(var d:drives.values())if(p.getUUID().equals(d.pilot)){
            if(input.exit()||!eligible(d,p)){release(d,p);return;}
            d.forward=Math.clamp(input.forward(),-1,1);d.turn=Math.clamp(input.turn(),-1,1);d.vertical=Math.clamp(input.vertical(),-1,1);d.last=p.serverLevel().getGameTime();return;
        }
        PacketDistributor.sendToPlayer(p,new AirshipPayload.State(false));
    }
    private static boolean eligible(Drive d,ServerPlayer p){return p!=null&&p.isAlive()&&(p.isCreative()||p.hasPermissions(2))&&p.level().dimension().equals(d.dimension);}
    static void expireInput(Drive d,long now){if(now-d.last>15){d.forward=d.turn=d.vertical=0;d.throttle=0;}}
    private static void attachPilot(Drive d,ServerPlayer p){
        if(d.pilotAnchor==null)return;
        if(d.seat==null||d.seat.isRemoved()){
            d.seat=new AirshipPilotSeat(AirshipContent.PILOT_SEAT.get(),p.serverLevel());
            d.seat.setPos(d.pilotAnchor);p.serverLevel().addFreshEntity(d.seat);
        }
        if(p.getVehicle()!=d.seat)p.startRiding(d.seat,true);
        p.fallDistance=0;
    }
    static void release(Drive d,ServerPlayer p){boolean active=d.pilot!=null;AirshipTerrain.release(d);d.pilot=null;d.forward=d.turn=d.vertical=0;d.throttle=0;if(d.seat!=null){d.seat.ejectPassengers();d.seat.discard();d.seat=null;}d.pilotAnchor=null;if(active&&p!=null)PacketDistributor.sendToPlayer(p,new AirshipPayload.State(false));}
    public static double speed(ServerSubLevel ship){var body=ship==null?null:RigidBodyHandle.of(ship);return body==null||!body.isValid()?0:body.getLinearVelocity(new Vector3d()).length();}
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){drives.clear();FlightChunkHints.clear();}
    @SubscribeEvent public static void tick(ServerTickEvent.Post e){
        var live=new HashSet<UUID>();
        Map<Object,List<FlightChunkHints.Hint>> flightHints=new IdentityHashMap<>();
        Map<ServerLevel,Integer> ticketBudgets=new IdentityHashMap<>();
        for(var l:e.getServer().getAllLevels())for(var s:ships(l)){
            live.add(s.getUniqueId());var d=drives.computeIfAbsent(s.getUniqueId(),key->new Drive());var p=d.pilot==null?null:e.getServer().getPlayerList().getPlayer(d.pilot);
            // Proximity is checked when acquiring control, not while rider positions catch up.
            if(d.pilot!=null&&(!eligible(d,p)||!owner(s,p)||l.hasChunkAt(controller(s))&&!l.getBlockState(controller(s)).is(AirshipContent.CONTROLLER.get())))release(d,p);
            expireInput(d,l.getGameTime());
            if(d.pilot!=null&&p!=null)attachPilot(d,p);
            d.power=l.getBlockEntity(controller(s)) instanceof AirshipBlockEntity controller?controller.power():0;
            double time=l.getGameTime()/1200.0;d.windX=2*Math.sin(time);d.windZ=1.5*Math.cos(time*.7);
            var w=world(s,controller(s));var b=BlockPos.containing(w);
            var flightBody=RigidBodyHandle.of(s);
            if(d.pilot!=null&&flightBody!=null&&flightBody.isValid()){
                var velocity=flightBody.getLinearVelocity(new Vector3d());
                int heading=FlightChunkOrder.heading(velocity.x,velocity.z);
                if(heading>=0)flightHints.computeIfAbsent(l.getChunkSource().chunkMap,key->new ArrayList<>()).add(new FlightChunkHints.Hint(b.getX()>>4,b.getZ()>>4,heading));
            }
            var travel=flightBody!=null&&flightBody.isValid()?flightBody.getLinearVelocity(new Vector3d()):new Vector3d();
            double travelSpeed=Math.hypot(travel.x,travel.z);
            if(travelSpeed<40&&d.forward!=0){
                var facing=l.getBlockState(controller(s)).getOptionalValue(CivicBlock.FACING).orElse(Direction.NORTH);
                travel=s.logicalPose().transformNormal(new Vector3d(facing.getStepX()*d.forward,0,facing.getStepZ()*d.forward),new Vector3d());
            }
            double mass=Math.max(1,s.getMassTracker().getMass());
            double launchLead=Math.min(FlightTerrain.MAX_DISTANCE,AirshipFlight.driveDelta(d.power,mass,travelSpeed,.05)*.3);
            var facing=l.getBlockState(controller(s)).getOptionalValue(CivicBlock.FACING).orElse(Direction.NORTH);
            var nose=s.logicalPose().transformNormal(new Vector3d(facing.getStepX(),0,facing.getStepZ()),new Vector3d());
            if(d.throttle<0)nose.negate();
            double yawRate=flightBody!=null&&flightBody.isValid()?flightBody.getAngularVelocity(new Vector3d()).y:0;
            double acceleration=AirshipFlight.driveDelta(d.power,mass,travelSpeed,.05)/.05/AirshipFlight.HORIZONTAL_INERTIA*Math.abs(d.throttle);
            var cells=FlightTerrain.predictedCorridor(w.x,w.z,travel.x,travel.z,
                128+Math.max(travelSpeed*(.5+d.preparationSeconds),launchLead),nose.x,nose.z,yawRate,d.turn,acceleration);
            int budget=ticketBudgets.getOrDefault(l,AirshipTerrain.WORLD_BUDGET);
            int used=AirshipTerrain.update(d,l,s.getUniqueId(),cells,d.pilot!=null&&(travelSpeed>=40||d.forward!=0)&&d.power>0,budget);
            ticketBudgets.put(l,budget-used);
            if(p!=null&&d.pilot!=null&&l.getGameTime()%10==0){
                PacketDistributor.sendToPlayer(p,new AirshipPayload.State(true));
                p.displayClientMessage(Component.literal(String.format(Locale.ROOT,"Power %.3g W | %.1f blocks/s | %s | W/S thrust, A/D turn, Space/Ctrl height, Shift exit",d.power,speed(s),d.hold.isEmpty()?(d.streamingBrake?"Preparing terrain / smooth braking":"Hover assist"):d.hold)),true);
            }
        }
        FlightChunkHints.publish(flightHints);
        // Keep sessions across temporary vessel unloads; stale/missing vessels receive no thrust.
        for(var entry:drives.entrySet())if(!live.contains(entry.getKey())){
            var d=entry.getValue();AirshipTerrain.release(d);d.terrain=FlightTerrain.Snapshot.EMPTY;var p=d.pilot==null?null:e.getServer().getPlayerList().getPlayer(d.pilot);
            if(!eligible(d,p)){release(d,p);drives.remove(entry.getKey(),d);}
            else {d.forward=d.turn=d.vertical=0;d.throttle=0;if(p.serverLevel().getGameTime()%10==0)PacketDistributor.sendToPlayer(p,new AirshipPayload.State(true));}
        }
    }
    @SubscribeEvent public static void physics(ForgeSablePrePhysicsTickEvent e){for(var s:ships(e.getPhysicsSystem().getLevel()))step(s,e.getTimeStep());}
    public static void step(ServerSubLevel s,double dt){
        var d=drives.get(s.getUniqueId());if(d==null||!Double.isFinite(dt)||dt<=0)return;
        var body=RigidBodyHandle.of(s);if(body==null||!body.isValid())return;
        double mass=s.getMassTracker().getMass();if(!Double.isFinite(mass)||mass<=0)return;
        var pose=s.logicalPose();var v=body.getLinearVelocity(new Vector3d());var av=body.getAngularVelocity(new Vector3d());
        if(!v.isFinite()||!av.isFinite())return;
        var gravity=DimensionPhysicsData.getGravity(s.getLevel());
        double weight=Math.max(0,-gravity.y()*mass);var origin=world(s,controller(s));
        if(!Double.isFinite(d.altitude))d.altitude=origin.y;
        double available=d.power;
        double support=AirshipFlight.support(available,weight);
        double remaining=Math.max(0,available-support*10);
        double authority=Math.min(1,remaining/(mass*200));
        if(d.vertical!=0)d.altitude=origin.y+d.vertical*AirshipFlight.VERTICAL_SPEED;
        double lift=support+Math.clamp((d.altitude-origin.y)*3-v.y*3,-36,36)*mass*authority;
        lift=Math.clamp(lift,0,Math.min(available/10,weight+mass*36));
        double propulsion=Math.max(0,available-lift*10);
        var force=new Vector3d(0,lift,0);
        var localAir=pose.transformNormalInverse(new Vector3d(v).sub(d.windX,0,d.windZ),new Vector3d());var t=s.getUserDataTag();
        var drag=new Vector3d(AirshipFlight.drag(t.getDouble("areaX"),localAir.x),AirshipFlight.drag(t.getDouble("areaY"),localAir.y),AirshipFlight.drag(t.getDouble("areaZ"),localAir.z));
        // Prevent a stiff drag term reversing velocity within one substep.
        for(int axis=0;axis<3;axis++)drag.setComponent(axis,Math.clamp(drag.get(axis),-Math.abs(localAir.get(axis))*mass/dt,Math.abs(localAir.get(axis))*mass/dt));
        var worldDrag=pose.transformNormal(drag,new Vector3d());force.add(worldDrag);
        var facing=s.getLevel().getBlockState(controller(s)).getOptionalValue(CivicBlock.FACING).orElse(Direction.NORTH);
        var forward=pose.transformNormal(new Vector3d(facing.getStepX(),0,facing.getStepZ()),new Vector3d());forward.y=0;if(forward.lengthSquared()>1e-8)forward.normalize();
        double budget=mass*AirshipFlight.driveDelta(propulsion,mass,v.length(),dt)/dt;
        d.throttle=AirshipFlight.rampThrottle(d.throttle,d.forward,dt);
        boolean driving=Math.abs(d.throttle)>1e-8;
        if(driving){d.hoverX=Double.NaN;d.hoverZ=Double.NaN;}
        else if(!Double.isFinite(d.hoverX)&&Math.hypot(v.x,v.z)<.5){d.hoverX=origin.x;d.hoverZ=origin.z;}
        // Brake first, then retain the resting position. Counter known wind force within the same power budget.
        double errorX=Double.isFinite(d.hoverX)?d.hoverX-origin.x:0,errorZ=Double.isFinite(d.hoverZ)?d.hoverZ-origin.z:0;
        double damping=Double.isFinite(d.hoverX)?6:2;
        var requested=!driving?new Vector3d(mass*(errorX-v.x*damping)-worldDrag.x,0,mass*(errorZ-v.z*damping)-worldDrag.z):new Vector3d(forward).mul(d.throttle*budget);
        if(!driving&&requested.length()>budget)requested.normalize(budget);force.add(requested);
        // Prototype horizontal inertial mass: preserve the force balance/top speed, slow its response.
        force.x/=AirshipFlight.HORIZONTAL_INERTIA;force.z/=AirshipFlight.HORIZONTAL_INERTIA;
        var delta=new Vector3d(force).mul(dt/mass);
        var predicted=new Vector3d(v).add(delta).fma(dt,gravity);
        // Publish/read coverage as a unit. Prediction uses current position, so snapshot age consumes lead naturally.
        var terrain=d.terrain;
        double horizontal=Math.hypot(predicted.x,predicted.z);
        d.streamingBrake=false;
        if(Double.isFinite(horizontal)&&horizontal>1e-8){
            double distance=terrain.distance(origin.x,origin.z,predicted.x,predicted.z,FlightTerrain.MAX_DISTANCE);
            double permitted=FlightTerrain.permittedSpeed(Math.hypot(v.x,v.z),horizontal,distance,dt);
            if(permitted<horizontal-1e-6){
                predicted.x*=permitted/horizontal;predicted.z*=permitted/horizontal;
                delta.set(predicted).sub(v).fma(-dt,gravity);d.streamingBrake=true;
            }
        }
        // An emergency fallback remains for invalid state, height limits or unexpectedly lost coverage.
        if(!clearFlight(s,terrain,origin,predicted,Math.max(dt,.05))){
            d.hold="Unloaded terrain / height hold";
            var halt=new Vector3d(v).negate().fma(-dt,gravity);
            body.applyLinearImpulse(pose.transformNormalInverse(halt,new Vector3d()).mul(mass));
            var torque=pose.transformNormalInverse(new Vector3d(av).negate(),new Vector3d());s.getMassTracker().getInertiaTensor().transform(torque);body.applyAngularImpulse(torque);d.altitude=origin.y;return;
        }
        d.hold="";
        // Collision preparation may shorten the conservative sweep before Sable integrates this impulse.
        delta.set(predicted).sub(v).fma(-dt,gravity);
        body.applyLinearImpulse(pose.transformNormalInverse(delta,new Vector3d()).mul(mass));
        var up=pose.transformNormal(new Vector3d(0,1,0),new Vector3d());
        var correction=up.cross(new Vector3d(0,1,0)).mul(8).add(-av.x*4,-av.y*3-d.turn*3,-av.z*4).mul(authority);
        if(correction.length()>8)correction.normalize(8);
        var torque=pose.transformNormalInverse(correction,new Vector3d());s.getMassTracker().getInertiaTensor().transform(torque).mul(dt);body.applyAngularImpulse(torque);
    }
    private static boolean clearFlight(ServerSubLevel ship,FlightTerrain.Snapshot terrain,Vec3 origin,Vector3d velocity,double seconds){
        if(!velocity.isFinite())return false;
        double x=origin.x+velocity.x*seconds,y=origin.y+velocity.y*seconds,z=origin.z+velocity.z*seconds;
        int radius=RADIUS*2;
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||y<ship.getLevel().getMinBuildHeight()+radius||y>ship.getLevel().getMaxBuildHeight()-radius)return false;
        return terrain.sweep(origin.x,origin.z,velocity.x*seconds,velocity.z*seconds)
            &&AirshipTerrain.prepareCollision(ship,terrain,origin,velocity,seconds);
    }
}
