package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class OvenGameTests {
    @GameTest(template="industrial") public static void bodyHeightAndLegacyResize(GameTestHelper h){
        var at=h.absolutePos(new BlockPos(4,1,4));clear(h,at);var l=h.getLevel();var p=player(h);p.setYRot(0);p.setItemInHand(InteractionHand.MAIN_HAND,BakingOvenContent.ITEM.toStack());
        for(int i=0;i<4;i++)l.setBlockAndUpdate(at.offset(BakingOvenBlock.offset(Direction.NORTH,i)).above(3),Blocks.STONE.defaultBlockState());
        h.assertTrue(place(h,p,at).consumesAction(),"Oven body fits its two-block envelope with flue clearance");
        var oven=(BakingOvenEntity)l.getBlockEntity(at);oven.turn(.037);
        // Front cells contain only the body; the rear upper cells also contain the flue.
        for(int i=0;i<6;i++){var q=at.offset(BakingOvenBlock.offset(Direction.NORTH,i));var s=l.getBlockState(q);var shape=s.getCollisionShape(l,q);if(!shape.isEmpty())h.assertTrue(shape.bounds().maxY+q.getY()-at.getY()<=1.800001,"Body collision stays within standing player height");}
        for(int i=12;i<16;i++)l.setBlockAndUpdate(at.offset(BakingOvenBlock.offset(Direction.NORTH,i)),oven.getBlockState().setValue(BakingOvenBlock.CELL,i));
        var preserved=at.offset(BakingOvenBlock.offset(Direction.NORTH,15));oven.removing=true;l.setBlockAndUpdate(preserved,Blocks.STONE.defaultBlockState());oven.removing=false;
        var saved=oven.saveWithoutMetadata(l.registryAccess());saved.remove("sizeVersion");oven.loadWithComponents(saved,l.registryAccess());oven.serverTick();
        h.assertTrue(l.getBlockEntity(at)==oven&&Math.abs(oven.dial()-.537)<1e-8,"Migration retains controller and continuous setting");
        for(int i=12;i<15;i++)h.assertTrue(l.getBlockState(at.offset(BakingOvenBlock.offset(Direction.NORTH,i))).isAir(),"Legacy upper cells are removed");
        h.assertTrue(l.getBlockState(preserved).is(Blocks.STONE)&&oven.saveWithoutMetadata(l.registryAccess()).getInt("sizeVersion")==1,"Migration preserves unrelated blocks and records completion");
        h.succeed();
    }
    private static net.neoforged.neoforge.common.util.FakePlayer player(GameTestHelper h){var p=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"oven"));p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);return p;}
    private static void clear(GameTestHelper h,BlockPos pos){for(var q:BlockPos.betweenClosed(pos.offset(-2,-1,-2),pos.offset(2,4,2)))h.getLevel().setBlockAndUpdate(q,q.getY()==pos.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());}
    private static InteractionResult place(GameTestHelper h,net.minecraft.world.entity.player.Player p,BlockPos at){return p.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(at.below().getCenter().add(0,.5,0),Direction.UP,at.below(),false)));}
    @GameTest(template="industrial") public static void rotatedPlacementAndPartialBreak(GameTestHelper h){var at=h.absolutePos(new BlockPos(4,1,4));var p=player(h);for(var front:Direction.Plane.HORIZONTAL){clear(h,at);p.setYRot(front.getOpposite().toYRot());p.setItemInHand(InteractionHand.MAIN_HAND,BakingOvenContent.ITEM.toStack(2));h.assertTrue(place(h,p,at).consumesAction()&&p.getMainHandItem().getCount()==1,"Actual placement consumes one oven");var oven=(BakingOvenEntity)h.getLevel().getBlockEntity(at);h.assertTrue(oven!=null&&oven.getBlockState().getValue(CivicBlock.FACING)==front,"Controller faces player");for(int i=0;i<12;i++){var q=at.offset(BakingOvenBlock.offset(front,i));h.assertTrue(BakingOvenBlock.owner(h.getLevel(),q,h.getLevel().getBlockState(q))==oven,"Rotated cells resolve one owner");}BakingOvenBlock.toggle(oven);h.assertTrue(h.getLevel().getBlockState(at).getValue(BakingOvenBlock.OPEN),"Door state opens");h.getLevel().destroyBlock(at.offset(BakingOvenBlock.offset(front,3)),true);for(int i=0;i<16;i++)h.assertTrue(h.getLevel().getBlockState(at.offset(BakingOvenBlock.offset(front,i))).isAir(),"Breaking a part removes the single oven");}h.succeed();}
    @GameTest(template="industrial") public static void obstructionAndUnsupportedPlacement(GameTestHelper h){var at=h.absolutePos(new BlockPos(4,1,4));clear(h,at);var p=player(h);p.setYRot(0);p.setItemInHand(InteractionHand.MAIN_HAND,BakingOvenContent.ITEM.toStack());var obstruction=at.east().south().above();h.getLevel().setBlockAndUpdate(obstruction,Blocks.STONE.defaultBlockState());h.assertTrue(!place(h,p,at).consumesAction()&&p.getMainHandItem().getCount()==1&&h.getLevel().getBlockState(at).isAir(),"Overhead obstruction rejects placement without consuming item");h.getLevel().removeBlock(obstruction,false);h.getLevel().removeBlock(at.east().south().below(),false);h.assertTrue(!place(h,p,at).consumesAction()&&!p.getMainHandItem().isEmpty(),"All four feet require support");h.succeed();}
    @GameTest(template="industrial") public static void controlPersistenceAndSingleDrop(GameTestHelper h){var at=h.absolutePos(new BlockPos(4,1,4));clear(h,at);var p=player(h);p.setYRot(0);p.setItemInHand(InteractionHand.MAIN_HAND,BakingOvenContent.ITEM.toStack());place(h,p,at);var oven=(BakingOvenEntity)h.getLevel().getBlockEntity(at);oven.turn(.037);oven.turn(Double.NaN);oven.turn(.11);var saved=oven.saveWithoutMetadata(h.getLevel().registryAccess());var restored=new BakingOvenEntity(at,oven.getBlockState());restored.loadWithComponents(saved,h.getLevel().registryAccess());h.assertTrue(Math.abs(restored.dial()-.537)<1e-8,"Continuous setting persists and rejects malformed input");h.getLevel().destroyBlock(at.east(),true);int count=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(at).inflate(4)).stream().filter(e->e.getItem().is(BakingOvenContent.ITEM.get())).mapToInt(e->e.getItem().getCount()).sum();h.assertTrue(count==1,"Destroying a shell cell drops exactly one oven");h.succeed();}
}
