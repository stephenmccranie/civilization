package dev.civilization;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("civilization")
@PrefixGameTestTemplate(false)
public class FarmingGameTests {
    private static FakePlayer farmer(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "farmer-test"));
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }
    private static UseOnContext context(FakePlayer player, BlockPos pos) {
        return new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }
    private static int count(List<ItemStack> stacks, net.minecraft.world.item.Item item) {
        return stacks.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    @GameTest(template = "empty")
    public static void everyVanillaFoodHasIntentionalValue(GameTestHelper helper) {
        var player = farmer(helper);
        for (var item : BuiltInRegistries.ITEM) {
            var id = BuiltInRegistries.ITEM.getKey(item);
            if (id.getNamespace().equals("minecraft") && item.getDefaultInstance().getFoodProperties(player) != null)
                helper.assertTrue(FoodCatalog.KCAL.containsKey(id.getPath()), "Missing vanilla food: " + id);
        }
        var beef = Items.COOKED_BEEF.getDefaultInstance();
        var pork = Items.COOKED_PORKCHOP.getDefaultInstance();
        helper.assertTrue(FoodCalories.of(beef, beef.getFoodProperties(player)) == 700, "Beef must be 700 kcal");
        helper.assertTrue(FoodCalories.of(pork, pork.getFoodProperties(player)) == 700, "Pork must match beef");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lightClearingAndCropsHaveSeparateCosts(GameTestHelper helper) {
        helper.assertTrue(LaborCosts.breaking(Blocks.STONE.defaultBlockState()) == 4, "Stone stays 4 kcal");
        helper.assertTrue(LaborCosts.breaking(Blocks.OAK_LOG.defaultBlockState()) == 4, "Logs stay 4 kcal");
        for (var block : List.of(Blocks.POPPY, Blocks.SHORT_GRASS, Blocks.OAK_LEAVES))
            helper.assertTrue(LaborCosts.breaking(block.defaultBlockState()) == 0.25, "Vegetation costs 0.25 kcal");
        helper.assertTrue(LaborCosts.breaking(Blocks.WHEAT.defaultBlockState()) == 1, "Harvest costs 1 kcal");
        helper.assertTrue(LaborCosts.placing(Blocks.WHEAT.defaultBlockState()) == 1, "Planting costs 1 kcal");
        helper.assertTrue(LaborCosts.breaking(FarmingContent.FERTILIZED_WHEAT.get().defaultBlockState()) == 1, "Fertilized crop is still light labor");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void realSeedPlacementCostsOneCalorie(GameTestHelper helper) {
        var player = farmer(helper);
        var soil = helper.absolutePos(new BlockPos(1, 0, 1));
        helper.getLevel().setBlockAndUpdate(soil, Blocks.FARMLAND.defaultBlockState());
        player.setPos(Vec3.atCenterOf(soil.above()));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT_SEEDS, 2));
        player.getMainHandItem().useOn(context(player, soil));
        helper.assertTrue(helper.getLevel().getBlockState(soil.above()).is(Blocks.WHEAT), "Seed must plant ordinary wheat");
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "Planting uses one seed");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(CalorieFoodData.of(player).reserve().calories() == 2399, "Planting costs exactly one calorie");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void fertilizerIsSingleUseAndStatePersists(GameTestHelper helper) {
        var level = GeographyGameTests.region(helper, "river");
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "fertilizer-test"));
        player.setGameMode(GameType.SURVIVAL);
        BlockPos pos = new BlockPos(16, 64, 16);
        level.setBlockAndUpdate(pos.below(), Blocks.FARMLAND.defaultBlockState());
        level.setBlockAndUpdate(pos, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 3));
        player.setItemInHand(InteractionHand.MAIN_HAND, FarmingContent.FERTILIZER.toStack(2));
        player.setPos(Vec3.atCenterOf(pos.above()));
        FarmingContent.FERTILIZER.get().useOn(context(player, pos));
        var fertilized = level.getBlockState(pos);
        helper.assertTrue(fertilized.is(FarmingContent.FERTILIZED_WHEAT.get()) && fertilized.getValue(CropBlock.AGE) == 3,
                "Fertilizer must change crop identity, preserving age");
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "Consume exactly one fertilizer");
        helper.assertTrue(CalorieFoodData.of(player).reserve().calories() == 2399, "Fertilizing costs one calorie");
        FarmingContent.FERTILIZER.get().useOn(context(player, pos));
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "Cannot apply twice");
        var saved = NbtUtils.writeBlockState(fertilized);
        var restored = NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), saved);
        helper.assertTrue(restored.equals(fertilized), "Fertilizer state must survive block-state save/load");
        for (int i = 0; i < 3; i++) FarmingContent.FERTILIZED_WHEAT.get().performBonemeal(
                level, level.random, pos, level.getBlockState(pos));
        helper.assertTrue(level.getBlockState(pos).is(FarmingContent.FERTILIZED_WHEAT.get())
                && level.getBlockState(pos).getValue(CropBlock.AGE) == 7, "Growing must preserve fertilizer until maturity");
        level.setBlockAndUpdate(pos, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
        FarmingContent.FERTILIZER.get().useOn(context(player, pos));
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "Mature wheat cannot be fertilized at harvest time");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wheatLootIsSustainableAndFertilizerBoostsYield(GameTestHelper helper) {
        var player = farmer(helper);
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        for (var crop : List.of(Blocks.WHEAT, FarmingContent.FERTILIZED_WHEAT.get())) {
            boolean boosted = crop == FarmingContent.FERTILIZED_WHEAT.get();
            var mature = crop.defaultBlockState().setValue(CropBlock.AGE, 7);
            var drops = Block.getDrops(mature, helper.getLevel(), pos, null, player, ItemStack.EMPTY);
            helper.assertTrue(count(drops, Items.WHEAT) == (boosted ? 3 : 1), "Correct wheat yield");
            helper.assertTrue(count(drops, Items.WHEAT_SEEDS) == 2, "Harvest guarantees replanting and a spare seed");
            drops = Block.getDrops(crop.defaultBlockState(), helper.getLevel(), pos, null, player, ItemStack.EMPTY);
            helper.assertTrue(count(drops, Items.WHEAT) == 0 && count(drops, Items.WHEAT_SEEDS) == 1,
                    "Immature crop returns only its seed; no food or fertilizer refund");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fieldRationRecipePreservesFoodEnergy(GameTestHelper helper) {
        var player = farmer(helper);
        var input = CraftingInput.of(3, 1, List.of(Items.BREAD.getDefaultInstance(), Items.BREAD.getDefaultInstance(), Items.BREAD.getDefaultInstance()));
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow();
        helper.assertTrue(recipe.id().equals(ResourceLocation.parse("civilization:field_ration")), "Three bread must match the ration recipe");
        var result = recipe.value().assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(result.is(FarmingContent.RATION.get()) && result.getCount() == 1, "One field ration per three bread");
        helper.assertTrue(FoodCalories.of(result, result.getFoodProperties(player)) == 3 * FoodCalories.breadCalories(), "No energy created by packing");
        var shortInput = CraftingInput.of(2, 1, List.of(Items.BREAD.getDefaultInstance(), Items.BREAD.getDefaultInstance()));
        helper.assertTrue(!recipe.value().matches(shortInput, helper.getLevel()), "Two loaves cannot make a ration");
        result.onCraftedBy(helper.getLevel(), player, 1);
        CalorieFoodData.of(player).reserve().set(100);
        player.eat(helper.getLevel(), result, result.getFoodProperties(player));
        helper.assertTrue(CalorieFoodData.of(player).reserve().calories() == 1600, "Eating restores 1500 kcal once");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void realFertilizedHarvestProducesFoodLogs(GameTestHelper helper) {
        var player = farmer(helper);
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.FARMLAND.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos, FarmingContent.FERTILIZED_WHEAT.get().defaultBlockState().setValue(CropBlock.AGE, 7));
        player.setPos(Vec3.atCenterOf(pos.above()));
        helper.assertTrue(player.gameMode.destroyBlock(pos), "Manual harvest must succeed");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(CalorieFoodData.of(player).reserve().calories() == 2399, "Harvest costs one calorie");
            helper.succeed();
        });
    }
}
