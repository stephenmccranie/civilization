package dev.civilization;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class MassCargoGameTests {
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h){
        var profile=new GameProfile(UUID.randomUUID(),"cargo-handler");
        var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,net.minecraft.server.level.ClientInformation.createDefault());
        p.connection=new FakePlayer(h.getLevel(),profile).connection;p.setGameMode(GameType.SURVIVAL);p.setPos(h.absolutePos(new BlockPos(6,1,5)).getCenter());return p;
    }
    private static StockpileEntity stockpile(GameTestHelper h){
        for(int x=3;x<=9;x++)for(int z=3;z<=9;z++)for(int y=0;y<=4;y++)h.setBlock(new BlockPos(x,y,z),y==0?Blocks.STONE:Blocks.AIR);
        h.setBlock(new BlockPos(5,1,5),CargoContent.STOCKPILE.get());return (StockpileEntity)h.getBlockEntity(new BlockPos(5,1,5));
    }
    private static WheelbarrowEntity barrow(GameTestHelper h){var e=new WheelbarrowEntity(WheelbarrowContent.ENTITY.get(),h.getLevel());var at=h.absolutePos(new BlockPos(7,1,5));e.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);h.getLevel().addFreshEntity(e);return e;}
    @GameTest(template="industrial") public static void massOnlyStacksSaveBeyondNativeAndIntegerCounts(GameTestHelper h){
        var item=CuttingContent.stack(Blocks.OAK_PLANKS.defaultBlockState(),3,1);
        var store=new MassCargo(()->1e12,()->{});long n=5_000_000_000L;
        h.assertTrue(store.insert(item,n)==n&&store.size()==1,"One cargo stack exceeds both native and integer item counts");
        var saved=store.save(h.getLevel().registryAccess());
        h.assertTrue(saved.getCompound(0).getCompound("item").getInt("count")==1,"Persistent ItemStack identity stays at count one");
        var loaded=new MassCargo(()->1000,()->{});loaded.load(saved,h.getLevel().registryAccess());
        h.assertTrue(loaded.entry(0).count()==n&&loaded.insert(item,1)==0,"Reduced mass limit preserves old stock and refuses more");
        h.assertTrue(loaded.remove(0,32)==32&&loaded.entry(0).count()==n-32,"Overfull stock remains recoverable");h.succeed();
    }
    @GameTest(template="industrial") public static void unknownContainersAndMalformedPiecesCannotEnterAsWeightless(GameTestHelper h){
        var store=new MassCargo(()->64000,()->{});
        for(var item:new ItemStack[]{new ItemStack(Items.DIAMOND),new ItemStack(Items.CHEST),new ItemStack(Items.SHULKER_BOX),new ItemStack(CuttingContent.ITEM.get())})
            h.assertTrue(store.insert(item,100)==0,"Unsupported or container item is rejected");
        h.assertTrue(store.insert(new ItemStack(Items.STONE),100)==32,"Only mass limits a full stone load");
        h.assertTrue(store.insert(new ItemStack(Items.STONE),1)==0,"A full mass allowance accepts nothing");
        store.remove(0,32);
        h.assertTrue(store.insert(CuttingContent.stack(Blocks.STONE.defaultBlockState(),3,1),1000)==256&&store.size()==1,"Eighth pieces merge past native stack limits using the same mass");h.succeed();
    }
    @GameTest(template="industrial") public static void adjacentStockpileTransfersOnlyWhatFitsAndKeepsRemainder(GameTestHelper h){
        var stock=stockpile(h);var wheel=barrow(h);var p=player(h);
        stock.cargo().insert(new ItemStack(Items.COBBLESTONE),64);wheel.cargo().insert(new ItemStack(Items.COBBLESTONE),300);
        var menu=new CargoMenu(1,p.getInventory(),wheel);
        h.assertTrue(menu.clickMenuButton(p,0)&&wheel.cargo().entry(0).count()==320&&stock.cargo().entry(0).count()==44,"Ten 32-block cobblestone stacks fit; the rest stays on the pallet");
        menu.clickMenuButton(p,0);
        h.assertTrue(wheel.cargo().entry(0).count()==320&&stock.cargo().entry(0).count()==44,"Repeated load cannot duplicate stock");
        h.assertTrue(menu.clickMenuButton(p,1)&&wheel.cargo().entry(0).count()==300&&stock.cargo().entry(0).count()==64,"Unload fills the pallet's stack and keeps the rest in the wheelbarrow");
        var tag=stock.saveWithoutMetadata(h.getLevel().registryAccess());var restored=new StockpileEntity(stock.getBlockPos(),stock.getBlockState());restored.loadWithComponents(tag,h.getLevel().registryAccess());
        h.assertTrue(restored.cargo().entry(0).count()==64,"Stockpile balance survives native persistence");h.succeed();
    }
    @GameTest(template="industrial") public static void nativeMenuClicksConserveCargoCursorAndPlayerInventory(GameTestHelper h){
        var stock=stockpile(h);var p=player(h);var menu=new CargoMenu(1,p.getInventory(),stock);
        p.getInventory().setItem(9,new ItemStack(Items.STONE,32));
        menu.clicked(27,0,ClickType.QUICK_MOVE,p);
        h.assertTrue(stock.cargo().entry(0).count()==32&&p.getInventory().getItem(9).isEmpty(),"Native shift-click deposits exactly its inventory stock");
        menu.clicked(0,1,ClickType.PICKUP,p);
        h.assertTrue(menu.getCarried().getCount()==16&&stock.cargo().entry(0).count()==16,"Right-click withdraws a normal half stack");
        menu.clicked(0,0,ClickType.PICKUP,p);
        h.assertTrue(menu.getCarried().isEmpty()&&stock.cargo().entry(0).count()==32,"Click deposits cursor contents without cloning");
        stock.cargo().insert(new ItemStack(Items.STONE),100);menu.clicked(0,0,ClickType.QUICK_MOVE,p);
        h.assertTrue(stock.cargo().count(0)==32&&p.getOffhandItem().getCount()==32&&p.getInventory().countItem(Items.STONE)==32,"Shift-click withdraws only the offhand allowance, leaving cargo remainder");
        menu.clicked(CargoMenu.OFFHAND_SLOT,0,ClickType.QUICK_MOVE,p);
        h.assertTrue(p.getOffhandItem().isEmpty()&&stock.cargo().count(0)==64,"Visible offhand slot deposits directly back into cargo");
        for(int i=0;i<36;i++)h.assertTrue(p.getInventory().getItem(i).getCount()<=p.getInventory().getItem(i).getMaxStackSize(),"Player stack caps remain unchanged");h.succeed();
    }
    @GameTest(template="industrial") public static void menuPagesOrganizeTypesWithoutCappingThem(GameTestHelper h){
        var stock=stockpile(h);var p=player(h);
        for(int i=0;i<31;i++){var item=new ItemStack(Items.STONE);item.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Stone "+i));stock.cargo().insert(item,1);}
        var menu=new CargoMenu(1,p.getInventory(),stock);
        h.assertTrue(stock.cargo().size()==31&&menu.pageCount()==2,"More types than visible slots create another page");
        menu.clickMenuButton(p,3);menu.clicked(0,0,ClickType.PICKUP,p);
        h.assertTrue(menu.getCarried().getHoverName().getString().equals("Stone 27")&&stock.cargo().size()==30,"A page withdraws its exact component identity");h.succeed();
    }
    @GameTest(template="industrial") public static void menuTransportsFullLongQuantitiesAndMass(GameTestHelper h){
        double old=CargoConfig.WHEELBARROW.get();
        try{
            CargoConfig.WHEELBARROW.set(1e12);stockpile(h);var wheel=barrow(h);var p=player(h);
            var item=CuttingContent.stack(Blocks.OAK_PLANKS.defaultBlockState(),3,1);long quantity=5_000_000_000L;
            h.assertTrue(wheel.cargo().insert(item,quantity)==quantity,"Large cargo fits a tuned mass allowance");
            var server=new CargoMenu(1,p.getInventory(),wheel);var client=new CargoMenu(1,p.getInventory());
            for(int i=0;i<CargoMenu.DATA;i++)client.data.set(i,(short)server.data.get(i));
            h.assertTrue(client.count(0)==quantity&&client.load()==wheel.cargo().load()&&client.capacity()==1e12,"Signed-short menu synchronization retains full long counts and double mass");
        }finally{CargoConfig.WHEELBARROW.set(old);}h.succeed();
    }
    @GameTest(template="industrial") public static void loadedStockpileBreakAndRevokedClaimCannotRemoveStock(GameTestHelper h){
        var stock=stockpile(h);var p=player(h);stock.cargo().insert(new ItemStack(Items.STONE),10);var menu=new CargoMenu(1,p.getInventory(),stock);
        var breaking=new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(h.getLevel(),stock.getBlockPos(),stock.getBlockState(),p);StockpileBlock.breaking(breaking);
        h.assertTrue(breaking.isCanceled(),"Loaded stockpile refuses removal");
        var data=CivicData.get(h.getLevel().getServer());var claim=new CivicData.Claim();
        claim.at=new CivicData.Address(h.getLevel().dimension().location().toString(),stock.getBlockPos());claim.owner=new CivicData.Owner(UUID.randomUUID(),false);claim.radius=8;claim.height=32;claim.upkeep=1;claim.coalUnit=3600000;claim.energy=claim.coalUnit*200;claim.lastUpdate=System.currentTimeMillis();data.claims.put(claim.at,claim);data.rebuildIndex();
        try{menu.clicked(0,0,ClickType.QUICK_MOVE,p);h.assertTrue(stock.cargo().entry(0).count()==10&&p.getInventory().countItem(Items.STONE)==0,"Revocation blocks an already-open menu without losing cargo");}
        finally{data.claims.remove(claim.at);data.rebuildIndex();}h.succeed();
    }
    @GameTest(template="industrial") public static void loadedRecoveryAndMovingOrRemovedStoresCannotBypassCustody(GameTestHelper h){
        var stock=stockpile(h);var e=barrow(h);var p=player(h);e.cargo().insert(new ItemStack(Items.STONE),1);
        p.setShiftKeyDown(true);e.interact(p,InteractionHand.MAIN_HAND);
        h.assertTrue(!e.isRemoved()&&e.cargo().entry(0).count()==1,"Loaded barrow cannot become a portable inventory item");
        p.setShiftKeyDown(false);var menu=new CargoMenu(1,p.getInventory(),e);
        p.setPos(e.handlerPosition(e.position(),0));e.interact(p,InteractionHand.MAIN_HAND);
        h.assertTrue(p.isPassenger()&&!menu.clickMenuButton(p,1)&&e.cargo().entry(0).count()==1,"Taking handles invalidates open cargo transfers");p.stopRiding();
        var stockMenu=new CargoMenu(2,p.getInventory(),stock);h.setBlock(new BlockPos(5,1,5),Blocks.AIR);
        stockMenu.setCarried(new ItemStack(Items.STONE,1));stockMenu.clicked(0,0,ClickType.PICKUP,p);
        h.assertTrue(stock.cargo().isEmpty()&&stockMenu.getCarried().getCount()==1,"A stale menu cannot deposit into a removed store");
        var tag=new CompoundTag();e.saveWithoutId(tag);var loaded=new WheelbarrowEntity(WheelbarrowContent.ENTITY.get(),h.getLevel());loaded.load(tag);
        h.assertTrue(loaded.cargo().entry(0).count()==1,"Wheelbarrow cargo survives native entity persistence");h.succeed();
    }
}
