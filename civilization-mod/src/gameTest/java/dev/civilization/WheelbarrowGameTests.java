package dev.civilization;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class WheelbarrowGameTests {
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h){
        var profile=new GameProfile(UUID.randomUUID(),"barrow-handler");
        var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,net.minecraft.server.level.ClientInformation.createDefault());
        // FakePlayer intentionally refuses all mounts; mock transport only.
        p.connection=new FakePlayer(h.getLevel(),profile).connection;p.setGameMode(GameType.SURVIVAL);return p;
    }
    private static WheelbarrowEntity barrow(GameTestHelper h){
        BlockPos a=h.absolutePos(new BlockPos(5,3,5));
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
            h.getLevel().setBlockAndUpdate(a.offset(x,-1,z),Blocks.STONE.defaultBlockState());
            for(int y=0;y<=3;y++)h.getLevel().setBlockAndUpdate(a.offset(x,y,z),Blocks.AIR.defaultBlockState());
        }
        var e=new WheelbarrowEntity(WheelbarrowContent.ENTITY.get(),h.getLevel());e.setPos(a.getX()+.5,a.getY(),a.getZ()+.5);h.getLevel().addFreshEntity(e);e.tick();return e;
    }
    private static void take(GameTestHelper h,WheelbarrowEntity e,net.minecraft.server.level.ServerPlayer p){p.setPos(e.gripPosition(e.position(),e.getYRot()));var result=e.interact(p,InteractionHand.MAIN_HAND);h.assertTrue(result.consumesAction()&&p.getVehicle()==e,"Empty-hand handler takes the handles: "+result+" vehicle="+p.getVehicle());}
    @GameTest(template="industrial") public static void handlesWorkAtDiagonalAndCardinalHeadings(GameTestHelper h){
        var e=barrow(h);
        for(float yaw:new float[]{0,33,45,90,135,180,225,270,315}){
            e.setYRot(yaw);e.setPos(e.position());var p=player(h);take(h,e,p);
            h.assertTrue(p.position().distanceTo(e.gripPosition(e.position(),yaw))<.001,"Handler stands at the scaled grip offset at "+yaw);
            p.stopRiding();
        }
        e.setYRot(45);e.setPos(e.position());var holding=player(h);holding.setItemInHand(InteractionHand.MAIN_HAND,WheelbarrowContent.ITEM.toStack());
        take(h,e,holding);h.assertTrue(holding.getMainHandItem().getCount()==1,"Taking handles while holding the placement item does not consume it");holding.stopRiding();
        h.succeed();
    }
    @GameTest(template="industrial") public static void solidBodyBlocksWalkingButHandlesAndEscapeStayClear(GameTestHelper h){
        var e=barrow(h);var p=player(h);
        p.setPos(e.position().add(-2.5,0,0));p.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(5,0,0));
        h.assertTrue(p.getX()<e.getX()-.73&&p.getX()>e.getX()-.77,"Solid tray body blocks a walking player");
        p.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(-1,0,0));
        h.assertTrue(p.getX()<e.getX()-1.7,"Brushing the solid body does not prevent walking away");
        p.setPos(e.position());p.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(2.5,0,0));
        h.assertTrue(p.getX()>e.getX()+2.4,"A player already overlapping the body can leave");
        e.setYRot(45);e.setPos(e.position());var grip=e.gripPosition(e.position(),45);
        p.setPos(grip.add(1,0,0));p.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(-1,0,0));
        h.assertTrue(p.position().distanceTo(grip)<.001,"Player can approach diagonal handles without striking an invisible envelope");
        h.assertTrue(e.canBeCollidedWith()&&e.isPickable(),"Carrier retains player collision and interaction");h.succeed();
    }
    @GameTest(template="industrial") public static void entireHandleAreaIsWalkableAndStillPickableAtEveryHeading(GameTestHelper h){
        var e=barrow(h);var p=player(h);
        for(float yaw:new float[]{0,17.125f,45,90,135,180,225,270,315}){
            e.setYRot(yaw);e.setPos(e.position());float angle=(180-yaw)*net.minecraft.util.Mth.DEG_TO_RAD;
            var start=e.position().add(new Vec3(1.2,0,1.35).yRot(angle));var end=e.position().add(new Vec3(-1.2,0,1.35).yRot(angle));p.setPos(start);
            var step=end.subtract(start).scale(1d/96);for(int i=0;i<96;i++)p.move(net.minecraft.world.entity.MoverType.SELF,step);
            h.assertTrue(p.position().distanceTo(end)<.002,"Players can walk across both visual handles without hitting invisible extensions at "+yaw);
            h.assertTrue(e.getBoundingBox().getZsize()<e.interactionBounds().getZsize()+1e-5,"Interaction reach is separate from physical body bounds");
            p.setPos(e.gripPosition(e.position(),yaw));
            var target=e.position().add(new Vec3(.3586,.8,1.4343).yRot(angle));var eye=p.getEyePosition();var ray=target.subtract(eye).normalize().scale(3);
            var hit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(p,eye,eye.add(ray),p.getBoundingBox().expandTowards(ray).inflate(1),candidate->candidate==e,9);
            h.assertTrue(hit!=null&&hit.getEntity()==e&&e.interactAt(p,hit.getLocation().subtract(e.position()),InteractionHand.MAIN_HAND).consumesAction()&&p.getVehicle()==e,"Non-solid handles remain targetable and grabbable through the native mouse-ray path at "+yaw);p.stopRiding();
        }h.succeed();
    }
    @GameTest(template="industrial") public static void serverInputMovesStopsTurnsAndKeepsOneHandler(GameTestHelper h){
        var e=barrow(h);var p=player(h);take(h,e,p);var other=player(h);
        e.input(other,new WheelbarrowPayload(1,1,false));var start=e.position();e.tick();
        h.assertTrue(e.position().distanceTo(start)<.001,"Non-handler input cannot move a carrier");
        e.input(p,new WheelbarrowPayload(127,0,false));for(int i=0;i<10;i++)e.tick();
        h.assertTrue(Math.abs(e.getZ()-start.z-PlayerMovement.WALK/2)<.001,"Server clamps input and pushes at normal walking speed");
        float wheel=e.travel();e.input(p,new WheelbarrowPayload(0,1,false));e.tick();
        h.assertTrue(e.getYRot()>0&&e.travel()==wheel,"Stationary turning does not spin the wheel");
        h.assertTrue(!other.startRiding(e),"Two handlers cannot take one carrier");
        p.stopRiding();var parked=e.position();for(int i=0;i<5;i++)e.tick();
        h.assertTrue(e.position().distanceTo(parked)<.001&&!e.held(),"Released carrier stops instead of coasting");h.succeed();
    }
    @GameTest(template="industrial") public static void playerCanJumpOntoTrayStandAndWalkOff(GameTestHelper h){
        var e=barrow(h);var p=player(h);double y=e.getY();p.setPos(e.position().add(-1.3,0,0));
        double velocity=.42;
        for(int i=0;i<35;i++){
            double dx=p.getX()<e.getX()?.09:0;
            p.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(dx,velocity,0));velocity=p.onGround()?-.08:(velocity-.08)*.98;
        }
        h.assertTrue(Math.abs(p.getY()-y-WheelbarrowCollision.TRAY_TOP)<.002&&p.onGround(),"Ordinary jump lands on the visible rim-height tray: "+p.position());
        for(int i=0;i<10;i++)p.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0,-.08,0));
        h.assertTrue(Math.abs(p.getY()-y-WheelbarrowCollision.TRAY_TOP)<.002,"Standing on the carrier remains stable");
        p.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(1.5,0,0));for(int i=0;i<20;i++)p.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0,-.1,0));
        h.assertTrue(Math.abs(p.getY()-y)<.002&&p.getX()>e.getX()+1,"Walking off the tray returns to ground without sticking");h.succeed();
    }
    @GameTest(template="industrial") public static void diagonalEmptyCornersAndBrushingRemainClear(GameTestHelper h){
        var e=barrow(h);var p=player(h);e.setYRot(45);e.setPos(e.position());
        p.setPos(e.position().add(.9,0,.8));
        h.assertTrue(h.getLevel().noCollision(p,p.getBoundingBox()),"Empty diagonal bounding-box corner is not solid");
        var before=p.position();p.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0,0,.5));
        h.assertTrue(p.position().distanceTo(before.add(0,0,.5))<.001,"Brushing past a diagonal carrier does not catch on invisible box corners");h.succeed();
    }
    @GameTest(template="industrial") public static void forwardMotionKeepsExactFractionalHeadingWithoutSideDrift(GameTestHelper h){
        var e=barrow(h);var p=player(h);take(h,e,p);var center=e.position();
        for(float yaw:new float[]{0,17.125f,33.333f,89.875f,135.25f,225.125f,315.375f}){
            e.setYRot(yaw);e.setPos(center);e.input(p,new WheelbarrowPayload(1,0,false));for(int i=0;i<10;i++)e.tick();
            var delta=e.position().subtract(center);var forward=new Vec3(0,0,1).yRot(-yaw*net.minecraft.util.Mth.DEG_TO_RAD);
            h.assertTrue(Math.abs(delta.x*forward.z-delta.z*forward.x)<1e-6&&Math.abs(delta.horizontalDistance()-PlayerMovement.WALK/2)<.001,"W follows its precise fractional heading without a lateral component: "+yaw);
        }h.succeed();
    }
    @GameTest(template="industrial") public static void pushingCostsWalkingCaloriesForActualHandleTravel(GameTestHelper h){
        var e=barrow(h);var p=player(h);take(h,e,p);var data=CalorieFoodData.of(p);data.reserve().set(1000);
        var start=e.position();e.input(p,new WheelbarrowPayload(1,0,false));for(int i=0;i<10;i++)e.tick();
        double distance=e.position().subtract(start).horizontalDistance();
        double factor=ThermalRules.calorieFactor(ThermalSystem.comfort(p))*data.workFactor();
        h.assertTrue(Math.abs(data.reserve().calories()-(1000-distance*CalorieConfig.WALK.get()*factor))<1e-6,"Forward travel spends exactly walking calories, once");
        double spent=data.travelSpent;var grip=e.gripPosition(e.position(),e.getYRot());
        e.input(p,new WheelbarrowPayload(0,1,false));e.tick();
        double arc=e.gripPosition(e.position(),e.getYRot()).subtract(grip).horizontalDistance();
        h.assertTrue(arc>.1&&Math.abs(data.travelSpent-spent-arc*CalorieConfig.WALK.get()*factor)<1e-6,"Turning charges the handler's actual walk around the handles");
        double before=data.reserve().calories();e.input(p,new WheelbarrowPayload(0,0,false));for(int i=0;i<5;i++)e.tick();
        h.assertTrue(data.reserve().calories()==before,"Holding still spends no pushing calories");
        p.setGameMode(GameType.CREATIVE);e.input(p,new WheelbarrowPayload(-1,0,false));e.tick();
        h.assertTrue(data.reserve().calories()==before,"Creative handling is exempt");
        p.setGameMode(GameType.SURVIVAL);data.reserve().set(.001);e.tick();
        h.assertTrue(data.reserve().calories()==0&&data.isDepleted(),"Pushing uses shared depletion without calorie debt");h.succeed();
    }
    @GameTest(template="industrial") public static void blockedPushingCostsNothingAndReverseUsesActualDistance(GameTestHelper h){
        var e=barrow(h);var p=player(h);take(h,e,p);var data=CalorieFoodData.of(p);data.reserve().set(1000);
        double wallZ=e.getZ()+2.5;
        for(int x=-2;x<=2;x++)for(int y=0;y<2;y++)h.getLevel().setBlockAndUpdate(BlockPos.containing(e.getX()+x,e.getY()+y,wallZ),Blocks.STONE.defaultBlockState());
        e.input(p,new WheelbarrowPayload(1,0,false));for(int i=0;i<30;i++)e.tick();
        double stopped=data.reserve().calories();for(int i=0;i<5;i++)e.tick();
        h.assertTrue(data.reserve().calories()==stopped,"Holding forward against a wall costs no travel calories");
        var start=e.position();e.input(p,new WheelbarrowPayload(-1,0,false));for(int i=0;i<10;i++)e.tick();
        double distance=e.position().subtract(start).horizontalDistance();
        h.assertTrue(Math.abs(distance-PlayerMovement.WALK/4)<.001,"Backing up uses half walking speed");
        h.assertTrue(data.reserve().calories()<stopped&&Math.abs(data.walkDistance-data.travelSpent/(CalorieConfig.WALK.get()*ThermalRules.calorieFactor(ThermalSystem.comfort(p))))<1e-5,"Reverse travel spends positive walking calories");
        h.succeed();
    }
    @GameTest(template="industrial") public static void bodyAndHandlerCannotPassSolidWalls(GameTestHelper h){
        var e=barrow(h);var p=player(h);take(h,e,p);double wallZ=e.getZ()+2.5;
        for(int x=-2;x<=2;x++)for(int y=0;y<2;y++)h.getLevel().setBlockAndUpdate(BlockPos.containing(e.getX()+x,e.getY()+y,wallZ),Blocks.STONE.defaultBlockState());
        e.input(p,new WheelbarrowPayload(1,0,false));for(int i=0;i<35;i++)e.tick();
        h.assertTrue(e.getBoundingBox().maxZ<=Math.floor(wallZ)+.001,"Long carrier footprint stops at a wall");
        e.input(p,new WheelbarrowPayload(0,0,false));var pos=e.position();e.tick();h.assertTrue(e.position().distanceTo(pos)<.001,"Blocked carrier remains parked");h.succeed();
    }
    @GameTest(template="industrial") public static void halfStepStillClimbs(GameTestHelper h){
        var e=barrow(h);var p=player(h);take(h,e,p);double y=e.getY();int z=(int)Math.floor(e.getZ()+2);
        for(int x=-2;x<=2;x++)h.getLevel().setBlockAndUpdate(BlockPos.containing(e.getX()+x,y,z),Blocks.STONE_SLAB.defaultBlockState());
        e.input(p,new WheelbarrowPayload(1,0,false));for(int i=0;i<20;i++)e.tick();
        h.assertTrue(e.getY()>y+.45&&e.getY()<y+.6,"Ordinary collision stepping climbs a slab");h.succeed();
    }
    @GameTest(template="industrial") public static void climbsSuccessiveOneBlockInclines(GameTestHelper h){
        var e=barrow(h);
        // Keep the three rises and standing handler inside the 12x6x12 fixture.
        for(int x=1;x<=9;x++)for(int z=1;z<=10;z++)for(int height=0;height<=5;height++)
            h.getLevel().setBlockAndUpdate(h.absolutePos(new BlockPos(x,height,z)),height==0?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
        var base=h.absolutePos(new BlockPos(5,1,3));e.setPos(base.getX()+.5,base.getY(),base.getZ()+.5);e.tick();
        var p=player(h);take(h,e,p);double y=e.getY();int first=(int)Math.floor(e.getZ()+2);
        for(int x=-3;x<=3;x++)for(int z=first;z<=first+5;z++)for(int height=0;height<=Math.min(2,(z-first)/2);height++)
            h.getLevel().setBlockAndUpdate(BlockPos.containing(e.getX()+x,y+height,z),Blocks.STONE.defaultBlockState());
        e.input(p,new WheelbarrowPayload(1,0,false));for(int i=0;i<30;i++)e.tick();
        e.positionRider(p);
        h.assertTrue(e.getY()>y+2.9&&e.getY()<y+3.1,"Successive one-block rises climb without jumping");
        h.assertTrue(Math.abs(p.getY()-e.getY())<.001&&h.getLevel().noCollision(p,p.getBoundingBox()),"Handler follows incline without colliding with terrain");h.succeed();
    }
    @GameTest(template="industrial") public static void oneBlockStepRequiresHandlerHeadroom(GameTestHelper h){
        var e=barrow(h);var p=player(h);take(h,e,p);double y=e.getY();int first=(int)Math.floor(e.getZ()+2);
        for(int x=-3;x<=3;x++)for(int z=first;z<=first+3;z++)h.getLevel().setBlockAndUpdate(BlockPos.containing(e.getX()+x,y,z),Blocks.STONE.defaultBlockState());
        var grip=e.gripPosition(e.position(),e.getYRot());
        for(int x=-1;x<=1;x++)for(int z=-1;z<=2;z++)h.getLevel().setBlockAndUpdate(BlockPos.containing(grip.x+x,y+2,grip.z+z),Blocks.STONE.defaultBlockState());
        e.input(p,new WheelbarrowPayload(1,0,false));for(int i=0;i<15;i++)e.tick();e.positionRider(p);
        h.assertTrue(Math.abs(e.getY()-y)<.001&&h.getLevel().noCollision(p,p.getBoundingBox()),"Low handler ceiling prevents the step instead of embedding the player");h.succeed();
    }
    @GameTest(template="industrial") public static void pickupAndPlacementConserveOneEmptyCarrier(GameTestHelper h){
        var e=barrow(h);var p=player(h);p.setPos(e.position().add(2,0,0));p.setShiftKeyDown(true);
        e.interact(p,InteractionHand.MAIN_HAND);h.assertTrue(e.isRemoved()&&p.getInventory().countItem(WheelbarrowContent.ITEM.get())==1,"Recovery removes exactly one entity and returns one item");
        e.interact(p,InteractionHand.MAIN_HAND);h.assertTrue(p.getInventory().countItem(WheelbarrowContent.ITEM.get())==1,"Repeated recovery cannot duplicate the item");
        p.setShiftKeyDown(false);p.setItemInHand(InteractionHand.MAIN_HAND,WheelbarrowContent.ITEM.toStack());var pos=BlockPos.containing(e.position()).below();
        var ctx=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atBottomCenterOf(pos.above()),Direction.UP,pos,false));
        h.assertTrue(WheelbarrowContent.ITEM.get().useOn(ctx).consumesAction()&&p.getMainHandItem().isEmpty(),"Successful placement consumes one item");h.succeed();
    }
    @GameTest(template="industrial") public static void saveReloadKeepsIdentityAndClearsDriveInput(GameTestHelper h){
        var e=barrow(h);var p=player(h);take(h,e,p);e.input(p,new WheelbarrowPayload(1,0,false));e.tick();
        var tag=new CompoundTag();e.saveWithoutId(tag);var restored=new WheelbarrowEntity(WheelbarrowContent.ENTITY.get(),h.getLevel());restored.load(tag);
        h.assertTrue(restored.getUUID().equals(e.getUUID())&&restored.travel()==e.travel(),"Native persistence retains identity and wheel distance");
        var pos=restored.position();restored.tick();h.assertTrue(restored.position().subtract(pos).horizontalDistance()<.001&&!restored.held(),"Reload never restores stale pushing input");h.succeed();
    }
    @GameTest(template="industrial") public static void accessRevocationReleasesHandlerAndDeniesRecovery(GameTestHelper h){
        var e=barrow(h);var p=player(h);take(h,e,p);var data=CivicData.get(h.getLevel().getServer());var claim=new CivicData.Claim();
        claim.at=new CivicData.Address(h.getLevel().dimension().location().toString(),e.blockPosition());claim.owner=new CivicData.Owner(UUID.randomUUID(),false);claim.radius=8;claim.height=32;claim.upkeep=1;claim.coalUnit=3600000;claim.energy=claim.coalUnit*200;claim.lastUpdate=System.currentTimeMillis();data.claims.put(claim.at,claim);data.rebuildIndex();
        try{e.input(p,new WheelbarrowPayload(1,0,false));e.tick();h.assertTrue(!p.isPassenger()&&!e.held(),"Access loss releases the current handler");p.setShiftKeyDown(true);e.interact(p,InteractionHand.MAIN_HAND);h.assertTrue(!e.isRemoved()&&p.getInventory().countItem(WheelbarrowContent.ITEM.get())==0,"Denied recovery cannot steal a carrier");}
        finally{data.claims.remove(claim.at);data.rebuildIndex();}h.succeed();
    }
}
