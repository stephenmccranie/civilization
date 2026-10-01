package dev.civilization;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class ModularCutGameTests {
    private static net.neoforged.neoforge.common.util.FakePlayer player(GameTestHelper h,BlockPos pos) {
        var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ModularTest"));player.setPos(pos.getX()+4,pos.getY(),pos.getZ()+4);return player;
    }
    private static UseOnContext context(net.minecraft.world.entity.player.Player player,BlockPos pos,int cell) {
        var box=CutCells.box(cell);return new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(pos.getX()+box.minX+.25,pos.getY()+box.minY,pos.getZ()+box.minZ+.25),Direction.UP,box.minY==0?pos.below():pos,false));
    }
    @GameTest(template="industrial") public static void eightCubesInNonRectangularOrderFillOneCell(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(4,2,4));var player=player(h,pos);h.getLevel().setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND,CuttingContent.stack(Blocks.BRICKS.defaultBlockState(),3,8));int mask=0;
        for(int i:new int[]{0,1,4,2,7,5,6,3}) {
            var context=context(player,pos,i);var plan=CutPlacement.plan(context);h.assertTrue(plan.valid() && plan.pos().equals(pos),"Preview targets the same block's free cell");
            h.assertTrue(player.getMainHandItem().useOn(context).consumesAction(),"Eighth placement succeeds after previous pieces");mask|=1<<i;
            h.assertTrue(mask==255?h.getLevel().getBlockState(pos).is(Blocks.BRICKS):CutCells.mask(CutCells.read(h.getLevel(),pos))==mask,"Exact occupied cells retained");
        }
        h.assertTrue(player.getMainHandItem().isEmpty(),"Exactly eight eighth items consumed");h.succeed();
    }
    @GameTest(template="industrial") public static void fourBeamsAndMixedSizesFillWithoutBlocking(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(4,2,4));var player=player(h,pos);h.getLevel().setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND,CuttingContent.stack(Blocks.BRICKS.defaultBlockState(),1,4));
        for(int i:new int[]{0,3,1,2})h.assertTrue(player.getMainHandItem().useOn(context(player,pos,i)).consumesAction(),"All four beams fit, including diagonal placement");
        h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.BRICKS) && player.getMainHandItem().isEmpty(),"Four beams restore one full block");
        h.getLevel().setBlockAndUpdate(pos,Blocks.BRICK_SLAB.defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND,CuttingContent.stack(Blocks.BRICKS.defaultBlockState(),1,1));
        h.assertTrue(player.getMainHandItem().useOn(context(player,pos,2)).consumesAction(),"Beam fits above existing vanilla slab");
        player.setItemInHand(InteractionHand.MAIN_HAND,CuttingContent.stack(Blocks.BRICKS.defaultBlockState(),3,2));
        for(int i:new int[]{3,7})h.assertTrue(player.getMainHandItem().useOn(context(player,pos,i)).consumesAction(),"Cubes fill remaining slab-plus-beam gaps");
        h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.BRICKS),"Mixed sizes complete a full block");h.succeed();
    }
    @GameTest(template="industrial") public static void arbitraryCellsPersistCollideAndDropWithoutLoss(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(4,2,4));var state=CuttingContent.PIECE.get().defaultBlockState();h.getLevel().setBlockAndUpdate(pos,state);
        var cells=new net.minecraft.world.level.block.state.BlockState[8];cells[0]=Blocks.BRICKS.defaultBlockState();cells[1]=Blocks.COPPER_BLOCK.defaultBlockState();cells[6]=Blocks.OAK_PLANKS.defaultBlockState();
        var cut=(CutBlockEntity)h.getLevel().getBlockEntity(pos);cut.cells(cells);
        var loaded=new CutBlockEntity(pos,state);loaded.setLevel(h.getLevel());loaded.loadWithComponents(cut.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());h.getLevel().setBlockEntity(loaded);
        h.assertTrue(Arrays.equals(cells,loaded.cells()),"Every cell and material survives reload");
        double volume=0;for(var box:state.getCollisionShape(h.getLevel(),pos).toAabbs())volume+=box.getXsize()*box.getYsize()*box.getZsize();h.assertTrue(volume==.375,"Collision contains precisely three cubes with real gaps");
        var drops=Block.getDrops(state,h.getLevel(),pos,loaded);h.assertTrue(drops.size()==3,"Mixed materials all returned");
        for(var material:List.of(Blocks.BRICKS,Blocks.COPPER_BLOCK,Blocks.OAK_PLANKS))h.assertTrue(drops.stream().anyMatch(s->CuttingContent.material(s).is(material) && CuttingContent.units(s)==3 && s.getCount()==1),"Exact material volumes dropped");
        var player=player(h,pos);player.setItemInHand(InteractionHand.MAIN_HAND,CuttingContent.stack(Blocks.STONE.defaultBlockState(),3,2));
        h.assertTrue(!player.getMainHandItem().useOn(context(player,pos,0)).consumesAction() && player.getMainHandItem().getCount()==2,"Occupied cell rejects overlap without consuming item");
        h.assertTrue(player.getMainHandItem().useOn(context(player,pos,2)).consumesAction(),"Different material fills free cell");
        h.assertTrue(CutCells.mask(CutCells.read(h.getLevel(),pos))==71,"New placement preserves existing mixed cells");h.succeed();
    }
    @GameTest(template="industrial") public static void allMasksHaveExactGeometryAndLosslessDrops(GameTestHelper h) {
        for(int mask=1;mask<256;mask++) {
            var cells=new net.minecraft.world.level.block.state.BlockState[8];for(int i=0;i<8;i++)if((mask&(1<<i))!=0)cells[i]=Blocks.COBBLESTONE.defaultBlockState();
            double expected=Integer.bitCount(mask)/8.0,volume=0;for(var b:CutCells.shape(cells).toAabbs())volume+=b.getXsize()*b.getYsize()*b.getZsize();
            h.assertTrue(volume==expected,"Exact collision volume for occupancy "+mask);
            double dropped=CutCells.drops(cells).stream().mapToDouble(s->CuttingContent.volume(CuttingContent.units(s))*s.getCount()).sum();h.assertTrue(dropped==expected,"No material loss or multiplication for occupancy "+mask);
        }
        var player=player(h,h.absolutePos(new BlockPos(4,2,4)));var legacy=new ItemStack(CuttingContent.ITEM.get(),7);player.getInventory().setItem(0,legacy);
        CuttingContent.ITEM.get().inventoryTick(legacy,h.getLevel(),player,0,true);
        h.assertTrue(player.getInventory().getItem(0).is(Items.COBBLESTONE_SLAB) && player.getInventory().getItem(0).getCount()==7,"Legacy generic cobblestone halves normalize to the single vanilla item");h.succeed();
    }
    @GameTest(template="industrial") public static void unchangedStateStillFiresCancelablePlacementAndRejectsEntities(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(4,2,4));var player=player(h,pos);var state=CuttingContent.PIECE.get().defaultBlockState();h.getLevel().setBlockAndUpdate(pos,state);
        var cells=new net.minecraft.world.level.block.state.BlockState[8];for(int i:new int[]{0,1,4})cells[i]=Blocks.BRICKS.defaultBlockState();((CutBlockEntity)h.getLevel().getBlockEntity(pos)).cells(cells);
        player.setItemInHand(InteractionHand.MAIN_HAND,CuttingContent.stack(Blocks.BRICKS.defaultBlockState(),3,2));int[] events={0};
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent> listener=event->{if(event.getPos().equals(pos)){events[0]++;event.setCanceled(true);}};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
        try {net.neoforged.neoforge.common.CommonHooks.onPlaceItemIntoWorld(context(player,pos,6));}
        finally {net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);}
        h.assertTrue(events[0]==1,"Cell-only mutation fires exactly one placement event for protections and calorie accounting");
        h.assertTrue(CutCells.mask(CutCells.read(h.getLevel(),pos))==19 && player.getMainHandItem().getCount()==2,"Cancelled placement restores all cells and item count");
        var pig=net.minecraft.world.entity.EntityType.PIG.create(h.getLevel());pig.setPos(pos.getX()+.25,pos.getY()+.5,pos.getZ()+.75);h.getLevel().addFreshEntity(pig);
        h.assertTrue(!CutPlacement.plan(context(player,pos,6)).valid(),"Preview rejects collision with an entity in the free cell");pig.discard();h.succeed();
    }
}
