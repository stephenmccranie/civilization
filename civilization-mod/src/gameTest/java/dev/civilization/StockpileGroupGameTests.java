package dev.civilization;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class StockpileGroupGameTests {
    private static final ItemStack STONE=new ItemStack(Items.STONE),COBBLE=new ItemStack(Items.COBBLESTONE),PLANKS=new ItemStack(Items.OAK_PLANKS);
    private static StockpileEntity pallet(GameTestHelper h,int x,int y,int z){h.setBlock(new BlockPos(x,y,z),CargoContent.STOCKPILE.get());return (StockpileEntity)h.getBlockEntity(new BlockPos(x,y,z));}
    private static void floor(GameTestHelper h){for(int x=2;x<=9;x++)for(int z=2;z<=9;z++)for(int y=0;y<=4;y++)h.setBlock(new BlockPos(x,y,z),y==0?Blocks.STONE:Blocks.AIR);}
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h){
        var profile=new GameProfile(UUID.randomUUID(),"stockpile-handler");var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,net.minecraft.server.level.ClientInformation.createDefault());
        p.connection=new FakePlayer(h.getLevel(),profile).connection;p.setGameMode(GameType.SURVIVAL);p.setPos(h.absolutePos(new BlockPos(3,1,4)).getCenter());return p;
    }
    private static double top(StockpileEntity e){return e.getBlockState().getShape(e.getLevel(),e.getBlockPos()).max(Direction.Axis.Y);}
    private static void use(StockpileEntity e,net.minecraft.server.level.ServerPlayer p){e.getBlockState().useItemOn(p.getMainHandItem(),e.getLevel(),p,InteractionHand.MAIN_HAND,new BlockHitResult(e.getBlockPos().getCenter(),Direction.UP,e.getBlockPos(),false));}
    /** The native tag an older mass-limited pallet saved, with no volume limit applied. */
    private static CompoundTag legacy(GameTestHelper h,Object... goods){
        var cargo=new MassCargo(()->1e12,()->{});for(int i=0;i<goods.length;i+=2)cargo.insert((ItemStack)goods[i],(int)goods[i+1]);
        var tag=new CompoundTag();tag.put("Cargo",cargo.save(h.getLevel().registryAccess()));return tag;
    }
    @GameTest(template="industrial") public static void palletHoldsTheSixtyFourBlocksItDrawsAndItsShapeGrows(GameTestHelper h){
        floor(h);var a=pallet(h,3,1,3);
        h.assertTrue(top(a)==2/16d&&a.cargo().capacity()==64,"An empty pallet is its timber base");
        h.assertTrue(a.cargo().insert(STONE,100)==64&&a.cargo().load()==64&&top(a)==1,"A pallet holds exactly one full block-sized stack, and its shape fills the block");
        h.assertTrue(a.cargo().insert(PLANKS,1)==0,"Volume, not mass, limits a pallet: lighter goods do not fit either");
        a.cargo().remove(0,47);h.assertTrue(top(a)==(2+2*3.5)/16,"Seventeen blocks start a second layer");
        a.cargo().remove(0,17);
        h.assertTrue(a.cargo().insert(CuttingContent.stack(Blocks.STONE.defaultBlockState(),2,3),3)==3&&a.cargo().insert(CuttingContent.stack(Blocks.STONE.defaultBlockState(),3,1),600)==500&&a.cargo().load()==64,"Slabs and saw-cut pieces count by their volume");
        h.assertTrue(a.cargo().insert(new ItemStack(Items.DIAMOND),1)==0&&a.cargo().insert(new ItemStack(Items.CHEST),1)==0,"Only solid building goods go on a pallet");h.succeed();
    }
    @GameTest(template="industrial") public static void sidePalletsShareOneMenuButFillOneStackAtATime(GameTestHelper h){
        floor(h);var a=pallet(h,3,1,3);var b=pallet(h,4,1,3);var c=pallet(h,5,1,3);var corner=pallet(h,6,1,4);var above=pallet(h,3,2,3);
        h.assertTrue(a.cargo()==c.cargo()&&a.palletCount()==3&&a.cargo().capacity()==192&&corner.palletCount()==1&&above.palletCount()==1,"Side contact shares one menu and sums capacity; corners and vertical stacking stay separate");
        h.assertTrue(a.cargo().insert(STONE,100)==100&&a.stack().count(0)==64&&b.stack().count(0)==36&&c.stack().isEmpty(),"Goods fill one pallet to a full stack before starting the next");
        h.assertTrue(b.cargo().insert(PLANKS,20)==20&&c.stack().count(0)==20&&b.stack().size()==1,"A new item starts an empty pallet instead of topping another item's stack");
        h.assertTrue(c.cargo().insert(STONE,30)==30&&b.stack().count(0)==64&&c.stack().count(1)==2,"More stone tops up the stone pallet first, then goes on top of other goods");
        h.assertTrue(a.cargo().count(0)==130&&a.cargo().remove(0,10)==10&&c.stack().size()==1&&b.stack().count(0)==56&&a.stack().count(0)==64,"Taking goods empties the last pallet holding them first");
        h.assertTrue(a.cargo().transferTo(c.cargo())==0,"Transfers between aliases of one group cannot duplicate stock");h.succeed();
    }
    @GameTest(template="industrial") public static void emptyPalletsLeaveAGroupFreelyAndOpenMenusFollow(GameTestHelper h){
        floor(h);var a=pallet(h,3,1,3);var c=pallet(h,5,1,3);a.cargo().insert(STONE,10);c.cargo().insert(COBBLE,20);
        var p=player(h);var menu=new CargoMenu(1,p.getInventory(),a);var stale=a.cargo();var bridge=pallet(h,4,1,3);menu.broadcastChanges();
        h.assertTrue(menu.count(0)+menu.count(1)==30&&a.cargo()==c.cargo()&&a.palletCount()==3,"An already-open menu adopts both stacks after a bridge joins them");
        h.assertTrue(stale.insert(STONE,1)==0&&stale.remove(0,1)==0&&stale.transferTo(new MassCargo(()->1e9,()->{}))==0,"Retained stale group views cannot mutate or export stock");
        var loaded=new BlockEvent.BreakEvent(h.getLevel(),a.getBlockPos(),a.getBlockState(),p);StockpileBlock.breaking(loaded);
        var empty=new BlockEvent.BreakEvent(h.getLevel(),bridge.getBlockPos(),bridge.getBlockState(),p);StockpileBlock.breaking(empty);
        h.assertTrue(loaded.isCanceled()&&!empty.isCanceled(),"A pallet with goods on it refuses breaking; an empty one can leave its group");
        h.setBlock(new BlockPos(4,1,3),Blocks.AIR);menu.broadcastChanges();
        h.assertTrue(a.cargo().count(0)==10&&c.cargo().count(0)==20&&menu.count(0)==10&&menu.count(1)==0,"Separating keeps each pallet's own stack and clears stale menu icons");
        pallet(h,4,1,3);h.assertTrue(a.cargo().load()==30&&c.cargo().size()==2,"Rejoining adds the original stacks once");h.succeed();
    }
    @GameTest(template="industrial") public static void rightClickStacksHeldBlocksAndOpensWhenNothingFits(GameTestHelper h){
        floor(h);var a=pallet(h,3,1,3);var p=player(h);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STONE,32));use(a,p);
        h.assertTrue(a.stack().count(0)==32&&p.getOffhandItem().isEmpty()&&!(p.containerMenu instanceof CargoMenu),"Right-click stacks the offhand blocks onto the pallet");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STONE,32));use(a,p);
        h.assertTrue(a.stack().count(0)==64&&p.getOffhandItem().isEmpty()&&p.getMainHandItem().is(Items.IRON_PICKAXE),"A tool in the main hand does not stop the offhand blocks going on");
        // Opening the menu needs a real client connection; the pallet visual scene clicks a full pallet for that half.
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STONE,32));
        h.assertTrue(!a.putHeld(p)&&p.getOffhandItem().getCount()==32&&a.stack().count(0)==64,"With no room left, right-click falls through to the menu and the hand keeps its blocks");h.succeed();
    }
    @GameTest(template="industrial") public static void overloadedPalletsFromBeforeTheLimitKeepEveryItemAndDrainFirst(GameTestHelper h){
        floor(h);var registry=h.getLevel().registryAccess();var a=pallet(h,3,1,3);a.loadWithComponents(legacy(h,STONE,700,PLANKS,1000),registry);
        h.assertTrue(a.cargo().count(0)==700&&a.cargo().count(1)==1000&&a.cargo().load()==1700&&a.layers()==4,"A pallet saved over the limit keeps every item and draws as a full stack");
        h.assertTrue(a.cargo().insert(STONE,1)==0&&StockpileEntity.blocks(a.cargo().load(),a.cargo().capacity()).equals("Overloaded: 1,700 / 64"),"An overloaded pallet accepts nothing and says so");
        var restored=pallet(h,8,1,8);restored.loadWithComponents(a.saveWithoutMetadata(registry),registry);
        h.assertTrue(restored.stack().count(0)==700&&restored.stack().count(1)==1000,"Overloaded stock saves unchanged");
        var b=pallet(h,4,1,3);
        h.assertTrue(a.cargo().insert(COBBLE,10)==10&&b.stack().count(0)==10,"An overloaded pallet does not block its neighbour");
        h.assertTrue(a.cargo().remove(0,650)==650&&a.stack().count(0)==50&&a.cargo().remove(1,1000)==1000&&a.stack().load()==50,"Taking goods drains the overloaded pallet first");
        h.assertTrue(a.cargo().insert(STONE,20)==20&&a.stack().count(0)==64&&b.stack().count(1)==6,"Back under the limit, the pallet fills like any other");
        var c=pallet(h,3,1,6);var d=pallet(h,4,1,6);c.loadWithComponents(legacy(h,STONE,512),registry);d.loadWithComponents(legacy(h,STONE,188),registry);
        h.assertTrue(c.cargo().count(0)==700&&StockpileEntity.blocks(c.cargo().load(),c.cargo().capacity()).equals("Overloaded: 700 / 128"),"An old mass-shared group keeps both saved shares");
        h.assertTrue(c.cargo().remove(0,100)==100&&c.stack().count(0)+d.stack().count(0)==600,"Withdrawing from an overloaded group conserves every item");h.succeed();
    }
    @GameTest(template="industrial") public static void sharedCapacityAllowsPartialWheelbarrowTransfersAndCompetingMenus(GameTestHelper h){
        floor(h);var a=pallet(h,3,1,3);var b=pallet(h,4,1,3);
        h.assertTrue(a.cargo().insert(COBBLE,200)==128&&b.stack().count(0)==64,"Two pallets accept exactly their two stacks, with no native stack cap");
        var p=player(h);var q=player(h);var first=new CargoMenu(1,p.getInventory(),a);var second=new CargoMenu(2,q.getInventory(),b);
        first.clicked(0,0,ClickType.QUICK_MOVE,p);second.clicked(0,0,ClickType.QUICK_MOVE,q);
        h.assertTrue(a.cargo().count(0)==64&&b.stack().isEmpty()&&p.getOffhandItem().getCount()==32&&q.getOffhandItem().getCount()==32,"Two menus withdraw from the same authoritative stacks");
        var wheel=new WheelbarrowEntity(WheelbarrowContent.ENTITY.get(),h.getLevel());wheel.setPos(a.cargoPosition().add(0,0,2));h.getLevel().addFreshEntity(wheel);wheel.cargo().insert(COBBLE,100);
        var carrier=new CargoMenu(3,p.getInventory(),wheel);
        h.assertTrue(carrier.clickMenuButton(p,1)&&wheel.cargo().count(0)==36&&b.stack().count(0)==64,"Unloading fills the free stack and keeps the rest in the wheelbarrow");
        h.assertTrue(carrier.clickMenuButton(p,0)&&wheel.cargo().count(0)==164&&a.cargo().isEmpty(),"Loading takes from the whole group");h.succeed();
    }
    @GameTest(template="industrial") public static void nativePalletSavesKeepOnlyTheirOwnStack(GameTestHelper h){
        floor(h);var a=pallet(h,3,1,3);var b=pallet(h,4,1,3);a.cargo().insert(STONE,100);
        var registry=h.getLevel().registryAccess();var ta=a.saveWithoutMetadata(registry);var tb=b.saveWithoutMetadata(registry);
        h.assertTrue(ta.getList("Cargo",10).getCompound(0).getLong("count")==64&&tb.getList("Cargo",10).getCompound(0).getLong("count")==36,"Each pallet saves only its own stack, never the shared menu");
        var stale=a.cargo();b.setRemoved();b.clearRemoved();b.onLoad();
        h.assertTrue(a.palletCount()==2&&a.cargo().count(0)==100&&stale.insert(STONE,1)==0,"Removal/reload hooks invalidate cached views without copying stock");
        // A removed BE in a still-loaded chunk is recreated by vanilla while
        // its block remains. Remove the block too to model an absent member.
        h.setBlock(new BlockPos(4,1,3),Blocks.AIR);h.assertTrue(a.palletCount()==1&&a.cargo().count(0)==64,"Absent members contribute neither capacity nor stock");
        b=pallet(h,4,1,3);b.loadWithComponents(tb,registry);h.assertTrue(a.palletCount()==2&&a.cargo().count(0)==100,"Restoring a member reconnects its saved stack once");
        h.setBlock(new BlockPos(3,1,3),Blocks.AIR);h.setBlock(new BlockPos(4,1,3),Blocks.AIR);
        var rb=pallet(h,4,1,3);rb.loadWithComponents(tb,registry);var ra=pallet(h,3,1,3);ra.loadWithComponents(ta,registry);
        h.assertTrue(ra.cargo().count(0)==100&&rb.cargo().count(0)==100,"Restoring native tags in reverse order rebuilds one shared menu");h.succeed();
    }
    @GameTest(template="industrial") public static void everyConnectedClaimIsCheckedBeforeUsingAnOpenMenu(GameTestHelper h){
        floor(h);var a=pallet(h,3,1,3);var b=pallet(h,4,1,3);a.cargo().insert(STONE,10);var p=player(h);var menu=new CargoMenu(1,p.getInventory(),a);
        var data=CivicData.get(h.getLevel().getServer());var claim=new CivicData.Claim();claim.at=new CivicData.Address(h.getLevel().dimension().location().toString(),b.getBlockPos().east());claim.owner=new CivicData.Owner(UUID.randomUUID(),false);claim.radius=1;claim.height=32;claim.upkeep=1;claim.coalUnit=3600000;claim.energy=claim.coalUnit*200;claim.lastUpdate=System.currentTimeMillis();data.claims.put(claim.at,claim);data.rebuildIndex();
        try{
            h.assertTrue(CivicAccess.allowed(h.getLevel(),a.getBlockPos(),p)&&!CivicAccess.allowed(h.getLevel(),b.getBlockPos(),p),"The entry pallet is accessible while a connected member is denied");
            h.assertTrue(!menu.stillValid(p),"Connected protected cargo cannot be reached through an unclaimed neighbor");menu.clicked(0,0,ClickType.QUICK_MOVE,p);
            h.assertTrue(a.cargo().count(0)==10&&p.getOffhandItem().isEmpty(),"Revocation prevents an already-open menu from withdrawing shared stock");
            p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STONE,32));use(a,p);
            h.assertTrue(a.cargo().count(0)==10&&p.getOffhandItem().getCount()==32,"Right-click cannot stack goods onto a group with a denied member");
        }finally{data.claims.remove(claim.at);data.rebuildIndex();}h.succeed();
    }
}
