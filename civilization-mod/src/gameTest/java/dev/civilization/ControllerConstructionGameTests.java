package dev.civilization;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class ControllerConstructionGameTests {
    private static ItemStack supply(MachineStructure.Part part) {
        var material=MachineStructure.materialState(part.material());
        return part.units()==4?new ItemStack(material.getBlock()):CuttingContent.stack(material,part.units(),1);
    }

    @GameTest(template="empty",batch="construction-controllers") public static void everyGuideControllerPlacesItsFirstRequiredPiece(GameTestHelper h) {
        var level=h.getLevel();var at=h.absolutePos(new BlockPos(8,67,8));var front=Direction.EAST;
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"guide-builder"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(at.getX()+.5,at.getY()+2,at.getZ()-3);
        var hit=new BlockHitResult(Vec3.atCenterOf(at),Direction.NORTH,at,false);
        List<Block> controllers=List.of(KilnContent.KILN.get(),KilnContent.RETORT.get(),KilnContent.FOUNDRY.get(),
                WorkshopContent.TANNERY.get(),WorkshopContent.TEXTILE.get(),WorkshopContent.SMITHY.get(),
                IndustrialContent.REFINERY.get(),IndustrialContent.DRILL.get(),
                IndustrialContent.COLUMN.get(),IndustrialContent.CONDENSER.get(),BulkContent.BUNKER.get(),
                BulkContent.TANK.get(),OilEngineContent.ENGINE.get(),CivicContent.TABLE.get());
        for(var block:controllers) {
            var state=block.defaultBlockState().setValue(CivicBlock.FACING,front);
            level.setBlockAndUpdate(at,state);
            var part=MachineStructure.guideParts(state).stream().filter(p->!p.material().equals("air")).findFirst().orElseThrow();
            var target=MachineStructure.position(at,front,part);
            level.getChunkAt(target);
            level.setBlockAndUpdate(target,Blocks.AIR.defaultBlockState());
            var stack=supply(part);
            player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            h.assertTrue(player.gameMode.useItemOn(player,level,stack,InteractionHand.MAIN_HAND,hit).consumesAction(),"Controller accepts a held part: "+block);
            h.assertTrue(stack.isEmpty()&&MachineConstruction.present(level,target,part,front),"Correct first part is placed and charged once: "+block);
            level.removeBlock(target,false);
            level.removeBlock(at,false);
        }
        h.succeed();
    }

    @GameTest(template="empty",batch="construction-shared") public static void sharedWorkshopCellAcceptsEachPieceOnce(GameTestHelper h) {
        var level=h.getLevel();var at=h.absolutePos(new BlockPos(8,67,8));var front=Direction.NORTH;
        var state=WorkshopContent.TEXTILE.get().defaultBlockState().setValue(CivicBlock.FACING,front);
        level.setBlockAndUpdate(at,state);
        var shared=WorkshopStructure.parts(1).stream().filter(p->p.sharedMask()!=0).toList();
        var foot=shared.getFirst();
        var top=shared.stream().filter(p->p.x()==foot.x()&&p.y()==foot.y()&&p.depth()==foot.depth()&&p!=foot).findFirst().orElseThrow();
        var target=MachineStructure.position(at,front,foot);
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"shared-builder"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(at.getX()+.5,at.getY()+2,at.getZ()-3);
        var hit=new BlockHitResult(Vec3.atCenterOf(at),Direction.NORTH,at,false);
        var first=supply(foot);player.setItemInHand(InteractionHand.MAIN_HAND,first);
        player.gameMode.useItemOn(player,level,first,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(first.isEmpty()&&MachineConstruction.present(level,target,foot,front)&&!MachineConstruction.present(level,target,top,front),"First piece occupies only its own half-grid cells");
        var second=supply(top);player.setItemInHand(InteractionHand.MAIN_HAND,second);
        player.gameMode.useItemOn(player,level,second,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(second.isEmpty()&&MachineConstruction.present(level,target,foot,front)&&MachineConstruction.present(level,target,top,front),"Sibling joins without replacing or paying for first piece again");
        h.succeed();
    }

    @GameTest(template="empty",batch="construction-materials") public static void controllerNeverCutsHeldBlocks(GameTestHelper h) {
        var level=h.getLevel();var at=h.absolutePos(new BlockPos(8,67,8));var front=Direction.NORTH;
        var state=KilnContent.KILN.get().defaultBlockState().setValue(CivicBlock.FACING,front);
        level.setBlockAndUpdate(at,state);
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"cut-check"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(at.getX()+.5,at.getY()+2,at.getZ()-3);
        for(int units=1;units<=3;units++) {
            int size=units;
            var part=MachineStructure.guideParts(state).stream().filter(p->p.units()==size).findFirst().orElseThrow();
            var target=MachineStructure.position(at,front,part);
            level.getChunkAt(target);
            level.setBlockAndUpdate(target,Blocks.AIR.defaultBlockState());
            var full=new ItemStack(Blocks.COBBLESTONE,4);
            h.assertTrue(!MachineConstruction.hasMissing(level,at,front,List.of(part),full),"Full blocks do not supply a "+units+"-unit cut piece");
            h.assertTrue(MachineConstruction.build(level,at,front,List.of(part),player,full)==0&&full.getCount()==4&&level.getBlockState(target).isAir(),"Controller cannot cut full blocks into "+units+"-unit pieces");
            int other=units==1?2:1;
            var wrong=CuttingContent.stack(Blocks.COBBLESTONE.defaultBlockState(),other,4);
            h.assertTrue(!MachineConstruction.hasMissing(level,at,front,List.of(part),wrong),"A different cut size does not supply a "+units+"-unit piece");
            h.assertTrue(MachineConstruction.build(level,at,front,List.of(part),player,wrong)==0&&wrong.getCount()==4&&level.getBlockState(target).isAir(),"Controller cannot recut a "+other+"-unit piece");
        }
        h.succeed();
    }
}
