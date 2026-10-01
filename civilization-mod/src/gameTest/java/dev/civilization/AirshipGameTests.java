package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

@GameTestHolder("civilization")
@PrefixGameTestTemplate(false)
public final class AirshipGameTests {
    @GameTest(template="empty",templateNamespace="civilization",timeoutTicks=200)
    public static void disassemblyPreservesCargoCutCellsAndRejectsObstructions(GameTestHelper h){
        var l=h.getLevel();var player=FakePlayerFactory.get(l,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"DisassemblyTest"));
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        for(int turn=0;turn<4;turn++){
            var root=new BlockPos(4400+turn*32,180,4400);
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){l.getChunk(root.offset(x*16,0,z*16));}
            l.setBlockAndUpdate(root,AirshipContent.CONTROLLER.get().defaultBlockState());
            ((AirshipBlockEntity)l.getBlockEntity(root)).power(1e9);
            l.setBlockAndUpdate(root.east(),Blocks.CHEST.defaultBlockState());
            ((net.minecraft.world.level.block.entity.ChestBlockEntity)l.getBlockEntity(root.east())).setItem(0,KilnContent.STEEL.toStack(23));
            l.setBlockAndUpdate(root.west(),CuttingContent.PIECE.get().defaultBlockState());
            var cells=new net.minecraft.world.level.block.state.BlockState[8];cells[0]=Blocks.OAK_PLANKS.defaultBlockState();cells[7]=Blocks.STONE.defaultBlockState();
            ((CutBlockEntity)l.getBlockEntity(root.west())).cells(cells);
            player.setPos(root.getCenter().add(0,2,0));
            var ship=AirshipSystem.launch(l,root,player,AirshipSystem.scan(l,root,player));
            var handle=dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle.of(ship);
            handle.addLinearAndAngularVelocity(new org.joml.Vector3d(10,0,0),new org.joml.Vector3d());boolean movingDenied=false;
            try{AirshipDisassembly.disassemble(ship,player);}catch(IllegalStateException ex){movingDenied=true;}
            h.assertTrue(movingDenied,"Moving ships cannot disassemble");
            handle.addLinearAndAngularVelocity(new org.joml.Vector3d(-10,0,0),new org.joml.Vector3d());
            // Rotate about the controller rather than the center of mass.
            var anchor=AirshipSystem.controller(ship);var before=AirshipSystem.world(ship,anchor);
            ship.logicalPose().orientation().rotationY(turn*Math.PI/2);
            var after=AirshipSystem.world(ship,anchor);ship.logicalPose().position().add(before.x-after.x,before.y-after.y,before.z-after.z);
            var rotation=new net.minecraft.world.level.block.Rotation[]{net.minecraft.world.level.block.Rotation.NONE,net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90,net.minecraft.world.level.block.Rotation.CLOCKWISE_180,net.minecraft.world.level.block.Rotation.CLOCKWISE_90}[turn];
            var transform=new dev.ryanhcode.sable.api.SubLevelAssemblyHelper.AssemblyTransform(anchor,root,turn,rotation,l);
            var chestPos=transform.apply(anchor.east());var cutPos=transform.apply(anchor.west());
            l.setBlockAndUpdate(chestPos,Blocks.BEDROCK.defaultBlockState());boolean denied=false;
            try{AirshipDisassembly.disassemble(ship,player);}catch(IllegalStateException ex){denied=true;}
            h.assertTrue(denied&&l.getBlockState(chestPos).is(Blocks.BEDROCK)&&l.getBlockState(anchor).is(AirshipContent.CONTROLLER.get()),"Obstruction must preserve both world and ship");
            l.setBlockAndUpdate(chestPos,Blocks.AIR.defaultBlockState());
            var placed=AirshipDisassembly.disassemble(ship,player);
            h.assertTrue(placed.equals(root)&&l.getBlockState(root).is(AirshipContent.CONTROLLER.get()),"Controller returns to world grid");
            h.assertTrue(((AirshipBlockEntity)l.getBlockEntity(root)).power()==1e9,"Controller power survives");
            h.assertTrue(((net.minecraft.world.level.block.entity.ChestBlockEntity)l.getBlockEntity(chestPos)).getItem(0).getCount()==23,"Cargo transfers once");
            var expected=new net.minecraft.world.level.block.state.BlockState[8];expected[new int[]{0,4,5,1}[turn]]=Blocks.OAK_PLANKS.defaultBlockState();expected[new int[]{7,3,2,6}[turn]]=Blocks.STONE.defaultBlockState();
            h.assertTrue(java.util.Arrays.equals(((CutBlockEntity)l.getBlockEntity(cutPos)).cells(),expected),"Mixed cells rotate without material loss");
            h.assertTrue(l.getBlockState(anchor).isAir(),"Original blocks removed");
            for(var pos:java.util.List.of(root,chestPos,cutPos)){l.removeBlockEntity(pos);l.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());}
        }
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace="civilization")
    public static void preparationBatchesRetainTicketsAndReleaseObsoletePaths(GameTestHelper h){
        var drive=new AirshipSystem.Drive();var id=java.util.UUID.randomUUID();var l=h.getLevel();
        var origin=h.absolutePos(BlockPos.ZERO);
        var cells=FlightTerrain.predictedCorridor(origin.getX(),origin.getZ(),1000,0,2000,1,0,0,0,0);
        try{
            h.assertTrue(AirshipTerrain.update(drive,l,id,cells,true,1024)==128,"First request batch is bounded");
            var retained=new it.unimi.dsi.fastutil.longs.LongOpenHashSet(drive.tickets);
            h.assertTrue(AirshipTerrain.update(drive,l,id,cells,true,1024)==256,"Next batch extends preparation");
            h.assertTrue(drive.tickets.containsAll(retained),"Existing tickets survive batch admission");
            h.assertTrue(AirshipTerrain.update(drive,l,id,cells,true,64)==64,"World budget is respected");
            h.assertTrue(drive.preparing.size()<=64,"Canceled requests do not retain latency state");
            AirshipTerrain.update(drive,l,id,java.util.Set.of(),false,1024);
            h.assertTrue(drive.tickets.isEmpty()&&drive.preparing.isEmpty(),"Obsolete paths release all owned state");
        }finally{AirshipTerrain.release(drive);}
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace="civilization")
    public static void pilotAttachmentIsTransientAndReleases(GameTestHelper h){
        var l=h.getLevel();var seat=new AirshipPilotSeat(AirshipContent.PILOT_SEAT.get(),l);
        seat.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2,3,2))));l.addFreshEntity(seat);
        var rider=new net.minecraft.world.entity.decoration.ArmorStand(l,seat.getX(),seat.getY(),seat.getZ());l.addFreshEntity(rider);
        h.assertTrue(rider.startRiding(seat,true),"Pilot anchor accepts a rider");seat.positionRider(rider);
        h.assertTrue(rider.position().distanceToSqr(seat.position())<1e-9,"Anchor preserves standing position");
        h.assertTrue(seat.getType().is(dev.ryanhcode.sable.index.SableTags.RETAIN_IN_SUB_LEVEL),"Anchor stays in ship coordinates");
        h.assertTrue(!seat.getType().canSerialize(),"Pilot anchors do not persist as abandoned seats");
        var drive=new AirshipSystem.Drive();drive.pilot=java.util.UUID.randomUUID();drive.seat=seat;
        AirshipSystem.release(drive,null);
        h.assertTrue(seat.isRemoved()&&!rider.isPassenger()&&drive.seat==null,"Release removes the anchor and passenger link");rider.discard();h.succeed();
    }
    @GameTest(template="empty",templateNamespace="civilization")
    public static void staleControlsRetainPilotSession(GameTestHelper h){
        var drive=new AirshipSystem.Drive();var pilot=java.util.UUID.randomUUID();
        drive.pilot=pilot;drive.last=100;drive.forward=1;drive.turn=-1;drive.vertical=1;
        AirshipSystem.expireInput(drive,115);
        h.assertTrue(drive.forward==1,"Recent inputs remain active");
        AirshipSystem.expireInput(drive,116);
        h.assertTrue(drive.forward==0&&drive.turn==0&&drive.vertical==0,"Lost packets neutralize all movement inputs");
        h.assertTrue(pilot.equals(drive.pilot),"Streaming or packet gaps must not release the pilot");
        AirshipSystem.release(drive,null);
        h.assertTrue(drive.pilot==null,"Explicit release still clears the session");h.succeed();
    }
    @GameTest(template="empty",templateNamespace="civilization")
    public static void powerPersistsWithoutUpperGameplayCap(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(2,3,2));var l=h.getLevel();l.setBlockAndUpdate(pos,AirshipContent.CONTROLLER.get().defaultBlockState());
        var block=(AirshipBlockEntity)l.getBlockEntity(pos);block.power(1e250);
        var saved=block.saveWithFullMetadata(l.registryAccess());var copy=new AirshipBlockEntity(pos,block.getBlockState());copy.loadWithComponents(saved,l.registryAccess());
        h.assertTrue(copy.power()==1e250,"Extreme finite power must persist exactly");
        boolean rejected=false;try{block.power(Double.NaN);}catch(IllegalArgumentException e){rejected=true;}
        h.assertTrue(rejected&&block.power()==1e250,"Invalid packets cannot overwrite valid power");h.succeed();
    }
    @GameTest(template="empty",templateNamespace="civilization")
    public static void assemblyRejectsTerrainAndUnsupportedControllers(GameTestHelper h){
        var l=h.getLevel();var root=new BlockPos(4200,150,4200);l.getChunk(root);var player=FakePlayerFactory.getMinecraft(l);
        l.setBlockAndUpdate(root,AirshipContent.CONTROLLER.get().defaultBlockState());l.setBlockAndUpdate(root.east(),Blocks.OAK_PLANKS.defaultBlockState());
        h.assertTrue(AirshipSystem.scan(l,root,player).size()==2,"Freeform connected blocks should assemble");
        l.setBlockAndUpdate(root.east(2),CivicContent.LAND.get().defaultBlockState());
        // Fixed-world controllers without block entities must also be explicitly rejected.
        boolean denied=false;try{AirshipSystem.scan(l,root,player);}catch(IllegalStateException e){denied=true;}
        h.assertTrue(denied,"Fixed-world controller must be rejected");l.setBlockAndUpdate(root.east(2),Blocks.AIR.defaultBlockState());
        for(int i=2;i<=13;i++){l.getChunk(root.east(i));l.setBlockAndUpdate(root.east(i),Blocks.OAK_PLANKS.defaultBlockState());}
        denied=false;try{AirshipSystem.scan(l,root,player);}catch(IllegalStateException e){denied=true;}
        h.assertTrue(denied,"Connected structure outside bounds must fail without moving blocks");
        h.assertTrue(l.getBlockState(root).is(AirshipContent.CONTROLLER.get()),"Failed scan preserves controller");
        for(int i=0;i<=13;i++)l.setBlockAndUpdate(root.east(i),Blocks.AIR.defaultBlockState());h.succeed();
    }
}
