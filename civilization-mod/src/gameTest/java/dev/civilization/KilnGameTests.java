package dev.civilization;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization")
@PrefixGameTestTemplate(false)
public class KilnGameTests {
    @GameTest(template="industrial")
    public static void twoMaterialBanksProcessPersistAndRespectLimits(GameTestHelper h){
        var k=kiln(h);k.setItem(0,new ItemStack(Items.CLAY,8));k.setItem(4,new ItemStack(Items.CLAY,9));k.setItem(1,KilnContent.MINERAL_COAL.toStack(12));
        ticks(h,k,3400);
        h.assertTrue(k.getItem(2).getCount()==32&&k.getItem(5).getCount()==32,"Both outputs fill to 32 without oversized stacks");
        h.assertTrue(MachineInventory.count(k,MachineInventory.KILN_INPUT)==1,"Backup input feeds exactly sixteen batches and then waits");
        var saved=k.saveWithFullMetadata(h.getLevel().registryAccess());var copy=new KilnBlockEntity(k.getBlockPos(),k.getBlockState());copy.setLevel(h.getLevel());copy.loadWithComponents(saved,h.getLevel().registryAccess());h.getLevel().setBlockEntity(copy);
        h.assertTrue(copy.getItem(5).getCount()==32,"Second output survives save/load");
        copy.setItem(2,new ItemStack(Items.BRICK,30));copy.setItem(5,new ItemStack(Items.BRICK,30));ticks(h,copy,200);
        h.assertTrue(copy.getItem(2).getCount()==32&&copy.getItem(5).getCount()==32&&copy.getItem(0).isEmpty(),"Four-brick batch fits across two partial output stacks");
        copy.removeItem(2,32);
        h.assertTrue(copy.getItem(2).isEmpty()&&copy.getItem(5).getCount()==32,"Taking the first output leaves the second output in place");
        copy.setItem(0,new ItemStack(Items.CLAY));ticks(h,copy,200);
        h.assertTrue(copy.getItem(2).getCount()==4&&copy.getItem(5).getCount()==32,"New batches fill the cleared first slot without moving the second");
        copy.setItem(4,new ItemStack(Items.SAND,32));
        var restored=new KilnBlockEntity(copy.getBlockPos(),copy.getBlockState());restored.setLevel(h.getLevel());restored.loadWithComponents(copy.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(restored.getItem(4).getCount()==32,"Second input survives save/load without legacy spill");
        h.assertTrue(restored.canPlaceItemThroughFace(4,new ItemStack(Items.SAND),Direction.UP)&&restored.canTakeItemThroughFace(5,new ItemStack(Items.BRICK),Direction.DOWN),"Hoppers access both banks");h.succeed();
    }
    @GameTest(template="industrial")
    public static void selectedClayRecipeControlsProductionAndSurvivesReload(GameTestHelper h) {
        var k=kiln(h);
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"kiln-selection"));
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(k.getBlockPos()));
        var menu=(KilnMenu)k.createMenu(0,player.getInventory(),player);
        int bricks=-1,terracotta=-1;
        for(int i=0;i<menu.recipes.size();i++){
            var result=menu.recipes.get(i).value().getResultItem(h.getLevel().registryAccess());
            if(result.is(Items.BRICK))bricks=i;
            if(result.is(Items.TERRACOTTA))terracotta=i;
        }
        h.assertTrue(bricks>=0&&terracotta>=0,"Both clay recipes exist");
        k.setItem(0,new ItemStack(Items.CLAY,3));k.setItem(1,KilnContent.MINERAL_COAL.toStack(8));
        h.assertTrue(menu.clickMenuButton(player,terracotta),"Select terracotta through menu");
        ticks(h,k,50);
        h.assertTrue(menu.clickMenuButton(player,bricks),"Switch an active batch to bricks");
        var saved=k.saveWithFullMetadata(h.getLevel().registryAccess());
        var restored=new KilnBlockEntity(k.getBlockPos(),k.getBlockState());restored.setLevel(h.getLevel());
        restored.loadWithComponents(saved,h.getLevel().registryAccess());h.getLevel().setBlockEntity(restored);
        h.assertTrue(restored.selection()==bricks,"Selection persists");
        ticks(h,restored,200);
        h.assertTrue(restored.getItem(2).is(Items.BRICK)&&restored.getItem(2).getCount()==4,"Selected clay batch makes four bricks, never terracotta");
        restored.removeItem(2,64);restored.selectRecipe(terracotta);ticks(h,restored,200);
        h.assertTrue(restored.getItem(2).is(Items.TERRACOTTA),"Terracotta is still explicitly available");
        restored.removeItem(2,64);restored.selectRecipe(-1);ticks(h,restored,200);
        h.assertTrue(restored.getItem(2).is(Items.BRICK),"Automatic clay deterministically defaults to bricks");
        h.succeed();
    }
    static KilnBlockEntity kiln(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(2, 1, 1));
        h.getLevel().setBlockAndUpdate(pos, KilnContent.KILN.get().defaultBlockState());
        buildShell(h, pos, Direction.NORTH, false);
        return (KilnBlockEntity) h.getLevel().getBlockEntity(pos);
    }
    static void ticks(GameTestHelper h, KilnBlockEntity kiln, int count) {
        CoalFireFixture.light(kiln);
        for (int i = 0; i < count; i++) KilnBlockEntity.tick(h.getLevel(), kiln.getBlockPos(), kiln.getBlockState(), kiln);
    }
    private static FertilizerRetortBlockEntity retort(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(7, 1, 1));
        h.getLevel().setBlockAndUpdate(pos, KilnContent.RETORT.get().defaultBlockState());
        buildShell(h, pos, Direction.NORTH, true);
        return (FertilizerRetortBlockEntity) h.getLevel().getBlockEntity(pos);
    }
    static void buildShell(GameTestHelper h, BlockPos pos, Direction facing, boolean works) {
        for (var part : MachineStructure.parts(works)) {
            MachineStructure.placePart(h.getLevel(), pos, facing, part);
        }
    }
    @GameTest(template="industrial")
    public static void unifiedCoalAndThermalProgression(GameTestHelper h) {
        var manager = h.getLevel().getRecipeManager();
        for (var type : List.of(RecipeType.SMELTING, RecipeType.BLASTING, RecipeType.SMOKING, RecipeType.CAMPFIRE_COOKING))
            h.assertTrue(manager.getAllRecipesFor(type).isEmpty(), "No native heat recipes remain: " + type);
        for (var recipe : manager.getRecipes())
            h.assertTrue(!VanillaRetirement.retired(recipe.value().getResultItem(h.getLevel().registryAccess())), "No retired output: " + recipe.id());
        var torch = CraftingInput.of(1, 2, List.of(KilnContent.MINERAL_COAL.toStack(), new ItemStack(Items.STICK)));
        var torches = manager.getRecipeFor(RecipeType.CRAFTING, torch, h.getLevel()).orElseThrow().value().assemble(torch,h.getLevel().registryAccess());
        h.assertTrue(torches.is(Items.TORCH) && torches.getCount()==4, "Unified coal crafts four torches");
        var grid = CraftingInput.of(3, 3, java.util.stream.IntStream.range(0,9).mapToObj(i -> new ItemStack(Items.COBBLESTONE)).toList());
        h.assertTrue(manager.getRecipeFor(RecipeType.CRAFTING,grid,h.getLevel()).orElseThrow().value().assemble(grid,h.getLevel().registryAccess()).is(KilnContent.KILN_ITEM.get()), "Kiln bootstrap needs only cobblestone");
        var k=kiln(h); k.setItem(0,new ItemStack(Items.SAND)); k.setItem(1,KilnContent.MINERAL_COAL.toStack()); ticks(h,k,201);
        h.assertTrue(k.getItem(2).is(Items.GLASS) && k.getItem(2).getCount()==1 && k.getItem(0).isEmpty(), "Kiln actually consumes sand and produces glass");
        var debris=new SingleRecipeInput(new ItemStack(Items.ANCIENT_DEBRIS));
        h.assertTrue(manager.getRecipeFor(KilnContent.FOUNDRY_RECIPE_TYPE.get(),debris,h.getLevel()).orElseThrow().value().assemble(debris,h.getLevel().registryAccess()).is(Items.NETHERITE_SCRAP), "Foundry retains ancient debris processing");
        h.succeed();
    }
    @GameTest(template="industrial")
    public static void tierOneSlotsAndLegacyOverflow(GameTestHelper h){
        var k=kiln(h);var old=net.minecraft.core.NonNullList.withSize(12,ItemStack.EMPTY);
        old.set(0,new ItemStack(Items.CLAY,2));old.set(1,KilnContent.MINERAL_COAL.toStack());old.set(2,new ItemStack(Items.BRICK,4));
        old.set(3,new ItemStack(Items.CLAY,5));old.set(6,KilnContent.MINERAL_COAL.toStack(2));old.set(9,new ItemStack(Items.BRICK,12));
        var tag=k.saveWithFullMetadata(h.getLevel().registryAccess());net.minecraft.world.ContainerHelper.saveAllItems(tag,old,h.getLevel().registryAccess());tag.remove("TwoFuelSlots");tag.remove("TwoMaterialSlots");
        k.loadWithComponents(tag,h.getLevel().registryAccess());
        // A save before the first load callback must preserve the pending return.
        k.loadWithComponents(k.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(k.getContainerSize()==6&&k.getSlotsForFace(Direction.UP).length==2&&k.getSlotsForFace(Direction.NORTH).length==2&&k.getSlotsForFace(Direction.DOWN).length==2,"T1 exposes two input, two fuel and two output slots");
        h.assertTrue(k.getItem(0).getCount()==2&&k.getItem(1).getCount()==1&&k.getItem(2).getCount()==4,"Original slots are preserved");
        k.onLoad();k.onLoad();
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(k.getBlockPos()).inflate(2));
        for(var expected:List.of(new ItemStack(Items.CLAY,5),KilnContent.MINERAL_COAL.toStack(2),new ItemStack(Items.BRICK,12)))
            h.assertTrue(drops.stream().filter(e->ItemStack.isSameItemSameComponents(e.getItem(),expected)).mapToInt(e->e.getItem().getCount()).sum()==expected.getCount(),"Legacy overflow returns exactly once");
        h.succeed();
    }
    @GameTest(template = "industrial")
    public static void bricksCannotBeMadeInVanillaFurnaces(GameTestHelper h) {
        var input = new SingleRecipeInput(new ItemStack(Items.CLAY_BALL));
        for (var type : List.of(RecipeType.SMELTING, RecipeType.BLASTING, RecipeType.SMOKING, RecipeType.CAMPFIRE_COOKING))
            h.assertTrue(h.getLevel().getRecipeManager().getRecipeFor(type, input, h.getLevel()).isEmpty(), "Clay balls cannot bypass the kiln: " + type);
        var pos = h.absolutePos(new BlockPos(1, 1, 1));
        h.getLevel().setBlockAndUpdate(pos, Blocks.FURNACE.defaultBlockState());
        var furnace = (net.minecraft.world.level.block.entity.FurnaceBlockEntity) h.getLevel().getBlockEntity(pos);
        furnace.setItem(0, new ItemStack(Items.CLAY_BALL));
        furnace.setItem(1, new ItemStack(Items.COAL));
        for (int i = 0; i < 250; i++) net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.serverTick(h.getLevel(), pos, furnace.getBlockState(), furnace);
        h.assertTrue(furnace.getItem(2).isEmpty() && furnace.getItem(1).getCount() == 1, "Old furnace must neither produce bricks nor waste new fuel on removed recipe");
        h.succeed();
    }
    @GameTest(template = "industrial")
    public static void newAndSavedVillagersCannotSellBricks(GameTestHelper h) {
        var villager = net.minecraft.world.entity.EntityType.VILLAGER.create(h.getLevel());
        villager.setVillagerData(villager.getVillagerData().setProfession(net.minecraft.world.entity.npc.VillagerProfession.MASON));
        h.assertTrue(villager.getOffers().stream().noneMatch(o -> o.getResult().is(Items.BRICK)), "New mason must not sell bricks");
        var saved = villager.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        var offers = new net.minecraft.world.item.trading.MerchantOffers();
        offers.add(new net.minecraft.world.item.trading.MerchantOffer(new net.minecraft.world.item.trading.ItemCost(Items.EMERALD), new ItemStack(Items.BRICK, 10), 16, 1, 0.05f));
        offers.add(new net.minecraft.world.item.trading.MerchantOffer(new net.minecraft.world.item.trading.ItemCost(Items.EMERALD), new ItemStack(Items.STONE), 16, 1, 0.05f));
        saved.put("Offers", net.minecraft.world.item.trading.MerchantOffers.CODEC.encodeStart(
                h.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), offers).getOrThrow());
        villager.load(saved);
        h.assertTrue(villager.getOffers().size() == 1 && villager.getOffers().getFirst().getResult().is(Items.STONE), "Saved brick trade removed, unrelated trade retained");
        h.succeed();
    }
    @GameTest(template = "industrial")
    public static void fuelProducesFertilizerThatBoostsHarvest(GameTestHelper h) {
        var ingredients = CraftingInput.of(2, 1, List.of(IndustrialContent.SULFUR.toStack(), new ItemStack(Items.GRAVEL)));
        var recipe = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, ingredients, h.getLevel()).orElseThrow();
        h.assertTrue(recipe.value().assemble(ingredients, h.getLevel().registryAccess()).is(IndustrialContent.ENRICHED_BLEND.get()), "Sulfur and gravel craft blend");
        h.assertTrue(h.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(IndustrialContent.ENRICHED_BLEND.toStack()), h.getLevel()).isEmpty(), "Blend cannot bypass finite fuel in a furnace");
        var kiln = retort(h);
        kiln.setItem(0, IndustrialContent.ENRICHED_BLEND.toStack(5));
        kiln.setItem(1, KilnContent.MINERAL_COAL.toStack());
        ticks(h, kiln, 1800);
        h.assertTrue(kiln.getItem(2).is(FarmingContent.FERTILIZER.get()) && kiln.getItem(2).getCount() == 8, "One fuel makes exactly eight fertilizer");
        h.assertTrue(kiln.getItem(0).getCount() == 4 && kiln.getItem(1).isEmpty(), "One blend and one fuel consumed");
        var field = GeographyGameTests.region(h, "river");
        var player = new FakePlayer(field, new GameProfile(UUID.randomUUID(), "kiln-farmer"));
        var pos = new BlockPos(24, 64, 24);
        field.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(pos));
        field.setBlockAndUpdate(pos.below(), Blocks.FARMLAND.defaultBlockState());
        field.setBlockAndUpdate(pos, Blocks.WHEAT.defaultBlockState());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, kiln.removeItem(2, 1));
        player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), Direction.UP, pos, false)));
        var crop = field.getBlockState(pos);
        h.assertTrue(crop.is(FarmingContent.FERTILIZED_WHEAT.get()) && player.getMainHandItem().isEmpty(), "Manufactured fertilizer must apply once");
        var mature = crop.setValue(net.minecraft.world.level.block.CropBlock.AGE, 7);
        var drops = Block.getDrops(mature, field, pos, null, player, ItemStack.EMPTY);
        h.assertTrue(drops.stream().filter(s -> s.is(Items.WHEAT)).mapToInt(ItemStack::getCount).sum() == 3, "Manufactured fertilizer yields three wheat");
        h.succeed();
    }
    @GameTest(template = "industrial")
    public static void machinesOnlyProcessTheirOwnRecipes(GameTestHelper h) {
        var kiln = kiln(h);
        var retort = retort(h);
        kiln.setItem(0, IndustrialContent.ENRICHED_BLEND.toStack(2));
        retort.setItem(0, new ItemStack(Items.CLAY));
        for (var machine : List.of(kiln, retort)) {
            machine.setItem(1, KilnContent.MINERAL_COAL.toStack());
            h.assertTrue(!machine.canPlaceItemThroughFace(0, machine.getItem(0), Direction.UP), "Hoppers reject wrong machine input");
            ticks(h, machine, 450);
            h.assertTrue(machine.getItem(2).isEmpty() && machine.getItem(1).isEmpty(), "Wrong recipe produces nothing while lit fire idles");
        }
        retort.fire.extinguish();retort.setItem(0, IndustrialContent.ENRICHED_BLEND.toStack());
        retort.setItem(1, new ItemStack(Items.CHARCOAL));
        ticks(h, retort, 450);
        h.assertTrue(retort.getItem(2).isEmpty() && retort.getItem(1).getCount() == 1, "Retort also rejects renewable fuel when forced");
        h.succeed();
    }
    @GameTest(template = "industrial")
    public static void retortRecipeRequiresBricksAndCopper(GameTestHelper h) {
        var grid = CraftingInput.of(3, 3, List.of(new ItemStack(Items.BRICKS), new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.BRICKS),
                new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.COBBLESTONE), new ItemStack(Items.COPPER_INGOT),
                new ItemStack(Items.BRICKS), new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.BRICKS)));
        var recipe = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, h.getLevel()).orElseThrow();
        h.assertTrue(recipe.value().assemble(grid, h.getLevel().registryAccess()).is(KilnContent.RETORT_ITEM.get()), "Four brick blocks, four copper and cobblestone make a retort");
        h.succeed();
    }
    @GameTest(template = "industrial")
    public static void retortSaveMenuOutputAndDropsAreCorrect(GameTestHelper h) {
        var retort = retort(h);
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "retort-slots"));
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(retort.getBlockPos()));
        var menu = (KilnMenu) retort.createMenu(0, player.getInventory(), player);
        h.assertTrue(menu.getType() == KilnContent.RETORT_MENU.get(), "Retort opens its own synchronized menu");
        player.getInventory().setItem(9, IndustrialContent.ENRICHED_BLEND.toStack(2));
        player.getInventory().setItem(10, KilnContent.MINERAL_COAL.toStack());
        menu.quickMoveStack(player, 6); menu.quickMoveStack(player, 7);
        ticks(h, retort, 90);
        var saved = retort.saveWithFullMetadata(h.getLevel().registryAccess());
        var restored = new FertilizerRetortBlockEntity(retort.getBlockPos(), retort.getBlockState());
        restored.setLevel(h.getLevel()); restored.loadWithComponents(saved, h.getLevel().registryAccess());
        h.getLevel().setBlockEntity(restored);
        ticks(h, restored, 310);
        h.assertTrue(restored.getItem(2).is(FarmingContent.FERTILIZER.get()) && restored.getItem(2).getCount() == 8, "Retort preserves partial work on load");
        restored.setItem(2, FarmingContent.FERTILIZER.toStack(29)); restored.setItem(5,new ItemStack(Items.STONE,32));
        ticks(h, restored, 400);
        h.assertTrue(restored.getItem(0).getCount() == 1 && restored.getItem(2).getCount() == 29, "Retort cannot overflow output");
        var pos = restored.getBlockPos();
        h.getLevel().destroyBlock(pos, true);
        var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(0.5));
        h.assertTrue(drops.stream().filter(e -> e.getItem().is(KilnContent.RETORT_ITEM.get())).mapToInt(e -> e.getItem().getCount()).sum() == 1, "Retort drops its own item");
        h.assertTrue(drops.stream().filter(e -> e.getItem().is(FarmingContent.FERTILIZER.get())).mapToInt(e -> e.getItem().getCount()).sum() == 29, "Retort drops exact stored fertilizer");
        h.succeed();
    }
    @GameTest(template = "industrial")
    public static void oneFuelMakesExactlyTwoBatches(GameTestHelper h) {
        var kiln = kiln(h);
        kiln.setItem(0, new ItemStack(Items.CLAY, 9));
        kiln.setItem(1, KilnContent.MINERAL_COAL.toStack());
        ticks(h, kiln, 1800);
        h.assertTrue(kiln.getItem(2).is(Items.BRICK) && kiln.getItem(2).getCount() == 8, "One coal must fire two four-brick batches");
        h.assertTrue(kiln.getItem(0).getCount() == 7 && kiln.getItem(1).isEmpty(), "No third batch or duplicate fuel");
        h.succeed();
    }
    @GameTest(template = "industrial")
    public static void renewableFuelsNeverBurn(GameTestHelper h) {
        var kiln = kiln(h);
        kiln.setItem(0, new ItemStack(Items.CLAY));
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "kiln-test"));
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(kiln.getBlockPos()));
        var menu = (KilnMenu) kiln.createMenu(0, player.getInventory(), player);
        for (var item : List.of(Items.COAL, Items.COAL_BLOCK, Items.CHARCOAL, Items.OAK_LOG, Items.LAVA_BUCKET, Items.BLAZE_ROD, Items.DRIED_KELP_BLOCK, Items.BUCKET)) {
            var stack = new ItemStack(item);
            h.assertTrue(!kiln.canPlaceItemThroughFace(1, stack, Direction.NORTH), "Hopper must reject " + item);
            h.assertTrue(!menu.getSlot(1).mayPlace(stack), "Manual slot must reject " + item);
            kiln.setItem(1, stack);
            ticks(h, kiln, 201);
            h.assertTrue(kiln.getItem(1).getCount() == 1 && kiln.getItem(2).isEmpty(), "Even forced inventory fuel must not burn " + item);
        }
        h.succeed();
    }
    @GameTest(template = "industrial")
    public static void blockedOutputDoesNotConsumeNewFuelOrOverflow(GameTestHelper h) {
        var kiln = kiln(h);
        kiln.setItem(0, new ItemStack(Items.CLAY, 2));
        kiln.setItem(1, KilnContent.MINERAL_COAL.toStack(2));
        kiln.setItem(2, new ItemStack(Items.BRICK, 29)); kiln.setItem(5,new ItemStack(Items.STONE,32));
        ticks(h, kiln, 250);
        h.assertTrue(kiln.getItem(1).getCount() == 1 && kiln.getItem(0).getCount() == 2, "Blocked recipe idles without consuming inputs");
        kiln.setItem(2, new ItemStack(Items.BRICK, 28));
        ticks(h, kiln, 200);
        h.assertTrue(kiln.getItem(2).getCount() == 32 && kiln.getItem(0).getCount() == 1, "Exactly fill the output slot");
        ticks(h, kiln, 1800);
        h.assertTrue(kiln.getItem(1).isEmpty() && kiln.getItem(2).getCount() == 32, "Idle burn automatically feeds next coal without overflowing output");
        h.succeed();
    }
    @GameTest(template = "industrial")
    public static void reloadPreservesPartialBatchAndFuel(GameTestHelper h) {
        var kiln = kiln(h);
        kiln.setItem(0, new ItemStack(Items.CLAY, 8));
        kiln.setItem(1, KilnContent.MINERAL_COAL.toStack());
        ticks(h, kiln, 75);
        var nbt = kiln.saveWithFullMetadata(h.getLevel().registryAccess());
        var restored = new KilnBlockEntity(kiln.getBlockPos(), kiln.getBlockState());
        restored.setLevel(h.getLevel());
        restored.loadWithComponents(nbt, h.getLevel().registryAccess());
        h.getLevel().setBlockEntity(restored);
        ticks(h, restored, 125);
        h.assertTrue(restored.getItem(2).getCount() == 4, "Reload must retain the first 75 work ticks");
        ticks(h, restored, 1401);
        h.assertTrue(restored.getItem(2).getCount() == 8 && restored.getItem(0).getCount() == 6, "Reload must not duplicate or erase fuel energy");
        h.succeed();
    }
    @GameTest(template = "industrial")
    public static void oreFuelAndConversionHaveNoReverseRecipe(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(1, 1, 1));
        for (var ore : List.of(Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE)) {
            var drops = Block.getDrops(ore.defaultBlockState(), h.getLevel(), pos, null, null, new ItemStack(Items.IRON_PICKAXE));
            h.assertTrue(drops.size() == 1 && drops.getFirst().is(KilnContent.MINERAL_COAL.get()), "Coal ore must drop mineral coal");
        }
        var input = CraftingInput.of(1, 1, List.of(KilnContent.MINERAL_COAL.toStack()));
        h.assertTrue(h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel()).isEmpty(), "Unified coal has no legacy conversion");
        var reverse = CraftingInput.of(1, 1, List.of(new ItemStack(Items.COAL)));
        h.assertTrue(h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, reverse, h.getLevel()).isEmpty(), "Ordinary coal cannot become mineral coal");
        h.succeed();
    }
    @GameTest(template = "industrial")
    public static void quickMoveRoutesInputsAndBreakingDropsInventory(GameTestHelper h) {
        var kiln = kiln(h);
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "kiln-slots"));
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(kiln.getBlockPos()));
        var menu = (KilnMenu) kiln.createMenu(0, player.getInventory(), player);
        player.getInventory().setItem(9, new ItemStack(Items.CLAY, 2));
        player.getInventory().setItem(10, KilnContent.MINERAL_COAL.toStack(2));
        menu.quickMoveStack(player, 6);
        menu.quickMoveStack(player, 7);
        h.assertTrue(kiln.getItem(0).getCount() == 2 && kiln.getItem(1).getCount() == 2, "Shift click routes clay and fuel");
        kiln.setItem(2, new ItemStack(Items.BRICK, 4));
        var pos = kiln.getBlockPos();
        h.getLevel().destroyBlock(pos, true);
        var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
        for (var entry : List.of(new ItemStack(Items.CLAY, 2), KilnContent.MINERAL_COAL.toStack(2), new ItemStack(Items.BRICK, 4), KilnContent.KILN_ITEM.toStack())) {
            int total = drops.stream().filter(e -> e.getItem().is(entry.getItem())).mapToInt(e -> e.getItem().getCount()).sum();
            h.assertTrue(total == entry.getCount(), "Breaking must drop exactly inventory plus machine: " + entry);
        }
        h.succeed();
    }
    @GameTest(template="industrial") public static void secondFuelSlotSurvivesAndBurns(GameTestHelper h){
        var k=kiln(h);k.setItem(0,new ItemStack(Items.CLAY));k.setItem(3,KilnContent.MINERAL_COAL.toStack(9));
        k.loadWithComponents(k.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(k.getItem(3).getCount()==9,"New backup slot is not mistaken for legacy overflow");
        ticks(h,k,200);h.assertTrue(k.getItem(2).getCount()==4&&k.getItem(1).getCount()==8&&k.getItem(3).isEmpty(),"Backup fuel supports one exact native batch");h.succeed();
    }

}
