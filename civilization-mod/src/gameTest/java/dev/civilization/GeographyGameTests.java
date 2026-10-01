package dev.civilization;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("civilization")
@PrefixGameTestTemplate(false)
public class GeographyGameTests {
    static ServerLevel region(GameTestHelper helper, String name) {
        return helper.getLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath("civilization", "test_" + name)));
    }
    @GameTest(template = "empty")
    public static void riverHeightAndCacheRefresh(GameTestHelper helper) {
        var level = region(helper, "river");
        var low = new BlockPos(100, 58, 100);
        helper.assertTrue(Geography.canFarm(level, low), "River ground at lower boundary qualifies");
        helper.assertTrue(Geography.canFarm(level, low.atY(70)), "Upper terrace qualifies");
        helper.assertTrue(!Geography.canFarm(level, low.atY(57)), "Deep ground fails");
        helper.assertTrue(!Geography.canFarm(level, low.atY(71)), "High ground fails");
        var before = Geography.inspect(level, low);
        Geography.tagsChanged();
        helper.assertTrue(before.equals(Geography.inspect(level, low)), "Tag refresh preserves geography");
        Geography.unload(level);
        helper.assertTrue(before.equals(Geography.inspect(level, low)), "Cache recreation preserves geography");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void waterCannotCreateFarmlandOrConsumeFertilizer(GameTestHelper helper) {
        var level = region(helper, "plains");
        var pos = new BlockPos(16, 64, 16);
        level.setBlockAndUpdate(pos.below(), Blocks.FARMLAND.defaultBlockState());
        level.setBlockAndUpdate(pos.below().east(), Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(pos, Blocks.WHEAT.defaultBlockState());
        helper.assertTrue(!Geography.canFarm(level, pos.below()), "Placed water cannot create a natural river band");
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "geography-test"));
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, FarmingContent.FERTILIZER.toStack(2));
        double calories = CalorieFoodData.of(player).reserve().calories();
        FarmingContent.FERTILIZER.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
        helper.assertTrue(player.getMainHandItem().getCount() == 2, "Ineligible fertilizer is not consumed");
        helper.assertTrue(level.getBlockState(pos).is(Blocks.WHEAT), "Crop remains untreated");
        helper.assertTrue(calories == CalorieFoodData.of(player).reserve().calories(), "No labor charge on failure");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void actualCropDispatchBlocksAllTaggedCropsOutsideRegion(GameTestHelper helper) {
        var level = region(helper, "plains");
        var pos = new BlockPos(32, 64, 32);
        var random = RandomSource.create(19);
        level.setBlockAndUpdate(pos.below(), Blocks.FARMLAND.defaultBlockState());
        for (var block : List.of(Blocks.WHEAT, FarmingContent.FERTILIZED_WHEAT.get(), Blocks.CARROTS,
                Blocks.POTATOES, Blocks.BEETROOTS, Blocks.MELON_STEM, Blocks.PUMPKIN_STEM,
                Blocks.TORCHFLOWER_CROP, Blocks.PITCHER_CROP, Blocks.SWEET_BERRY_BUSH, Blocks.SUGAR_CANE, Blocks.COCOA)) {
            var state = block.defaultBlockState();
            helper.assertTrue(state.is(Geography.CROPS), "Crop must be covered: " + block);
            level.setBlock(pos, state, 2);
            for (int i = 0; i < 100; i++) state.randomTick(level, pos, random);
            helper.assertTrue(level.getBlockState(pos).equals(state), "Ineligible crop must remain dormant: " + block);
        }
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void eligibleWheatReallyGrowsAndSkyWheatDoesNot(GameTestHelper helper) {
        var level = region(helper, "river");
        var pos = new BlockPos(48, 64, 48);
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
            level.getChunkAt(pos.offset(x * 16, 0, z * 16));
        level.setBlockAndUpdate(pos.below(), Blocks.FARMLAND.defaultBlockState());
        level.setBlockAndUpdate(pos.above(), Blocks.GLOWSTONE.defaultBlockState());
        level.setBlockAndUpdate(pos, Blocks.WHEAT.defaultBlockState());
        helper.runAfterDelay(5, () -> {
            // These isolated dimensions have no players: renew their temporary chunk tickets after lighting settles.
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
                level.getChunkAt(pos.offset(x * 16, 0, z * 16));
            var random = RandomSource.create(88);
            // This target previously rounded the final stage down to 6.999999999999999.
            var soil=SoilSystem.observe(level,pos.below());
            soil.crop="wheat";soil.work=0;soil.target=7087.834079944438;
            SoilSystem.advance(level,level.getChunkAt(pos),16000);
            helper.assertTrue(level.getBlockState(pos).getValue(CropBlock.AGE) == 7,
                    "Eligible wheat matures through active soil production; loaded=" + level.isAreaLoaded(pos, 1)
                            + ", light=" + level.getRawBrightness(pos, 0));
            var sky = pos.atY(200);
            level.setBlockAndUpdate(sky.below(), Blocks.FARMLAND.defaultBlockState());
            level.setBlockAndUpdate(sky, Blocks.WHEAT.defaultBlockState());
            for (int i = 0; i < 1000; i++) level.getBlockState(sky).randomTick(level, sky, random);
            helper.assertTrue(level.getBlockState(sky).getValue(CropBlock.AGE) == 0, "River under a sky platform is insufficient");
            helper.succeed();
        });
    }
    @GameTest(template = "empty")
    public static void woodlandRateAndClearingAreStable(GameTestHelper helper) {
        var forest = region(helper, "forest");
        var plains = region(helper, "plains");
        var pos = new BlockPos(80, 64, 80);
        forest.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        helper.assertTrue(Geography.woodland(forest, pos), "Clearing keeps geographic suitability");
        var sapling = Blocks.OAK_SAPLING.defaultBlockState();
        int forestTicks = 0, plainTicks = 0;
        var random = RandomSource.create(104);
        for (int i = 0; i < 4000; i++) {
            if (Geography.allowRandomTick(sapling, forest, pos, random)) forestTicks++;
            if (Geography.allowRandomTick(sapling, plains, pos, random)) plainTicks++;
        }
        helper.assertTrue(forestTicks == 4000 && plainTicks > 850 && plainTicks < 1150, "Woodland retains vanilla rate, ordinary ground roughly one quarter");
        helper.assertTrue(Blocks.MANGROVE_PROPAGULE.defaultBlockState().is(Geography.TREES), "Mangroves share forestry policy");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void remoteQueriesDoNotLoadChunksAndNetherCannotFarm(GameTestHelper helper) {
        var level = region(helper, "river");
        var remote = new BlockPos(1000000, 64, -1000000);
        int before = level.getChunkSource().getLoadedChunksCount();
        Geography.inspect(level, remote);
        helper.assertTrue(!level.hasChunkAt(remote), "Query cannot force-load its target");
        helper.assertTrue(before == level.getChunkSource().getLoadedChunksCount(), "Query cannot load neighboring chunks");
        helper.assertTrue(!Geography.canFarm(level.getServer().getLevel(net.minecraft.world.level.Level.NETHER), remote), "Nether is ineligible");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void hoeInspectionConsumesNoLaborAndDoesNotTill(GameTestHelper helper) {
        var level = region(helper, "river");
        var pos = new BlockPos(96, 63, 96);
        level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "ground-inspector"));
        player.setGameMode(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, Items.IRON_HOE.getDefaultInstance());
        double calories = CalorieFoodData.of(player).reserve().calories();
        var event = new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(
                player, InteractionHand.MAIN_HAND, pos, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        helper.assertTrue(event.isCanceled(), "Inspection must consume the interaction before tilling");
        helper.assertTrue(level.getBlockState(pos).is(Blocks.DIRT), "Inspection leaves ground unchanged");
        helper.assertTrue(player.getMainHandItem().getDamageValue() == 0, "Inspection does not damage hoe");
        helper.assertTrue(calories == CalorieFoodData.of(player).reserve().calories(), "Inspection costs no calories");
        helper.succeed();
    }
}
