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
    @GameTest(template="industrial")
    public static void nativeSlabAndCutItemUseChargeByVolume(GameTestHelper h) {
        var origin=h.absolutePos(new net.minecraft.core.BlockPos(4,2,4));
        var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"SlabCalories"));
        player.setGameMode(GameType.SURVIVAL);player.setPos(origin.getX()+3,origin.getY(),origin.getZ()+3);
        var data=CalorieFoodData.of(player);data.reserve().set(1000);
        for(int i=0;i<3;i++) {
            var pos=origin.east(i);
            h.getLevel().setBlockAndUpdate(pos.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,i==0
                    ? new net.minecraft.world.item.ItemStack(Items.BIRCH_SLAB)
                    : CuttingContent.stack(net.minecraft.world.level.block.Blocks.BRICKS.defaultBlockState(),i==1?1:3,1));
            var hit=new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5),
                    net.minecraft.core.Direction.UP,pos.below(),false);
            h.assertTrue(player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player,
                    net.minecraft.world.InteractionHand.MAIN_HAND,hit)).consumesAction(),"Real item-use path places slab/beam/cube");
        }
        var denied=origin.south(2);
        h.getLevel().setBlockAndUpdate(denied.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(Items.BIRCH_SLAB));
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent> cancel=event->{
            if(event.getPos().equals(denied))event.setCanceled(true);
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
        try {
            var hit=new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(denied.getX()+0.5,denied.getY(),denied.getZ()+0.5),
                    net.minecraft.core.Direction.UP,denied.below(),false);
            h.assertTrue(!player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player,
                    net.minecraft.world.InteractionHand.MAIN_HAND,hit)).consumesAction(),"Claim/protection cancellation rejects native slab");
        } finally {net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel);}
        h.assertTrue(h.getLevel().getBlockState(denied).isAir() && player.getMainHandItem().getCount()==1,
                "Cancelled slab restores world and item");
        h.runAfterDelay(2,()->{
            h.assertTrue(Math.abs(data.reserve().calories()-998.25)<.00001 && data.placed==3,
                    "Native slab, quarter and eighth cost 1 + 0.5 + 0.25 kcal; cancelled placement costs zero");
            h.succeed();
        });
    }
    @GameTest(template="industrial")
    public static void cutPlacementChargesVolumeEvenWhenPiecesJoin(GameTestHelper h) {
        var pos=h.absolutePos(new net.minecraft.core.BlockPos(4,2,4));
        var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"CutCalories"));
        player.setGameMode(GameType.SURVIVAL);player.setPos(pos.getX()+3,pos.getY(),pos.getZ()+3);
        var data=CalorieFoodData.of(player);data.reserve().set(1000);
        h.getLevel().setBlockAndUpdate(pos.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                CuttingContent.stack(net.minecraft.world.level.block.Blocks.BRICKS.defaultBlockState(),3,8));
        for(int cell:new int[]{0,1,4,2,7,5,6,3}) {
            var box=CutCells.box(cell);
            var hit=new net.minecraft.world.phys.BlockHitResult(
                    new net.minecraft.world.phys.Vec3(pos.getX()+box.minX+.25,pos.getY()+box.minY,pos.getZ()+box.minZ+.25),
                    net.minecraft.core.Direction.UP,box.minY==0?pos.below():pos,false);
            var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
            h.assertTrue(net.neoforged.neoforge.common.CommonHooks.onPlaceItemIntoWorld(context).consumesAction(),"Cut cube placement accepted");
        }
        h.assertTrue(h.getLevel().getBlockState(pos).is(net.minecraft.world.level.block.Blocks.BRICKS),"Eight cubes join into a full block");
        for(int units:new int[]{2,1}) {
            var target=pos.offset(units,0,2);
            h.getLevel().setBlockAndUpdate(target.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                    CuttingContent.stack(net.minecraft.world.level.block.Blocks.BRICKS.defaultBlockState(),units,1));
            var hit=new net.minecraft.world.phys.BlockHitResult(
                    new net.minecraft.world.phys.Vec3(target.getX()+.5,target.getY(),target.getZ()+.5),
                    net.minecraft.core.Direction.UP,target.below(),false);
            h.assertTrue(net.neoforged.neoforge.common.CommonHooks.onPlaceItemIntoWorld(
                    new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit)).consumesAction(),
                    "Slab or beam placement accepted");
        }
        h.runAfterDelay(2,()->{
            h.assertTrue(Math.abs(data.reserve().calories()-996.5)<.00001,
                    "Eight eighths, one half and one quarter cost 2 + 1 + 0.5 kcal; actual="+data.reserve().calories()+", placed="+data.placed+", factor="+ThermalRules.calorieFactor(ThermalSystem.comfort(player)));
            h.assertTrue(data.placed==10,"Every accepted cut piece counts as one placement");
            h.assertTrue(Math.abs(LaborCosts.sleeping(1000)-50)<.00001&&Math.abs(LaborCosts.sleeping(11000)-550)<.00001,
                    "Sleep costs 50 kcal per in-game hour, including skipped hours");
            h.succeed();
        });
    }
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
        for (int i = 0; i < 10; i++) player.checkMovementStatistics(10, 0, 0);
        helper.assertTrue(Math.abs(data.reserve().calories() - 1960) < 0.00001, "Sprinting 100 blocks costs 30 kcal regardless of movement step size");
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
    public static void coldExposureUsesTheCalorieReserve(GameTestHelper helper) {
        var player = new net.neoforged.neoforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "cold-test"));
        player.setGameMode(GameType.SURVIVAL);
        var data = CalorieFoodData.of(player);
        data.reserve().set(1000);
        ThermalSystem.chargeCold(player,10); // 50°F air: one quarter of the maximum rate.
        helper.assertTrue(Math.abs(data.reserve().calories()-999.95)<1e-8,"One cold second spends 0.05 kcal at 50°F");
        ThermalSystem.chargeCold(player,18);
        helper.assertTrue(Math.abs(data.reserve().calories()-999.95)<1e-8,"Comfortable air has no passive cost");
        player.setGameMode(GameType.CREATIVE);
        ThermalSystem.chargeCold(player,2);
        helper.assertTrue(Math.abs(data.reserve().calories()-999.95)<1e-8,"Creative is exempt");
        player.setGameMode(GameType.SURVIVAL);
        data.reserve().set(.02);
        ThermalSystem.chargeCold(player,2);
        ThermalSystem.chargeCold(player,2);
        helper.assertTrue(data.reserve().calories()==0&&data.isDepleted(),"Cold cannot create calorie debt after depletion");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "civilization")
    public static void rowingChargesOnlyThePaddlingPilot(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var data = CalorieFoodData.of(player);
        data.reserve().set(1000);
        var boat = new net.minecraft.world.entity.vehicle.Boat(level, 2, 2, 2);
        helper.assertTrue(player.startRiding(boat), "Pilot boards the boat");
        data.row(player); // Boarding anchors the boat position without charging travel.
        boat.setPaddleState(true, true);
        boat.setPos(12, 2, 2);
        data.row(player);
        helper.assertTrue(Math.abs(data.reserve().calories() - 999.5) < 0.00001 && Math.abs(data.rowDistance - 10) < 0.00001,
                "Ten paddled blocks cost 0.5 kcal, below sprinting's 3 kcal");
        boat.setPaddleState(false, false);
        boat.setPos(22, 2, 2);
        data.row(player);
        helper.assertTrue(Math.abs(data.reserve().calories() - 999.5) < 0.00001, "Drifting spends nothing");
        player.stopRiding();
        data.row(player);
        helper.assertTrue(Math.abs(data.reserve().calories() - 999.5) < 0.00001, "Dismounting spends nothing");
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

