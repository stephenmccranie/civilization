package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class StoveGameTests {
    private static PrototypeStoveEntity stove(GameTestHelper h){var pos=h.absolutePos(new BlockPos(4,1,4));h.getLevel().setBlockAndUpdate(pos,PrototypeStoveContent.STOVE.get().defaultBlockState());var s=(PrototypeStoveEntity)h.getLevel().getBlockEntity(pos);s.setItem(0,new ItemStack(Items.POTATO,2));s.setItem(1,new ItemStack(Items.CARROT,2));s.setItem(2,new ItemStack(Items.BREAD));return s;}
    private static void state(GameTestHelper h,PrototypeStoveEntity s,double dial,double work,int heat,boolean lit){var tag=s.saveWithoutMetadata(h.getLevel().registryAccess());tag.putDouble("dial",dial);tag.putDouble("work",work);tag.putInt("heat",heat);var f=tag.getCompound("coalFire");f.putBoolean("lit",lit);f.putInt("budget",400);tag.put("coalFire",f);s.loadWithComponents(tag,h.getLevel().registryAccess());}
    @GameTest(template="industrial") public static void qualityPlateauAndContinuousHeat(GameTestHelper h){
        h.assertTrue(StoveCooking.rate(.301)>StoveCooking.rate(.300)&&StoveCooking.rate(1)==1&&StoveCooking.rate(Double.NaN)==0,"Heat responds continuously and rejects nonfinite values");
        for(double w:new double[]{800,850,900,950,1000})h.assertTrue(StoveCooking.quality(w)==1,"Broad plateau has equal maximum quality");
        h.assertTrue(StoveCooking.quality(799)<1&&StoveCooking.quality(1001)<1&&StoveCooking.quality(1600)<StoveCooking.quality(1200),"Quality rises and declines smoothly around the plateau");h.succeed();
    }
    @GameTest(template="industrial") public static void batchFuelPersistenceAndConservation(GameTestHelper h){
        var s=stove(h);h.assertTrue(s.start()&&!s.start()&&s.getItem(0).isEmpty()&&s.getItem(1).isEmpty()&&s.getItem(2).isEmpty(),"Ingredients reserved exactly once");
        h.assertTrue(!s.finish()&&s.batch(),"Raw batch remains after early finish");state(h,s,.6,0,10,true);
        for(int i=0;i<100;i++)s.tick();h.assertTrue(s.work()+s.skillet().warmth()<=10.00001&&!s.fire.lit(),"Finite paid fuel stops progress");for(int i=0;i<2000;i++)s.tick();double work=s.work();for(int i=0;i<100;i++)s.tick();h.assertTrue(s.work()==work&&s.skillet().warmth()==0,"Stored heat finishes dissipating; truly cold food stops");
        var saved=s.saveWithoutMetadata(h.getLevel().registryAccess());var restored=new PrototypeStoveEntity(s.getBlockPos(),s.getBlockState());restored.setLevel(h.getLevel());restored.loadWithComponents(saved,h.getLevel().registryAccess());h.assertTrue(restored.batch()&&restored.work()==s.work()&&restored.dial()==s.dial(),"Cooking state survives reload");
        state(h,s,1,900,0,false);h.assertTrue(s.finish()&&!s.finish()&&s.getItem(4).getCount()==4,"Serving produces exactly four portions once");var out=s.getItem(4);double ceiling=2*food(Items.BAKED_POTATO)+2*food(Items.CARROT)+food(Items.BREAD);h.assertTrue(Math.abs(WorkMealItem.calories(out)*4-ceiling)<.00001&&WorkMealItem.quality(out)==1,"Best meal never exceeds configured ingredient budget");
        s.setItem(0,new ItemStack(Items.POTATO,2));s.setItem(1,new ItemStack(Items.CARROT,2));s.setItem(2,new ItemStack(Items.BREAD));h.assertTrue(!s.start()&&s.getItem(0).getCount()==2,"Occupied output rejects next batch without consuming inputs");h.succeed();
    }
    @GameTest(template="industrial") public static void savedStoveFuelEmitsGentleHeat(GameTestHelper h){
        var s=stove(h);s.start();state(h,s,1,0,400,true);
        var saved=s.saveWithoutMetadata(h.getLevel().registryAccess());
        saved.getCompound("coalFire").putDouble("unspentWasteHeat",ThermalRules.COAL_WASTE_HEAT);
        s.loadWithComponents(saved,h.getLevel().registryAccess());
        var field=ThermalField.get(h.getLevel());long key=s.getBlockPos().asLong();double before=field.pending.get(key);
        for(int i=0;i<400;i++)s.tick();
        h.assertTrue(Math.abs(s.work()+s.skillet().warmth()-400)<.00001&&s.fireHeat()==0&&!s.fire.lit(),"Lower room heat preserves saved coal work and exhaustion");
        h.assertTrue(Math.abs(field.pending.get(key)-before-650)<.001,"Already-loaded stove coal emits 5% of industrial waste heat");
        var p=s.getBlockPos();var l=h.getLevel();
        // Enclosed five-by-five kitchen, three air blocks below its roof.
        for(var q:BlockPos.betweenClosed(p.offset(-2,-1,-2),p.offset(2,4,2))){
            int dx=Math.abs(q.getX()-p.getX()),dz=Math.abs(q.getZ()-p.getZ()),dy=q.getY()-p.getY();
            if(!q.equals(p))l.setBlockAndUpdate(q,dy==-1||dy==4||dx==2||dz==2?net.minecraft.world.level.block.Blocks.OAK_PLANKS.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
        var room=new ThermalField();var previous=new ThermalField();var torso=p.north().above();
        for(int second=0;second<120;second++){
            room.pending.put(key,ThermalRules.COAL_WASTE_HEAT*s.coalWasteHeatFactor()*20/ProductionEnergy.HEAT_TICKS);room.step(l);
            previous.pending.put(key,ThermalRules.COAL_WASTE_HEAT*.025*20/ProductionEnergy.HEAT_TICKS);previous.step(l);
        }
        double rise=room.excess(l,torso)*1.8,oldRise=previous.excess(l,torso)*1.8;
        System.out.printf(java.util.Locale.ROOT,"STOVE_ADJACENT_ROOM_F after=120s previous=%.2f rise=%.2f%n",oldRise,rise);
        h.assertTrue(rise>10&&rise<30&&rise>oldRise*1.8&&rise<oldRise*2.2,"Standing beside a working stove is warm without industrial overheating: +"+rise+" F");h.succeed();
    }
    private static double food(Item i){var s=i.getDefaultInstance();return FoodCalories.of(s,s.getFoodProperties(null));}
    @GameTest(template="industrial") public static void servingUsesPresentQualityAndDialValidation(GameTestHelper h){
        var s=stove(h);var p=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"stove"));p.setPos(s.getBlockPos().getX()+.5,s.getBlockPos().getY()+1,s.getBlockPos().getZ()+.5);
        h.assertTrue(s.dial(p,.371234)&&s.dial()==.371234&&!s.dial(p,Double.NaN)&&!s.dial(p,1.01),"Valid continuous inputs survive; invalid inputs rejected");p.setPos(0,-1000,0);h.assertTrue(!s.dial(p,.5),"Remote control rejected");
        s.start();state(h,s,1,1600,10,false);h.assertTrue(s.finish()&&WorkMealItem.quality(s.getItem(4))<.2,"Late serving cannot recover previously reached perfect quality");h.succeed();
    }
    @GameTest(template="industrial") public static void eatenMealBenefitsOnlyWorkAndDoesNotStack(GameTestHelper h){
        var p=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"meal"));p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);var d=CalorieFoodData.of(p);d.reserve().set(500);
        var meal=PrototypeStoveContent.meal(1,1600).copyWithCount(1);meal.finishUsingItem(h.getLevel(),p);h.assertTrue(Math.abs(d.reserve().calories()-900)<.00001&&d.workFactor()==.85,"Actual consumption grants saved calories and work benefit");
        d.workMeal(0);h.assertTrue(d.workFactor()==.9&&d.mealTicks()==36000,"A new meal replaces rather than stacks");double before=d.reserve().calories();d.spendOther(p,100,"sleep");h.assertTrue(d.reserve().calories()==before-100,"Sleep cost receives no discount");before=d.reserve().calories();d.spendOther(p,100,"fertilizer_labor");h.assertTrue(d.reserve().calories()==before-90,"Agricultural work receives discount");var saved=new CompoundTag();d.addAdditionalSaveData(saved);var restored=new CalorieFoodData();restored.readAdditionalSaveData(saved);h.assertTrue(restored.workFactor()==.9&&restored.mealTicks()==36000,"Benefit persists without offline time loss");d.tick(p);h.assertTrue(d.mealTicks()==35999,"Duration counts online ticks");h.succeed();
    }
    @GameTest(template="industrial") public static void carryoverUsesOnlyPaidHeatAndEarlyRemovalRecovers(GameTestHelper h){
        var s=stove(h);s.start();state(h,s,1,0,400,true);for(int i=0;i<400;i++)s.tick();
        double work=s.work(),warmth=s.skillet().warmth();h.assertTrue(work<400&&warmth>100,"Cold cookware stores some paid energy instead of cooking instantly");
        var item=s.liftSkillet();h.assertTrue(!s.batch()&&!s.hasSkillet()&&s.liftSkillet().isEmpty(),"Lifting transfers the batch exactly once");
        s.setItem(0,new ItemStack(Items.POTATO,2));s.setItem(1,new ItemStack(Items.CARROT,2));s.setItem(2,new ItemStack(Items.BREAD));
        h.assertTrue(!s.start()&&s.getItem(0).getCount()==2,"An empty stove cannot reserve a second batch");
        var tag=new CompoundTag();var pan=SkilletItem.contents(item);h.assertTrue(pan.work()==work&&pan.warmth()==warmth,"Vessel keeps food and stored heat");
        for(int i=0;i<2000;i++)pan.tick(0);
        h.assertTrue(pan.work()>work&&pan.work()<=work+warmth&&pan.warmth()==0,"Off-fire cooking draws solely on stored heat and stops");
        h.assertTrue(pan.serve().isEmpty(),"Early removal is an intact undercooked batch, not a lost batch");
        SkilletItem.contents(item,pan);h.assertTrue(s.putSkillet(item)&&item.isEmpty()&&s.batch(),"Undercooked vessel can return to the stove without cloning");
        state(h,s,1,s.work(),800,true);for(int i=0;i<700;i++)s.tick();
        h.assertTrue(s.finish()&&s.getItem(4).getCount()==4,"Recovering an early batch yields four portions");h.succeed();
    }
    @GameTest(template="industrial") public static void actualLiftRestServeAndSupportLossPreserveOneVessel(GameTestHelper h){
        var l=h.getLevel();var s=stove(h);s.start();state(h,s,1,750,400,true);
        var tag=s.saveWithoutMetadata(l.registryAccess());tag.putDouble("warmth",130);s.loadWithComponents(tag,l.registryAccess());
        var p=new net.neoforged.neoforge.common.util.FakePlayer(l,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"pan"));p.setPos(s.getBlockPos().getX()+.5,s.getBlockPos().getY()+1,s.getBlockPos().getZ()+.5);p.setShiftKeyDown(true);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        var hit=new net.minecraft.world.phys.BlockHitResult(s.getBlockPos().getCenter(),Direction.UP,s.getBlockPos(),false);
        PrototypeStoveContent.STOVE.get().useWithoutItem(s.getBlockState(),l,s.getBlockPos(),p,hit);
        h.assertTrue(p.getMainHandItem().is(PrototypeStoveContent.SKILLET.get())&&!s.hasSkillet(),"Actual empty-hand crouch-use lifts the vessel");
        var counter=s.getBlockPos().east(2);l.setBlockAndUpdate(counter,net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState());
        var context=new net.minecraft.world.item.context.UseOnContext(p,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(counter.getCenter().add(0,.5,0),Direction.UP,counter,false));
        h.assertTrue(p.gameMode.useItemOn(p,l,p.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(counter.getCenter().add(0,.5,0),Direction.UP,counter,false)).consumesAction()&&p.getMainHandItem().isEmpty(),"Actual placement transfers, even in Creative");
        var rest=(RestingSkilletEntity)l.getBlockEntity(counter.above());h.assertTrue(rest!=null&&rest.skillet().work()==750&&rest.skillet().warmth()==130,"Supported pan has exactly the transferred contents");
        var saved=rest.saveWithoutMetadata(l.registryAccess());var reload=new RestingSkilletEntity(rest.getBlockPos(),rest.getBlockState());reload.loadWithComponents(saved,l.registryAccess());h.assertTrue(reload.skillet().warmth()==130&&reload.skillet().work()==750,"Resting food and heat survive reload");
        for(int i=0;i<110;i++)rest.tick();h.assertTrue(rest.skillet().work()>800&&rest.skillet().work()<880,"Carryover finishes inside the broad golden band");
        p.setShiftKeyDown(false);var restHit=new net.minecraft.world.phys.BlockHitResult(rest.getBlockPos().getCenter(),Direction.UP,rest.getBlockPos(),false);
        PrototypeStoveContent.RESTING_SKILLET.get().useWithoutItem(rest.getBlockState(),l,rest.getBlockPos(),p,restHit);
        h.assertTrue(p.getInventory().countItem(PrototypeStoveContent.MEAL.get())==4&&!rest.skillet().batch(),"Resting serve produces four portions once and preserves empty cookware");
        PrototypeStoveContent.RESTING_SKILLET.get().useWithoutItem(rest.getBlockState(),l,rest.getBlockPos(),p,restHit);
        h.assertTrue(p.getInventory().countItem(PrototypeStoveContent.MEAL.get())==4,"Repeated serve cannot duplicate portions");
        p.getInventory().selected=1;p.setShiftKeyDown(true);PrototypeStoveContent.RESTING_SKILLET.get().useWithoutItem(rest.getBlockState(),l,rest.getBlockPos(),p,restHit);
        h.assertTrue(l.getBlockState(counter.above()).isAir()&&p.getMainHandItem().is(PrototypeStoveContent.SKILLET.get()),"Resting lift removes the placed vessel");
        h.assertTrue(l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(counter.above())).isEmpty(),"Lifting does not also drop a duplicate empty pan");
        p.gameMode.useItemOn(p,l,p.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(counter.getCenter().add(0,.5,0),Direction.UP,counter,false));l.setBlockAndUpdate(counter,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        var drops=l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(counter).inflate(1));
        h.assertTrue(l.getBlockState(counter.above()).isAir()&&drops.stream().filter(e->e.getItem().is(PrototypeStoveContent.SKILLET.get())).count()==1,"Lost support drops exactly one actual vessel");h.succeed();
    }
    @GameTest(template="industrial") public static void carriedWarmthLegacyMigrationAndFailedPlacement(GameTestHelper h){
        var s=stove(h);s.start();state(h,s,.7,650,400,true);var l=h.getLevel();
        var old=s.saveWithoutMetadata(l.registryAccess());old.remove("hasSkillet");old.remove("warmth");s.loadWithComponents(old,l.registryAccess());
        h.assertTrue(s.hasSkillet()&&s.batch()&&s.work()==650&&s.skillet().warmth()==0,"Existing stove saves migrate with their original pan and intact batch");
        var t=s.saveWithoutMetadata(l.registryAccess());t.putDouble("warmth",100);s.loadWithComponents(t,l.registryAccess());var item=s.liftSkillet();
        var p=new net.neoforged.neoforge.common.util.FakePlayer(l,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"carry"));p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,item);
        var below=s.getBlockPos().below();l.setBlockAndUpdate(below,net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState());
        var ctx=new net.minecraft.world.item.context.UseOnContext(p,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(below.getCenter(),Direction.UP,below,false));
        h.assertTrue(PrototypeStoveContent.SKILLET.get().useOn(ctx)==net.minecraft.world.InteractionResult.FAIL&&item.getCount()==1&&SkilletItem.contents(item).work()==650,"Occupied resting position rejects transfer without changing contents");
        h.onEachTick(()->{PrototypeStoveContent.SKILLET.get().inventoryTick(item,l,p,0,true);if(SkilletItem.contents(item).warmth()<100){h.assertTrue(SkilletItem.contents(item).work()>650,"Carried cookware continues gradually cooling and cooking");p.setPos(s.getBlockPos().getX()+.5,s.getBlockPos().getY()+1,s.getBlockPos().getZ()+.5);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.setShiftKeyDown(false);p.gameMode.useItemOn(p,l,p.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(s.getBlockPos().getCenter(),Direction.UP,s.getBlockPos(),false));h.assertTrue(s.hasSkillet()&&s.batch()&&p.getMainHandItem().isEmpty(),"Ordinary Creative use returns the vessel without opening the menu or cloning it");h.succeed();}});
    }

}
