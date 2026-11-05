package dev.civilization;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class ManufacturingGameTests {
    static net.neoforged.neoforge.common.util.FakePlayer operator(WorkshopBlockEntity w){
        var p=new net.neoforged.neoforge.common.util.FakePlayer((net.minecraft.server.level.ServerLevel)w.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"Assembler"));
        p.setGameMode(GameType.SURVIVAL);p.setPos(w.getBlockPos().getX()+.5,w.getBlockPos().getY(),w.getBlockPos().getZ()-1);
        CalorieFoodData.of(p).reserve().set(2400);p.containerMenu=new WorkshopMenu(1,p.getInventory(),w,w.data);return p;
    }
    static void supply(WorkshopBlockEntity w,WorkshopJobs.Job job){
        w.clearContent();w.select(WorkshopJobs.index(w.kind,job.id()));
        for(int i=0;i<4;i++)w.setItem(i,job.needs().get(i).icon());
    }
    static WorkshopJobs.Job job(int kind,Item item){return WorkshopJobs.jobs(kind).stream().filter(j->j.output().is(item)).findFirst().orElseThrow();}
    @GameTest(template="industrial") public static void everyAssemblyBillConsumesExactlyOnce(GameTestHelper h){
        var w=WorkshopGameTests.build(h,3,Direction.NORTH);var p=operator(w);
        for(var job:WorkshopJobs.jobs(3)){
            if(job.output().isEmpty())continue;
            supply(w,job);
            if(job.output().getItem().toString().contains("netherite_"))EquipmentGrade.apply(w.getItem(0),1000,71);
            h.assertTrue(w.beginWork(p),"Job can start: "+job.id());
            for(int ticks=0;ticks<job.ticks();ticks+=10)w.process();
            h.assertTrue(w.getItem(5).is(job.output().getItem())&&w.getItem(5).getCount()==job.output().getCount(),"Actual output: "+job.id());
            for(int i=0;i<4;i++)h.assertTrue(w.getItem(i).isEmpty(),"Exact consumption: "+job.id());
            int output=w.getItem(5).getCount();w.process();h.assertTrue(w.getItem(5).getCount()==output,"No automatic second batch");
            h.assertTrue(w.fire.heat()==0&&w.getItem(4).isEmpty()&&w.getItem(6).isEmpty(),"Hand work has no fuel");
            CalorieFoodData.of(p).reserve().set(2400);
        }
        h.succeed();
    }
    @GameTest(template="industrial") public static void forgingCatalogHasNoFinishedEquipment(GameTestHelper h){
        var w=WorkshopGameTests.build(h,2,Direction.NORTH);
        for(var job:WorkshopJobs.jobs(2)){
            h.assertTrue(!WorkshopJobs.gated(job.output()),"Smithy only makes stock: "+job.id());
            supply(w,job);w.setItem(4,KilnContent.MINERAL_COAL.toStack(16));
            WorkshopGameTests.run(w,job.ticks());
            h.assertTrue(w.getItem(5).is(job.output().getItem())&&w.getItem(5).getCount()==job.output().getCount(),"Forged output: "+job.id());
            for(int i=0;i<4;i++)h.assertTrue(w.getItem(i).isEmpty(),"Forging paid inputs");
        }
        h.succeed();
    }
    @GameTest(template="industrial") public static void benchRequiresPresentAuthorizedWorkerAndCalories(GameTestHelper h){
        var w=WorkshopGameTests.build(h,3,Direction.NORTH);var job=job(3,ManufacturingContent.HANDLE.get());supply(w,job);var p=operator(w);
        w.process();h.assertTrue(w.progress==0,"Stock and hoppers alone cannot work");
        h.assertTrue(w.beginWork(p),"Present worker starts");w.process();double paid=2400-CalorieFoodData.of(p).reserve().calories();
        h.assertTrue(Math.abs(paid-2.5*ThermalRules.calorieFactor(ThermalSystem.comfort(p)))<1e-6&&w.progress==10,"Performed half-second costs 2.5 kcal before comfort");
        p.setPos(w.getBlockPos().getX()+20,w.getBlockPos().getY(),w.getBlockPos().getZ());w.process();
        h.assertTrue(w.progress==10&&w.getItem(5).isEmpty()&&Math.abs(CalorieFoodData.of(p).reserve().calories()-(2400-paid))<1e-6,"Distance stops work without charging calories or losing physical inputs");
        p.setPos(w.getBlockPos().getX()+.5,w.getBlockPos().getY(),w.getBlockPos().getZ()-1);CalorieFoodData.of(p).reserve().set(0);
        w.beginWork(p);w.process();h.assertTrue(w.status==MachineStatus.Workshop.NEEDS_FOOD&&w.progress==10,"Depleted labor cannot finish");
        CalorieFoodData.of(p).reserve().set(2400);w.beginWork(p);p.containerMenu=p.inventoryMenu;w.process();
        h.assertTrue(w.progress==10,"Closing the menu stops work");
        h.assertTrue(!w.canPlaceItem(4,KilnContent.MINERAL_COAL.toStack())&&w.getSlotsForFace(Direction.EAST).length==0,"Bench rejects fuel including hopper access");
        h.succeed();
    }
    @GameTest(template="industrial") public static void benchReloadFullOutputAndInputChangesPreserveStock(GameTestHelper h){
        var w=WorkshopGameTests.build(h,3,Direction.NORTH);var job=job(3,Items.IRON_PICKAXE);supply(w,job);var p=operator(w);w.beginWork(p);w.process();
        var copy=new WorkshopBlockEntity(w.getBlockPos(),w.getBlockState());copy.setLevel(h.getLevel());copy.loadWithComponents(w.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(copy.progress==10&&copy.getItem(0).is(w.getItem(0).getItem())&&copy.selected==w.selected,"Work and actual stock persist");
        copy.process();h.assertTrue(copy.progress==10,"Reload does not restore a worker session");
        w.setItem(5,new ItemStack(Items.STONE));w.process();h.assertTrue(w.getItem(0).getCount()==1&&w.getItem(1).getCount()==1,"Blocked output does not spend stock");
        w.setItem(5,ItemStack.EMPTY);w.removeItem(0,1);h.assertTrue(w.progress==0,"Removing an input resets its work");
        h.succeed();
    }
    @GameTest(template="industrial") public static void bundledHandleAndRepairCannotMintGradeOrLeather(GameTestHelper h){
        var handle=job(3,ManufacturingContent.HANDLE.get());h.assertTrue(handle.needs().get(1).ingredient().test(new ItemStack(Items.LEATHER))&&handle.output().getCount()==1,"One leather makes one complete handle");
        var door=job(3,Items.OAK_DOOR);var trapdoor=job(3,Items.OAK_TRAPDOOR);var fasteners=new ItemStack(ManufacturingContent.FASTENERS.get());h.assertTrue(door.output().getCount()==3&&door.needs().stream().noneMatch(n->n.ingredient().test(fasteners))&&trapdoor.needs().stream().noneMatch(n->n.ingredient().test(fasteners)),"Wooden doors and trapdoors need no iron fasteners");
        for(var item:List.of(Items.IRON_PICKAXE,Items.IRON_AXE,Items.IRON_SHOVEL)){
            var tool=job(3,item);h.assertTrue(tool.needs().stream().noneMatch(n->n.count()>0&&n.ingredient().test(new ItemStack(Items.LEATHER))),"Final tool does not bill leather again");
        }
        var w=WorkshopGameTests.build(h,3,Direction.NORTH);var p=operator(w);var damaged=new ItemStack(Items.IRON_PICKAXE);EquipmentGrade.apply(damaged,800,123);damaged.setDamageValue(100);damaged.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Kept"));
        var repair=WorkshopJobs.repair(damaged);w.select(0);w.setItem(0,new ItemStack(Items.IRON_PICKAXE));w.setItem(3,damaged);w.setItem(1,repair.needs().get(1).icon());w.setItem(2,repair.needs().get(2).icon());
        h.assertTrue(w.beginWork(p),"Repair matches the actual damaged target despite a pristine same-item stack first");
        for(int i=0;i<repair.ticks();i+=10)w.process();
        var out=w.getItem(5);h.assertTrue(out.getDamageValue()==0&&Grade.value(out)<800&&EquipmentGrade.wearSeed(out)==123&&out.getHoverName().getString().equals("Kept"),"Repair preserves identity and lowers grade");
        h.assertTrue(w.getItem(0).is(Items.IRON_PICKAXE)&&w.getItem(3).isEmpty(),"Only the damaged item was consumed");h.succeed();
    }
    @GameTest(template="industrial") public static void tableBypassesRetiredAndBootstrapRemains(GameTestHelper h){
        var manager=h.getLevel().getRecipeManager();
        for(String id:List.of("civilization:machine_parts","civilization:empty_canister","civilization:oil_pump","civilization:engine_cylinder","civilization:coal_minecart","minecraft:oak_door","minecraft:minecart","minecraft:rail"))
            h.assertTrue(manager.byKey(net.minecraft.resources.ResourceLocation.parse(id)).isEmpty(),"Old direct crafting disabled: "+id);
        for(String id:List.of("civilization:assembly_bench","civilization:foundry","civilization:smithy","civilization:tannery","minecraft:chest"))
            h.assertTrue(manager.byKey(net.minecraft.resources.ResourceLocation.parse(id)).isPresent(),"Bootstrap available: "+id);
        var ids=new HashSet<String>();for(var job:WorkshopJobs.jobs(3))h.assertTrue(ids.add(job.id()),"Unique persisted output identity: "+job.id());h.succeed();
    }
    @GameTest(template="industrial") public static void benchRevocationAndCompetingWorkersCannotFinish(GameTestHelper h){
        var w=WorkshopGameTests.build(h,3,Direction.NORTH);supply(w,job(3,ManufacturingContent.HANDLE.get()));var p=operator(w);var other=operator(w);
        var data=CivicData.get(h.getLevel().getServer());var c=new CivicData.Claim();c.at=new CivicData.Address(h.getLevel().dimension().location().toString(),w.getBlockPos());c.owner=new CivicData.Owner(UUID.randomUUID(),false);c.radius=2;c.height=32;c.upkeep=1;c.coalUnit=3600000;c.energy=360000000;c.lastUpdate=System.currentTimeMillis();c.whitelist.put(p.getUUID(),"assembler");data.claims.put(c.at,c);data.rebuildIndex();
        try{
            c.whitelist.put(other.getUUID(),"second assembler");
            h.assertTrue(w.beginWork(p)&&!w.beginWork(other),"Only the admitted present worker can start");w.process();int work=w.progress;
            c.whitelist.clear();w.process();h.assertTrue(w.progress==work&&w.getItem(5).isEmpty()&&!w.getItem(0).isEmpty(),"Access is rechecked during active work");
        }finally{data.claims.remove(c.at);data.rebuildIndex();}h.succeed();
    }
    @GameTest(template="industrial") public static void oldSmithyJobCancelsWithoutChangingPhysicalStock(GameTestHelper h){
        var w=WorkshopGameTests.build(h,2,Direction.NORTH);w.setItem(0,new ItemStack(Items.IRON_INGOT,3));w.setItem(1,new ItemStack(Items.LEATHER));w.setItem(2,new ItemStack(Items.STICK,2));w.setItem(5,new ItemStack(Items.IRON_PICKAXE));
        var tag=w.saveWithFullMetadata(h.getLevel().registryAccess());tag.remove("partsCatalog");tag.putString("jobId","minecraft:iron_pickaxe");tag.putInt("progress",590);
        var copy=new WorkshopBlockEntity(w.getBlockPos(),w.getBlockState());copy.setLevel(h.getLevel());copy.loadWithComponents(tag,h.getLevel().registryAccess());
        h.assertTrue(copy.selected==-1&&copy.progress==0&&copy.getItem(0).getCount()==3&&copy.getItem(1).is(Items.LEATHER)&&copy.getItem(5).is(Items.IRON_PICKAXE),"Legacy work resets, paid stock and existing output remain");h.succeed();
    }
    @GameTest(template="industrial") public static void eachForgedPartHasAnActualCustomer(GameTestHelper h){
        for(var forged:WorkshopJobs.jobs(2))h.assertTrue(WorkshopJobs.jobs(3).stream().anyMatch(j->j.needs().stream().anyMatch(n->n.count()>0&&n.ingredient().test(forged.output()))),"Forged part earns a stock slot: "+forged.id());h.succeed();
    }
    @GameTest(template="industrial") public static void legacyRepairInputReceivesStableIdentityBeforeWork(GameTestHelper h){
        var w=WorkshopGameTests.build(h,3,Direction.NORTH);var gear=new ItemStack(Items.IRON_PICKAXE);EquipmentGrade.apply(gear,800,123);gear.setDamageValue(100);w.setItem(0,gear);
        gear.remove(EquipmentGrade.WEAR_SEED.get());var quote=WorkshopJobs.repair(gear);for(int i=1;i<4;i++)w.setItem(i,quote.needs().get(i).icon());w.select(0);var p=operator(w);h.assertTrue(w.beginWork(p),"Legacy gear accepts repair work");
        for(int i=0;i<quote.ticks();i+=10)w.process();
        h.assertTrue(w.getItem(5).is(Items.IRON_PICKAXE)&&w.getItem(5).getDamageValue()==0&&w.getItem(5).has(EquipmentGrade.WEAR_SEED.get()),"Repair completes instead of rerolling a missing identity each work step");h.succeed();
    }
}
