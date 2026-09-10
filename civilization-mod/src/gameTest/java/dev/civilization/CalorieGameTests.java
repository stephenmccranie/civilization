package dev.civilization;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("civilization")
@PrefixGameTestTemplate(false)
public class CalorieGameTests {
    @GameTest(template = "empty", templateNamespace = "civilization")
    public static void auditFoodClampingAndActions(GameTestHelper helper) {
        var player = new net.neoforged.neoforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "audit-test"));
        player.setGameMode(GameType.SURVIVAL);
        var data = CalorieFoodData.of(player);
        data.reserve().set(2300);
        EnergyLog.marker(player, "login");
        var bread = Items.BREAD.getDefaultInstance();
        player.eat(helper.getLevel(), bread, bread.getFoodProperties(player));
        player.jumpFromGround();
        player.checkMovementStatistics(1, 0, 0);
        EnergyLog.marker(player, "logout");
        helper.assertTrue(Math.abs(data.reserve().calories() - 2397.9) < 0.00001,
                "Audit hooks must not change energy accounting");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "civilization")
    public static void realServerMovementHook(GameTestHelper helper) {
        var player = new net.neoforged.neoforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "calorie-test"));
        player.setGameMode(GameType.SURVIVAL);
        var data = CalorieFoodData.of(player);
        data.reserve().set(2000);
        player.setSprinting(false);
        for (int i = 0; i < 100; i++) player.checkMovementStatistics(1, 0, 0);
        helper.assertTrue(Math.abs(data.reserve().calories() - 1990) < 0.00001, "Walking 100 blocks must cost 10 kcal");
        player.setSprinting(true);
        for (int i = 0; i < 100; i++) player.checkMovementStatistics(1, 0, 0);
        helper.assertTrue(Math.abs(data.reserve().calories() - 1960) < 0.00001, "Sprinting 100 blocks must cost 30 kcal");
        player.setGameMode(GameType.CREATIVE);
        player.checkMovementStatistics(1, 0, 0);
        helper.assertTrue(Math.abs(data.reserve().calories() - 1960) < 0.00001, "Creative movement must not consume calories");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "civilization")
    public static void deathClonePreservesReserve(GameTestHelper helper) {
        var original = helper.makeMockPlayer(GameType.SURVIVAL);
        var replacement = helper.makeMockPlayer(GameType.SURVIVAL);
        CalorieFoodData.of(original).reserve().set(123.456);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                new net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone(replacement, original, true));
        helper.assertTrue(CalorieFoodData.of(replacement).reserve().calories() == 123.456, "Death must not mint calories");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "civilization")
    public static void cakeSliceAndJump(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var data = CalorieFoodData.of(player);
        data.reserve().set(1000);
        helper.setBlock(1, 1, 1, net.minecraft.world.level.block.Blocks.CAKE);
        helper.useBlock(new net.minecraft.core.BlockPos(1, 1, 1), player);
        helper.assertTrue(data.reserve().calories() == 1200, "One cake slice must add 200 kcal");
        player.jumpFromGround();
        helper.assertTrue(data.reserve().calories() == 1198, "Jump must spend 2 kcal");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "civilization")
    public static void hungerEffectAndCounterReset(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var data = CalorieFoodData.of(player);
        data.reserve().set(1000);
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.HUNGER, 100, 0));
        for (int i = 0; i < 20; i++) data.tick(player);
        helper.assertTrue(Math.abs(data.reserve().calories() - 998) < 0.00001, "Hunger I must spend 2 kcal per second");
        data.resetCounters();
        helper.assertTrue(Math.abs(data.reserve().calories() - 998) < 0.00001, "Counter reset must not refill energy");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "civilization")
    public static void foodReplacementAndPersistence(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(player.getFoodData() instanceof CalorieFoodData, "Player constructor must install calorie food data");
        var data = CalorieFoodData.of(player);
        data.reserve().set(1000);
        data.addExhaustion(40);
        data.setSaturation(20);
        data.setFoodLevel(20);
        data.eat(20, 20);
        helper.assertTrue(data.reserve().calories() == 1000, "Vanilla food/saturation mutations must not change calories");
        var bread = Items.BREAD.getDefaultInstance();
        player.eat(helper.getLevel(), bread, bread.getFoodProperties(player));
        helper.assertTrue(data.reserve().calories() == 1500, "Eating bread must add exactly 500 kcal once");
        helper.assertTrue(data.getSaturationLevel() == 0 && data.getExhaustionLevel() == 0, "No hidden stores");
        var saved = new CompoundTag();
        data.addAdditionalSaveData(saved);
        var replacement = helper.makeMockPlayer(GameType.SURVIVAL);
        replacement.getFoodData().readAdditionalSaveData(saved);
        helper.assertTrue(CalorieFoodData.of(replacement).reserve().calories() == 1500, "Calories must survive save/load");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "civilization")
    public static void regenerationSpendsCalories(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var data = CalorieFoodData.of(player);
        data.reserve().set(1000);
        player.setHealth(10);
        for (int i = 0; i < 80; i++) data.tick(player);
        helper.assertTrue(player.getHealth() == 11, "Natural healing must restore one health point");
        helper.assertTrue(data.reserve().calories() == 960, "Healing must cost 40 kcal");
        player.setHealth(10);
        data.reserve().set(39);
        for (int i = 0; i < 80; i++) data.tick(player);
        helper.assertTrue(player.getHealth() == 10, "Insufficient calories must prevent natural healing");
        helper.succeed();
    }
}

