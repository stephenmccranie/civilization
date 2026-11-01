package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class BlockCarryGameTests {
    private static FakePlayer player(GameTestHelper h){var p=new FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"block-carry"));p.setGameMode(GameType.SURVIVAL);p.setPos(h.absolutePos(new BlockPos(4,2,4)).getCenter());return p;}
    private static ItemEntity drop(GameTestHelper h,ItemStack stack){var d=new ItemEntity(h.getLevel(),0,2,0,stack);d.setNoPickUpDelay();return d;}
    @GameTest(template="industrial") public static void realGroundPickupsFillOnlyOffhandAndLeaveRemainder(GameTestHelper h){
        var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));
        var stone=drop(h,new ItemStack(Items.STONE,20));stone.playerTouch(p);
        h.assertTrue(stone.isRemoved()&&p.getOffhandItem().is(Items.STONE)&&p.getOffhandItem().getCount()==20&&p.getMainHandItem().is(Items.IRON_PICKAXE),"Mined ground blocks go to offhand while the mining tool stays selected");
        var more=drop(h,new ItemStack(Items.STONE,20));more.playerTouch(p);
        h.assertTrue(!more.isRemoved()&&more.getItem().getCount()==8&&p.getOffhandItem().getCount()==32,"Only matching space is collected; overflow remains on the ground");
        var dirt=drop(h,new ItemStack(Items.DIRT,12));dirt.playerTouch(p);
        h.assertTrue(!dirt.isRemoved()&&dirt.getItem().getCount()==12,"Different blocks remain on the ground");
        for(int i=0;i<40;i++)h.assertTrue(!BlockCarry.block(p.getInventory().getItem(i)),"No second building stack enters another inventory slot");h.succeed();
    }
    @GameTest(template="industrial") public static void occupiedOffhandAndFullPocketsRespectTheRule(GameTestHelper h){
        var p=player(h);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));var block=drop(h,new ItemStack(Items.STONE,3));block.playerTouch(p);
        h.assertTrue(!block.isRemoved()&&p.getOffhandItem().is(Items.SHIELD),"An occupied offhand is never replaced");
        p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STICK,32));block.playerTouch(p);
        h.assertTrue(block.isRemoved()&&p.getOffhandItem().getCount()==3,"A free offhand accepts blocks even with full normal pockets");
        p.getInventory().setItem(1,ItemStack.EMPTY);var food=new ItemStack(Items.APPLE,4);p.getInventory().add(food);
        h.assertTrue(food.isEmpty()&&p.getInventory().getItem(1).is(Items.APPLE),"Non-block goods retain ordinary inventory behavior");h.succeed();
    }
    @GameTest(template="industrial") public static void menuClicksShiftDragAndSwapCannotPutBlocksInPockets(GameTestHelper h){
        var p=player(h);var chest=new net.minecraft.world.SimpleContainer(27);chest.setItem(0,new ItemStack(Items.STONE,32));chest.setItem(1,new ItemStack(Items.STONE,32));
        var menu=ChestMenu.threeRows(1,p.getInventory(),chest);menu.quickMoveStack(p,0);menu.quickMoveStack(p,1);
        BlockCarry.attachMenu(menu,p.getInventory());
        h.assertTrue(menu.getSlot(63).container==p.getInventory()&&menu.getSlot(63).getContainerSlot()==40,"Native chest gains a usable hand slot without changing existing indices");
        h.assertTrue(p.getOffhandItem().getCount()==32&&chest.getItem(1).getCount()==32,"Native chest shift-click uses the offhand and stops at its limit");
        menu.setCarried(new ItemStack(Items.DIRT,4));menu.clicked(27,0,ClickType.PICKUP,p);
        h.assertTrue(menu.getCarried().getCount()==4&&p.getInventory().getItem(9).isEmpty()&&!menu.canDragTo(menu.getSlot(27)),"Normal pickup and drag cannot place blocks into player pockets");
        menu.setCarried(ItemStack.EMPTY);menu.clicked(1,0,ClickType.SWAP,p);
        h.assertTrue(chest.getItem(1).getCount()==32&&p.getInventory().getItem(0).isEmpty(),"Number-key transfer cannot bypass offhand-only storage");
        var swap=new net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent.Hands(p);BlockCarry.swap(swap);h.assertTrue(swap.isCanceled(),"F cannot move carried blocks into the main hand");h.succeed();
    }
    @GameTest(template="industrial") public static void recipeBookCraftsFromOffhandAndCannotSelfDuplicate(GameTestHelper h){
        var p=player(h);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.OAK_PLANKS,3));p.getInventory().setItem(1,new ItemStack(Items.STICK,2));
        h.assertTrue(BlockCarry.insert(p.getInventory(),p.getOffhandItem())==0&&p.getOffhandItem().getCount()==3,"Moving the hand stack to itself cannot duplicate or report a transfer");
        var table=h.absolutePos(new BlockPos(5,2,5));h.getLevel().setBlockAndUpdate(table,Blocks.CRAFTING_TABLE.defaultBlockState());
        var menu=new CraftingMenu(1,p.getInventory(),ContainerLevelAccess.create(h.getLevel(),table));p.containerMenu=menu;BlockCarry.attachMenu(menu,p.getInventory());
        var recipe=h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("civilization:material_stockpile")).orElseThrow();p.getRecipeBook().add(recipe);
        menu.handlePlacement(false,recipe,p);
        h.assertTrue(menu.getSlot(0).getItem().is(CargoContent.STOCKPILE.get().asItem())&&p.getOffhandItem().isEmpty(),"Native recipe book fills the grid from offhand blocks");
        menu.clicked(0,0,ClickType.QUICK_MOVE,p);
        h.assertTrue(p.getOffhandItem().is(CargoContent.STOCKPILE.get().asItem())&&p.getOffhandItem().getCount()==1&&p.getInventory().countItem(Items.STICK)==0,"Crafting result enters the same offhand allowance and consumes real ingredients");h.succeed();
    }
    @GameTest(template="industrial") public static void oldStockMigratesLosslesslyAndRecoversOneOffhandStack(GameTestHelper h){
        var p=player(h);p.getInventory().setItem(0,new ItemStack(Items.STONE,32));p.getInventory().setItem(1,new ItemStack(Items.DIRT,20));p.getInventory().setItem(2,new ItemStack(Items.STONE,12));
        BlockCarry.normalize(p);var data=BlockCarryRecovery.get(p.server);
        h.assertTrue(p.getOffhandItem().getCount()==32&&data.pending(p.getUUID())==32&&p.getInventory().getItem(0).isEmpty()&&p.getInventory().getItem(1).isEmpty(),"Extra old blocks are saved privately, not destroyed or dropped");
        var saved=data.save(new CompoundTag(),h.getLevel().registryAccess());var restored=BlockCarryRecovery.load(saved,h.getLevel().registryAccess());
        h.assertTrue(restored.pending(p.getUUID())==32&&restored.recover(p)==0,"Recovery persists and cannot exceed a full offhand");
        p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);h.assertTrue(restored.recover(p)==20&&p.getOffhandItem().is(Items.DIRT)&&restored.pending(p.getUUID())==12,"Recovery withdraws one matching type into offhand");
        p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);h.assertTrue(restored.recover(p)==12&&restored.pending(p.getUUID())==0,"Remaining saved stock is conserved exactly");h.succeed();
    }
    @GameTest(template="industrial") public static void cutPiecesAndCreativeKeepTheirIntendedLimits(GameTestHelper h){
        var p=player(h);var cut=CuttingContent.stack(Blocks.STONE.defaultBlockState(),3,40);p.getInventory().add(cut);
        h.assertTrue(p.getOffhandItem().getCount()==32&&cut.getCount()==8,"Small cut pieces still use a 32-item hand allowance regardless of mass");
        p.setGameMode(GameType.CREATIVE);p.getInventory().setItem(0,ItemStack.EMPTY);var dirt=new ItemStack(Items.DIRT,32);p.getInventory().add(dirt);
        h.assertTrue(dirt.isEmpty()&&p.getInventory().getItem(0).is(Items.DIRT),"Creative authoring stays unrestricted");
        var menu=ChestMenu.threeRows(1,p.getInventory());int size=menu.slots.size();BlockCarry.attachMenu(menu,p.getInventory());
        h.assertTrue(menu.slots.size()==size&&p.inventoryMenu.getSlot(36).mayPlace(new ItemStack(Items.STONE)),"Creative keeps native menu slot counts and hotbar admission");
        var swap=new net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent.Hands(p);BlockCarry.swap(swap);h.assertTrue(!swap.isCanceled(),"Creative hand swap is unrestricted");
        h.assertTrue(BlockCarry.normalize(p)==0&&p.getInventory().getItem(0).is(Items.DIRT),"Creative inventory is not migrated to recovery");h.succeed();
    }
    @GameTest(template="industrial") public static void filledContainersCannotHideExtraBlocksAndOldContentsRecover(GameTestHelper h){
        var p=player(h);var box=new ItemStack(Items.SHULKER_BOX);box.set(net.minecraft.core.component.DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.fromItems(java.util.List.of(new ItemStack(Items.STONE,32),new ItemStack(Items.DIRT,8))));
        h.assertTrue(!p.getInventory().add(box)&&box.getCount()==1&&!p.inventoryMenu.getSlot(45).mayPlace(box),"Filled portable containers cannot hide another block load");
        p.getInventory().setItem(0,box);BlockCarry.normalize(p);var saved=BlockCarryRecovery.get(p.server);var data=BlockCarryRecovery.load(saved.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(data.recover(p)==-1&&data.recover(p)==32&&p.getOffhandItem().is(Items.STONE),"A saved container unpacks into the ordinary offhand allowance");
        p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);h.assertTrue(data.recover(p)==-1&&data.recover(p)==8&&p.getOffhandItem().is(Items.DIRT),"The remaining contained block type stays recoverable");
        p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);h.assertTrue(data.recover(p)==1&&p.getOffhandItem().is(Items.SHULKER_BOX)&&!BlockCarry.filledContainer(p.getOffhandItem())&&data.pending(p.getUUID())==0,"The empty container itself is also preserved");h.succeed();
    }
}
