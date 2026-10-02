package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.util.FakePlayer;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class BuilderLineGameTests {
    private static FakePlayer player(GameTestHelper h,BlockPos at,ItemStack stack){
        var p=new FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"line-test"));p.setGameMode(GameType.SURVIVAL);p.setOnGround(true);p.setPos(at.getX()+1,at.getY(),at.getZ()-3);p.setItemInHand(InteractionHand.OFF_HAND,BuilderLineContent.LINE.toStack());p.setItemInHand(InteractionHand.MAIN_HAND,stack);return p;
    }
    private static BlockPos setup(GameTestHelper h){var a=h.absolutePos(new BlockPos(3,2,4));for(var p:BlockPos.betweenClosed(a.offset(-2,-1,-4),a.offset(5,3,3)))h.getLevel().setBlockAndUpdate(p,p.getY()==a.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());return a;}
    private static void select(FakePlayer p,BlockPos pos,boolean build){var at=build?pos.below():pos;BuilderLineSystem.select(p,new BlockHitResult(at.getCenter().add(0,build?.5:0,0),build?Direction.UP:Direction.NORTH,at,false));}
    private static void run(FakePlayer p,int ticks){for(int i=0;i<ticks;i++)BuilderLineSystem.step(p);}
    @GameTest(template="industrial") public static void axisLengthsAndCoverage(GameTestHelper h){
        var a=BlockPos.ZERO;for(var d:Direction.values()){var line=BuilderLineSystem.line(a,a.relative(d,15));h.assertTrue(line.size()==16&&line.getFirst().equals(a)&&line.getLast().equals(a.relative(d,15)),"Inclusive maximum in all six directions");h.assertTrue(BuilderLineSystem.line(a,a.relative(d,16)).isEmpty(),"Reject over-limit line");}
        h.assertTrue(BuilderLineSystem.line(a,a.offset(1,1,0)).isEmpty()&&BuilderLineSystem.line(a,a.offset(1,1,1)).isEmpty(),"Reject diagonals");h.assertTrue(BuilderLineSystem.line(a,a).size()==1,"One cell valid");h.assertTrue(!BuilderLineSystem.supported(Items.CHEST.getDefaultInstance())&&!BuilderLineSystem.supported(Items.OAK_SLAB.getDefaultInstance())&&!BuilderLineSystem.supported(Items.SAND.getDefaultInstance())&&!BuilderLineSystem.supported(Items.TNT.getDefaultInstance()),"Exclude functional, shaped and unstable blocks");h.succeed();
    }
    @GameTest(template="industrial") public static void secondMarkPlacesAndPaysOnce(GameTestHelper h){
        var a=setup(h);var p=player(h,a,new ItemStack(Items.STONE_BRICKS,4));var energy=CalorieFoodData.of(p);
        select(p,a,true);h.assertTrue(h.getLevel().getBlockState(a).isAir()&&p.getMainHandItem().getCount()==4,"First point performs no placement");
        select(p,a.east(2),true);h.assertTrue(h.getLevel().getBlockState(a).is(Blocks.STONE_BRICKS)&&p.getMainHandItem().getCount()==3,"Second point immediately starts real placement");run(p,15);
        h.assertTrue(!BuilderLineSystem.selected(p)&&p.getMainHandItem().getCount()==1,"Three inclusive cells consume exactly three held blocks");
        h.runAfterDelay(2,()->{h.assertTrue(energy.placed==3&&energy.laborSpent>0,"Ordinary placement hooks charge each cell once");h.succeed();});
    }
    @GameTest(template="industrial") public static void obstructionExhaustionAndCancel(GameTestHelper h){
        var a=setup(h);var p=player(h,a,new ItemStack(Items.STONE_BRICKS,3));h.getLevel().setBlockAndUpdate(a.east(),Blocks.DIRT.defaultBlockState());select(p,a,true);select(p,a.east(2),true);run(p,10);
        h.assertTrue(h.getLevel().getBlockState(a).is(Blocks.STONE_BRICKS)&&h.getLevel().getBlockState(a.east()).is(Blocks.DIRT)&&h.getLevel().getBlockState(a.east(2)).isAir()&&p.getMainHandItem().getCount()==2&&!BuilderLineSystem.selected(p),"Stop at obstruction and retain paid prefix");
        h.getLevel().removeBlock(a,false);h.getLevel().removeBlock(a.east(),false);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STONE_BRICKS,1));select(p,a,true);select(p,a.east(2),true);run(p,10);h.assertTrue(p.getMainHandItem().isEmpty()&&!BuilderLineSystem.selected(p)&&h.getLevel().getBlockState(a.east()).isAir(),"Empty main stack cannot source inventory");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STONE_BRICKS,2));select(p,a.east(),true);BuilderLineSystem.mark(p,true);h.assertTrue(!BuilderLineSystem.selected(p)&&p.getMainHandItem().getCount()==2,"Explicit cancel costs nothing");h.succeed();
    }
    @GameTest(template="industrial") public static void demolitionUsesRealToolDropsAndCalories(GameTestHelper h){
        var a=setup(h);var p=player(h,a,Items.IRON_PICKAXE.getDefaultInstance());for(int i=0;i<3;i++)h.getLevel().setBlockAndUpdate(a.east(i),Blocks.STONE.defaultBlockState());var energy=CalorieFoodData.of(p);
        select(p,a,false);select(p,a.east(2),false);h.assertTrue(BuilderLineSystem.active(p)&&h.getLevel().getBlockState(a).is(Blocks.STONE),"Normal mining starts without instant removal");run(p,200);
        h.assertTrue(!BuilderLineSystem.selected(p)&&p.getMainHandItem().getDamageValue()==3,"Ordinary durability paid per mined block");
        int drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(a).inflate(5)).stream().filter(e->e.getItem().is(Items.COBBLESTONE)).mapToInt(e->e.getItem().getCount()).sum();h.assertTrue(drops==3,"Normal world drops, not inventory teleportation");
        h.runAfterDelay(2,()->{h.assertTrue(energy.broken==3&&energy.laborSpent>0,"Ordinary mining hooks pay once");h.succeed();});
    }
    @GameTest(template="industrial") public static void brokenAndChangedToolsStop(GameTestHelper h){
        var a=setup(h);var tool=Items.IRON_PICKAXE.getDefaultInstance();tool.setDamageValue(tool.getMaxDamage()-1);var p=player(h,a,tool);for(int i=0;i<3;i++)h.getLevel().setBlockAndUpdate(a.east(i),Blocks.STONE.defaultBlockState());select(p,a,false);select(p,a.east(2),false);run(p,50);h.assertTrue(p.getMainHandItem().isEmpty()&&!BuilderLineSystem.selected(p)&&h.getLevel().getBlockState(a.east()).is(Blocks.STONE),"Final tool use cannot mine a second block");
        p.setItemInHand(InteractionHand.MAIN_HAND,Items.IRON_PICKAXE.getDefaultInstance());select(p,a.east(),false);select(p,a.east(2),false);p.setItemInHand(InteractionHand.MAIN_HAND,Items.DIAMOND_PICKAXE.getDefaultInstance());run(p,50);h.assertTrue(!BuilderLineSystem.selected(p)&&h.getLevel().getBlockState(a.east()).is(Blocks.STONE),"Changing working tool cancels progress");h.succeed();
    }
    @GameTest(template="industrial") public static void targetRaceAndSightStop(GameTestHelper h){
        var a=setup(h);var p=player(h,a,Items.IRON_PICKAXE.getDefaultInstance());h.getLevel().setBlockAndUpdate(a,Blocks.STONE.defaultBlockState());h.getLevel().setBlockAndUpdate(a.east(),Blocks.STONE.defaultBlockState());select(p,a,false);select(p,a.east(),false);h.getLevel().setBlockAndUpdate(a,Blocks.DIRT.defaultBlockState());run(p,50);h.assertTrue(!BuilderLineSystem.selected(p)&&p.getMainHandItem().getDamageValue()==0&&h.getLevel().getBlockState(a).is(Blocks.DIRT),"Changed target cannot spend mining progress on replacement");
        select(p,a.east(),false);select(p,a.east(),false);p.setPos(a.getX(),a.getY(),a.getZ()-25);run(p,50);h.assertTrue(!BuilderLineSystem.selected(p)&&h.getLevel().getBlockState(a.east()).is(Blocks.STONE),"Work reach is bounded");h.succeed();
    }
    @GameTest(template="industrial") public static void canceledEventsRollBack(GameTestHelper h){
        var a=setup(h);var p=player(h,a,new ItemStack(Items.STONE_BRICKS,2));
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent> cancel=e->{if(e.getEntity()==p)e.setCanceled(true);};net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
        try{select(p,a,true);select(p,a.east(),true);run(p,10);h.assertTrue(p.getMainHandItem().getCount()==2&&h.getLevel().getBlockState(a).isAir()&&!BuilderLineSystem.selected(p),"Canceled real placement restores stock/world");}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel);BuilderLineSystem.finish(p,"");}
        h.getLevel().setBlockAndUpdate(a,Blocks.STONE.defaultBlockState());p.setItemInHand(InteractionHand.MAIN_HAND,Items.IRON_PICKAXE.getDefaultInstance());
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.BreakEvent> stop=e->{if(e.getPlayer()==p)e.setCanceled(true);};net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(stop);
        try{select(p,a,false);select(p,a,false);run(p,50);h.assertTrue(p.getMainHandItem().getDamageValue()==0&&h.getLevel().getBlockState(a).is(Blocks.STONE)&&!BuilderLineSystem.selected(p),"Canceled harvest spends no tool or block");}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(stop);BuilderLineSystem.finish(p,"");}
        h.runAfterDelay(2,()->{h.assertTrue(CalorieFoodData.of(p).placed==0&&CalorieFoodData.of(p).broken==0,"Canceled events charge no calories");h.succeed();});
    }
    @GameTest(template="industrial") public static void whitelistRevocationStopsPaidPrefix(GameTestHelper h){
        var a=setup(h);var p=player(h,a,new ItemStack(Items.STONE_BRICKS,3));var data=CivicData.get(h.getLevel().getServer());var c=new CivicData.Claim();c.at=new CivicData.Address(h.getLevel().dimension().location().toString(),a);c.owner=new CivicData.Owner(java.util.UUID.randomUUID(),false);c.radius=2;c.height=32;c.upkeep=1;c.coalUnit=3600000;c.energy=360000000;c.lastUpdate=System.currentTimeMillis();c.whitelist.put(p.getUUID(),"line-test");data.claims.put(c.at,c);data.rebuildIndex();
        try{select(p,a,true);select(p,a.east(),true);c.whitelist.clear();run(p,10);h.assertTrue(h.getLevel().getBlockState(a).is(Blocks.STONE_BRICKS)&&h.getLevel().getBlockState(a.east()).isAir()&&p.getMainHandItem().getCount()==2&&!BuilderLineSystem.selected(p),"Revoke access mid-operation: retain paid prefix, reject remaining cell");}finally{data.claims.remove(c.at);data.rebuildIndex();BuilderLineSystem.finish(p,"");}h.succeed();
    }
    @GameTest(template="industrial") public static void smithyRecipeQuoteAndInputs(GameTestHelper h){
        var job=WorkshopJobs.smithy().stream().filter(j->j.output().is(BuilderLineContent.LINE.get())).findFirst().orElseThrow();
        h.assertTrue(job.ticks()==400&&job.output().getCount()==1&&job.needs().size()==4,"One line reel in 20 seconds of productive Smithy work");
        h.assertTrue(job.needs().get(0).matches(new ItemStack(Items.IRON_INGOT,2))&&!job.needs().get(0).matches(new ItemStack(Items.IRON_INGOT,1))&&job.needs().get(1).matches(new ItemStack(Items.BIRCH_PLANKS))&&job.needs().get(2).matches(new ItemStack(Items.STRING,2))&&job.needs().get(3).matches(ItemStack.EMPTY),"Explicit metal, interchangeable planks and cord requirements");
        var w=WorkshopGameTests.build(h,2,Direction.NORTH);w.select(WorkshopJobs.index(2,"civilization:builders_line"));w.setItem(0,new ItemStack(Items.IRON_INGOT,4));w.setItem(1,new ItemStack(Items.BIRCH_PLANKS,2));w.setItem(2,new ItemStack(Items.STRING,4));w.setItem(4,KilnContent.MINERAL_COAL.toStack(2));WorkshopGameTests.run(w,400);
        h.assertTrue(w.getItem(5).is(BuilderLineContent.LINE.get())&&w.getItem(5).getCount()==1&&w.getItem(0).getCount()==2&&w.getItem(1).getCount()==1&&w.getItem(2).getCount()==2&&w.getItem(4).getCount()==1,"Actual supplied Smithy produces one reel and conserves all inputs/coal");WorkshopGameTests.run(w,100);h.assertTrue(w.getItem(0).getCount()==2&&w.getItem(2).getCount()==2,"Occupied output prevents paying for a second reel");h.succeed();
    }
}
