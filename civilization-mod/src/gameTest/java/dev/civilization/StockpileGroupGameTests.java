package dev.civilization;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class StockpileGroupGameTests {
    private static StockpileEntity pallet(GameTestHelper h,int x,int y,int z){h.setBlock(new BlockPos(x,y,z),CargoContent.STOCKPILE.get());return (StockpileEntity)h.getBlockEntity(new BlockPos(x,y,z));}
    private static void floor(GameTestHelper h){for(int x=2;x<=9;x++)for(int z=2;z<=9;z++)for(int y=0;y<=4;y++)h.setBlock(new BlockPos(x,y,z),y==0?Blocks.STONE:Blocks.AIR);}
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h){
        var profile=new GameProfile(UUID.randomUUID(),"stockpile-handler");var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,net.minecraft.server.level.ClientInformation.createDefault());
        p.connection=new FakePlayer(h.getLevel(),profile).connection;p.setGameMode(GameType.SURVIVAL);p.setPos(h.absolutePos(new BlockPos(3,1,4)).getCenter());return p;
    }
    @GameTest(template="industrial") public static void horizontalSidesMergeTransitivelyButCornersAndStackingStaySeparate(GameTestHelper h){
        floor(h);var a=pallet(h,3,1,3);var b=pallet(h,4,1,3);var c=pallet(h,4,1,4);var corner=pallet(h,5,1,5);var above=pallet(h,3,2,3);
        h.assertTrue(a.cargo()==b.cargo()&&b.cargo()==c.cargo()&&a.palletCount()==3,"Side-connected L-shaped stockpiles share one cached inventory");
        h.assertTrue(a.cargo().capacity()==3*CargoConfig.STOCKPILE.get()&&corner.palletCount()==1&&above.palletCount()==1,"Capacity adds per pallet; corner and vertical contacts do not connect");
        a.cargo().insert(new ItemStack(Items.STONE),20);c.cargo().insert(new ItemStack(Items.STONE),30);
        h.assertTrue(b.cargo().size()==1&&b.cargo().count(0)==50&&a.cargo().transferTo(c.cargo())==0,"Matching counts merge and transfer between aliases cannot duplicate stock");h.succeed();
    }
    @GameTest(template="industrial") public static void bridgeMergeSplitAndRejoinPreserveBalancesAndRefreshOpenMenus(GameTestHelper h){
        floor(h);var a=pallet(h,3,1,3);var c=pallet(h,5,1,3);a.cargo().insert(new ItemStack(Items.STONE),10);c.cargo().insert(new ItemStack(Items.COBBLESTONE),20);
        var p=player(h);var menu=new CargoMenu(1,p.getInventory(),a);var stale=a.cargo();var bridge=pallet(h,4,1,3);menu.broadcastChanges();
        h.assertTrue(menu.count(0)+menu.count(1)==30&&a.cargo()==c.cargo()&&a.palletCount()==3,"An already-open inventory adopts both preloaded stores after adding a bridge");
        h.assertTrue(stale.insert(new ItemStack(Items.STONE),1)==0&&stale.remove(0,1)==0&&stale.transferTo(new MassCargo(()->1e9,()->{}))==0,"Retained stale group views cannot mutate or export balances");
        var breaking=new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(h.getLevel(),bridge.getBlockPos(),bridge.getBlockState(),p);StockpileBlock.breaking(breaking);
        h.assertTrue(breaking.isCanceled(),"Even an empty bridge remains protected while the shared inventory is loaded");
        h.setBlock(new BlockPos(4,1,3),Blocks.AIR);menu.broadcastChanges();
        h.assertTrue(a.cargo().count(0)==10&&c.cargo().count(0)==20&&menu.count(0)==10&&menu.count(1)==0,"Separating a bridge retains each pallet's saved balance and clears stale menu icons");
        pallet(h,4,1,3);h.assertTrue(a.cargo().mass()==60000&&c.cargo().size()==2,"Rejoining adds the original balances once");h.succeed();
    }
    @GameTest(template="industrial") public static void sharedCapacityAllowsPartialWheelbarrowTransfersAndCompetingMenus(GameTestHelper h){
        floor(h);var a=pallet(h,3,1,3);var b=pallet(h,4,1,3);var item=new ItemStack(Items.COBBLESTONE);
        h.assertTrue(a.cargo().insert(item,1100)==1024&&b.cargo().count(0)==1024,"Two default pallets accept exactly their combined mass, with no native stack cap");
        var p=player(h);var q=player(h);var first=new CargoMenu(1,p.getInventory(),a);var second=new CargoMenu(2,q.getInventory(),b);
        first.clicked(0,0,ClickType.QUICK_MOVE,p);second.clicked(0,0,ClickType.QUICK_MOVE,q);
        h.assertTrue(a.cargo().count(0)==960&&p.getOffhandItem().getCount()==32&&q.getOffhandItem().getCount()==32,"Two menus withdraw from the same authoritative count");
        var wheel=new WheelbarrowEntity(WheelbarrowContent.ENTITY.get(),h.getLevel());wheel.setPos(a.cargoPosition().add(0,0,2));h.getLevel().addFreshEntity(wheel);
        var carrier=new CargoMenu(3,p.getInventory(),wheel);h.assertTrue(carrier.clickMenuButton(p,0)&&wheel.cargo().count(0)==320&&b.cargo().count(0)==640,"Loading from either end uses the complete group, leaving overflow in shared cargo");
        h.assertTrue(carrier.clickMenuButton(p,1)&&a.cargo().count(0)==960&&wheel.cargo().isEmpty(),"Unloading returns stock once to the combined inventory");h.succeed();
    }
    @GameTest(template="industrial") public static void nativePalletSavesAndUnloadLifecycleNeverSaveTheGroupTwice(GameTestHelper h){
        floor(h);var a=pallet(h,3,1,3);var b=pallet(h,4,1,3);a.cargo().insert(new ItemStack(Items.STONE),700);
        var registry=h.getLevel().registryAccess();var ta=a.saveWithoutMetadata(registry);var tb=b.saveWithoutMetadata(registry);
        h.assertTrue(ta.getList("Cargo",10).getCompound(0).getLong("count")==512&&tb.getList("Cargo",10).getCompound(0).getLong("count")==188,"Native saves retain only the local shard, never a replicated shared ledger");
        var stale=a.cargo();b.setRemoved();b.clearRemoved();b.onLoad();
        h.assertTrue(a.palletCount()==2&&a.cargo().count(0)==700&&stale.insert(new ItemStack(Items.STONE),1)==0,"Removal/reload hooks invalidate cached views without copying saved stock");
        // A removed BE in a still-loaded chunk is recreated by vanilla while
        // its block remains. Remove the block too to model an absent member.
        h.setBlock(new BlockPos(4,1,3),Blocks.AIR);h.assertTrue(a.palletCount()==1&&a.cargo().count(0)==512,"Absent members contribute neither capacity nor accessible stock");
        b=pallet(h,4,1,3);b.loadWithComponents(tb,registry);h.assertTrue(a.palletCount()==2&&a.cargo().count(0)==700,"Restoring a member reconnects its saved stock once");
        h.setBlock(new BlockPos(3,1,3),Blocks.AIR);h.setBlock(new BlockPos(4,1,3),Blocks.AIR);
        var rb=pallet(h,4,1,3);rb.loadWithComponents(tb,registry);var ra=pallet(h,3,1,3);ra.loadWithComponents(ta,registry);
        h.assertTrue(ra.cargo().count(0)==700&&rb.cargo().count(0)==700,"Restoring native tags in reverse order reconstructs one shared balance");h.succeed();
    }
    @GameTest(template="industrial") public static void fragmentedMassAndOverfullSplitsRetainEveryItem(GameTestHelper h){
        floor(h);double old=CargoConfig.STOCKPILE.get();
        try{
            CargoConfig.STOCKPILE.set(3000d);var a=pallet(h,3,1,3);var b=pallet(h,4,1,3);
            h.assertTrue(a.cargo().insert(new ItemStack(Items.STONE),4)==3&&a.cargo().mass()==6000,"Shared mass includes both small per-pallet remainders; indivisible blocks do not add a hidden packing cap");
            var saved=b.saveWithoutMetadata(h.getLevel().registryAccess());h.setBlock(new BlockPos(4,1,3),Blocks.AIR);
            h.assertTrue(a.cargo().count(0)==2&&a.cargo().insert(new ItemStack(Items.STONE),1)==0,"A split can retain overfull saved stock without accepting more");
            h.assertTrue(a.cargo().remove(0,2)==2,"Overfull stock can still be withdrawn completely");
            b=pallet(h,4,1,3);b.loadWithComponents(saved,h.getLevel().registryAccess());h.assertTrue(b.cargo().count(0)==1,"Reconnecting retains the other pallet's exact remainder");
        }finally{CargoConfig.STOCKPILE.set(old);}h.succeed();
    }
    @GameTest(template="industrial") public static void everyConnectedClaimIsCheckedBeforeUsingAnOpenMenu(GameTestHelper h){
        floor(h);var a=pallet(h,3,1,3);var b=pallet(h,4,1,3);a.cargo().insert(new ItemStack(Items.STONE),10);var p=player(h);var menu=new CargoMenu(1,p.getInventory(),a);
        var data=CivicData.get(h.getLevel().getServer());var claim=new CivicData.Claim();claim.at=new CivicData.Address(h.getLevel().dimension().location().toString(),b.getBlockPos().east());claim.owner=new CivicData.Owner(UUID.randomUUID(),false);claim.radius=1;claim.height=32;claim.upkeep=1;claim.coalUnit=3600000;claim.energy=claim.coalUnit*200;claim.lastUpdate=System.currentTimeMillis();data.claims.put(claim.at,claim);data.rebuildIndex();
        try{
            h.assertTrue(CivicAccess.allowed(h.getLevel(),a.getBlockPos(),p)&&!CivicAccess.allowed(h.getLevel(),b.getBlockPos(),p),"The entry pallet is accessible while a connected member is denied");
            h.assertTrue(!menu.stillValid(p),"Connected protected cargo cannot be reached through an unclaimed neighbor");menu.clicked(0,0,ClickType.QUICK_MOVE,p);
            h.assertTrue(a.cargo().count(0)==10&&p.getOffhandItem().isEmpty(),"Revocation prevents an already-open menu from withdrawing shared stock");
        }finally{data.claims.remove(claim.at);data.rebuildIndex();}h.succeed();
    }
}
