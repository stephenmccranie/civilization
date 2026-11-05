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
    @GameTest(template="industrial") public static void dropThrowsFromTheHandBeforeTheMainHand(GameTestHelper h){
        var p=player(h);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STONE,BlockCarry.LIMIT));p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
        h.assertTrue(p.drop(false)&&p.getOffhandItem().getCount()==BlockCarry.LIMIT-1&&p.getMainHandItem().is(Items.STICK),"Drop throws one block from the hand before the main hand");
        h.assertTrue(p.drop(true)&&p.getOffhandItem().isEmpty()&&p.getMainHandItem().is(Items.STICK),"Ctrl+drop throws the whole stack");
        h.assertTrue(p.drop(false)&&p.getMainHandItem().isEmpty(),"With the hand empty, drop throws the main hand's item");
        int thrown=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(p.blockPosition()).inflate(3)).stream().mapToInt(e->e.getItem().is(Items.STONE)?e.getItem().getCount():0).sum();
        h.assertTrue(thrown==BlockCarry.LIMIT,"Every thrown block lands in the world: "+thrown);h.succeed();
    }
    @GameTest(template="industrial") public static void smallPlaceablesUseNormalPocketsAndHandControls(GameTestHelper h){
        var p=player(h);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STONE,BlockCarry.LIMIT));
        for(var item:new Item[]{Items.WHEAT_SEEDS,Items.BEETROOT_SEEDS,Items.PUMPKIN_SEEDS,Items.MELON_SEEDS,Items.TORCHFLOWER_SEEDS,Items.PITCHER_POD,
                Items.CARROT,Items.POTATO,Items.SWEET_BERRIES,Items.GLOW_BERRIES,Items.COCOA_BEANS,Items.NETHER_WART,
                Items.OAK_SAPLING,Items.DANDELION,Items.RED_MUSHROOM,Items.SUGAR_CANE,Items.BAMBOO,Items.CACTUS,Items.KELP,Items.VINE,
                Items.TORCH,Items.LANTERN,Items.RED_CANDLE,Items.RAIL,Items.LADDER,Items.CHAIN,Items.REDSTONE,Items.REPEATER,Items.COMPARATOR,
                Items.STONE_BUTTON,Items.STONE_PRESSURE_PLATE,Items.POLISHED_BLACKSTONE_PRESSURE_PLATE,Items.OAK_SIGN,Items.OAK_HANGING_SIGN,Items.WHITE_CARPET,Items.FLOWER_POT,Items.TUBE_CORAL_FAN,
                Items.CAKE,Items.AMETHYST_CLUSTER,Items.LIGHTNING_ROD}){
            h.assertTrue(!BlockCarry.block(item.getDefaultInstance()),"Small supplies are ordinary inventory goods: "+item);
            var entity=drop(h,new ItemStack(item,3));entity.playerTouch(p);
            h.assertTrue(entity.isRemoved()&&p.getInventory().countItem(item)==3&&p.getOffhandItem().getCount()==BlockCarry.LIMIT,"Real pickup admits small supplies beside a full building hand: "+item);
            p.getInventory().clearContent();p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STONE,BlockCarry.LIMIT));
        }
        var chest=new net.minecraft.world.SimpleContainer(27);chest.setItem(0,new ItemStack(Items.WHEAT_SEEDS,32));
        var menu=ChestMenu.threeRows(1,p.getInventory(),chest);menu.quickMoveStack(p,0);
        h.assertTrue(chest.getItem(0).isEmpty()&&p.getInventory().countItem(Items.WHEAT_SEEDS)==32,"Seed shift-click enters normal pockets");
        menu.setCarried(new ItemStack(Items.WHEAT_SEEDS,8));menu.clicked(27,0,ClickType.PICKUP,p);
        h.assertTrue(menu.getCarried().isEmpty()&&p.getInventory().getItem(9).is(Items.WHEAT_SEEDS),"Seed cursor placement is admitted");
        h.assertTrue(BlockCarry.normalize(p)==0&&p.getInventory().countItem(Items.WHEAT_SEEDS)==40,"Normalization preserves seeds instead of saving them as cargo");
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.WHEAT_SEEDS,8));
        var swap=new net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent.Hands(p);BlockCarry.swap(swap);
        h.assertTrue(!swap.isCanceled()&&p.inventoryMenu.getSlot(36).mayPlace(new ItemStack(Items.WHEAT_SEEDS)),"Seeds can enter the main hand and hotbar");
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STONE,BlockCarry.LIMIT));
        var recovery=BlockCarryRecovery.get(p.server);recovery.store(p.getUUID(),new ItemStack(Items.WHEAT_SEEDS,5));
        h.assertTrue(recovery.recover(p)==5&&p.getInventory().countItem(Items.WHEAT_SEEDS)==45,"Seeds saved by older builds recover into ordinary pockets");h.succeed();
    }
    @GameTest(template="industrial") public static void reviewedCargoStillIncludesConstructionAndBulkyFixtures(GameTestHelper h){
        for(var item:new Item[]{Items.STONE,Items.DIRT,Items.OAK_LOG,Items.OAK_PLANKS,Items.STONE_SLAB,Items.STONE_STAIRS,Items.COBBLESTONE_WALL,
                Items.OAK_FENCE,Items.OAK_DOOR,Items.OAK_TRAPDOOR,Items.GLASS,Items.WHITE_WOOL,Items.REDSTONE_BLOCK,Items.HAY_BLOCK,Items.CHEST,
                Items.FURNACE,Items.RED_BED,Items.SHULKER_BOX,Items.TUBE_CORAL_BLOCK,Items.CHERRY_LEAVES,Items.FLOWERING_AZALEA_LEAVES,IndustrialContent.PUMP.get().asItem()})
            h.assertTrue(BlockCarry.block(item.getDefaultInstance()),"Bulky construction/fixtures retain the hand allowance: "+item);
        h.assertTrue(BlockCarry.block(CuttingContent.stack(Blocks.STONE.defaultBlockState(),3,1)),"Cut pieces retain the hand allowance");
        // A complete registry receipt makes other unusual BlockItems reviewable without geometry heuristics.
        try{
            var rows=new java.util.ArrayList<String>();
            for(var item:net.minecraft.core.registries.BuiltInRegistries.ITEM)if(item instanceof BlockItem)
                rows.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item)+"\t"+(BlockCarry.carried(item.getDefaultInstance())?"offhand":"inventory"));
            java.nio.file.Files.write(java.nio.file.Path.of("block-carry-classification.tsv"),rows);
        }catch(java.io.IOException e){throw new RuntimeException(e);}
        h.succeed();
    }
    @GameTest(template="industrial") public static void realGroundPickupsFillOnlyOffhandAndLeaveRemainder(GameTestHelper h){
        var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));
        var stone=drop(h,new ItemStack(Items.STONE,BlockCarry.LIMIT-3));stone.playerTouch(p);
        h.assertTrue(stone.isRemoved()&&p.getOffhandItem().is(Items.STONE)&&p.getOffhandItem().getCount()==BlockCarry.LIMIT-3&&p.getMainHandItem().is(Items.IRON_PICKAXE),"Mined ground blocks go to offhand while the mining tool stays selected");
        var more=drop(h,new ItemStack(Items.STONE,20));more.playerTouch(p);
        h.assertTrue(!more.isRemoved()&&more.getItem().getCount()==17&&p.getOffhandItem().getCount()==BlockCarry.LIMIT,"Only matching space is collected; overflow remains on the ground");
        var dirt=drop(h,new ItemStack(Items.DIRT,12));dirt.playerTouch(p);
        h.assertTrue(!dirt.isRemoved()&&dirt.getItem().getCount()==12,"Different blocks remain on the ground");
        for(int i=0;i<40;i++)h.assertTrue(!BlockCarry.carried(p.getInventory().getItem(i)),"No second building stack enters another inventory slot");h.succeed();
    }
    @GameTest(template="industrial") public static void occupiedOffhandAndFullPocketsRespectTheRule(GameTestHelper h){
        var p=player(h);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));var block=drop(h,new ItemStack(Items.STONE,3));block.playerTouch(p);
        h.assertTrue(!block.isRemoved()&&p.getOffhandItem().is(Items.SHIELD),"An occupied offhand is never replaced");
        p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STICK,32));block.playerTouch(p);
        h.assertTrue(block.isRemoved()&&p.getOffhandItem().getCount()==3,"A free offhand accepts blocks even with full normal pockets");
        p.getInventory().setItem(1,ItemStack.EMPTY);var food=new ItemStack(Items.APPLE,4);p.getInventory().add(food);
        h.assertTrue(food.isEmpty()&&p.getInventory().getItem(1).is(Items.APPLE),"Non-block goods retain ordinary inventory behavior");h.succeed();
    }
    @GameTest(template="industrial") public static void shiftClickedGoodsFillTheHotbarNotTheHand(GameTestHelper h){
        var p=player(h);var chest=new net.minecraft.world.SimpleContainer(27);chest.setItem(0,new ItemStack(Items.STONE_PICKAXE));
        var menu=ChestMenu.threeRows(1,p.getInventory(),chest);BlockCarry.attachMenu(menu,p.getInventory());menu.quickMoveStack(p,0);
        int at=-1;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(Items.STONE_PICKAXE))at=i;
        h.assertTrue(chest.getItem(0).isEmpty()&&p.getOffhandItem().isEmpty()&&net.minecraft.world.entity.player.Inventory.isHotbarSlot(at),"A shift-clicked tool fills the hotbar, never the empty Hand: slot "+at);h.succeed();
    }
    @GameTest(template="industrial") public static void menuClicksShiftDragAndSwapCannotPutBlocksInPockets(GameTestHelper h){
        var p=player(h);var chest=new net.minecraft.world.SimpleContainer(27);chest.setItem(0,new ItemStack(Items.STONE,32));chest.setItem(1,new ItemStack(Items.STONE,32));
        var menu=ChestMenu.threeRows(1,p.getInventory(),chest);menu.quickMoveStack(p,0);menu.quickMoveStack(p,1);
        BlockCarry.attachMenu(menu,p.getInventory());
        h.assertTrue(menu.getSlot(63).container==p.getInventory()&&menu.getSlot(63).getContainerSlot()==40,"Native chest gains a usable hand slot without changing existing indices");
        h.assertTrue(p.getOffhandItem().getCount()==BlockCarry.LIMIT&&chest.getItem(0).getCount()==32-BlockCarry.LIMIT&&chest.getItem(1).getCount()==32,"Native chest shift-click uses the offhand and stops at its limit");
        menu.setCarried(new ItemStack(Items.DIRT,4));menu.clicked(27,0,ClickType.PICKUP,p);
        h.assertTrue(menu.getCarried().getCount()==4&&p.getInventory().getItem(9).isEmpty()&&!menu.canDragTo(menu.getSlot(27)),"Normal pickup and drag cannot place blocks into player pockets");
        menu.setCarried(ItemStack.EMPTY);menu.clicked(1,0,ClickType.SWAP,p);
        h.assertTrue(chest.getItem(1).getCount()==32&&p.getInventory().getItem(0).isEmpty(),"Number-key transfer cannot bypass offhand-only storage");
        var swap=new net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent.Hands(p);BlockCarry.swap(swap);h.assertTrue(swap.isCanceled(),"F cannot move carried blocks into the main hand");h.succeed();
    }
    @GameTest(template="industrial") public static void recipeBookCraftsFromOffhandAndCannotSelfDuplicate(GameTestHelper h){
        var p=player(h);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.OAK_PLANKS,3));p.getInventory().setItem(1,new ItemStack(Items.STICK,3));
        h.assertTrue(BlockCarry.insert(p.getInventory(),p.getOffhandItem())==0&&p.getOffhandItem().getCount()==3,"Moving the hand stack to itself cannot duplicate or report a transfer");
        var table=h.absolutePos(new BlockPos(5,2,5));h.getLevel().setBlockAndUpdate(table,Blocks.CRAFTING_TABLE.defaultBlockState());
        var menu=new CraftingMenu(1,p.getInventory(),ContainerLevelAccess.create(h.getLevel(),table));p.containerMenu=menu;BlockCarry.attachMenu(menu,p.getInventory());
        var recipe=h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("civilization:material_stockpile")).orElseThrow();p.getRecipeBook().add(recipe);
        menu.handlePlacement(false,recipe,p);
        h.assertTrue(menu.getSlot(0).getItem().is(CargoContent.STOCKPILE.get().asItem())&&p.getOffhandItem().isEmpty(),"Native recipe book fills the grid from offhand blocks");
        menu.clicked(0,0,ClickType.QUICK_MOVE,p);
        h.assertTrue(p.getOffhandItem().isEmpty()&&p.getInventory().countItem(CargoContent.STOCKPILE.get().asItem())==1&&p.getInventory().countItem(Items.STICK)==0,"Crafted pallets enter ordinary inventory and consume real offhand ingredients");h.succeed();
    }
    @GameTest(template="industrial") public static void palletsUseNormalPickupPocketsClicksSwapAndRecovery(GameTestHelper h){
        var p=player(h);var pallet=CargoContent.STOCKPILE.get().asItem();p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STONE,BlockCarry.LIMIT));
        var ground=drop(h,new ItemStack(pallet,6));ground.playerTouch(p);
        h.assertTrue(ground.isRemoved()&&p.getInventory().countItem(pallet)==6&&p.getOffhandItem().is(Items.STONE),"Pallet pickup uses normal pockets even while carrying a full block stack");
        var chest=new net.minecraft.world.SimpleContainer(27);chest.setItem(0,new ItemStack(pallet,4));var menu=ChestMenu.threeRows(1,p.getInventory(),chest);menu.quickMoveStack(p,0);
        h.assertTrue(chest.getItem(0).isEmpty()&&p.getInventory().countItem(pallet)==10,"Chest shift-click keeps pallets in ordinary inventory");
        menu.setCarried(new ItemStack(pallet,2));menu.clicked(27,0,ClickType.PICKUP,p);
        h.assertTrue(menu.getCarried().isEmpty()&&p.getInventory().getItem(9).is(pallet)&&BlockCarry.normalize(p)==0&&p.getOffhandItem().getCount()==BlockCarry.LIMIT,"Ordinary pallet clicks are admitted and normalization preserves pockets");
        p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);var swap=new net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent.Hands(p);BlockCarry.swap(swap);
        h.assertTrue(!swap.isCanceled()&&p.inventoryMenu.getSlot(36).mayPlace(new ItemStack(pallet)),"Pallets retain normal hand swaps and hotbar admission");
        var recovery=BlockCarryRecovery.get(p.server);recovery.store(p.getUUID(),new ItemStack(pallet,3));
        h.assertTrue(recovery.recover(p)==3&&p.getOffhandItem().isEmpty()&&p.getInventory().countItem(pallet)==15,"Previously saved pallets recover into ordinary inventory");h.succeed();
    }
    @GameTest(template="industrial") public static void oldStockMigratesLosslesslyAndRecoversOneOffhandStack(GameTestHelper h){
        // A hand of 32 from before the smaller allowance, plus pocketed blocks from before the offhand rule.
        var p=player(h);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STONE,32));p.getInventory().setItem(1,new ItemStack(Items.DIRT,20));p.getInventory().setItem(2,new ItemStack(Items.STONE,12));
        BlockCarry.normalize(p);var data=BlockCarryRecovery.get(p.server);long saved=32+20+12-BlockCarry.LIMIT;
        h.assertTrue(p.getOffhandItem().getCount()==BlockCarry.LIMIT&&data.pending(p.getUUID())==saved&&p.getInventory().getItem(1).isEmpty()&&p.getInventory().getItem(2).isEmpty(),"Extra old blocks are saved privately, not destroyed or dropped");
        var restored=BlockCarryRecovery.load(data.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(restored.pending(p.getUUID())==saved&&restored.recover(p)==0,"Recovery persists and cannot exceed a full offhand");
        long recovered=0;
        while(restored.pending(p.getUUID())>0){
            p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);int n=restored.recover(p);
            h.assertTrue(n>0&&n<=BlockCarry.LIMIT&&p.getOffhandItem().getCount()==n,"Recovery withdraws at most one hand of one type at a time");recovered+=n;
        }
        h.assertTrue(recovered==saved,"Saved stock is conserved exactly");h.succeed();
    }
    @GameTest(template="industrial") public static void cutPiecesAndCreativeKeepTheirIntendedLimits(GameTestHelper h){
        var p=player(h);var cut=CuttingContent.stack(Blocks.STONE.defaultBlockState(),3,40);p.getInventory().add(cut);
        h.assertTrue(p.getOffhandItem().getCount()==BlockCarry.LIMIT&&cut.getCount()==40-BlockCarry.LIMIT,"Small cut pieces still use the hand allowance regardless of mass");
        p.setGameMode(GameType.CREATIVE);p.getInventory().setItem(0,ItemStack.EMPTY);var dirt=new ItemStack(Items.DIRT,32);p.getInventory().add(dirt);
        h.assertTrue(dirt.isEmpty()&&p.getInventory().getItem(0).is(Items.DIRT),"Creative authoring stays unrestricted");
        p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);int full=new ItemStack(Items.STONE).getMaxStackSize();var chest=new net.minecraft.world.SimpleContainer(27);chest.setItem(0,new ItemStack(Items.STONE,full));
        var menu=ChestMenu.threeRows(1,p.getInventory(),chest);int size=menu.slots.size();BlockCarry.attachMenu(menu,p.getInventory());var hand=menu.getSlot(size);
        h.assertTrue(menu.slots.size()==size+1&&hand instanceof BlockCarry.HandSlot&&hand.mayPlace(new ItemStack(Items.STONE))&&hand.getMaxStackSize(new ItemStack(Items.STONE))==full&&full>BlockCarry.LIMIT&&p.inventoryMenu.getSlot(36).mayPlace(new ItemStack(Items.STONE)),"Creative menus show the Hand after their native slots, holding a full stack, and keep hotbar admission");
        menu.quickMoveStack(p,0);int at=-1;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(Items.STONE))at=i;
        h.assertTrue(chest.getItem(0).isEmpty()&&p.getOffhandItem().isEmpty()&&net.minecraft.world.entity.player.Inventory.isHotbarSlot(at)&&p.getInventory().getItem(at).getCount()==full,"Creative shift-click treats blocks as ordinary goods: slot "+at);
        p.setGameMode(GameType.SPECTATOR);var watched=ChestMenu.threeRows(2,p.getInventory());int seen=watched.slots.size();BlockCarry.attachMenu(watched,p.getInventory());
        h.assertTrue(watched.slots.size()==seen,"Spectators see no Hand");p.setGameMode(GameType.CREATIVE);
        var swap=new net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent.Hands(p);BlockCarry.swap(swap);h.assertTrue(!swap.isCanceled(),"Creative hand swap is unrestricted");
        h.assertTrue(BlockCarry.normalize(p)==0&&p.getInventory().getItem(0).is(Items.DIRT),"Creative inventory is not migrated to recovery");h.succeed();
    }
    @GameTest(template="industrial") public static void filledContainersCannotHideExtraBlocksAndOldContentsRecover(GameTestHelper h){
        var p=player(h);var box=new ItemStack(Items.SHULKER_BOX);box.set(net.minecraft.core.component.DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.fromItems(java.util.List.of(new ItemStack(Items.STONE,BlockCarry.LIMIT),new ItemStack(Items.DIRT,3))));
        h.assertTrue(!p.getInventory().add(box)&&box.getCount()==1&&!p.inventoryMenu.getSlot(45).mayPlace(box),"Filled portable containers cannot hide another block load");
        p.getInventory().setItem(0,box);BlockCarry.normalize(p);var saved=BlockCarryRecovery.get(p.server);var data=BlockCarryRecovery.load(saved.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(data.recover(p)==-1&&data.recover(p)==BlockCarry.LIMIT&&p.getOffhandItem().is(Items.STONE),"A saved container unpacks into the ordinary offhand allowance");
        p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);h.assertTrue(data.recover(p)==-1&&data.recover(p)==3&&p.getOffhandItem().is(Items.DIRT),"The remaining contained block type stays recoverable");
        p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);h.assertTrue(data.recover(p)==1&&p.getOffhandItem().is(Items.SHULKER_BOX)&&!BlockCarry.filledContainer(p.getOffhandItem())&&data.pending(p.getUUID())==0,"The empty container itself is also preserved");h.succeed();
    }
}
