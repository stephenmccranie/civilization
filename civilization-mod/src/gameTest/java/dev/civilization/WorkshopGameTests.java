package dev.civilization;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class WorkshopGameTests {
    @GameTest(template="industrial") public static void widerEquipmentGradeRollHasBothTails(GameTestHelper h){
        var random=net.minecraft.util.RandomSource.create(987654321L);
        int low=0,high=0;long total=0;
        for(int i=0;i<20000;i++){
            int grade=EquipmentGrade.roll(random);total+=grade;
            if(grade<50)low++;
            if(grade>=100)high++;
        }
        h.assertTrue(total/20000.0>74&&total/20000.0<76,"The wider roll stays centered near 75%");
        h.assertTrue(low>1600&&low<2800&&high>1600&&high<2800,
                "Both low (<50%) and exceptional (100%+) grades appear on roughly one in ten rolls");
        h.succeed();
    }
    @GameTest(template="industrial") public static void pairedWorkshopStorageAndSmithySingleOutput(GameTestHelper h){
        var w=build(h,0,Direction.NORTH);w.setItem(0,WorkshopContent.HIDE.toStack());w.setItem(1,WorkshopContent.HIDE.toStack());w.setItem(5,new ItemStack(Items.LEATHER,31));w.setItem(4,KilnContent.MINERAL_COAL.toStack(4));run(w,1200);
        h.assertTrue(w.getItem(5).getCount()==32&&w.getItem(7).getCount()==1&&w.getItem(0).isEmpty()&&w.getItem(1).isEmpty(),"Two input stacks feed both output stacks");
        var copy=new WorkshopBlockEntity(w.getBlockPos(),w.getBlockState());copy.setLevel(h.getLevel());copy.loadWithComponents(w.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());h.assertTrue(copy.getItem(7).getCount()==1,"Backup workshop output persists");
        copy.removeItem(5,32);
        h.assertTrue(copy.getItem(5).isEmpty()&&copy.getItem(7).getCount()==1,"Taking the first Tannery output leaves the second in place");
        w=build(h,2,Direction.NORTH);w.select(1);w.setItem(0,new ItemStack(Items.IRON_INGOT,6));w.setItem(1,new ItemStack(Items.LEATHER,2));w.setItem(2,new ItemStack(Items.STICK,4));w.setItem(4,KilnContent.MINERAL_COAL.toStack(4));run(w,1200);
        h.assertTrue(w.getItem(5).is(Items.IRON_PICKAXE)&&w.getItem(7).isEmpty()&&w.getItem(0).getCount()==3&&w.status==2,"Smithy pauses after one unstackable output without consuming the next job");
        var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"smithy-tester"));
        var menu=new WorkshopMenu(0,player.getInventory(),w,w.data);
        h.assertTrue(!menu.getSlot(7).isActive()&&java.util.Arrays.equals(w.getSlotsForFace(Direction.DOWN),new int[]{5}),"Smithy exposes one output to players and hoppers");
        var first=w.removeItem(5,1);run(w,600);
        h.assertTrue(w.getItem(5).is(Items.IRON_PICKAXE)&&w.getItem(7).isEmpty()&&w.getItem(0).isEmpty(),"Clearing the output lets the next tool finish");
        h.assertTrue(EquipmentGrade.hasGrade(first)&&EquipmentGrade.hasGrade(w.getItem(5))&&EquipmentGrade.wearSeed(first)!=EquipmentGrade.wearSeed(w.getItem(5)),"Each completed tool receives its own grade and wear seed");
        h.succeed();
    }
    static WorkshopBlockEntity build(GameTestHelper h,int kind,Direction front){
        var pos=h.absolutePos(new BlockPos(5,1,4));var block=switch(kind){case 0->WorkshopContent.TANNERY.get();case 1->WorkshopContent.TEXTILE.get();default->WorkshopContent.SMITHY.get();};
        for(int oldKind=0;oldKind<3;oldKind++)for(var oldFront:Direction.Plane.HORIZONTAL)
            for(var part:WorkshopStructure.parts(oldKind))h.getLevel().removeBlock(MachineStructure.position(pos,oldFront,part),false);
        h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState().setValue(WorkshopBlock.FACING,front));
        for(var part:WorkshopStructure.parts(kind))MachineStructure.placePart(h.getLevel(),pos,front,part);
        return (WorkshopBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    static void run(WorkshopBlockEntity w,int ticks){CoalFireFixture.light(w);for(int i=0;i<ticks;i+=10)w.process();}
    @GameTest(template="industrial") public static void coalHeatFollowsIdleAndMaterialBurn(GameTestHelper h){
        var l=h.getLevel();var field=ThermalField.get(l);
        double[] emissions=new double[3];int n=0;
        for(var item:List.of(Items.IRON_PICKAXE,Items.DIAMOND_PICKAXE)){
            if(n>0)l.removeBlock(h.absolutePos(new BlockPos(5,1,4)),false);
            var w=build(h,2,Direction.NORTH);var job=WorkshopJobs.smithy().stream().filter(j->j.output().is(item)).findFirst().orElseThrow();
            w.select(WorkshopJobs.smithy().indexOf(job));
            for(int i=0;i<4;i++)w.setItem(i,job.needs().get(i).icon());
            w.setItem(4,KilnContent.MINERAL_COAL.toStack(2));
            field.pending.remove(w.getBlockPos().asLong());run(w,10);
            emissions[n++]=field.pending.get(w.getBlockPos().asLong());
        }
        l.removeBlock(h.absolutePos(new BlockPos(5,1,4)),false);
        var idle=build(h,2,Direction.NORTH);idle.setItem(4,KilnContent.MINERAL_COAL.toStack());
        field.pending.remove(idle.getBlockPos().asLong());run(idle,10);
        emissions[2]=field.pending.get(idle.getBlockPos().asLong());
        h.assertTrue(emissions[0]>0&&Math.abs(emissions[1]/emissions[0]-1)<.01,
                "Diamond and iron work emit the same heat per second: "+Arrays.toString(emissions));
        h.assertTrue(Math.abs(emissions[0]/emissions[2]-10)<.01,
                "Idle fire emits one tenth of base productive heat, not a full coal pulse");
        h.succeed();
    }
    @GameTest(template="industrial") public static void coalHeatLedgerSurvivesReloadWithoutReemittingOldFuel(GameTestHelper h){
        var l=h.getLevel();var w=build(h,2,Direction.NORTH);var field=ThermalField.get(l);
        field.pending.remove(w.getBlockPos().asLong());
        w.setItem(4,KilnContent.MINERAL_COAL.toStack());CoalFireFixture.light(w);
        h.assertTrue(w.fire.supply(1),"A lit fire can buy one coal reserve");
        h.assertTrue(field.pending.get(w.getBlockPos().asLong())==0,"Buying coal does not release heat before burning it");
        int first=w.heat/2;w.heat-=first;w.fire.spent(first);
        double half=field.pending.get(w.getBlockPos().asLong());
        h.assertTrue(Math.abs(half-ThermalRules.COAL_WASTE_HEAT*first/(double)(first+w.heat))<.001,"The spent half queues only its share of heat");
        var copy=new WorkshopBlockEntity(w.getBlockPos(),w.getBlockState());copy.setLevel(l);
        copy.loadWithComponents(w.saveWithFullMetadata(l.registryAccess()),l.registryAccess());
        int rest=copy.heat;copy.heat=0;copy.fire.spent(rest);
        h.assertTrue(Math.abs(field.pending.get(w.getBlockPos().asLong())-ThermalRules.COAL_WASTE_HEAT)<.001,
                "Reloaded reserve releases exactly one coal's total heat");
        var legacy=new net.minecraft.nbt.CompoundTag();var oldFire=new net.minecraft.nbt.CompoundTag();oldFire.putBoolean("lit",true);legacy.put("coalFire",oldFire);
        copy.fire.load(legacy);copy.heat=100;copy.heat=0;copy.fire.spent(100);
        h.assertTrue(Math.abs(field.pending.get(w.getBlockPos().asLong())-ThermalRules.COAL_WASTE_HEAT)<.001,
                "Old saves have no unspent ledger and cannot emit the same coal twice");
        h.succeed();
    }
    @GameTest(template="industrial") public static void materialCoalRatesAndDiamondRepair(GameTestHelper h){
        var jobs=WorkshopJobs.smithy();
        for(var pair:java.util.Map.of(Items.IRON_PICKAXE,1,Items.GOLDEN_PICKAXE,2,Items.DIAMOND_PICKAXE,4,Items.NETHERITE_PICKAXE,8).entrySet()){
            var job=jobs.stream().filter(j->j.output().is(pair.getKey())).findFirst().orElseThrow();
            h.assertTrue(job.materialMultiplier()==pair.getValue()&&job.ticks()==600*pair.getValue(),"Material scales total energy demand through production time");
        }
        var w=build(h,2,Direction.NORTH);w.select(0);
        var tool=new ItemStack(Items.DIAMOND_PICKAXE);tool.setDamageValue(300);var job=WorkshopJobs.repair(tool);
        w.setItem(0,tool);for(int i=1;i<4;i++)w.setItem(i,job.needs().get(i).icon());w.setItem(4,KilnContent.MINERAL_COAL.toStack(16));
        run(w,400);h.assertTrue(w.getItem(5).isEmpty(),"Diamond repair cannot finish at the old duration");
        run(w,1200);h.assertTrue(w.getItem(5).is(Items.DIAMOND_PICKAXE)&&w.getItem(5).getDamageValue()==0&&w.heat==0&&w.getItem(4).getCount()==12,"Diamond repair spends four coal in eighty seconds");h.succeed();
    }
    @GameTest(template="industrial") public static void tanningClothProductiveCoal(GameTestHelper h){
        var w=build(h,0,Direction.NORTH);w.setItem(0,WorkshopContent.HIDE.toStack(2));w.setItem(4,KilnContent.MINERAL_COAL.toStack(4));run(w,400);
        h.assertTrue(w.getItem(5).is(Items.LEATHER)&&w.getItem(5).getCount()==2&&w.heat==200&&w.getItem(4).getCount()==3,"Two hides use half a coal of productive heat");
        h.assertTrue(WorkshopJobs.jobs(0).getFirst().coalNote().contains("0.25"),"Tannery quote shows the lower coal cost");
        run(w,1600);h.assertTrue(w.heat==40&&w.getItem(4).getCount()==3,"Idle consumes remaining coal at ten percent rate");
        w=build(h,1,Direction.NORTH);w.setItem(0,new ItemStack(Items.RED_WOOL,2));w.setItem(4,KilnContent.MINERAL_COAL.toStack(4));run(w,190);
        h.assertTrue(w.getItem(5).isEmpty(),"Weaving needs the full ten seconds");
        run(w,210);
        h.assertTrue(w.getItem(5).is(WorkshopContent.CLOTH.get())&&w.getItem(5).getCount()==4&&w.heat==200&&w.getItem(4).getCount()==3,"Two colored wool yield four Cloth in twenty seconds for half a coal");
        h.assertTrue(WorkshopJobs.jobs(1).getFirst().coalNote().contains("0.25"),"Textile quote shows the lower coal cost");
        h.assertTrue(!w.canPlaceItem(0,new ItemStack(Items.WHEAT))&&!w.canPlaceItem(4,new ItemStack(Items.CHARCOAL)),"No plant cloth or biomass fuel");h.succeed();
    }
    @GameTest(template="industrial") public static void everyEquipmentJobConservesIngredients(GameTestHelper h){
        var w=build(h,2,Direction.NORTH);var jobs=WorkshopJobs.smithy();
        for(int n=1;n<jobs.size();n++){
            w.clearContent();w.select(n);w.heat=0;var j=jobs.get(n);
            for(int i=0;i<4;i++)w.setItem(i,j.needs().get(i).icon());
            int supplied=0;CoalFireFixture.light(w);
            for(int tick=0;tick<j.ticks();tick+=10){
                if(w.getItem(4).isEmpty()){w.setItem(4,KilnContent.MINERAL_COAL.toStack(32));supplied+=32;}
                w.process();
            }
            h.assertTrue(w.getItem(5).is(j.output().getItem()),"Output for "+j.name());
            for(int i=0;i<4;i++)h.assertTrue(w.getItem(i).isEmpty(),"Exact input consumption "+j.name());
            h.assertTrue(w.heat==(400-j.ticks()%400)%400 && w.getItem(4).getCount()==supplied-(j.ticks()+399)/400,"Exact productive coal for "+j.name());
        }h.succeed();
    }
    @GameTest(template="industrial") public static void repairLowersGradeAndRetainsIdentity(GameTestHelper h){
        var w=build(h,2,Direction.NORTH);var tool=new ItemStack(Items.IRON_PICKAXE);EquipmentGrade.apply(tool,75);tool.setDamageValue(165);tool.set(DataComponents.CUSTOM_NAME,Component.literal("Townsmith"));
        var unbreaking=h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING);tool.enchant(unbreaking,2);
        long originalWearSeed=EquipmentGrade.wearSeed(tool);
        w.setItem(0,tool);w.setItem(1,new ItemStack(Items.IRON_INGOT,2));w.setItem(2,new ItemStack(Items.LEATHER));w.setItem(4,KilnContent.MINERAL_COAL.toStack(4));run(w,400);
        var out=w.getItem(5);h.assertTrue(EquipmentGrade.value(out)==70&&out.getMaxDamage()==175&&out.getDamageValue()==0,"Repair lowers 75% grade to 70%, which determines maximum durability");
        h.assertTrue(out.getHoverName().getString().equals("Townsmith")&&EnchantmentHelper.getItemEnchantmentLevel(unbreaking,out)==2&&EquipmentGrade.wearSeed(out)==originalWearSeed,"Name, enchantment and personal wear pattern retained");
        out.setDamageValue(174);var quote=WorkshopJobs.repair(out);
        h.assertTrue(EquipmentGrade.value(quote.output())==63&&quote.output().getMaxDamage()==158,"The next repair lowers grade further, with no old durability floor");h.succeed();
    }
    @GameTest(template="industrial") public static void gradeScalesEquipmentStatsAndLowGradeRepairsEnd(GameTestHelper h){
        var sword=new ItemStack(Items.IRON_SWORD);var base=sword.get(DataComponents.ATTRIBUTE_MODIFIERS);EquipmentGrade.apply(sword,70);
        var scaled=sword.get(DataComponents.ATTRIBUTE_MODIFIERS);
        var baseDamage=base.modifiers().stream().filter(e->e.attribute().is(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)).findFirst().orElseThrow().modifier().amount();
        var damage=scaled.modifiers().stream().filter(e->e.attribute().is(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)).findFirst().orElseThrow().modifier().amount();
        var baseSpeed=base.modifiers().stream().filter(e->e.attribute().is(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED)).findFirst().orElseThrow().modifier().amount();
        var speedMod=scaled.modifiers().stream().filter(e->e.attribute().is(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED)).findFirst().orElseThrow().modifier().amount();
        h.assertTrue(sword.getMaxDamage()==175&&Math.abs((1+damage)-(1+baseDamage)*.7)<.001,"Durability and final attack damage scale together");
        h.assertTrue(Math.abs((4+speedMod)-(4+baseSpeed)*.7)<.001,"Negative attack-speed modifiers do not invert grade");
        var pick=new ItemStack(Items.IRON_PICKAXE);float speed=pick.get(DataComponents.TOOL).getMiningSpeed(Blocks.STONE.defaultBlockState());EquipmentGrade.apply(pick,70);
        h.assertTrue(Math.abs(pick.get(DataComponents.TOOL).getMiningSpeed(Blocks.STONE.defaultBlockState())-speed*.7)<.001,"Mining speed scales directly");
        EquipmentGrade.apply(pick,50);pick.setDamageValue(pick.getMaxDamage()-1);var first=WorkshopJobs.repair(pick).output();
        first.setDamageValue(first.getMaxDamage()-1);var second=WorkshopJobs.repair(first).output();
        second.setDamageValue(second.getMaxDamage()-1);
        h.assertTrue(EquipmentGrade.value(first)==35&&EquipmentGrade.value(second)==8&&WorkshopJobs.repair(second)==null,"Weak gear exhausts its repair life without a grade floor");h.succeed();
    }
    @GameTest(template="industrial") public static void gradeChangesActualBlockBreakingRate(GameTestHelper h){
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"GradeMiner"));
        player.getInventory().selected=0;
        var pos=h.absolutePos(new BlockPos(2,1,2));
        var materials=Map.<Item,net.minecraft.world.level.block.Block>of(
                Items.IRON_PICKAXE,Blocks.STONE,
                Items.GOLDEN_PICKAXE,Blocks.STONE,
                Items.DIAMOND_AXE,Blocks.OAK_LOG,
                Items.IRON_SHOVEL,Blocks.DIRT,
                Items.IRON_HOE,Blocks.HAY_BLOCK);
        for(var entry:materials.entrySet()){
            var state=entry.getValue().defaultBlockState();h.getLevel().setBlockAndUpdate(pos,state);
            float base=new ItemStack(entry.getKey()).getDestroySpeed(state),lowProgress=0,normalProgress=0;
            for(int grade:new int[]{50,100,125}){
                var tool=new ItemStack(entry.getKey());EquipmentGrade.apply(tool,grade);
                player.getInventory().setItem(0,tool);
                h.assertTrue(Math.abs(tool.getDestroySpeed(state)-base*grade/100.0)<.001,"The held tool's mining speed follows its grade");
                float progress=state.getDestroyProgress(player,h.getLevel(),pos);
                h.assertTrue(progress>0,"The player can mine the test block");
                if(grade==50)lowProgress=progress;
                if(grade==100){normalProgress=progress;h.assertTrue(Math.abs(lowProgress-normalProgress*.5)<.001,"Actual block-breaking progress decreases with grade");}
                if(grade==125)h.assertTrue(Math.abs(progress-normalProgress*1.25)<.001,"Actual block-breaking progress increases with grade");
            }
        }
        h.succeed();
    }
    @GameTest(template="industrial") public static void goldIsAUsefulIntermediateTierAndArmorGradeAffectsProtection(GameTestHelper h){
        var ironPick=new ItemStack(Items.IRON_PICKAXE);var goldPick=new ItemStack(Items.GOLDEN_PICKAXE);var diamondPick=new ItemStack(Items.DIAMOND_PICKAXE);
        EquipmentGrade.apply(goldPick,100);
        h.assertTrue(ironPick.getMaxDamage()<goldPick.getMaxDamage()&&goldPick.getMaxDamage()<diamondPick.getMaxDamage(),"Gold tool durability sits between iron and diamond");
        float ironSpeed=ironPick.get(DataComponents.TOOL).getMiningSpeed(Blocks.STONE.defaultBlockState());
        float goldSpeed=goldPick.get(DataComponents.TOOL).getMiningSpeed(Blocks.STONE.defaultBlockState());
        float diamondSpeed=diamondPick.get(DataComponents.TOOL).getMiningSpeed(Blocks.STONE.defaultBlockState());
        h.assertTrue(ironSpeed<goldSpeed&&goldSpeed<diamondSpeed,"Gold mining speed sits between iron and diamond");
        h.assertTrue(goldPick.get(DataComponents.TOOL).isCorrectForDrops(Blocks.DIAMOND_ORE.defaultBlockState())
                &&!goldPick.get(DataComponents.TOOL).isCorrectForDrops(Blocks.OBSIDIAN.defaultBlockState()),"Gold harvests iron-tier ores, not diamond-tier blocks");
        var goldSword=new ItemStack(Items.GOLDEN_SWORD);EquipmentGrade.apply(goldSword,100);
        double ironDamage=stat(new ItemStack(Items.IRON_SWORD),net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)+1;
        double goldDamage=stat(goldSword,net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)+1;
        double diamondDamage=stat(new ItemStack(Items.DIAMOND_SWORD),net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)+1;
        h.assertTrue(ironDamage<goldDamage&&goldDamage<diamondDamage,"Gold weapon damage is genuinely intermediate");
        for(var tier:List.of(List.of(Items.IRON_AXE,Items.GOLDEN_AXE,Items.DIAMOND_AXE),
                List.of(Items.IRON_SHOVEL,Items.GOLDEN_SHOVEL,Items.DIAMOND_SHOVEL),
                List.of(Items.IRON_HOE,Items.GOLDEN_HOE,Items.DIAMOND_HOE))){
            var middle=new ItemStack(tier.get(1));EquipmentGrade.apply(middle,100);
            double low=stat(new ItemStack(tier.get(0)),net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
            double mid=stat(middle,net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
            double high=stat(new ItemStack(tier.get(2)),net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
            h.assertTrue(low<=mid&&mid<=high,"Gold tool attack damage follows matching iron and diamond endpoints");
        }
        var armorTiers=List.of(List.of(Items.IRON_HELMET,Items.GOLDEN_HELMET,Items.DIAMOND_HELMET),
                List.of(Items.IRON_CHESTPLATE,Items.GOLDEN_CHESTPLATE,Items.DIAMOND_CHESTPLATE),
                List.of(Items.IRON_LEGGINGS,Items.GOLDEN_LEGGINGS,Items.DIAMOND_LEGGINGS),
                List.of(Items.IRON_BOOTS,Items.GOLDEN_BOOTS,Items.DIAMOND_BOOTS));
        for(var tier:armorTiers){
            Item armor=tier.get(1);
            var item=new ItemStack(armor);EquipmentGrade.apply(item,100);
            double defense=stat(item,net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
            h.assertTrue(stat(new ItemStack(tier.get(0)),net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)<defense
                    &&defense<stat(new ItemStack(tier.get(2)),net.minecraft.world.entity.ai.attributes.Attributes.ARMOR),"Gold armor protection is intermediate on each piece");
            h.assertTrue(defense>0&&Math.abs(stat(item,net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS)-1)<.001,"Gold armor has defense and intermediate toughness");
            EquipmentGrade.apply(item,70);
            h.assertTrue(Math.abs(stat(item,net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)-defense*.7)<.001
                    &&Math.abs(stat(item,net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS)-.7)<.001,"Grade scales every gold armor piece's protection and toughness");
        }
        var ironArmor=new ItemStack(Items.IRON_CHESTPLATE);EquipmentGrade.apply(ironArmor,70);
        h.assertTrue(Math.abs(stat(ironArmor,net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)-4.2)<.001,"Armor without a component still grades its vanilla defense");
        var netherite=new ItemStack(Items.NETHERITE_CHESTPLATE);EquipmentGrade.apply(netherite,70);
        h.assertTrue(Math.abs(stat(netherite,net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS)-2.1)<.001
                &&Math.abs(stat(netherite,net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE)-.07)<.001,"Grade scales netherite armor's special stats");h.succeed();
    }
    private static double stat(ItemStack stack,net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute){
        return stack.getAttributeModifiers().modifiers().stream().filter(e->e.attribute().is(attribute)).mapToDouble(e->e.modifier().amount()).sum();
    }
    @GameTest(template="industrial") public static void exceptionalArmorTrimsComeFromGradeNotSmithing(GameTestHelper h){
        var armor=new ItemStack(Items.LEATHER_CHESTPLATE);EquipmentGrade.apply(armor,105,12345L);
        EquipmentGrade.ensure(armor,h.getLevel().random,h.getLevel().registryAccess());
        var trim=armor.get(DataComponents.TRIM);
        h.assertTrue(trim!=null&&trim.pattern().unwrapKey().orElseThrow().location().getNamespace().equals("minecraft")
                &&trim.material().unwrapKey().orElseThrow().location().getNamespace().equals("minecraft"),"100%+ leather armor receives a random vanilla trim");
        EquipmentGrade.ensure(armor,h.getLevel().random,h.getLevel().registryAccess());
        h.assertTrue(trim.equals(armor.get(DataComponents.TRIM)),"The trim is stable across inventory scans");
        EquipmentGrade.apply(armor,99);h.assertTrue(!armor.has(DataComponents.TRIM),"Falling below 100% removes the status trim");
        var ordinary=new ItemStack(Items.DIAMOND_CHESTPLATE);EquipmentGrade.apply(ordinary,99);
        EquipmentGrade.ensure(ordinary,h.getLevel().random,h.getLevel().registryAccess());
        h.assertTrue(!ordinary.has(DataComponents.TRIM),"Lower grade armor stays untrimmed");
        long active=h.getLevel().getRecipeManager().getRecipes().stream().filter(recipe->recipe.value() instanceof SmithingTrimRecipe).count();
        h.assertTrue(active==0,"Vanilla smithing trim application recipes are disabled");h.succeed();
    }
    @GameTest(template="industrial") public static void projectileDamageUsesFiredWeaponsGrade(GameTestHelper h){
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"GradeArcher"));
        var target=h.spawn(net.minecraft.world.entity.EntityType.ZOMBIE,new BlockPos(4,2,2));
        for(int grade:new int[]{50,120}){
            var bow=new ItemStack(Items.BOW);EquipmentGrade.apply(bow,grade);
            var arrow=new net.minecraft.world.entity.projectile.Arrow(h.getLevel(),player,new ItemStack(Items.ARROW),bow);
            var source=h.getLevel().damageSources().arrow(arrow,player);
            var hit=new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(target,
                    new net.neoforged.neoforge.common.damagesource.DamageContainer(source,8));
            EquipmentGrade.projectileDamage(hit);
            h.assertTrue(Math.abs(hit.getAmount()-8*grade/100.0)<.001,"Fired bow grade scales damage before armor mitigation");
        }
        h.succeed();
    }
    @GameTest(template="industrial") public static void cheapTanneryPrecedesMetalworking(GameTestHelper h){
        long stone=WorkshopStructure.parts(0).stream().filter(part->part.material().equals("stone")&&part.y()==0).count();
        long brick=WorkshopStructure.parts(0).stream().filter(part->part.material().equals("brick")&&part.y()==0).count();
        h.assertTrue(stone==6&&brick==2,"Tannery has a cobblestone plinth with just two brick accents");
        var recipe=h.getLevel().getRecipeManager().getRecipes().stream().filter(holder->holder.id().toString().equals("civilization:tannery")).findFirst().orElseThrow().value();
        var ingredients=recipe.getIngredients();
        h.assertTrue(ingredients.stream().noneMatch(ingredient->ingredient.test(new ItemStack(Items.IRON_INGOT)))
                &&ingredients.stream().anyMatch(ingredient->ingredient.test(new ItemStack(Items.BARREL))),"Tannery controller needs a barrel but no iron");h.succeed();
    }
    @GameTest(template="industrial") public static void blockedStructureAndSaveResume(GameTestHelper h){
        var w=build(h,0,Direction.NORTH);w.setItem(0,WorkshopContent.HIDE.toStack());w.setItem(4,KilnContent.MINERAL_COAL.toStack(4));run(w,100);
        var saved=w.saveWithFullMetadata(h.getLevel().registryAccess());var copy=new WorkshopBlockEntity(w.getBlockPos(),w.getBlockState());copy.setLevel(h.getLevel());copy.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(copy.progress==100&&copy.heat==350&&copy.getItem(0).getCount()==1,"Saved work and fuel retained");
        copy.setItem(5,new ItemStack(Items.STONE,32));copy.setItem(7,new ItemStack(Items.STONE,32));run(copy,200);h.assertTrue(copy.progress==100&&copy.heat==330,"Blocked output preserves work but spends idle heat");copy.setItem(5,ItemStack.EMPTY);copy.setItem(7,ItemStack.EMPTY);
        var wall=w.getBlockPos().east();h.getLevel().removeBlock(wall,false);run(copy,200);h.assertTrue(copy.getItem(0).getCount()==1&&copy.heat==0&&!copy.fire.lit(),"Broken shell extinguishes fire");
        h.getLevel().setBlockAndUpdate(wall,Blocks.BRICKS.defaultBlockState());run(copy,100);h.assertTrue(copy.getItem(5).is(Items.LEATHER)&&copy.heat==350,"Relit shell finishes saved job once");
        var oldProgress=build(h,0,Direction.NORTH);oldProgress.setItem(0,WorkshopContent.HIDE.toStack());oldProgress.setItem(4,KilnContent.MINERAL_COAL.toStack());run(oldProgress,100);
        oldProgress.progress=300;int remaining=oldProgress.heat;oldProgress.process();
        h.assertTrue(oldProgress.getItem(5).is(Items.LEATHER)&&oldProgress.heat==remaining,"Previously saved tanning work beyond the shorter duration completes without extra heat");h.succeed();
    }
    @GameTest(template="industrial") public static void workshopsRotateAndProtectSlots(GameTestHelper h){
        for(int kind=0;kind<3;kind++)for(var dir:Direction.Plane.HORIZONTAL){var w=build(h,kind,dir);h.assertTrue(MachineStructure.check(h.getLevel(),w.getBlockPos(),dir).status()==MachineStructure.COMPLETE,"Complete rotated workshop");}
        var w=build(h,2,Direction.NORTH);h.assertTrue(Arrays.equals(w.getSlotsForFace(Direction.DOWN),new int[]{5})&&!w.canTakeItemThroughFace(0,new ItemStack(Items.IRON_PICKAXE),Direction.DOWN),"Hoppers access only the Smithy output");
        w.select(99999);h.assertTrue(w.selected==-1,"Invalid selection rejected");h.succeed();
    }
    @GameTest(template="industrial") public static void smithyGroundAnvilAndCutBenchRequired(GameTestHelper h){
        var w=build(h,2,Direction.NORTH);var l=h.getLevel();var pos=w.getBlockPos();
        h.assertTrue(WorkshopStructure.parts(2).stream().mapToInt(MachineStructure.Part::depth).max().orElseThrow()==2,"Three-block envelope includes rear facade");
        h.assertTrue(WorkshopStructure.parts(2).stream().filter(p->p.material().equals("air")&&p.y()==1&&p.depth()<2&&p.x()<2).count()==4,"Two-by-two clear hearth");
        h.assertTrue(WorkshopStructure.parts(2).stream().filter(p->p.material().equals("planks")&&p.units()==3).count()==4,"Four eighth-cube feet");
        var rear=WorkshopStructure.parts(2).stream().filter(p->p.x()==0&&p.y()==1&&p.depth()==2).findFirst().orElseThrow();
        h.assertTrue(rear.units()==2,"Recessed slab rear panel");
        l.removeBlock(MachineStructure.position(pos,Direction.NORTH,rear),false);
        h.assertTrue(MachineStructure.check(l,pos,Direction.NORTH).status()==0,"Rear panel required");
        MachineStructure.placePart(l,pos,Direction.NORTH,rear);
        h.assertTrue(WorkshopStructure.parts(2).stream().anyMatch(p->p.units()==3),"Eighth-block hood corners required");
        var table=WorkshopStructure.parts(2).stream().filter(p->p.material().equals("planks")).map(p->CutBlock.bounds(MachineStructure.shape(p,Direction.NORTH)).move(p.x(),p.y(),p.depth())).reduce(net.minecraft.world.phys.AABB::minmax).orElseThrow();
        h.assertTrue(table.getXsize()==2&&table.getYsize()==1&&table.getZsize()==1.5,"Exact low table dimensions");
        var anvil=pos.east(WorkshopStructure.SMITHY_ANVIL_X);h.assertTrue(l.getBlockState(anvil).is(Blocks.ANVIL),"Real anvil at controller floor height");
        l.setBlockAndUpdate(anvil,Blocks.CHIPPED_ANVIL.defaultBlockState());
        h.assertTrue(MachineStructure.check(l,pos,Direction.NORTH).status()==1,"Worn anvils remain valid");
        l.removeBlock(anvil,false);w.select(1);w.setItem(0,new ItemStack(Items.IRON_INGOT,3));w.setItem(1,new ItemStack(Items.LEATHER));w.setItem(2,new ItemStack(Items.STICK,2));w.setItem(4,KilnContent.MINERAL_COAL.toStack());w.process();
        h.assertTrue(w.status==0&&w.getItem(4).getCount()==1,"Missing anvil stops work without fuel loss");
        l.setBlockAndUpdate(anvil,Blocks.ANVIL.defaultBlockState());
        var leg=WorkshopStructure.parts(2).stream().filter(p->p.material().equals("planks")&&p.units()==3).findFirst().orElseThrow();
        var legPos=MachineStructure.position(pos,Direction.NORTH,leg);l.setBlockAndUpdate(legPos,Blocks.OAK_PLANKS.defaultBlockState());
        h.assertTrue(MachineStructure.check(l,pos,Direction.NORTH).status()==0,"Full block cannot substitute for open bench leg");
        l.removeBlock(legPos,false);
        MachineStructure.placePart(l,pos,Direction.NORTH,leg);
        var top=WorkshopStructure.parts(2).stream().filter(p->p.x()==leg.x()&&p.y()==leg.y()&&p.depth()==leg.depth()&&p.units()!=3).findFirst().orElseThrow();
        h.assertTrue(!MachineStructure.matches(l,legPos,top,Direction.NORTH)&&!MachineStructure.obstructed(l,legPos,top,Direction.NORTH),"Foot permits missing tabletop guide");
        MachineStructure.placePart(l,pos,Direction.NORTH,top);h.assertTrue(MachineStructure.check(l,pos,Direction.NORTH).status()==1,"Foot and tabletop restore structure");h.succeed();
    }
    @GameTest(template="industrial") public static void recipesAndMendingCannotBypassWorkshops(GameTestHelper h){
        var manager=h.getLevel().getRecipeManager();
        for(var holder:manager.getRecipes())if(holder.value().getType()==RecipeType.CRAFTING||holder.value() instanceof SmithingTransformRecipe)
            h.assertTrue(!WorkshopJobs.gated(holder.value().getResultItem(h.getLevel().registryAccess())),"No direct equipment recipe: "+holder.id());
        var a=new ItemStack(Items.IRON_PICKAXE);a.setDamageValue(200);var b=a.copy();
        var input=CraftingInput.of(2,1,List.of(a,b));h.assertTrue(!new RepairItemRecipe(CraftingBookCategory.MISC).matches(input,h.getLevel()),"No inventory repair combining");
        var ench=h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING);
        h.assertTrue(!ench.value().isSupportedItem(a),"Mending cannot be applied");a.enchant(ench,1);h.assertTrue(EnchantmentHelper.modifyDurabilityToRepairFromXp(h.getLevel(),a,4)==4,"Existing Mending provides no durability repair effect");
        h.assertTrue(WorkshopGates.disabledTrade(a),"Equipment trade rejected");h.succeed();
    }
    @GameTest(template="industrial") public static void shieldBelongsToSmithy(GameTestHelper h){
        h.assertTrue(h.getLevel().getRecipeManager().getRecipes().stream().noneMatch(holder->holder.id().toString().equals("minecraft:shield")),"Vanilla shield crafting is disabled");
        var jobs=WorkshopJobs.smithy();
        var job=jobs.stream().filter(j->j.output().is(Items.SHIELD)).findFirst().orElseThrow();
        h.assertTrue(job.ticks()==600&&job.heatSpent(job.ticks())==600&&WorkshopJobs.gated(job.output()),"Shield has a gated thirty-second Smithy job");
        var w=build(h,2,Direction.NORTH);w.select(jobs.indexOf(job));
        w.setItem(0,new ItemStack(Items.IRON_INGOT));
        w.setItem(1,new ItemStack(Items.OAK_PLANKS,3));
        w.setItem(2,new ItemStack(Items.SPRUCE_PLANKS,3));
        w.setItem(3,new ItemStack(Items.LEATHER));
        w.setItem(4,KilnContent.MINERAL_COAL.toStack(2));
        run(w,600);
        h.assertTrue(w.getItem(5).is(Items.SHIELD)&&EquipmentGrade.hasGrade(w.getItem(5))&&w.heat==200&&w.getItem(4).isEmpty(),"Mixed planks and iron/leather produce a graded shield for 1.5 Coal");
        for(int i=0;i<4;i++)h.assertTrue(w.getItem(i).isEmpty(),"Shield consumes each material exactly once");
        h.assertTrue(jobs.stream().map(WorkshopJobs.Job::id).distinct().count()==jobs.size(),"Smithy jobs have unique stable IDs");
        var saved=w.saveWithFullMetadata(h.getLevel().registryAccess());saved.putInt("job",1);
        var restored=new WorkshopBlockEntity(w.getBlockPos(),w.getBlockState());restored.setLevel(h.getLevel());
        restored.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(restored.selected==jobs.indexOf(job),"Saved job ID wins when the old numeric row changes");
        saved.putString("jobId","civilization:removed_job");saved.putInt("progress",200);
        restored.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(restored.selected==-1&&restored.progress==0,"Unknown job returns to Automatic without reusing old work");
        saved.remove("jobId");restored.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(restored.selected==1,"Existing index-only saves still load");
        h.succeed();
    }
    @GameTest(template="industrial") public static void netheriteUpgradeRetainsWear(GameTestHelper h){
        var old=new ItemStack(Items.DIAMOND_PICKAXE);EquipmentGrade.apply(old,75);old.setDamageValue(old.getMaxDamage()/2);old.set(DataComponents.CUSTOM_NAME,Component.literal("Old pick"));
        var job=WorkshopJobs.smithy().stream().filter(j->j.output().is(Items.NETHERITE_PICKAXE)).findFirst().orElseThrow();var out=WorkshopJobs.result(job,old);
        h.assertTrue(EquipmentGrade.value(out)==75&&out.getMaxDamage()==(int)Math.round(2031*.75)&&out.getDamageValue()==(int)Math.ceil(old.getDamageValue()/(double)old.getMaxDamage()*out.getMaxDamage())&&EquipmentGrade.wearSeed(out)==EquipmentGrade.wearSeed(old),"Upgrade preserves grade, personal wear pattern and proportional damage");
        h.assertTrue(out.getHoverName().getString().equals("Old pick"),"Upgrade keeps identity");h.succeed();
    }
    @GameTest(template="industrial") public static void vanillaRepairMenusKeepWorkshopBoundary(GameTestHelper h){
        var p=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"WorkshopCheck"));
        var tool=new ItemStack(Items.IRON_PICKAXE);tool.set(DataComponents.MAX_DAMAGE,200);tool.setDamageValue(150);
        var anvil=new net.minecraft.world.inventory.AnvilMenu(1,p.getInventory());anvil.getSlot(0).set(tool.copy());anvil.getSlot(1).set(new ItemStack(Items.IRON_INGOT));
        h.assertTrue(anvil.getSlot(2).getItem().isEmpty(),"Anvil cannot repair using raw metal");
        anvil.getSlot(1).set(tool.copy());h.assertTrue(anvil.getSlot(2).getItem().isEmpty(),"Anvil cannot merge damaged equipment");
        var grind=new net.minecraft.world.inventory.GrindstoneMenu(2,p.getInventory());grind.getSlot(0).set(tool.copy());grind.getSlot(1).set(tool.copy());
        h.assertTrue(grind.getSlot(2).getItem().isEmpty(),"Grindstone cannot merge equipment");
        var enchant=h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING);tool.enchant(enchant,1);
        grind.getSlot(1).set(ItemStack.EMPTY);grind.getSlot(0).set(tool);
        h.assertTrue(grind.getSlot(2).getItem().getMaxDamage()==200&&grind.getSlot(2).getItem().getDamageValue()==150,"Disenchanting preserves wear and damage");h.succeed();
    }
    @GameTest(template="industrial",timeoutTicks=80) public static void mobGearAndBrokenWorkshopConservation(GameTestHelper h){
        var w=build(h,0,Direction.NORTH);w.setItem(0,WorkshopContent.HIDE.toStack(7));w.setItem(4,KilnContent.MINERAL_COAL.toStack(3));CoalFireFixture.light(w);w.process();
        h.runAfterDelay(10,()->{
            var zombie=h.spawn(net.minecraft.world.entity.EntityType.ZOMBIE,new BlockPos(4,2,2));
            for(var slot:net.minecraft.world.entity.EquipmentSlot.values())zombie.setItemSlot(slot,ItemStack.EMPTY);
            zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));zombie.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND,2);
            zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,new ItemStack(Items.DIAMOND_HELMET));zombie.setDropChance(net.minecraft.world.entity.EquipmentSlot.HEAD,1);
            zombie.hurt(h.getLevel().damageSources().genericKill(),1000);
            h.getLevel().destroyBlock(w.getBlockPos(),true);
        });
        h.runAfterDelay(15,()->{
            var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(w.getBlockPos()).inflate(6));
            java.util.function.ToIntFunction<Item> count=item->drops.stream().filter(e->e.getItem().is(item)).mapToInt(e->e.getItem().getCount()).sum();
            h.assertTrue(count.applyAsInt(Items.DIAMOND_SWORD)==1&&count.applyAsInt(Items.DIAMOND_HELMET)==0,"Player-supplied gear retained; naturally spawned gear excluded");
            h.assertTrue(count.applyAsInt(WorkshopContent.HIDE.get())==7&&count.applyAsInt(KilnContent.MINERAL_COAL.get())==2,"Breaking returns all physical stock; consumed coal remains consumed");h.succeed();
        });
    }
    @GameTest(template="industrial") public static void automaticRecipesAndTwoCoalSlots(GameTestHelper h){
        var w=build(h,2,Direction.NORTH);
        w.setItem(3,new ItemStack(Items.IRON_INGOT));w.setItem(1,new ItemStack(Items.IRON_INGOT));w.setItem(2,new ItemStack(Items.LEATHER));
        w.setItem(6,KilnContent.MINERAL_COAL.toStack(4));run(w,400);
        h.assertTrue(w.getItem(5).is(Items.SHEARS)&&w.getItem(6).isEmpty()&&w.getItem(4).getCount()==3&&w.heat==0,"Automatic recipe aggregates reordered split inputs and draws second coal slot");
        w.clearContent();w.heat=0;w.setItem(2,new ItemStack(Items.IRON_INGOT,3));w.setItem(0,new ItemStack(Items.STICK,2));w.setItem(3,new ItemStack(Items.LEATHER));w.setItem(6,KilnContent.MINERAL_COAL.toStack(4));run(w,600);
        h.assertTrue(w.status==5&&w.getItem(5).isEmpty()&&w.getItem(4).getCount()+w.getItem(6).getCount()==3&&w.heat==340,"Ambiguous job waits while fire idles");
        w.select(1);run(w,600);h.assertTrue(w.getItem(5).is(Items.IRON_PICKAXE)&&w.getItem(0).isEmpty()&&w.getItem(2).isEmpty(),"Selection resolves ambiguity with unordered materials");
        w.setItem(6,KilnContent.MINERAL_COAL.toStack(7));var tag=w.saveWithFullMetadata(h.getLevel().registryAccess());
        var copy=new WorkshopBlockEntity(w.getBlockPos(),w.getBlockState());copy.loadWithComponents(tag,h.getLevel().registryAccess());
        h.assertTrue(copy.getItem(6).getCount()==7&&copy.selected==1,"Backup coal and selection survive saving");h.succeed();
    }

}
