package dev.civilization;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.tags.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3d;
import java.util.*;
@EventBusSubscriber(modid="civilization")
public final class BoatSystem {
    public static final String TAG="civilization_boat";
    public static final int MAX_BLOCKS=4096;
    /** One original 31-block hull with its console remains the one-engine tuning reference. */
    static final class Drive {volatile UUID pilot;volatile int throttle,steer;volatile long last;volatile double power,mass;volatile int engines;volatile double surface=Double.NaN;}
    private static final Map<UUID,Drive> drives=new java.util.concurrent.ConcurrentHashMap<>();
    public static List<MachineStructure.Part> parts(){var a=new ArrayList<MachineStructure.Part>();for(int x=-2;x<=2;x++)for(int z=-3;z<=3;z++)if(!(Math.abs(x)==2&&Math.abs(z)==3))a.add(new MachineStructure.Part(x,-1,z,"planks",4,Direction.DOWN));return List.copyOf(a);}
    public static ServerSubLevel at(ServerLevel l,BlockPos p){var c=SubLevelContainer.getContainer(l);if(c==null||!c.inBounds(p))return null;var plot=c.getPlot(new ChunkPos(p));return plot!=null&&plot.getSubLevel() instanceof ServerSubLevel s&&isBoat(s)?s:null;}
    /** Sable fragments and unrelated vessels can legitimately have no custom data. */
    private static boolean isBoat(ServerSubLevel s){var tag=s.getUserDataTag();return tag!=null&&tag.getBoolean(TAG)&&tag.contains("helm")&&s.getLevel().getBlockState(BlockPos.of(tag.getLong("helm"))).is(BoatContent.HELM.get());}
    public static BlockPos helm(ServerSubLevel s){return BlockPos.of(s.getUserDataTag().getLong("helm"));}
    public static Vec3 world(ServerSubLevel s,BlockPos p){return s.logicalPose().transformPosition(Vec3.atCenterOf(p));}
    public static boolean owner(ServerSubLevel s,net.minecraft.world.entity.player.Player p){return isBoat(s)&&p!=null&&(p.hasPermissions(2)||s.getUserDataTag().hasUUID("owner")&&s.getUserDataTag().getUUID("owner").equals(p.getUUID()));}
    /** Positions may expand freely from any existing vessel block; there is no shape envelope. */
    public static boolean attached(ServerSubLevel s,BlockPos pos){for(var d:Direction.values()){var n=pos.relative(d);if(!s.getLevel().getBlockState(n).isAir()&&at(s.getLevel(),n)==s)return true;}return false;}
    public static List<ServerSubLevel> boats(ServerLevel l){var c=SubLevelContainer.getContainer(l);return c==null?List.of():c.getAllSubLevels().stream().filter(BoatSystem::isBoat).toList();}
    private static boolean supported(BlockState state){
        if(state.isAir()||!state.getFluidState().isEmpty()||state.getBlock() instanceof FallingBlock||state.getBlock() instanceof TntBlock||state.getBlock() instanceof net.minecraft.world.level.block.piston.PistonBaseBlock||state.is(Blocks.BEDROCK)||state.is(BlockTags.PORTALS)||state.is(AirshipContent.CONTROLLER.get())||state.getBlock() instanceof CivicBlock||state.getBlock() instanceof SurveyBlock)return false;
        return !state.hasBlockEntity()||state.is(BoatContent.HELM.get())||state.is(Blocks.CHEST)||state.is(Blocks.BARREL)||state.is(CuttingContent.PIECE.get())||state.getBlock() instanceof OilEngineBlock||state.getBlock() instanceof BulkBlock;
    }
    public static List<BlockPos> scan(ServerLevel l,BlockPos root,ServerPlayer player){
        var container=SubLevelContainer.getContainer(l);if(container!=null&&container.inBounds(root))throw new IllegalStateException("Launch from the world, not from another vessel.");
        var rootState=l.getBlockState(root);if(!rootState.is(BoatContent.HELM.get())||rootState.getValue(BoatHelmBlock.PANEL))throw new IllegalStateException("Use the wheel side of the helm.");
        var mate=BoatHelmBlock.other(root,rootState);var found=new LinkedHashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();found.add(root);queue.add(root);int helms=0,water=0;
        while(!queue.isEmpty()){
            var pos=queue.remove();var state=l.getBlockState(pos);
            if(!CivicAccess.allowed(l,pos,player))throw new IllegalStateException("Boat crosses protected land.");
            if(!supported(state))throw new IllegalStateException("Remove unsupported blocks, fluids, pistons, portals or civic machinery before launch.");
            if(state.is(BoatContent.HELM.get())&&++helms>2)throw new IllegalStateException("A boat may contain one two-block helm.");
            if(l.getFluidState(pos.below()).is(FluidTags.WATER)&&l.getFluidState(pos.below(2)).is(FluidTags.WATER))water++;
            for(var direction:Direction.values()){
                var next=pos.relative(direction);if(found.contains(next))continue;
                if(!l.hasChunkAt(next))throw new IllegalStateException("Load the complete boat and its surroundings first.");
                if(l.getBlockState(next).isAir()||!l.getFluidState(next).isEmpty())continue;
                if(found.size()>=MAX_BLOCKS)throw new IllegalStateException("Boat must be separate from terrain and contain at most 4,096 blocks.");
                found.add(next.immutable());queue.add(next.immutable());
            }
        }
        var mateState=l.getBlockState(mate);
        if(!found.contains(mate)||helms!=2||!mateState.is(BoatContent.HELM.get())||!mateState.getValue(BoatHelmBlock.PANEL)||mateState.getValue(BoatHelmBlock.FACING)!=rootState.getValue(BoatHelmBlock.FACING))throw new IllegalStateException("Complete the two-block helm.");
        if(found.size()<8)throw new IllegalStateException("Build a connected hull of at least eight blocks.");
        if(water<4)throw new IllegalStateException("At least four hull blocks need water two blocks deep beneath them.");
        return List.copyOf(found);
    }
    public static ServerSubLevel launch(ServerLevel l,BlockPos pos,ServerPlayer p){
        var state=l.getBlockState(pos);if(state.is(BoatContent.HELM.get())&&!state.getValue(BoatHelmBlock.PANEL)){
            var panel=BoatHelmBlock.other(pos,state);if(l.getBlockState(panel).isAir()&&CivicAccess.allowed(l,panel,p))l.setBlockAndUpdate(panel,state.setValue(BoatHelmBlock.PANEL,true));
        }
        var blocks=scan(l,pos,p);
        if(!CivicAccess.allowed(l,pos,p))throw new IllegalStateException("This land is protected.");
        var ship=SubLevelAssemblyHelper.assembleBlocks(l,pos,blocks,BoundingBox3i.from(blocks));ship.setName("Motor Vessel");
        var tag=new CompoundTag();tag.putBoolean(TAG,true);tag.putUUID("owner",p.getUUID());tag.putLong("helm",ship.getPlot().getCenterBlock().asLong());ship.setUserDataTag(tag);return ship;
    }
    public static void use(ServerLevel l,BlockPos pos,ServerPlayer p,InteractionHand hand){
        var ship=at(l,pos);
        if(ship==null){try{launch(l,pos,p);p.displayClientMessage(Component.literal("Vessel launched. Build and start a Hot-Bulb Engine, then use the helm."),false);}catch(IllegalStateException ex){p.displayClientMessage(Component.literal(ex.getMessage()),true);}return;}
        if(!owner(ship,p)||p.position().distanceToSqr(world(ship,pos))>36)return;
        var stack=p.getItemInHand(hand);
        if(!stack.isEmpty()||p.isShiftKeyDown()){status(ship,p);return;}
        var drive=drive(ship);
        if(drive.pilot!=null&&!drive.pilot.equals(p.getUUID())){p.displayClientMessage(Component.literal("Helm is in use."),true);return;}
        drive.pilot=p.getUUID();drive.last=l.getGameTime();PacketDistributor.sendToPlayer(p,new BoatPayload.State(true));status(ship,p);
    }
    public static void input(ServerPlayer p,int throttle,int steer,boolean exit){for(var s:boats(p.serverLevel())){var d=drives.get(s.getUniqueId());if(d!=null&&p.getUUID().equals(d.pilot)){if(exit){release(d,p);return;}d.throttle=Math.clamp(d.throttle+Math.clamp(throttle,-1,1),-1,2);d.steer=Math.clamp(steer,-1,1);d.last=p.serverLevel().getGameTime();return;}}}
    private static Drive drive(ServerSubLevel s){return drives.computeIfAbsent(s.getUniqueId(),k->{var d=new Drive();d.throttle=s.getLevel().getBlockState(helm(s)).getOptionalValue(BoatHelmBlock.GEAR).orElse(1)-1;return d;});}
    private static String gear(int n){return switch(n){case -1->"REVERSE";case 1->"SLOW";case 2->"FAST";default->"NEUTRAL";};}
    private static void console(ServerSubLevel s,int gear){
        var l=s.getLevel();var pos=helm(s);var state=l.getBlockState(pos);if(!state.is(BoatContent.HELM.get()))return;
        var panel=BoatHelmBlock.other(pos,state);
        // Upgrade old single-block helms only into empty space; never replace cabin or cargo.
        if(l.getBlockState(panel).isAir())l.setBlockAndUpdate(panel,state.setValue(BoatHelmBlock.PANEL,true));
        for(var p:List.of(pos,panel)){var b=l.getBlockState(p);if(b.is(BoatContent.HELM.get())&&b.getValue(BoatHelmBlock.GEAR)!=gear+1)l.setBlockAndUpdate(p,b.setValue(BoatHelmBlock.GEAR,gear+1));}
    }
    static void release(Drive d,ServerPlayer p){d.pilot=null;d.steer=0;if(p!=null)PacketDistributor.sendToPlayer(p,new BoatPayload.State(false));}
    private static void status(ServerSubLevel s,ServerPlayer p){var d=drive(s);p.displayClientMessage(Component.literal(String.format(Locale.ROOT,"%s | Engines %d | Output %.2f | Mass %.1f | W/S: gear · A/D: steer · Shift: leave helm",gear(d.throttle),d.engines,d.power,d.mass)),true);}
    static List<OilEngineEntity> engines(ServerSubLevel s){var out=new ArrayList<OilEngineEntity>();for(var holder:s.getPlot().getLoadedChunks())for(BlockEntity entity:holder.getChunk().getBlockEntities().values())if(entity instanceof OilEngineEntity engine)out.add(engine);return out;}
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){drives.clear();}
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        var live=new HashSet<UUID>();
        for(var l:event.getServer().getAllLevels())for(var s:boats(l)){
            live.add(s.getUniqueId());var d=drive(s);var p=d.pilot==null?null:event.getServer().getPlayerList().getPlayer(d.pilot);
            var w=world(s,helm(s));
            if(p==null||!p.isAlive()||p.serverLevel()!=l||!owner(s,p)||p.position().distanceToSqr(w)>36||l.getGameTime()-d.last>15||!l.getBlockState(helm(s)).is(BoatContent.HELM.get()))release(d,p);
            console(s,d.throttle);
            d.surface=Double.NaN;
            var base=BlockPos.containing(w);
            boolean loaded=true;for(int x=-8;x<=8;x+=8)for(int z=-8;z<=8;z+=8)if(!l.hasChunkAt(base.offset(x,0,z)))loaded=false;
            if(loaded)for(int y=1;y>=-3;y--){var at=base.offset(0,y,0);var fluid=l.getFluidState(at);if(fluid.is(FluidTags.WATER)){d.surface=at.getY()+fluid.getHeight(l,at);break;}}
            double demand=l.getBlockState(helm(s)).is(BoatContent.HELM.get())&&Double.isFinite(d.surface)?(d.throttle==2?1:d.throttle==1?6.0/14:d.throttle==-1?3.0/14:0):0;
            var engines=engines(s);d.engines=engines.size();d.mass=Math.max(1,s.getMassTracker().getMass());d.power=0;
            if(demand>0)for(var engine:engines)d.power+=engine.requestWork(engine.getBlockPos(),demand);
            if(p!=null&&d.pilot!=null&&l.getGameTime()%20==0){PacketDistributor.sendToPlayer(p,new BoatPayload.State(true));status(s,p);}
        }drives.keySet().retainAll(live);
    }
    @SubscribeEvent public static void physics(ForgeSablePrePhysicsTickEvent e){for(var s:boats(e.getPhysicsSystem().getLevel()))step(s,e.getTimeStep());}
    public static void step(ServerSubLevel s,double dt){
        if(!isBoat(s))return;
        var d=drives.get(s.getUniqueId());if(d==null)return;var body=RigidBodyHandle.of(s);if(body==null||!body.isValid())return;
        double mass=s.getMassTracker().getMass();if(!Double.isFinite(mass)||mass<=0)return;d.mass=mass;
        var pose=s.logicalPose();var v=body.getLinearVelocity(new Vector3d());var av=body.getAngularVelocity(new Vector3d());var origin=world(s,helm(s));boolean wet=Double.isFinite(d.surface);
        double drag=d.power>0?.8:3;
        var accel=new Vector3d(-v.x*drag,wet?Math.clamp(11+8*(d.surface+1.1-origin.y)-5*v.y,-25,25):0,-v.z*drag);
        var impulse=pose.transformNormalInverse(accel,new Vector3d()).mul(mass*dt);
        var facing=s.getLevel().getBlockState(helm(s)).getOptionalValue(BoatHelmBlock.FACING).orElse(Direction.NORTH);
        // Each engine supplies fixed thrust. More vessel mass therefore reduces both
        // acceleration and terminal speed; additional running engines add linearly.
        if(wet&&d.power>0){double thrust=BoatEngine.driveAcceleration(d.power,mass)*mass*dt;impulse.x+=facing.getStepX()*Math.signum(d.throttle)*thrust;impulse.z+=facing.getStepZ()*Math.signum(d.throttle)*thrust;}
        body.applyLinearImpulse(impulse);
        var up=pose.transformNormal(new Vector3d(0,1,0),new Vector3d());var correction=up.cross(new Vector3d(0,1,0)).mul(wet?12:0).add(-av.x*5,-av.y*3,-av.z*5);
        if(wet)correction.y+=-d.steer*d.power*1.8*BoatEngine.REFERENCE_MASS/mass;
        var torque=pose.transformNormalInverse(correction,new Vector3d());s.getMassTracker().getInertiaTensor().transform(torque).mul(dt);body.applyAngularImpulse(torque);
    }
}
