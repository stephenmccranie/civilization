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
public final class CargoLedgerGameTests {
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
    @GameTest(template="industrial") public static void stacksSaveBeyondNativeAndIntegerCounts(GameTestHelper h){
        var item=CuttingContent.stack(Blocks.OAK_PLANKS.defaultBlockState(),3,1);
        var store=new CargoLedger(()->1e12,()->{});long n=5_000_000_000L;
        h.assertTrue(store.insert(item,n)==n&&store.size()==1,"One cargo stack exceeds both native and integer item counts");
        var saved=store.save(h.getLevel().registryAccess());
        h.assertTrue(saved.getCompound(0).getCompound("item").getInt("count")==1,"Persistent ItemStack identity stays at count one");
        var loaded=new CargoLedger(()->1000,()->{});loaded.load(saved,h.getLevel().registryAccess());
        h.assertTrue(loaded.entry(0).count()==n&&loaded.insert(item,1)==0,"A smaller capacity preserves old stock and refuses more");
        h.assertTrue(loaded.remove(0,32)==32&&loaded.entry(0).count()==n-32,"Overfull stock remains recoverable");h.succeed();
    }
    @GameTest(template="industrial") public static void aHandCarriedMachineStacksOntoAPalletAndBack(GameTestHelper h){
        var stock=stockpile(h);var p=player(h);p.setItemInHand(InteractionHand.OFF_HAND,CookingContent.STATION_ITEM.toStack());
        h.assertTrue(stock.putHeld(p)&&p.getOffhandItem().isEmpty()&&stock.cargo().count(0)==1&&stock.cargo().load()==1,"Right-click stacks a held Cooking Stove onto a pallet as one block");
        new CargoMenu(1,p.getInventory(),stock).clicked(0,0,ClickType.QUICK_MOVE,p);
        h.assertTrue(stock.cargo().isEmpty()&&p.getOffhandItem().is(CookingContent.STATION_ITEM.get()),"Shift-click takes it back to the hand");h.succeed();
    }
    @GameTest(template="industrial") public static void unknownContainersAndMalformedPiecesCannotEnterAsWeightless(GameTestHelper h){
        var store=new CargoLedger(()->32,()->{});
        var filled=new ItemStack(Items.SHULKER_BOX);filled.set(net.minecraft.core.component.DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.fromItems(java.util.List.of(new ItemStack(Items.STONE))));
        var saved=new ItemStack(Items.CHEST);saved.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(new CompoundTag()));
        for(var item:new ItemStack[]{new ItemStack(Items.DIAMOND),new ItemStack(Items.WHEAT_SEEDS),new ItemStack(CargoContent.STOCKPILE.get()),filled,saved,new ItemStack(CuttingContent.ITEM.get())})
            h.assertTrue(store.insert(item,100)==0,"Goods the hand does not carry, filled containers, saved block data and malformed pieces are refused: "+item);
        // Every block the hand carries goes on a store too, one block of space each unless it is a slab or piece.
        for(var item:new ItemStack[]{CookingContent.STATION_ITEM.toStack(),new ItemStack(Items.CHEST),new ItemStack(Items.SHULKER_BOX),new ItemStack(Items.OAK_STAIRS),new ItemStack(Items.OAK_DOOR),new ItemStack(Items.OAK_FENCE),new ItemStack(Items.TNT),new ItemStack(Items.RED_BED)}){
            h.assertTrue(BlockCarry.block(item)&&store.insert(item,1)==1&&store.load()==1,"A hand-carried block goes on a store as one block: "+item);
            store.remove(0,1);
        }
        h.assertTrue(store.insert(new ItemStack(Items.STONE),100)==32,"Only volume limits a full stone load");
        h.assertTrue(store.insert(new ItemStack(Items.STONE),1)==0,"A full store accepts nothing");
        store.remove(0,32);
        h.assertTrue(store.insert(CuttingContent.stack(Blocks.STONE.defaultBlockState(),3,1),1000)==256&&store.size()==1,"Eighth pieces merge past native stack limits using the same volume");h.succeed();
    }
    @GameTest(template="industrial") public static void adjacentStockpileTransfersOnlyWhatFitsAndKeepsRemainder(GameTestHelper h){
        var stock=stockpile(h);var wheel=barrow(h);var p=player(h);
        int tray=WheelbarrowEntity.GRID.capacity();
        stock.cargo().insert(new ItemStack(Items.COBBLESTONE),120);wheel.cargo().insert(new ItemStack(Items.COBBLESTONE),20);
        var menu=new CargoMenu(1,p.getInventory(),wheel);
        h.assertTrue(menu.clickMenuButton(p,0)&&wheel.cargo().entry(0).count()==tray&&stock.cargo().entry(0).count()==120-(tray-20),"The wheelbarrow fills its tray; the rest stays on the pallet");
        menu.clickMenuButton(p,0);
        h.assertTrue(wheel.cargo().entry(0).count()==tray&&stock.cargo().entry(0).count()==120-(tray-20),"Repeated load cannot duplicate stock");
        h.assertTrue(menu.clickMenuButton(p,1)&&wheel.cargo().entry(0).count()==tray-(128-(120-(tray-20)))&&stock.cargo().entry(0).count()==128,"Unload fills the pallet's stack and keeps the rest in the wheelbarrow");
        var tag=stock.saveWithoutMetadata(h.getLevel().registryAccess());var restored=new StockpileEntity(stock.getBlockPos(),stock.getBlockState());restored.loadWithComponents(tag,h.getLevel().registryAccess());
        h.assertTrue(restored.cargo().entry(0).count()==128,"Stockpile balance survives native persistence");h.succeed();
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
        h.assertTrue(stock.cargo().count(0)==128-BlockCarry.LIMIT&&p.getOffhandItem().getCount()==BlockCarry.LIMIT&&p.getInventory().countItem(Items.STONE)==BlockCarry.LIMIT,"Shift-click withdraws only the offhand allowance, leaving cargo remainder");
        menu.clicked(CargoMenu.OFFHAND_SLOT,0,ClickType.QUICK_MOVE,p);
        h.assertTrue(p.getOffhandItem().isEmpty()&&stock.cargo().count(0)==128,"Visible offhand slot deposits directly back into cargo");
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
    @GameTest(template="industrial") public static void wheelbarrowHoldsTheVolumeItDraws(GameTestHelper h){
        var e=barrow(h);int tray=WheelbarrowEntity.GRID.capacity();
        h.assertTrue(tray==36&&e.cargo().insert(new ItemStack(Items.STONE),400)==tray&&e.cargo().insert(new ItemStack(Items.STONE),1)==0,"A wheelbarrow holds exactly the 36 blocks its tray draws");
        e.cargo().remove(0,tray);
        h.assertTrue(e.cargo().insert(new ItemStack(Items.OAK_PLANKS),400)==tray,"Volume, not mass, limits it: light planks fill the same tray");
        e.cargo().remove(0,tray);
        h.assertTrue(e.cargo().insert(new ItemStack(Items.STONE_SLAB),1000)==2*tray&&e.cargo().load()==tray,"Slabs count as half a block");h.succeed();
    }
    @GameTest(template="industrial") public static void wheelbarrowsLoadedBeforeTheSmallerTrayKeepTheirLoad(GameTestHelper h){
        // A barrow saved full under the old 324-block tray: nothing is removed, it draws full and takes nothing until under 36.
        var wheel=barrow(h);var legacy=new CargoLedger(()->324,()->{});legacy.insert(new ItemStack(Items.STONE),324);
        var tag=new CompoundTag();wheel.saveWithoutId(tag);tag.put("Cargo",legacy.save(h.getLevel().registryAccess()));
        var loaded=new WheelbarrowEntity(WheelbarrowContent.ENTITY.get(),h.getLevel());loaded.load(tag);
        h.assertTrue(loaded.cargo().count(0)==324&&loaded.cargo().insert(new ItemStack(Items.STONE),1)==0&&CargoInventory.blocks(loaded.cargo().load(),loaded.cargo().capacity()).equals("Overloaded: 324 / 36"),"An old full barrow keeps all 324 blocks and accepts nothing");
        h.assertTrue(loaded.cargo().remove(0,290)==290&&loaded.cargo().insert(new ItemStack(Items.STONE),10)==2&&loaded.cargo().count(0)==36,"Once under 36 it takes goods again, up to its tray");h.succeed();
    }
    @GameTest(template="industrial") public static void overloadedWheelbarrowKeepsSavedStockAndSyncsLongCounts(GameTestHelper h){
        stockpile(h);var wheel=barrow(h);var p=player(h);
        // A barrow saved under the old mass limit can hold more than its tray; nothing is removed.
        var item=CuttingContent.stack(Blocks.OAK_PLANKS.defaultBlockState(),3,1);long quantity=5_000_000_000L;
        var legacy=new CargoLedger(()->1e12,()->{});legacy.insert(item,quantity);wheel.cargo().load(legacy.save(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(wheel.cargo().count(0)==quantity&&wheel.cargo().insert(new ItemStack(Items.STONE),1)==0&&CargoInventory.blocks(wheel.cargo().load(),wheel.cargo().capacity()).equals("Overloaded: 625,000,000 / 36"),"An overloaded wheelbarrow keeps its stock and accepts nothing");
        var server=new CargoMenu(1,p.getInventory(),wheel);var client=new CargoMenu(1,p.getInventory());
        for(int i=0;i<CargoMenu.DATA;i++)client.data.set(i,(short)server.data.get(i));
        h.assertTrue(client.count(0)==quantity&&client.load()==wheel.cargo().load()&&client.capacity()==WheelbarrowEntity.GRID.capacity(),"Signed-short menu synchronization retains full long counts and double loads");
        h.assertTrue(wheel.cargo().remove(0,32)==32,"Overloaded stock can still be taken off");h.succeed();
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
