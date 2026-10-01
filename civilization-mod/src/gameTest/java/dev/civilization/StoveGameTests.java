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
        for(int i=0;i<100;i++)s.tick();h.assertTrue(s.work()<=10.00001&&!s.fire.lit(),"Finite paid fuel stops progress");double work=s.work();for(int i=0;i<100;i++)s.tick();h.assertTrue(s.work()==work,"No work while cold");
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
        h.assertTrue(s.work()==400&&s.fireHeat()==0&&!s.fire.lit(),"Lower room heat preserves saved coal work and exhaustion");
        h.assertTrue(Math.abs(field.pending.get(key)-before-325)<.001,"Already-loaded stove coal emits 2.5% of industrial waste heat");
        var p=s.getBlockPos();var l=h.getLevel();
        // Enclosed five-by-five kitchen, three air blocks below its roof.
        for(var q:BlockPos.betweenClosed(p.offset(-2,-1,-2),p.offset(2,4,2))){
            int dx=Math.abs(q.getX()-p.getX()),dz=Math.abs(q.getZ()-p.getZ()),dy=q.getY()-p.getY();
            if(!q.equals(p))l.setBlockAndUpdate(q,dy==-1||dy==4||dx==2||dz==2?net.minecraft.world.level.block.Blocks.OAK_PLANKS.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
        var room=new ThermalField();var torso=p.north().above();
        for(int second=0;second<120;second++){
            room.pending.put(key,ThermalRules.COAL_WASTE_HEAT*s.coalWasteHeatFactor()*20/ProductionEnergy.HEAT_TICKS);room.step(l);
        }
        double rise=room.excess(l,torso)*1.8;
        System.out.printf(java.util.Locale.ROOT,"STOVE_ADJACENT_ROOM_F after=120s rise=%.2f%n",rise);
        h.assertTrue(rise>5&&rise<22,"Standing beside a working stove is warm without industrial overheating: +"+rise+" F");h.succeed();
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
}
