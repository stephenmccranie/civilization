package dev.civilization;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.joml.Vector3d;
import net.neoforged.neoforge.fluids.FluidStack;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
final class BoatVisualCheck {
    private static int ticks;private static volatile boolean ready;private static volatile Throwable failure;private static ServerSubLevel boat;private static Vector3d start;private static OilEngineEntity engine;
    static void tick(Minecraft mc){
        ticks++;var server=mc.getSingleplayerServer();if(failure!=null)throw new IllegalStateException("Boat check failed",failure);
        if(ticks==80){mc.options.pauseOnLostFocus=false;mc.options.guiScale().set(3);mc.options.fov().set(65);server.execute(()->{try{
            var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();
            // Remove only old fixture vessels in this disposable scene before rebuilding its basin.
            var container=dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(l);
            for(var old:java.util.List.copyOf(BoatSystem.boats(l)))container.removeSubLevel(old,dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason.REMOVED);
            for(int x=96;x<=176;x++)for(int z=-120;z<=32;z++){l.getChunk(new BlockPos(x,98,z));l.setBlockAndUpdate(new BlockPos(x,96,z),Blocks.STONE.defaultBlockState());for(int y=97;y<=110;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),y<100?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState());}
            var at=new BlockPos(136,100,10);l.setBlockAndUpdate(at,BoatContent.HELM.get().defaultBlockState());for(var part:BoatSystem.parts())MachineStructure.placePart(l,at,Direction.NORTH,part);
            boat=BoatSystem.launch(l,at,p);var helm=BoatSystem.helm(boat);var engineAt=helm.offset(0,0,1);l.setBlockAndUpdate(engineAt,OilEngineContent.ENGINE.get().defaultBlockState().setValue(CivicBlock.FACING,Direction.NORTH));for(var part:OilEngineStructure.PARTS)MachineStructure.placePart(l,engineAt,Direction.NORTH,part);engine=(OilEngineEntity)l.getBlockEntity(engineAt);engine.fuel.fill(new FluidStack(IndustrialContent.FUEL.get(),2000),EXECUTE);engine.oil.fill(new FluidStack(IndustrialContent.LUBE.get(),1000),EXECUTE);engine.enabled=true;engine.warmup=100;
            l.setBlockAndUpdate(helm.offset(2,0,-1),Blocks.CHEST.defaultBlockState());((ChestBlockEntity)l.getBlockEntity(helm.offset(2,0,-1))).setItem(0,KilnContent.STEEL.toStack(23));
            for(int x=-2;x<=2;x++)l.setBlockAndUpdate(helm.offset(x,0,-2),Blocks.OAK_FENCE.defaultBlockState());
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getInventory().clearContent();l.setDayTime(6000);l.setWeatherParameters(100000,0,false,false);ready=true;
        }catch(Throwable t){failure=t;}});}
        if(ticks>80&&!ready){ticks=81;return;}
        if(ticks==115)server.execute(()->{try{
            var p=server.getPlayerList().getPlayers().getFirst();var at=boat.logicalPose().transformPosition(net.minecraft.world.phys.Vec3.atLowerCornerOf(BoatSystem.helm(boat)).add(-1,0.1,0));
            p.teleportTo(server.overworld(),at.x,at.y,at.z,java.util.Set.of(),0,50);p.getInventory().selected=0;p.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(Blocks.OAK_PLANKS,8));
            var deck=BoatSystem.helm(boat).offset(-2,-1,1);
            if(!CivicAccess.allowed(server.overworld(),deck,p))throw new IllegalStateException("Owner cannot interact with deck");
            if(!BoatSystem.attached(boat,BoatSystem.helm(boat).offset(-3,-1,1)))throw new IllegalStateException("Connected edge cannot expand freely");
            server.overworld().setBlockAndUpdate(BoatSystem.helm(boat).offset(-2,0,1),Blocks.OAK_PLANKS.defaultBlockState());
        }catch(Throwable t){failure=t;}});
        if(ticks==130){mc.player.getInventory().selected=0;var edge=BoatSystem.helm(boat).offset(-2,0,1);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atLowerCornerOf(edge).add(0,.5,.5),Direction.WEST,edge,false);System.out.println("BOAT_PLACE_CLIENT stack="+mc.player.getMainHandItem()+" edge="+mc.level.getBlockState(edge)+" result="+mc.gameMode.useItemOn(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));}
        if(ticks==138)server.execute(()->{try{var l=server.overworld();var target=BoatSystem.helm(boat).offset(-3,0,1);if(!l.getBlockState(target).is(Blocks.OAK_PLANKS))throw new IllegalStateException("Actual client freeform placement beyond old band failed: "+l.getBlockState(target));var p=server.getPlayerList().getPlayers().getFirst();if(p.getMainHandItem().getCount()!=7)throw new IllegalStateException("Placement did not spend exactly one block");p.getInventory().clearContent();System.out.println("BOAT_FREEFORM_PLACEMENT_PASS");}catch(Throwable t){failure=t;}});
        if(ticks==140)server.execute(()->{try{var p=server.getPlayerList().getPlayers().getFirst();var at=boat.logicalPose().transformPosition(net.minecraft.world.phys.Vec3.atLowerCornerOf(BoatSystem.helm(boat)).add(-1,0.05,0));p.teleportTo(server.overworld(),at.x,at.y,at.z,java.util.Set.of(),180,0);start=new Vector3d(boat.logicalPose().position());BoatSystem.use(server.overworld(),BoatSystem.helm(boat),p,net.minecraft.world.InteractionHand.MAIN_HAND);}catch(Throwable t){failure=t;}});
        if(ticks==160)mc.options.keyUp.setDown(true);
        if(ticks==170)mc.options.keyUp.setDown(false);
        if(ticks==175)mc.options.keyUp.setDown(true);
        if(ticks==185)mc.options.keyUp.setDown(false);
        if(ticks==260){mc.options.keyRight.setDown(true);server.execute(()->{try{if(server.overworld().getBlockState(BoatSystem.helm(boat)).getValue(BoatHelmBlock.GEAR)!=3)throw new IllegalStateException("Lever/gear input did not select fast");double moved=boat.logicalPose().position().distance(start);if(moved<20)throw new IllegalStateException("Boat failed to travel under physical engine: "+moved+" fuel="+engine.fuel.getFluidAmount());System.out.println("BOAT_MOVEMENT_PASS distance="+moved+" mass="+boat.getMassTracker().getMass());}catch(Throwable t){failure=t;}});}
        if(ticks==300){mc.options.keyUp.setDown(false);mc.options.keyRight.setDown(false);mc.options.keyShift.setDown(true);}
        if(ticks==305)mc.options.keyShift.setDown(false);
        if(ticks==360)server.execute(()->{try{if(engine.fuel.getFluidAmount()>=2000||engine.oil.getFluidAmount()>=1000)throw new IllegalStateException("Physical engine products not consumed");if(Math.abs(boat.logicalPose().orientation().y())<.01)throw new IllegalStateException("Steering failed");if(server.overworld().getBlockState(BoatSystem.helm(boat)).getValue(BoatHelmBlock.GEAR)!=3)throw new IllegalStateException("Leaving reset throttle");BoatSystem.use(server.overworld(),BoatSystem.helm(boat),server.getPlayerList().getPlayers().getFirst(),net.minecraft.world.InteractionHand.MAIN_HAND);BoatSystem.input(server.getPlayerList().getPlayers().getFirst(),-1,0,false);BoatSystem.input(server.getPlayerList().getPlayers().getFirst(),-1,0,false);BoatSystem.input(server.getPlayerList().getPlayers().getFirst(),0,0,true);var chest=(ChestBlockEntity)server.overworld().getBlockEntity(BoatSystem.helm(boat).offset(2,0,-1));if(chest.getItem(0).getCount()!=23)throw new IllegalStateException("Cargo changed");System.out.println("BOAT_ENGINE_CARGO_PASS fuel="+engine.fuel.getFluidAmount()+" oil="+engine.oil.getFluidAmount()+" engines="+BoatSystem.engines(boat).size());var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);camera(p,5,4,9);}catch(Throwable t){failure=t;}});
        if(ticks==365)mc.options.hideGui=true;
        if(ticks==380)Screenshot.grab(mc.gameDirectory,"boat-overview.png",mc.getMainRenderTarget(),m->{});
        if(ticks==385)server.execute(()->camera(server.getPlayerList().getPlayers().getFirst(),2.8,2.3,4));
        if(ticks==400){Screenshot.grab(mc.gameDirectory,"boat.png",mc.getMainRenderTarget(),m->System.out.println(m.getString()));System.out.println("BOAT VISUAL VERIFIED");}
        if(ticks==420)server.execute(()->{try{var p=server.getPlayerList().getPlayers().getFirst();var at=boat.logicalPose().transformPosition(net.minecraft.world.phys.Vec3.atLowerCornerOf(BoatSystem.helm(boat)).add(-1,0.1,0));p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.teleportTo(server.overworld(),at.x,at.y,at.z,java.util.Set.of(),180,0);start=new Vector3d(boat.logicalPose().position());BoatSystem.use(server.overworld(),BoatSystem.helm(boat),p,net.minecraft.world.InteractionHand.MAIN_HAND);}catch(Throwable t){failure=t;}});
        if(ticks==435)mc.options.keyDown.setDown(true);
        if(ticks==445)mc.options.keyDown.setDown(false);
        if(ticks==490)server.execute(()->{try{var delta=new Vector3d(boat.logicalPose().position()).sub(start);var forward=boat.logicalPose().transformNormal(new Vector3d(0,0,-1),new Vector3d());if(delta.dot(forward)>-3||server.overworld().getBlockState(BoatSystem.helm(boat)).getValue(BoatHelmBlock.GEAR)!=0)throw new IllegalStateException("Reverse failed: "+delta);System.out.println("BOAT_REVERSE_PASS distance="+delta.length());}catch(Throwable t){failure=t;}});
        if(ticks==495)mc.options.keyUp.setDown(true);
        if(ticks==505)mc.options.keyUp.setDown(false);
        if(ticks==545)server.execute(()->{try{var body=dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle.of(boat);if(body.getLinearVelocity(new Vector3d()).length()>1||server.overworld().getBlockState(BoatSystem.helm(boat)).getValue(BoatHelmBlock.GEAR)!=1)throw new IllegalStateException("Neutral did not brake");System.out.println("BOAT_NEUTRAL_PASS");}catch(Throwable t){failure=t;}});
        if(ticks==550)mc.options.keyShift.setDown(true);
        if(ticks==555)mc.options.keyShift.setDown(false);
        if(ticks>560)mc.stop();
    }
    private static void camera(net.minecraft.server.level.ServerPlayer p,double x,double y,double z){
        var root=net.minecraft.world.phys.Vec3.atLowerCornerOf(BoatSystem.helm(boat));
        var eye=boat.logicalPose().transformPosition(root.add(x,y,z));var target=boat.logicalPose().transformPosition(root.add(.9,.9,.6));
        var delta=target.subtract(eye);float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));float pitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z)));
        p.teleportTo(p.serverLevel(),eye.x,eye.y-p.getEyeHeight(),eye.z,java.util.Set.of(),yaw,pitch);
    }
}

