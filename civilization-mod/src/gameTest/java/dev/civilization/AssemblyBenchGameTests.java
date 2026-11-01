package dev.civilization;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class AssemblyBenchGameTests {
    private static BlockPos at(GameTestHelper h){return h.absolutePos(new BlockPos(4,1,4));}
    private static net.neoforged.neoforge.common.util.FakePlayer player(GameTestHelper h,Direction front){
        var p=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"BenchBuilder"));
        p.setGameMode(GameType.SURVIVAL);var a=at(h);p.setPos(a.getX()+.5,a.getY(),a.getZ()-3);p.setYRot(front.getOpposite().toYRot());p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(WorkshopContent.BENCH.get(),2));return p;
    }
    private static void clear(GameTestHelper h){
        var a=at(h);for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){h.getLevel().removeBlock(a.offset(x,0,z),false);h.getLevel().setBlockAndUpdate(a.offset(x,-1,z),Blocks.STONE.defaultBlockState());}
        for(var drop:h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(a).inflate(3)))drop.discard();
    }
    private static void place(GameTestHelper h,net.minecraft.server.level.ServerPlayer p){var below=at(h).below();p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(below.getCenter().add(0,.5,0),Direction.UP,below,false)));}
    private static int drops(GameTestHelper h,Item item){return h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(at(h)).inflate(3)).stream().filter(e->e.getItem().is(item)).mapToInt(e->e.getItem().getCount()).sum();}
    @GameTest(template="industrial") public static void nativeTwoCellPlacementRotatesAndOwnsOneInventory(GameTestHelper h){
        for(var front:Direction.Plane.HORIZONTAL){
            clear(h);var p=player(h,front);place(h,p);var a=at(h);var state=h.getLevel().getBlockState(a);
            h.assertTrue(state.is(WorkshopContent.BENCH.get())&&state.getValue(AssemblyBenchBlock.WIDE)&&!state.getValue(AssemblyBenchBlock.PART)&&state.getValue(WorkshopBlock.FACING)==front,"Native root placement faces the builder");
            var mate=AssemblyBenchBlock.other(a,state);var second=h.getLevel().getBlockState(mate);
            h.assertTrue(second.is(state.getBlock())&&second.getValue(AssemblyBenchBlock.PART)&&AssemblyBenchBlock.root(mate,second).equals(a),"Second cell resolves the same owner");
            h.assertTrue(h.getLevel().getBlockEntity(a) instanceof WorkshopBlockEntity&&h.getLevel().getBlockEntity(mate)==null&&p.getMainHandItem().getCount()==1,"Only one inventory and one paid bench");
            for(var pos:List.of(a,mate)){
                var shape=h.getLevel().getBlockState(pos).getCollisionShape(h.getLevel(),pos);
                h.assertTrue(!shape.isEmpty()&&shape.bounds().maxY==14.5/16&&shape.toAabbs().size()>1,"Each half has cached actual top/leg/shelf collision");
            }
        }h.succeed();
    }
    @GameTest(template="industrial") public static void blockedOrCanceledPlacementCannotLeaveHalfABench(GameTestHelper h){
        clear(h);var p=player(h,Direction.NORTH);h.getLevel().setBlockAndUpdate(at(h).east(),Blocks.STONE.defaultBlockState());place(h,p);
        h.assertTrue(h.getLevel().getBlockState(at(h)).isAir()&&p.getMainHandItem().getCount()==2,"Occupied second cell rejects without spending");
        h.getLevel().removeBlock(at(h).east(),false);
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent> cancel=e->{if(e.getLevel()==h.getLevel()&&e.getPos().equals(at(h)))e.setCanceled(true);};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
        try{place(h,p);h.assertTrue(h.getLevel().getBlockState(at(h)).isAir()&&h.getLevel().getBlockState(at(h).east()).isAir()&&p.getMainHandItem().getCount()==2,"Canceled native multi-placement rolls back both cells and item");h.assertTrue(drops(h,WorkshopContent.BENCH.get().asItem())==0,"Rollback cannot produce a loose duplicate bench");}
        finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel);}h.succeed();
    }
    @GameTest(template="industrial") public static void eitherHalfBreaksToOneBenchAndOneStockBalance(GameTestHelper h){
        for(boolean extension:List.of(false,true)){
            clear(h);var p=player(h,Direction.NORTH);place(h,p);var a=at(h);var w=(WorkshopBlockEntity)h.getLevel().getBlockEntity(a);
            w.setItem(0,new ItemStack(ManufacturingContent.part("iron","plate"),3));w.setItem(5,ManufacturingContent.HANDLE.toStack(2));
            h.getLevel().destroyBlock(extension?a.east():a,true);
            h.assertTrue(h.getLevel().getBlockState(a).isAir()&&h.getLevel().getBlockState(a.east()).isAir(),"Removing either half dismantles the pair");
            h.assertTrue(drops(h,WorkshopContent.BENCH.get().asItem())==1&&drops(h,ManufacturingContent.part("iron","plate"))==3&&drops(h,ManufacturingContent.HANDLE.get())==2,"Exactly one bench, input bank and output bank return");
        }
        clear(h);var p=player(h,Direction.NORTH);place(h,p);p.setGameMode(GameType.CREATIVE);var a=at(h).east();var state=h.getLevel().getBlockState(a);state.getBlock().playerWillDestroy(h.getLevel(),a,state,p);h.getLevel().destroyBlock(a,false);
        h.assertTrue(h.getLevel().getBlockState(at(h)).isAir()&&drops(h,WorkshopContent.BENCH.get().asItem())==0,"Creative extension removal cannot mint a bench");h.succeed();
    }
    @GameTest(template="industrial") public static void secondCellClaimProtectsPlacementWorkAndDismantling(GameTestHelper h){
        clear(h);var p=player(h,Direction.NORTH);var data=CivicData.get(h.getLevel().getServer());var c=new CivicData.Claim();c.at=new CivicData.Address(h.getLevel().dimension().location().toString(),at(h).east(2));c.owner=new CivicData.Owner(UUID.randomUUID(),false);c.radius=1;c.height=32;c.upkeep=1;c.coalUnit=3600000;c.energy=360000000;c.lastUpdate=System.currentTimeMillis();data.claims.put(c.at,c);data.rebuildIndex();
        try{
            h.assertTrue(CivicAccess.allowed(h.getLevel(),at(h),p)&&!CivicAccess.allowed(h.getLevel(),at(h).east(),p),"Fixture straddles the claim boundary");place(h,p);h.assertTrue(h.getLevel().getBlockState(at(h)).isAir()&&p.getMainHandItem().getCount()==2,"Cannot build into the protected second cell");
            c.whitelist.put(p.getUUID(),"builder");place(h,p);var w=(WorkshopBlockEntity)h.getLevel().getBlockEntity(at(h));ManufacturingGameTests.supply(w,ManufacturingGameTests.job(3,ManufacturingContent.HANDLE.get()));var worker=ManufacturingGameTests.operator(w);c.whitelist.put(worker.getUUID(),"worker");h.assertTrue(w.beginWork(worker),"Allowed worker starts");w.process();int progress=w.progress;
            c.whitelist.clear();w.process();h.assertTrue(w.progress==progress&&w.getItem(5).isEmpty()&&!w.stillValid(worker),"Losing access to only the extension halts work");
            var event=new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(h.getLevel(),at(h),h.getLevel().getBlockState(at(h)),p);CivicProtection.breaking(event);h.assertTrue(event.isCanceled(),"Root cannot be broken to bypass extension protection");
        }finally{data.claims.remove(c.at);data.rebuildIndex();}h.succeed();
    }
}
