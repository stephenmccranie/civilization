package dev.civilization;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.joml.Vector3d;
import net.neoforged.neoforge.fluids.FluidStack;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
/** A launched boat: freeform building, the helm at rest, its wheel and telegraph answering real keys, the boat under way,
 *  reverse and neutral, and the helm as a held item, an icon, a placement preview and a placed block. */
final class BoatVisualCheck {
    /** A stone quay clear of the boat, where the helm is held and placed. */
    private static final BlockPos QUAY=new BlockPos(123,100,23);
    private static int ticks;private static volatile boolean ready;private static volatile Throwable failure;private static ServerSubLevel boat;private static Vector3d start;private static OilEngineEntity engine;
    static void tick(Minecraft mc){
        ticks++;var server=mc.getSingleplayerServer();if(failure!=null)throw new IllegalStateException("Boat check failed",failure);
        if(ticks==80){mc.options.pauseOnLostFocus=false;mc.options.guiScale().set(3);mc.options.fov().set(65);server.execute(()->{try{
            var l=server.overworld();var p=player(server);
            // Remove only old fixture vessels in this disposable scene before rebuilding its basin.
            var container=dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(l);
            for(var old:java.util.List.copyOf(BoatSystem.boats(l)))container.removeSubLevel(old,dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason.REMOVED);
            for(int x=96;x<=176;x++)for(int z=-120;z<=32;z++){l.getChunk(new BlockPos(x,98,z));l.setBlockAndUpdate(new BlockPos(x,96,z),Blocks.STONE.defaultBlockState());for(int y=97;y<=110;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),y<100?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState());}
            for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)for(int y=97;y<=100;y++)l.setBlockAndUpdate(QUAY.offset(x,y-100,z),Blocks.STONE_BRICKS.defaultBlockState());
            // The hull with a stern deck behind it: the helm forward, the pilot's deck, then the engine.
            var at=new BlockPos(136,100,10);l.setBlockAndUpdate(at,BoatContent.HELM.get().defaultBlockState());for(var part:BoatSystem.parts())MachineStructure.placePart(l,at,Direction.NORTH,part);
            for(int x=0;x<=1;x++)for(int z=4;z<=7;z++)l.setBlockAndUpdate(at.offset(x,-1,z),Blocks.OAK_PLANKS.defaultBlockState());
            boat=BoatSystem.launch(l,at,p);var helm=BoatSystem.helm(boat);engine=OilEngineGameTests.place(l,helm.offset(0,0,4),Direction.NORTH);engine.fuel.fill(new FluidStack(IndustrialContent.FUEL.get(),2000),EXECUTE);engine.oil.fill(new FluidStack(IndustrialContent.LUBE.get(),1000),EXECUTE);engine.enabled=false;
            l.setBlockAndUpdate(helm.offset(2,0,-1),Blocks.CHEST.defaultBlockState());((ChestBlockEntity)l.getBlockEntity(helm.offset(2,0,-1))).setItem(0,KilnContent.STEEL.toStack(23));
            for(int x=-2;x<=2;x++)l.setBlockAndUpdate(helm.offset(x,0,-2),Blocks.OAK_FENCE.defaultBlockState());
            p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();l.setDayTime(6000);l.setWeatherParameters(100000,0,false,false);ready=true;
        }catch(Throwable t){failure=t;}});}
        if(ticks>80&&!ready){ticks=81;return;}
        // The owner builds onto the launched hull by a real click.
        if(ticks==115)check(server,()->{
            var p=player(server);var at=boat.logicalPose().transformPosition(Vec3.atLowerCornerOf(BoatSystem.helm(boat)).add(-1,0.1,0));
            p.teleportTo(server.overworld(),at.x,at.y,at.z,java.util.Set.of(),0,50);p.getInventory().setItem(BlockCarry.OFFHAND,new ItemStack(Blocks.OAK_PLANKS,8));
            var deck=BoatSystem.helm(boat).offset(-2,-1,1);
            if(!CivicAccess.allowed(server.overworld(),deck,p))throw new IllegalStateException("Owner cannot interact with deck");
            if(!BoatSystem.attached(boat,BoatSystem.helm(boat).offset(-3,-1,1)))throw new IllegalStateException("Connected edge cannot expand freely");
            server.overworld().setBlockAndUpdate(BoatSystem.helm(boat).offset(-2,0,1),Blocks.OAK_PLANKS.defaultBlockState());
        });
        if(ticks==130){var edge=BoatSystem.helm(boat).offset(-2,0,1);var hit=new net.minecraft.world.phys.BlockHitResult(Vec3.atLowerCornerOf(edge).add(0,.5,.5),Direction.WEST,edge,false);System.out.println("BOAT_PLACE_CLIENT stack="+mc.player.getOffhandItem()+" edge="+mc.level.getBlockState(edge)+" result="+mc.gameMode.useItemOn(mc.player,InteractionHand.OFF_HAND,hit));}
        if(ticks==138)check(server,()->{var l=server.overworld();var target=BoatSystem.helm(boat).offset(-3,0,1);if(!l.getBlockState(target).is(Blocks.OAK_PLANKS))throw new IllegalStateException("Actual client freeform placement beyond old band failed: "+l.getBlockState(target));var p=player(server);if(p.getOffhandItem().getCount()!=7)throw new IllegalStateException("Placement did not spend exactly one block");p.getInventory().clearContent();System.out.println("BOAT_FREEFORM_PLACEMENT_PASS");});
        // The helm at rest with its engine stopped, then its wheel and telegraph answering the pilot's keys.
        if(ticks==140){mc.options.hideGui=true;check(server,()->{var p=player(server);p.setGameMode(GameType.SPECTATOR);camera(p,2.7,2.0,2.9,1.0,.85,.5);BoatSystem.use(server.overworld(),BoatSystem.helm(boat),p,InteractionHand.MAIN_HAND);});}
        if(ticks==155){shows(server,"At rest",1,1);shot(mc,"boat-helm");}
        if(ticks==156)mc.options.keyLeft.setDown(true);
        if(ticks==170){shows(server,"A held",1,0);shot(mc,"boat-helm-port");}
        if(ticks==171){mc.options.keyLeft.setDown(false);mc.options.keyRight.setDown(true);}
        if(ticks==185){shows(server,"D held",1,2);shot(mc,"boat-helm-starboard");}
        if(ticks==186)mc.options.keyRight.setDown(false);
        if(ticks==198)shows(server,"Keys released",1,1);
        if(ticks==200){check(server,()->camera(player(server),3.3,1.7,1.4,1.5,1.1,.5));mc.options.keyDown.setDown(true);}
        if(ticks==203)mc.options.keyDown.setDown(false);
        if(ticks==214){shows(server,"S pressed",0,1);shot(mc,"boat-gear-reverse");}
        for(int gear=1;gear<=3;gear++){
            int pressed=200+15*gear;
            if(ticks==pressed)mc.options.keyUp.setDown(true);
            if(ticks==pressed+3)mc.options.keyUp.setDown(false);
            if(ticks==pressed+14){shows(server,"W pressed",gear,1);shot(mc,"boat-gear-"+new String[]{"neutral","slow","fast"}[gear-1]);}
        }
        // Under way in fast: the pilot's own view, steering to starboard, and the turned wheel from the deck beside it.
        if(ticks==262){mc.options.hideGui=false;check(server,()->{
            var p=player(server);var at=boat.logicalPose().transformPosition(Vec3.atLowerCornerOf(BoatSystem.helm(boat)).add(.5,.05,2.1));
            p.setGameMode(GameType.SURVIVAL);p.teleportTo(server.overworld(),at.x,at.y,at.z,java.util.Set.of(),180,18);
            engine.enabled=true;engine.warmup=100;start=new Vector3d(boat.logicalPose().position());
        });}
        if(ticks==330)shot(mc,"boat-underway-pilot");
        if(ticks==331)mc.options.keyRight.setDown(true);
        if(ticks==355){shows(server,"Steering under way",3,2);shot(mc,"boat-underway-steering");}
        if(ticks==362){mc.options.hideGui=true;check(server,()->camera(player(server),2.7,1.67,2.9,1.0,.95,.5));}
        if(ticks==372)shot(mc,"boat-underway");
        if(ticks==382)check(server,()->{double moved=boat.logicalPose().position().distance(start);if(moved<20)throw new IllegalStateException("Boat failed to travel under physical engine: "+moved+" fuel="+engine.fuel.getFluidAmount());System.out.println("BOAT_MOVEMENT_PASS distance="+moved+" mass="+boat.getMassTracker().getMass());});
        if(ticks==383){mc.options.keyRight.setDown(false);mc.options.keyShift.setDown(true);}
        if(ticks==388)mc.options.keyShift.setDown(false);
        if(ticks==440){shows(server,"Helm left in fast",3,1);check(server,()->{
            var p=player(server);
            if(engine.fuel.getFluidAmount()>=2000||engine.oil.getFluidAmount()>=1000)throw new IllegalStateException("Physical engine products not consumed");
            if(Math.abs(boat.logicalPose().orientation().y())<.01)throw new IllegalStateException("Steering failed");
            BoatSystem.use(server.overworld(),BoatSystem.helm(boat),p,InteractionHand.MAIN_HAND);BoatSystem.input(p,-1,0,false);BoatSystem.input(p,-1,0,false);BoatSystem.input(p,0,0,true);
            var chest=(ChestBlockEntity)server.overworld().getBlockEntity(BoatSystem.helm(boat).offset(2,0,-1));if(chest.getItem(0).getCount()!=23)throw new IllegalStateException("Cargo changed");
            System.out.println("BOAT_ENGINE_CARGO_PASS fuel="+engine.fuel.getFluidAmount()+" oil="+engine.oil.getFluidAmount()+" engines="+BoatSystem.engines(boat).size());
            p.setGameMode(GameType.SPECTATOR);camera(p,6,5,12,.9,.9,2);
        });}
        if(ticks==460)shot(mc,"boat-overview");
        if(ticks==465)check(server,()->camera(player(server),3.5,3,6.5,.9,.9,1.5));
        if(ticks==480){shot(mc,"boat");System.out.println("BOAT VISUAL VERIFIED");}
        // Reverse, then neutral, by real keys.
        if(ticks==500)check(server,()->{var p=player(server);var at=boat.logicalPose().transformPosition(Vec3.atLowerCornerOf(BoatSystem.helm(boat)).add(-1,0.1,0));p.setGameMode(GameType.SURVIVAL);p.teleportTo(server.overworld(),at.x,at.y,at.z,java.util.Set.of(),180,0);start=new Vector3d(boat.logicalPose().position());BoatSystem.use(server.overworld(),BoatSystem.helm(boat),p,InteractionHand.MAIN_HAND);});
        if(ticks==515)mc.options.keyDown.setDown(true);
        if(ticks==525)mc.options.keyDown.setDown(false);
        if(ticks==570){shows(server,"Reverse",0,1);check(server,()->{var delta=new Vector3d(boat.logicalPose().position()).sub(start);var forward=boat.logicalPose().transformNormal(new Vector3d(0,0,-1),new Vector3d());if(delta.dot(forward)>-3)throw new IllegalStateException("Reverse failed: "+delta);System.out.println("BOAT_REVERSE_PASS distance="+delta.length());});}
        if(ticks==575)mc.options.keyUp.setDown(true);
        if(ticks==585)mc.options.keyUp.setDown(false);
        if(ticks==625){shows(server,"Neutral",1,1);check(server,()->{if(dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle.of(boat).getLinearVelocity(new Vector3d()).length()>1)throw new IllegalStateException("Neutral did not brake");System.out.println("BOAT_NEUTRAL_PASS");});}
        if(ticks==630)mc.options.keyShift.setDown(true);
        if(ticks==635)mc.options.keyShift.setDown(false);
        // The helm carried in the Hand on the quay, as Survival carries it: held with its preview and icon, in the inventory, as others see it, then placed by a real click.
        if(ticks==640){mc.options.hideGui=false;mc.gui.setOverlayMessage(net.minecraft.network.chat.Component.empty(),false);check(server,()->{var p=player(server);p.getInventory().clearContent();p.getInventory().setItem(BlockCarry.OFFHAND,new ItemStack(BoatContent.HELM.get()));p.teleportTo(server.overworld(),QUAY.getX()+.5,QUAY.getY()+1,QUAY.getZ()+2.5,java.util.Set.of(),180,35);});}
        if(ticks==665){
            var plan=dev.civilization.client.PlacementPlan.of(mc);
            if(plan==null||!plan.valid()||plan.cells().size()!=2||!plan.at().equals(QUAY.above()))throw new IllegalStateException("Held helm previews "+plan);
            shot(mc,"boat-helm-held");
        }
        if(ticks==667)mc.setScreen(new InventoryScreen(mc.player));
        if(ticks==680)shot(mc,"boat-helm-icon");
        if(ticks==681){mc.setScreen(null);mc.options.hideGui=true;mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);check(server,()->{var p=player(server);p.teleportTo(server.overworld(),p.getX(),p.getY(),p.getZ(),java.util.Set.of(),180,0);});}
        if(ticks==696)shot(mc,"boat-helm-held-third");
        if(ticks==697){mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.hideGui=false;check(server,()->{var p=player(server);p.teleportTo(server.overworld(),p.getX(),p.getY(),p.getZ(),java.util.Set.of(),180,35);});}
        if(ticks==706)KeyMapping.click(mc.options.keyUse.getKey());
        if(ticks==716){mc.options.hideGui=true;check(server,()->{
            var l=server.overworld();var wheel=l.getBlockState(QUAY.above());var telegraph=l.getBlockState(QUAY.above().west());
            if(!wheel.is(BoatContent.HELM.get())||wheel.getValue(BoatHelmBlock.PANEL)||wheel.getValue(BoatHelmBlock.FACING)!=Direction.SOUTH||!telegraph.is(BoatContent.HELM.get())||!telegraph.getValue(BoatHelmBlock.PANEL))
                throw new IllegalStateException("The click placed "+wheel+" and "+telegraph);
            var p=player(server);if(!p.getOffhandItem().isEmpty())throw new IllegalStateException("Placement did not spend the helm");
            // Facing south, the pilot's side of the placed helm looks north.
            p.setGameMode(GameType.SPECTATOR);p.teleportTo(l,QUAY.getX()-1.6,QUAY.getY()+2.2-p.getEyeHeight(),QUAY.getZ()-1.9,java.util.Set.of(),-33,22);
        });}
        if(ticks==734)shot(mc,"boat-helm-placed");
        if(ticks>740)mc.stop();
    }
    private static ServerPlayer player(MinecraftServer server){return server.getPlayerList().getPlayers().getFirst();}
    private static void check(MinecraftServer server,Runnable step){server.execute(()->{try{step.run();}catch(Throwable t){failure=t;}});}
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,name+".png",mc.getMainRenderTarget(),m->System.out.println(m.getString()));}
    /** Both halves of the helm show this engine order and steering. */
    private static void shows(MinecraftServer server,String when,int gear,int steer){check(server,()->{
        var l=server.overworld();var at=BoatSystem.helm(boat);var wheel=l.getBlockState(at);
        for(var state:java.util.List.of(wheel,l.getBlockState(BoatHelmBlock.other(at,wheel))))
            if(!state.is(BoatContent.HELM.get())||state.getValue(BoatHelmBlock.GEAR)!=gear||state.getValue(BoatHelmBlock.STEER)!=steer)throw new IllegalStateException(when+": helm shows "+state+", not gear "+gear+" steer "+steer);
    });}
    /** Looks from `eye` to `target`, both measured in blocks from the helm's lower north-west corner aboard the boat. */
    private static void camera(ServerPlayer p,double ex,double ey,double ez,double tx,double ty,double tz){
        var root=Vec3.atLowerCornerOf(BoatSystem.helm(boat));
        var eye=boat.logicalPose().transformPosition(root.add(ex,ey,ez));var target=boat.logicalPose().transformPosition(root.add(tx,ty,tz));
        var delta=target.subtract(eye);float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));float pitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z)));
        p.teleportTo(p.serverLevel(),eye.x,eye.y-p.getEyeHeight(),eye.z,java.util.Set.of(),yaw,pitch);
    }
}
