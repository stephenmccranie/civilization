package dev.civilization;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization")
@PrefixGameTestTemplate(false)
public class EverydayGameTests {
    private static FakePlayer player(GameTestHelper h) {
        return new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "everyday-test"));
    }
    private static CookingStationBlockEntity stove(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 2, 4));
        h.getLevel().setBlockAndUpdate(pos, CookingContent.STATION.get().defaultBlockState());
        return (CookingStationBlockEntity) h.getLevel().getBlockEntity(pos);
    }
    private static void ticks(GameTestHelper h, KilnBlockEntity machine, int count) {
        CoalFireFixture.light(machine);
        for (int i = 0; i < count; i++) KilnBlockEntity.tick(h.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
    }
    private static ItemStack craft(GameTestHelper h, CraftingInput input) {
        return h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel())
                .orElseThrow().value().assemble(input, h.getLevel().registryAccess());
    }

    @GameTest(template = "industrial")
    public static void stackLimitsReachInventoryAndMachineSlots(GameTestHelper h) {
        for (var item : List.of(Items.COBBLESTONE, Items.OAK_PLANKS, Items.WHEAT, Items.BREAD,
                KilnContent.MINERAL_COAL.get(), FarmingContent.FERTILIZER.get(), CookingContent.BREAD_DOUGH.get()))
            h.assertTrue(item.getDefaultInstance().getMaxStackSize() == 32, "Ordinary stack must be 32: " + item);
        h.assertTrue(Items.EGG.getDefaultInstance().getMaxStackSize() == 16, "Keep original 16-stacks");
        h.assertTrue(Items.IRON_PICKAXE.getDefaultInstance().getMaxStackSize() == 1, "Tools stay unstackable");
        var p = player(h);
        p.getInventory().add(new ItemStack(Items.COBBLESTONE, 64));
        h.assertTrue(p.getInventory().items.stream().filter(s -> s.is(Items.COBBLESTONE)).count() == 2,
                "Adding 64 splits across two inventory slots");
        h.assertTrue(p.getInventory().items.stream().allMatch(s -> s.getCount() <= s.getMaxStackSize()), "Inventory respects item limit");
        var stove = stove(h);
        var menu = (KilnMenu) stove.createMenu(0, p.getInventory(), p);
        h.assertTrue(menu.getSlot(0).getMaxStackSize(new ItemStack(Items.BEEF)) == 32, "Input slot limit follows component");
        h.succeed();
    }

    @GameTest(template = "industrial")
    public static void allLogVariantsMakeOnePlank(GameTestHelper h) {
        int checked = 0;
        for (var item : BuiltInRegistries.ITEM) {
            var stack = item.getDefaultInstance();
            if (!stack.is(ItemTags.LOGS) && !stack.is(ItemTags.BAMBOO_BLOCKS)) continue;
            var result = craft(h, CraftingInput.of(1, 1, List.of(stack)));
            h.assertTrue(result.is(ItemTags.PLANKS) && result.getCount() == 1,
                    "One full wood block makes one plank, including stripped variants: " + item);
            checked++;
        }
        h.assertTrue(checked >= 40, "Cover wood, logs, stems, hyphae and bamboo blocks");
        h.succeed();
    }

    @GameTest(template = "industrial")
    public static void boneMealCannotGrowLandOrWaterPlants(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(3, 1, 3));
        var p = player(h);
        var meal = new ItemStack(Items.BONE_MEAL, 8);
        p.setItemInHand(InteractionHand.MAIN_HAND, meal);
        for (var block : List.of(Blocks.WHEAT, FarmingContent.FERTILIZED_WHEAT.get(), Blocks.OAK_SAPLING, Blocks.SHORT_GRASS)) {
            h.getLevel().setBlockAndUpdate(pos.below(), (block instanceof CropBlock ? Blocks.FARMLAND : Blocks.GRASS_BLOCK).defaultBlockState());
            h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
            var original = h.getLevel().getBlockState(pos);
            var context = new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            h.assertTrue(!Items.BONE_MEAL.useOn(context).consumesAction(), "Manual growth rejected");
            h.assertTrue(!BoneMealItem.applyBonemeal(meal, h.getLevel(), pos, p), "Programmatic growth rejected");
            h.assertTrue(!BoneMealItem.growCrop(meal, h.getLevel(), pos), "Dispenser/villager entry point rejected");
            h.assertTrue(h.getLevel().getBlockState(pos).equals(original), "No plant change");
        }
        h.getLevel().setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
        h.assertTrue(!BoneMealItem.growWaterPlant(meal, h.getLevel(), pos, Direction.UP), "Underwater growth rejected");
        h.assertTrue(meal.getCount() == 8, "Rejected attempts do not consume bone meal");
        h.succeed();
    }

    @GameTest(template = "industrial", timeoutTicks = 40)
    public static void poweredDispenserCannotUseBoneMeal(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(3, 2, 3));
        h.getLevel().setBlockAndUpdate(pos.east().below(), Blocks.FARMLAND.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos.east(), Blocks.WHEAT.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
        var dispenser = (DispenserBlockEntity) h.getLevel().getBlockEntity(pos);
        dispenser.setItem(0, new ItemStack(Items.BONE_MEAL, 4));
        h.getLevel().setBlockAndUpdate(pos.above(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(10, () -> {
            h.assertTrue(dispenser.getItem(0).getCount() == 4, "Powered dispenser consumes no meal");
            h.assertTrue(h.getLevel().getBlockState(pos.east()).getValue(CropBlock.AGE) == 0, "Crop stays young");
            h.succeed();
        });
    }

    @GameTest(template = "industrial")
    public static void cookingRecipeAndFuelBoundaryIsComplete(GameTestHelper h) {
        for (var item : List.of(Items.BEEF, Items.PORKCHOP, Items.CHICKEN, Items.MUTTON, Items.RABBIT,
                Items.COD, Items.SALMON, Items.POTATO, Items.KELP)) {
            var input = new SingleRecipeInput(new ItemStack(item));
            h.assertTrue(h.getLevel().getRecipeManager().getRecipeFor(CookingContent.RECIPE_TYPE.get(), input, h.getLevel()).isPresent(), "Stove cooks " + item);
            for (var type : List.of(RecipeType.SMELTING, RecipeType.SMOKING, RecipeType.CAMPFIRE_COOKING, RecipeType.BLASTING))
                h.assertTrue(h.getLevel().getRecipeManager().getRecipeFor(type, input, h.getLevel()).isEmpty(), "No vanilla heat bypass for " + item);
        }
        h.assertTrue(h.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING,
                new SingleRecipeInput(new ItemStack(Items.SAND)), h.getLevel()).isEmpty(), "Glass moves to the kiln");
        var machine = stove(h);
        var p = player(h);
        var menu = (KilnMenu) machine.createMenu(0, p.getInventory(), p);
        for (var fuel : List.of(Items.COAL, Items.CHARCOAL, Items.OAK_LOG, Items.LAVA_BUCKET, Items.COAL_BLOCK, Items.BLAZE_ROD, Items.BUCKET)) {
            var stack = new ItemStack(fuel);
            h.assertTrue(!menu.getSlot(1).mayPlace(stack) && !machine.canPlaceItemThroughFace(1, stack, Direction.NORTH), "Reject fuel from menu/hopper: " + fuel);
            machine.setItem(0, new ItemStack(Items.BEEF));
            machine.setItem(1, stack);
            ticks(h, machine, 201);
            h.assertTrue(machine.getItem(1).getCount() == 1 && machine.getItem(2).isEmpty(), "Forced invalid fuel cannot cook");
        }
        h.succeed();
    }

    @GameTest(template = "industrial")
    public static void stoveCraftsAndCooksWithoutShell(GameTestHelper h) {
        var copper = new ItemStack(Items.COPPER_INGOT);
        var stone = new ItemStack(Items.COBBLESTONE);
        var result = craft(h, CraftingInput.of(3, 3, List.of(copper, copper, copper, stone, new ItemStack(Items.COBBLESTONE), stone, stone, stone, stone)));
        h.assertTrue(result.is(CookingContent.STATION_ITEM.get()), "Copper top, six cobble make stove");
        var machine = stove(h);
        machine.setItem(0, new ItemStack(Items.BEEF, 9));
        machine.setItem(1, KilnContent.MINERAL_COAL.toStack());
        ticks(h, machine, 1);
        h.assertTrue(machine.getBlockState().getValue(MachineFeedback.WORKING) && machine.operatingStatus() == 1, "Working state set on ignition");
        ticks(h, machine, 1799);
        h.assertTrue(machine.getItem(2).is(Items.COOKED_BEEF) && machine.getItem(2).getCount() == 2, "Exactly two meals per uninterrupted coal");
        h.assertTrue(machine.getItem(0).getCount() == 7 && machine.getItem(1).isEmpty(), "No free third meal");
        h.assertTrue(!machine.getBlockState().getValue(MachineFeedback.WORKING) && machine.operatingStatus() == 4, "Stopped machine requests fuel");
        h.succeed();
    }

    @GameTest(template = "industrial")
    public static void bakingAndColdSubsistenceFormOneFoodLoop(GameTestHelper h) {
        var wheat = new ItemStack(Items.WHEAT);
        var dough = craft(h, CraftingInput.of(3, 1, List.of(wheat, wheat, wheat)));
        h.assertTrue(dough.is(CookingContent.BREAD_DOUGH.get()), "Wheat crafts uncooked dough");
        var p = player(h);
        CalorieFoodData.of(p).reserve().set(0);
        p.eat(h.getLevel(), dough.copy(), dough.getFoodProperties(p));
        h.assertTrue(CalorieFoodData.of(p).reserve().calories() == 300, "A farmer can eat without coal");
        var machine = stove(h);
        machine.setItem(0, dough);
        machine.setItem(1, KilnContent.MINERAL_COAL.toStack());
        ticks(h, machine, 200);
        var bread = machine.removeItem(2, 1);
        h.assertTrue(bread.is(Items.BREAD) && FoodCalories.of(bread, bread.getFoodProperties(p)) == 500, "Baking adds food value");
        var ration = craft(h, CraftingInput.of(3, 1, List.of(bread, bread, bread)));
        h.assertTrue(FoodCalories.of(ration, ration.getFoodProperties(p)) == 1500, "Packing adds no energy");
        var cookie = craft(h, CraftingInput.of(3, 1, List.of(wheat, new ItemStack(Items.COCOA_BEANS), wheat)));
        h.assertTrue(cookie.is(CookingContent.COOKIE_DOUGH.get()) && cookie.getCount() == 8, "Cookie ingredients yield eight uncooked portions");
        var pie = craft(h, CraftingInput.of(3, 1, List.of(new ItemStack(Items.PUMPKIN), new ItemStack(Items.SUGAR), new ItemStack(Items.EGG))));
        h.assertTrue(pie.is(CookingContent.UNBAKED_PIE.get()), "Pie requires baking");
        var milk = new ItemStack(Items.MILK_BUCKET);
        var sugar = new ItemStack(Items.SUGAR);
        var cakeInput = CraftingInput.of(3, 3, List.of(milk, milk, milk, sugar, new ItemStack(Items.EGG), sugar, wheat, wheat, wheat));
        h.assertTrue(craft(h, cakeInput).is(CookingContent.CAKE_BATTER.get()), "Cake requires baking too");
        var cakeRecipe = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, cakeInput, h.getLevel()).orElseThrow();
        h.assertTrue(cakeRecipe.value().getRemainingItems(cakeInput).stream().filter(s -> s.is(Items.BUCKET)).count() == 3, "Preparing cake returns all milk buckets");
        for (var entry : List.of(CookingContent.COOKIE_DOUGH, CookingContent.UNBAKED_PIE, CookingContent.CAKE_BATTER)) {
            machine.setItem(0, entry.toStack());
            machine.setItem(1, KilnContent.MINERAL_COAL.toStack());
            ticks(h, machine, 200);
            h.assertTrue(CookingContent.requiresCooking(machine.getItem(2)), "Prepared food bakes");
            machine.removeItem(2, 32);
        }
        h.succeed();
    }

    @GameTest(template = "industrial")
    public static void stoveSaveBlockedOutputAndBreakingPreserveStock(GameTestHelper h) {
        var machine = stove(h);
        machine.setItem(0, new ItemStack(Items.POTATO, 2));
        machine.setItem(1, KilnContent.MINERAL_COAL.toStack(2));
        machine.setItem(2, new ItemStack(Items.BAKED_POTATO, 32)); machine.setItem(5,new ItemStack(Items.STONE,32));
        ticks(h, machine, 200);
        h.assertTrue(machine.getItem(1).getCount() == 1 && machine.operatingStatus() == 3, "Full output preserves recipe while fire idles");
        machine.removeItem(2, 1);
        ticks(h, machine, 75);
        var saved = machine.saveWithFullMetadata(h.getLevel().registryAccess());
        var restored = new CookingStationBlockEntity(machine.getBlockPos(), machine.getBlockState());
        restored.setLevel(h.getLevel());
        restored.loadWithComponents(saved, h.getLevel().registryAccess());
        h.getLevel().setBlockEntity(restored);
        ticks(h, restored, 125);
        h.assertTrue(restored.getItem(2).getCount() == 32 && restored.getItem(0).getCount() == 1, "Saved partial meal completes exactly once");
        ticks(h, restored, 1800);
        h.assertTrue(restored.getItem(1).getCount() == 1 && restored.getItem(0).getCount() == 1, "Blocked stove continues low idle burn");
        var pos = restored.getBlockPos();
        h.getLevel().destroyBlock(pos, true);
        var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(0.6));
        for (var entry : List.of(new ItemStack(Items.BAKED_POTATO, 32), new ItemStack(Items.POTATO), KilnContent.MINERAL_COAL.toStack(), CookingContent.STATION_ITEM.toStack()))
            h.assertTrue(drops.stream().filter(e -> e.getItem().is(entry.getItem())).mapToInt(e -> e.getItem().getCount()).sum() == entry.getCount(), "Breaking drops exact stock: " + entry);
        h.succeed();
    }

    @GameTest(template = "industrial", timeoutTicks = 300)
    public static void realHoppersFeedAndCollectFromStove(GameTestHelper h) {
        var machine = stove(h);
        var pos = machine.getBlockPos();
        h.getLevel().setBlockAndUpdate(pos.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        h.getLevel().setBlockAndUpdate(pos.below(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        var top = (HopperBlockEntity) h.getLevel().getBlockEntity(pos.above());
        var bottom = (HopperBlockEntity) h.getLevel().getBlockEntity(pos.below());
        top.setItem(0, new ItemStack(Items.BEEF));
        machine.setItem(1, KilnContent.MINERAL_COAL.toStack());CoalFireFixture.light(machine);
        h.runAfterDelay(250, () -> {
            h.assertTrue(top.isEmpty() && machine.getItem(0).isEmpty() && machine.getItem(2).isEmpty(), "Hopper pipeline moves actual stock");
            h.assertTrue(bottom.getItem(0).is(Items.COOKED_BEEF) && bottom.getItem(0).getCount() == 1, "Bottom hopper collects cooked output");
            h.succeed();
        });
    }

    @GameTest(template = "industrial")
    public static void burningAnimalsStillDropRawFood(GameTestHelper h) {
        var cow = EntityType.COW.create(h.getLevel());
        cow.igniteForSeconds(30);
        var params = new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.THIS_ENTITY, cow)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 2, 2))))
                .withParameter(LootContextParams.DAMAGE_SOURCE, h.getLevel().damageSources().onFire()).create(LootContextParamSets.ENTITY);
        var drops = h.getLevel().getServer().reloadableRegistries().getLootTable(cow.getLootTable()).getRandomItems(params);
        h.assertTrue(drops.stream().anyMatch(s -> s.is(Items.BEEF)) && drops.stream().noneMatch(s -> s.is(Items.COOKED_BEEF)), "Fire is not free cooking");
        h.succeed();
    }

    @GameTest(template = "industrial")
    public static void livestockDropOneMeatEach(GameTestHelper h) {
        var types=List.of(EntityType.COW,EntityType.MOOSHROOM,EntityType.PIG,EntityType.HOGLIN,
                EntityType.SHEEP,EntityType.CHICKEN,EntityType.RABBIT);
        var meats=List.of(Items.BEEF,Items.BEEF,Items.PORKCHOP,Items.PORKCHOP,Items.MUTTON,Items.CHICKEN,Items.RABBIT);
        for(int i=0;i<types.size();i++) {
            var animal=types.get(i).create(h.getLevel());
            var params=new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.THIS_ENTITY,animal)
                    .withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(h.absolutePos(new BlockPos(2,2,2))))
                    .withParameter(LootContextParams.DAMAGE_SOURCE,h.getLevel().damageSources().generic())
                    .create(LootContextParamSets.ENTITY);
            var drops=h.getLevel().getServer().reloadableRegistries().getLootTable(animal.getLootTable()).getRandomItems(params);
            var meat=meats.get(i);
            h.assertTrue(drops.stream().filter(s->s.is(meat)).mapToInt(ItemStack::getCount).sum()==1,
                    "Exactly one raw meat from "+types.get(i));
        }
        h.succeed();
    }

    @GameTest(template = "industrial")
    public static void villagersCannotSellOrBakeCookedFood(GameTestHelper h) throws Exception {
        var villager = EntityType.VILLAGER.create(h.getLevel());
        var saved = villager.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        var offers = new net.minecraft.world.item.trading.MerchantOffers();
        for (var food : List.of(Items.BREAD, Items.COOKED_PORKCHOP, Items.COOKIE, Items.PUMPKIN_PIE, Items.CAKE, Items.CARROT))
            offers.add(new net.minecraft.world.item.trading.MerchantOffer(new net.minecraft.world.item.trading.ItemCost(Items.EMERALD), new ItemStack(food), 16, 1, 0.05f));
        saved.put("Offers", net.minecraft.world.item.trading.MerchantOffers.CODEC.encodeStart(
                h.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), offers).getOrThrow());
        villager.load(saved);
        h.assertTrue(villager.getOffers().size() == 1 && villager.getOffers().getFirst().getResult().is(Items.CARROT), "Cooked trades removed, raw food retained");
        villager.getInventory().addItem(new ItemStack(Items.WHEAT, 9));
        var work = new net.minecraft.world.entity.ai.behavior.WorkAtComposter();
        var method = work.getClass().getDeclaredMethod("makeBread", net.minecraft.world.entity.npc.Villager.class);
        method.setAccessible(true);
        method.invoke(work, villager);
        h.assertTrue(villager.getInventory().countItem(Items.WHEAT) == 9 && villager.getInventory().countItem(Items.BREAD) == 0, "Villagers cannot bypass stove");
        h.succeed();
    }
}
