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
    private static KilnBlockEntity kiln(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(2, 1, 1));
        h.getLevel().setBlockAndUpdate(pos, KilnContent.KILN.get().defaultBlockState());
        buildShell(h, pos, Direction.NORTH, false);
        return (KilnBlockEntity) h.getLevel().getBlockEntity(pos);
    }
    private static void ticks(GameTestHelper h, KilnBlockEntity kiln, int count) {
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
            var block = switch (part.material()) {
                case "air" -> Blocks.AIR;
                case "brick", "brick_hatch" -> Blocks.BRICKS;
                case "copper" -> Blocks.COPPER_BLOCK;
                default -> Blocks.COBBLESTONE;
            };
            h.getLevel().setBlockAndUpdate(MachineStructure.position(pos, facing, part), block.defaultBlockState());
        }
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
        var ingredients = CraftingInput.of(2, 1, List.of(new ItemStack(Items.CLAY_BALL), new ItemStack(Items.GRAVEL)));
        var recipe = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, ingredients, h.getLevel()).orElseThrow();
        h.assertTrue(recipe.value().assemble(ingredients, h.getLevel().registryAccess()).is(FarmingContent.MINERAL_BLEND.get()), "Clay and gravel craft raw blend");
        h.assertTrue(h.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(FarmingContent.MINERAL_BLEND.toStack()), h.getLevel()).isEmpty(), "Blend cannot bypass finite fuel in a furnace");
        var kiln = retort(h);
        kiln.setItem(0, FarmingContent.MINERAL_BLEND.toStack(5));
        kiln.setItem(1, KilnContent.MINERAL_COAL.toStack());
        ticks(h, kiln, 1800);
        h.assertTrue(kiln.getItem(2).is(FarmingContent.FERTILIZER.get()) && kiln.getItem(2).getCount() == 16, "One fuel makes exactly sixteen fertilizer");
        h.assertTrue(kiln.getItem(0).getCount() == 1 && kiln.getItem(1).isEmpty(), "Four blends and one fuel consumed");
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "kiln-farmer"));
        var pos = h.absolutePos(new BlockPos(3, 1, 1));
        h.getLevel().setBlockAndUpdate(pos.below(), Blocks.FARMLAND.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos, Blocks.WHEAT.defaultBlockState());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, kiln.removeItem(2, 1));
        player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), Direction.UP, pos, false)));
        var crop = h.getLevel().getBlockState(pos);
        h.assertTrue(crop.is(FarmingContent.FERTILIZED_WHEAT.get()) && player.getMainHandItem().isEmpty(), "Manufactured fertilizer must apply once");
        var mature = crop.setValue(net.minecraft.world.level.block.CropBlock.AGE, 7);
        var drops = Block.getDrops(mature, h.getLevel(), pos, null, player, ItemStack.EMPTY);
        h.assertTrue(drops.stream().filter(s -> s.is(Items.WHEAT)).mapToInt(ItemStack::getCount).sum() == 3, "Manufactured fertilizer yields three wheat");
        h.succeed();
    }
    @GameTest(template = "industrial")
    public static void machinesOnlyProcessTheirOwnRecipes(GameTestHelper h) {
        var kiln = kiln(h);
        var retort = retort(h);
        kiln.setItem(0, FarmingContent.MINERAL_BLEND.toStack(2));
        retort.setItem(0, new ItemStack(Items.CLAY));
        for (var machine : List.of(kiln, retort)) {
            machine.setItem(1, KilnContent.MINERAL_COAL.toStack());
            h.assertTrue(!machine.canPlaceItemThroughFace(0, machine.getItem(0), Direction.UP), "Hoppers reject wrong machine input");
            ticks(h, machine, 450);
            h.assertTrue(machine.getItem(2).isEmpty() && machine.getItem(1).getCount() == 1, "Wrong recipe neither produces output nor burns fuel");
        }
        retort.setItem(0, FarmingContent.MINERAL_BLEND.toStack());
        retort.setItem(1, new ItemStack(Items.CHARCOAL));
        ticks(h, retort, 450);
        h.assertTrue(retort.getItem(2).isEmpty() && retort.getItem(1).getCount() == 1, "Retort also rejects renewable fuel when forced");
        h.succeed();
    }
    @GameTest(template = "industrial")
    public static void retortRecipeRequiresBricksAndCopper(GameTestHelper h) {
        var grid = CraftingInput.of(3, 3, List.of(new ItemStack(Items.BRICKS), new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.BRICKS),
                new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.FURNACE), new ItemStack(Items.COPPER_INGOT),
                new ItemStack(Items.BRICKS), new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.BRICKS)));
        var recipe = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, h.getLevel()).orElseThrow();
        h.assertTrue(recipe.value().assemble(grid, h.getLevel().registryAccess()).is(KilnContent.RETORT_ITEM.get()), "Four brick blocks, four copper and a furnace make a retort");
        h.succeed();
    }
    @GameTest(template = "industrial")
    public static void retortSaveMenuOutputAndDropsAreCorrect(GameTestHelper h) {
        var retort = retort(h);
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "retort-slots"));
        var menu = (KilnMenu) retort.createMenu(0, player.getInventory(), player);
        h.assertTrue(menu.getType() == KilnContent.RETORT_MENU.get(), "Retort opens its own synchronized menu");
        player.getInventory().setItem(9, FarmingContent.MINERAL_BLEND.toStack(2));
        player.getInventory().setItem(10, KilnContent.MINERAL_COAL.toStack());
        menu.quickMoveStack(player, 3); menu.quickMoveStack(player, 4);
        ticks(h, retort, 90);
        var saved = retort.saveWithFullMetadata(h.getLevel().registryAccess());
        var restored = new FertilizerRetortBlockEntity(retort.getBlockPos(), retort.getBlockState());
        restored.setLevel(h.getLevel()); restored.loadWithComponents(saved, h.getLevel().registryAccess());
        h.getLevel().setBlockEntity(restored);
        ticks(h, restored, 310);
        h.assertTrue(restored.getItem(2).is(FarmingContent.FERTILIZER.get()) && restored.getItem(2).getCount() == 4, "Retort preserves partial work on load");
        restored.setItem(2, FarmingContent.FERTILIZER.toStack(61));
        ticks(h, restored, 400);
        h.assertTrue(restored.getItem(0).getCount() == 1 && restored.getItem(2).getCount() == 61, "Retort cannot overflow output");
        var pos = restored.getBlockPos();
        h.getLevel().destroyBlock(pos, true);
        var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(0.5));
        h.assertTrue(drops.stream().filter(e -> e.getItem().is(KilnContent.RETORT_ITEM.get())).mapToInt(e -> e.getItem().getCount()).sum() == 1, "Retort drops its own item");
        h.assertTrue(drops.stream().filter(e -> e.getItem().is(FarmingContent.FERTILIZER.get())).mapToInt(e -> e.getItem().getCount()).sum() == 61, "Retort drops exact stored fertilizer");
        h.succeed();
    }
    @GameTest(template = "industrial")
    public static void oneFuelMakesExactlyEightBatches(GameTestHelper h) {
        var kiln = kiln(h);
        kiln.setItem(0, new ItemStack(Items.CLAY, 9));
        kiln.setItem(1, KilnContent.MINERAL_COAL.toStack());
        ticks(h, kiln, 1800);
        h.assertTrue(kiln.getItem(2).is(Items.BRICK) && kiln.getItem(2).getCount() == 32, "One mineral coal must fire eight four-brick batches");
        h.assertTrue(kiln.getItem(0).getCount() == 1 && kiln.getItem(1).isEmpty(), "No ninth batch or duplicate fuel");
        h.succeed();
    }
    @GameTest(template = "industrial")
    public static void renewableFuelsNeverBurn(GameTestHelper h) {
        var kiln = kiln(h);
        kiln.setItem(0, new ItemStack(Items.CLAY));
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "kiln-test"));
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
        kiln.setItem(2, new ItemStack(Items.BRICK, 61));
        ticks(h, kiln, 250);
        h.assertTrue(kiln.getItem(1).getCount() == 2 && kiln.getItem(0).getCount() == 2, "No ignition without room for whole batch");
        kiln.setItem(2, new ItemStack(Items.BRICK, 60));
        ticks(h, kiln, 200);
        h.assertTrue(kiln.getItem(2).getCount() == 64 && kiln.getItem(0).getCount() == 1, "Exactly fill the output slot");
        ticks(h, kiln, 1800);
        h.assertTrue(kiln.getItem(1).getCount() == 1 && kiln.getItem(2).getCount() == 64, "Burn lit fuel but never ignite next fuel while blocked");
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
        h.assertTrue(restored.getItem(2).getCount() == 32 && restored.getItem(0).isEmpty(), "Reload must not duplicate or erase fuel energy");
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
        var recipe = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel()).orElseThrow();
        h.assertTrue(recipe.value().assemble(input, h.getLevel().registryAccess()).is(Items.COAL), "Fuel can become vanilla coal for torches");
        var reverse = CraftingInput.of(1, 1, List.of(new ItemStack(Items.COAL)));
        h.assertTrue(h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, reverse, h.getLevel()).isEmpty(), "Ordinary coal cannot become mineral coal");
        h.succeed();
    }
    @GameTest(template = "industrial")
    public static void quickMoveRoutesInputsAndBreakingDropsInventory(GameTestHelper h) {
        var kiln = kiln(h);
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "kiln-slots"));
        var menu = (KilnMenu) kiln.createMenu(0, player.getInventory(), player);
        player.getInventory().setItem(9, new ItemStack(Items.CLAY, 2));
        player.getInventory().setItem(10, KilnContent.MINERAL_COAL.toStack(2));
        menu.quickMoveStack(player, 3);
        menu.quickMoveStack(player, 4);
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
}
