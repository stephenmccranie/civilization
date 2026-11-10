package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class CoalFireGameTests {
    @GameTest(template="industrial") public static void coalEnergyReadsInJoules(GameTestHelper h){
        h.assertTrue(Energy.text(Energy.COAL).equals("250 MJ")&&Energy.text(16*Energy.COAL).equals("4.00 GJ")&&Energy.text(.62*Energy.COAL).equals("155 MJ")&&Energy.text(Energy.COAL/400).equals("625 kJ"),"A Coal is 250 MJ, a dense block's 16 are 4 GJ, and a work unit is 625 kJ");
        h.succeed();
    }
    @GameTest(template="industrial") public static void acceptedIgnitionStrikesCostCalories(GameTestHelper h){
        var w=WorkshopGameTests.build(h,2,Direction.NORTH);
        var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"ignition-labor"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(w.getBlockPos().getX()+.5,w.getBlockPos().getY()+1,w.getBlockPos().getZ()+.5);
        var data=CalorieFoodData.of(player);data.reserve().set(1000);
        var menu=new WorkshopMenu(0,player.getInventory(),w,w.data);
        h.assertTrue(!menu.clickMenuButton(player,CoalFire.BUTTON)&&data.reserve().calories()==1000,
                "A strike without coal is rejected and free");
        w.setItem(4,KilnContent.MINERAL_COAL.toStack());
        MachineFixture.spark(h.getLevel(),false);
        h.assertTrue(menu.clickMenuButton(player,CoalFire.BUTTON)&&!w.fire.lit(),"An accepted spark can fail");
        double first=data.reserve().calories();
        h.assertTrue(first<1000&&Math.abs(first-(1000-CalorieConfig.IGNITE.get()*ThermalRules.calorieFactor(ThermalSystem.comfort(player))))<1e-8,
                "An accepted failed spark costs exactly one comfort-adjusted strike");
        h.assertTrue(!menu.clickMenuButton(player,CoalFire.BUTTON)&&data.reserve().calories()==first,
                "Cooldown rejects a click without charging twice");
        h.runAfterDelay(21,()->{
            MachineFixture.spark(h.getLevel(),true);
            double secondCost=CalorieConfig.IGNITE.get()*ThermalRules.calorieFactor(ThermalSystem.comfort(player));
            h.assertTrue(menu.clickMenuButton(player,CoalFire.BUTTON)&&w.fire.lit(),"Later accepted spark lights the fire: valid="+menu.stillValid(player)+", structure="+MachineFixture.status(h.getLevel(),w.getBlockPos())+", wet="+w.fire.rainedOut()+", fuel="+w.getItem(4).getCount()+", state="+w.fire.state());
            h.assertTrue(Math.abs(data.reserve().calories()-(first-secondCost))<1e-8,
                    "Successful ignition costs one further strike");
            h.succeed();
        });
    }
    @GameTest(template="industrial") public static void aFireWarmsTheMasonryBeforeItWorks(GameTestHelper h){
        var w=WorkshopGameTests.build(h,2,Direction.NORTH);w.setItem(4,KilnContent.MINERAL_COAL.toStack(32));
        w.process();h.assertTrue(!w.fire.lit()&&w.fire.heat()==0&&w.getItem(4).getCount()==32,"Cold fuel does not self-ignite");
        MachineFixture.spark(h.getLevel(),false);h.assertTrue(w.fire.strike(true)&&!w.fire.lit(),"A valid spark may fail without consuming fuel");
        MachineFixture.spark(h.getLevel(),true);h.assertTrue(!w.fire.strike(true)&&w.getItem(4).getCount()==32,"Repeated packet rejected by server cooldown");
        var save=new CompoundTag();w.fire.save(save);var copy=new CoalFire(w);copy.load(save);
        h.assertTrue(!copy.strike(true),"Cooldown survives reload");
        h.runAfterDelay(21,()->{
            MachineFixture.spark(h.getLevel(),true);
            h.assertTrue(w.fire.strike(true)&&w.fire.lit()&&!w.fire.hot()&&!w.fire.draw(1),"A newly lit fire is cold and cannot work yet");
            double capacity=w.fire.capacity();
            h.assertTrue(Math.abs(capacity-24*.7*Energy.COAL)<1,"The Smithy's twenty-four masonry blocks store 16.8 Coal of heat");
            int ticks=0;while(!w.fire.hot()&&ticks<4000){w.process();ticks+=10;}
            double warming=CoalFire.WORKING*CoalFire.WARMING_TICKS;
            h.assertTrue(w.fire.hot()&&ticks>=warming&&ticks<warming*1.05,"The fire reaches working heat in about a hundred seconds, a little slower for the leak: "+ticks+" ticks");
            h.assertTrue(32-w.getItem(4).getCount()==(int)Math.ceil(CoalFireFixture.stored(w.fire)/Energy.COAL),"Warming burns whole Coal into the masonry");
            h.assertTrue(w.fire.draw(1),"A hot fire supplies work");
            var tag=w.saveWithFullMetadata(h.getLevel().registryAccess());var restored=new WorkshopBlockEntity(w.getBlockPos(),w.getBlockState());restored.setLevel(h.getLevel());restored.loadWithComponents(tag,h.getLevel().registryAccess());
            h.assertTrue(restored.fire.lit()&&CoalFireFixture.stored(restored.fire)==CoalFireFixture.stored(w.fire)&&CoalFireFixture.reserve(restored.fire)==CoalFireFixture.reserve(w.fire),"The fire, its reserve and the stored heat persist");
            h.succeed();
        });
    }
    @GameTest(template="industrial") public static void everyJouleBurnedIsStoredOrReleased(GameTestHelper h){
        var l=h.getLevel();var field=ThermalField.get(l);var w=WorkshopGameTests.build(h,2,Direction.NORTH);w.setItem(4,KilnContent.MINERAL_COAL.toStack(32));
        long key=w.getBlockPos().asLong();field.pending.remove(key);
        MachineFixture.spark(l,true);h.assertTrue(w.fire.strike(true),"The Smithy lights");
        for(int i=0;i<300;i++)w.process();
        for(int i=0;i<50;i++)w.fire.draw(4);
        for(int i=0;i<100;i++)w.process();
        double burned=(32-w.getItem(4).getCount())*Energy.COAL-CoalFireFixture.reserve(w.fire);
        double released=field.pending.get(key)/(ThermalRules.COAL_WASTE_HEAT/Energy.COAL);
        h.assertTrue(burned>0&&Math.abs(burned-CoalFireFixture.stored(w.fire)-released)<1e3,"Burned "+burned+" J = stored "+CoalFireFixture.stored(w.fire)+" + released "+released);
        h.succeed();
    }
    @GameTest(template="industrial") public static void anUnfedHotMachineStaysHotForAWhile(GameTestHelper h){
        var w=WorkshopGameTests.build(h,0,Direction.NORTH);CoalFireFixture.set(w.fire,false,w.fire.capacity());
        w.fire.tick(1);int minutes=w.fire.minutesHot();
        h.assertTrue(minutes>=13&&minutes<=22,"Unfed, a full store works for about twenty minutes in ordinary air: "+minutes);
        for(int i=0;i<(minutes-1)*60;i++)w.fire.tick(20);
        h.assertTrue(w.fire.hot(),"Still at working heat a minute before its estimate");
        for(int i=0;i<3*60;i++)w.fire.tick(20);
        h.assertTrue(!w.fire.hot()&&w.fire.heat()>700,"It then falls below working heat while still warm");
        h.succeed();
    }
    @GameTest(template="industrial") public static void aBurntOutFireLeavesItsMachineHot(GameTestHelper h){
        var p=h.absolutePos(new BlockPos(4,1,4));var l=h.getLevel();l.setBlockAndUpdate(p,CookingContent.STATION.get().defaultBlockState());var k=(KilnBlockEntity)l.getBlockEntity(p);k.setItem(1,KilnContent.MINERAL_COAL.toStack());
        for(int i=0;i<50;i++)KilnBlockEntity.tick(l,p,k.getBlockState(),k);
        h.assertTrue(k.fire.heat()==0&&k.getItem(1).getCount()==1,"Stove waits for manual ignition even with coal");
        MachineFixture.spark(l,true);k.fire.strike(true);
        for(int i=0;i<3000&&k.fire.lit();i++)KilnBlockEntity.tick(l,p,k.getBlockState(),k);
        h.assertTrue(!k.fire.lit()&&k.getItem(1).isEmpty()&&k.fire.hot(),"Its one Coal burns out, leaving the masonry-free stove at working heat");
        k.setItem(1,KilnContent.MINERAL_COAL.toStack());KilnBlockEntity.tick(l,p,k.getBlockState(),k);
        h.assertTrue(!k.fire.lit()&&k.getItem(1).getCount()==1,"Refilling needs a new strike");
        var w=WorkshopGameTests.build(h,2,Direction.NORTH);w.setItem(4,KilnContent.MINERAL_COAL.toStack());CoalFireFixture.light(w);l.removeBlock(w.getBlockPos().east(),false);w.process();
        h.assertTrue(!w.fire.lit()&&w.fire.heat()==0,"A broken shell loses its fire and its stored heat");h.succeed();
    }
    @GameTest(template="empty") public static void tooLittleCoalNeverReachesWorkingHeat(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(4,67,4));l.setBlockAndUpdate(p,IndustrialContent.COAL_ENGINE.get().defaultBlockState());
        var m=(IndustrialBlockEntity)l.getBlockEntity(p);MachineFixture.build((net.minecraft.server.level.ServerLevel)m.getLevel(),m.getBlockPos());
        m.setItem(0,KilnContent.MINERAL_COAL.toStack());m.process();h.assertTrue(m.fire.heat()==0&&m.getItem(0).getCount()==1,"Engine coal waits for strike");
        MachineFixture.spark(h.getLevel(),true);h.assertTrue(m.fire.strike(IndustrialStructure.bind(m))&&m.getItem(0).getCount()==1,"A strike lights without burning coal yet");
        for(int i=0;i<400;i++)m.fire.tick(10);
        h.assertTrue(!m.fire.lit()&&!m.fire.hot()&&m.getItem(0).isEmpty(),"One Coal cannot bring the Coal Engine's firebox to working heat");
        m.setItem(2,KilnContent.MINERAL_COAL.toStack());m.process();h.assertTrue(!m.fire.lit()&&m.getItem(2).getCount()==1,"Backup coal cannot relight an extinguished engine");h.succeed();
    }
}
