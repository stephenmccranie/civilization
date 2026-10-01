package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class CoalFireGameTests {
    private static void roll(GameTestHelper h,boolean success){
        for(long seed=0;;seed++){var r=net.minecraft.util.RandomSource.create(seed);r.nextFloat();if((r.nextInt(3)==0)==success){h.getLevel().random.setSeed(seed);return;}}
    }
    @GameTest(template="industrial") public static void acceptedIgnitionStrikesCostCalories(GameTestHelper h){
        var w=WorkshopGameTests.build(h,0,Direction.NORTH);
        var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"ignition-labor"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(w.getBlockPos().getX()+.5,w.getBlockPos().getY()+1,w.getBlockPos().getZ()+.5);
        var data=CalorieFoodData.of(player);data.reserve().set(1000);
        var menu=new WorkshopMenu(0,player.getInventory(),w,w.data);
        h.assertTrue(!menu.clickMenuButton(player,CoalFire.BUTTON)&&data.reserve().calories()==1000,
                "A strike without coal is rejected and free");
        w.setItem(4,KilnContent.MINERAL_COAL.toStack());
        roll(h,false);
        h.assertTrue(menu.clickMenuButton(player,CoalFire.BUTTON)&&!w.fire.lit(),"An accepted spark can fail");
        double first=data.reserve().calories();
        h.assertTrue(first<1000&&Math.abs(first-(1000-CalorieConfig.IGNITE.get()*ThermalRules.calorieFactor(ThermalSystem.comfort(player))))<1e-8,
                "An accepted failed spark costs exactly one comfort-adjusted strike");
        h.assertTrue(!menu.clickMenuButton(player,CoalFire.BUTTON)&&data.reserve().calories()==first,
                "Cooldown rejects a click without charging twice");
        h.runAfterDelay(21,()->{
            roll(h,true);
            double secondCost=CalorieConfig.IGNITE.get()*ThermalRules.calorieFactor(ThermalSystem.comfort(player));
            h.assertTrue(menu.clickMenuButton(player,CoalFire.BUTTON)&&w.fire.lit(),"Later accepted spark lights the fire");
            h.assertTrue(Math.abs(data.reserve().calories()-(first-secondCost))<1e-8,
                    "Successful ignition costs one further strike");
            h.succeed();
        });
    }
    @GameTest(template="industrial") public static void ignitionCooldownExhaustionAndRefill(GameTestHelper h){
        var w=WorkshopGameTests.build(h,0,Direction.NORTH);w.setItem(4,KilnContent.MINERAL_COAL.toStack());
        w.process();h.assertTrue(!w.fire.lit()&&w.getItem(4).getCount()==1,"Cold fuel does not self-ignite");
        roll(h,false);h.assertTrue(w.fire.strike(true)&&!w.fire.lit(),"A valid spark may fail without consuming fuel");
        roll(h,true);h.assertTrue(!w.fire.strike(true)&&w.getItem(4).getCount()==1,"Repeated packet rejected by server cooldown");
        var save=new CompoundTag();w.fire.save(save);var copy=new CoalFire(w);copy.load(save);
        h.assertTrue(!copy.strike(true),"Cooldown survives reload");
        h.runAfterDelay(21,()->{
            roll(h,true);h.assertTrue(w.fire.strike(true)&&w.fire.lit()&&w.getItem(4).isEmpty(),"First allowed successful strike lights one coal");
            h.assertTrue(w.fire.remaining()==1000,"Fresh coal starts with a full gauge");int start=w.heat;for(int i=0;i<10;i++)w.process();h.assertTrue(w.heat==start-10,"Idle uses ten percent thermal work rate");h.assertTrue(w.fire.remaining()<1000&&w.fire.remaining()>900,"Idle drain reduces the remaining-coal gauge");
            var tag=w.saveWithFullMetadata(h.getLevel().registryAccess());var restored=new WorkshopBlockEntity(w.getBlockPos(),w.getBlockState());restored.setLevel(h.getLevel());restored.loadWithComponents(tag,h.getLevel().registryAccess());h.assertTrue(restored.fire.lit()&&restored.heat==w.heat&&restored.fire.remaining()==w.fire.remaining(),"Lit reserve and gauge denominator persist");
            for(int i=0;i<500;i++)w.process();h.assertTrue(!w.fire.lit()&&w.heat==0&&w.fire.remaining()==0,"Exhausted fire and gauge go out");
            w.setItem(4,KilnContent.MINERAL_COAL.toStack());w.process();h.assertTrue(!w.fire.lit()&&w.getItem(4).getCount()==1,"Refilling needs a new strike");h.succeed();
        });
    }
    @GameTest(template="industrial") public static void stoveIdleAndWorkshopBroken(GameTestHelper h){
        var p=h.absolutePos(new BlockPos(4,1,4));var l=h.getLevel();l.setBlockAndUpdate(p,CookingContent.STATION.get().defaultBlockState());var k=(KilnBlockEntity)l.getBlockEntity(p);k.setItem(1,KilnContent.MINERAL_COAL.toStack());
        for(int i=0;i<50;i++)KilnBlockEntity.tick(l,p,k.getBlockState(),k);
        h.assertTrue(k.fireHeat()==0&&k.getItem(1).getCount()==1,"Stove waits for manual ignition even with coal");
        roll(h,true);k.fire.strike(true);int heat=k.fireHeat();for(int i=0;i<100;i++)KilnBlockEntity.tick(l,p,k.getBlockState(),k);
        h.assertTrue(k.fireHeat()==heat-10&&k.fire.lit(),"Stove stays lit at low idle rate");
        var w=WorkshopGameTests.build(h,2,Direction.NORTH);w.setItem(4,KilnContent.MINERAL_COAL.toStack());roll(h,true);w.fire.strike(true);l.removeBlock(w.getBlockPos().east(),false);w.process();h.assertTrue(!w.fire.lit()&&w.heat==0,"Broken shell extinguishes burning reserve");h.succeed();
    }
    @GameTest(template="empty") public static void industrialIdleIsFiniteAndNeedsRelight(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(4,67,4));l.setBlockAndUpdate(p,IndustrialContent.PUMP.get().defaultBlockState());
        var m=(IndustrialBlockEntity)l.getBlockEntity(p);DerrickFixture.assemble(m);
        m.setItem(0,KilnContent.MINERAL_COAL.toStack());m.process();h.assertTrue(m.heat==0&&m.getItem(0).getCount()==1,"Pump coal waits for strike");
        roll(h,true);h.assertTrue(m.fire.strike(IndustrialStructure.bind(m))&&m.heat==250,"Pump strike purchases one coal reserve");
        for(int i=0;i<400;i++)m.fire.finish(10,false);
        h.assertTrue(m.heat==0&&!m.fire.lit()&&m.output.isEmpty(),"Exactly one coal over 200 idle seconds, no output");
        m.setItem(2,KilnContent.MINERAL_COAL.toStack());m.process();h.assertTrue(!m.fire.lit()&&m.getItem(2).getCount()==1,"Backup coal cannot relight an extinguished pump");h.succeed();
    }
}
