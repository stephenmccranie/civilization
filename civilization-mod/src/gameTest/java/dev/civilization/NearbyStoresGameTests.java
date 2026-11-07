package dev.civilization;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class NearbyStoresGameTests {
    private static ServerPlayer player(GameTestHelper h){
        var profile=new GameProfile(UUID.randomUUID(),"neighbour");
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,ClientInformation.createDefault());
        p.connection=new FakePlayer(h.getLevel(),profile).connection;p.setGameMode(GameType.SURVIVAL);p.setPos(h.absolutePos(new BlockPos(7,1,7)).getCenter());return p;
    }
    private static StockpileEntity pallet(GameTestHelper h){
        for(int x=3;x<=9;x++)for(int z=3;z<=9;z++)for(int y=0;y<=4;y++)h.setBlock(new BlockPos(x,y,z),y==0?Blocks.STONE:Blocks.AIR);
        h.setBlock(new BlockPos(5,1,5),CargoContent.STOCKPILE.get());return (StockpileEntity)h.getBlockEntity(new BlockPos(5,1,5));
    }
    private static WheelbarrowEntity barrow(GameTestHelper h){
        var barrow=new WheelbarrowEntity(WheelbarrowContent.ENTITY.get(),h.getLevel());var at=h.absolutePos(new BlockPos(8,1,5));
        barrow.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);h.getLevel().addFreshEntity(barrow);return barrow;
    }
    private static long stored(CargoInventory store,Item item){int at=store.indexOf(new ItemStack(item));return at<0?0:store.count(at);}
    private static AbstractContainerMenu chest(ServerPlayer p){
        p.openMenu(new SimpleMenuProvider((id,inv,player)->ChestMenu.threeRows(id,inv,new SimpleContainer(27)),Component.literal("Chest")));return p.containerMenu;
    }

    @GameTest(template="industrial") public static void aMenuListsTheStoresWithinReachAndTakesAndStacksThroughThem(GameTestHelper h){
        var pallet=pallet(h);pallet.cargo().insert(new ItemStack(Items.STONE),64);
        var barrow=barrow(h);barrow.cargo().insert(new ItemStack(Items.STONE),10);barrow.cargo().insert(new ItemStack(Items.OAK_PLANKS),5);
        var p=player(h);var menu=chest(p);var near=NearbyStores.of(menu);menu.broadcastChanges();
        h.assertTrue(near!=null&&near.stores()==2&&near.size()==2&&near.count(near.indexOf(new ItemStack(Items.STONE)))==74,"A chest menu lists the Pallet and wheelbarrow within reach, their stone merged into one entry");
        CargoMenu.click(menu,p,near,near.indexOf(new ItemStack(Items.STONE)),0,false);int stack=new ItemStack(Items.STONE).getMaxStackSize();
        h.assertTrue(menu.getCarried().getCount()==stack&&stored(barrow.cargo(),Items.STONE)==0&&stored(pallet.cargo(),Items.STONE)==74-stack,"A click takes a full stack, from the nearer wheelbarrow first, and the Pallet keeps the rest");
        CargoMenu.click(menu,p,near,-1,0,false);
        h.assertTrue(menu.getCarried().isEmpty()&&stored(pallet.cargo(),Items.STONE)+stored(barrow.cargo(),Items.STONE)==74,"Clicking with the stack stacks all of it back onto the stores");
        CargoMenu.click(menu,p,near,near.indexOf(new ItemStack(Items.OAK_PLANKS)),0,true);
        h.assertTrue(p.getOffhandItem().is(Items.OAK_PLANKS)&&p.getOffhandItem().getCount()==5&&stored(barrow.cargo(),Items.OAK_PLANKS)==0,"Shift-click moves building goods into the Hand");
        p.setPos(p.position().add(20,0,0));menu.broadcastChanges();
        h.assertTrue(near.stores()==0&&near.size()==0,"Walking out of reach empties the list");
        var inventory=p.inventoryMenu;h.assertTrue(NearbyStores.of(inventory)==null,"The player's own inventory has no nearby list");
        h.succeed();
    }

    @GameTest(template="industrial") public static void goodsOnTheCursorGoOntoTheStoresWhicheverCellIsClicked(GameTestHelper h){
        var pallet=pallet(h);var cargo=pallet.cargo();cargo.insert(new ItemStack(Items.STONE),10);
        var p=player(h);var menu=chest(p);var near=NearbyStores.of(menu);menu.broadcastChanges();
        menu.setCarried(new ItemStack(Items.OAK_PLANKS,20));
        CargoMenu.click(menu,p,near,near.indexOf(new ItemStack(Items.STONE)),0,false);
        h.assertTrue(menu.getCarried().isEmpty()&&stored(cargo,Items.OAK_PLANKS)==20&&stored(cargo,Items.STONE)==10,"Planks clicked onto the stone's cell go onto the Pallet as a new entry");
        menu.setCarried(new ItemStack(Items.COBBLESTONE,4));
        CargoMenu.click(menu,p,near,near.indexOf(new ItemStack(Items.OAK_PLANKS)),1,false);
        h.assertTrue(menu.getCarried().getCount()==3&&stored(cargo,Items.COBBLESTONE)==1,"A right-click on another item's cell stacks one");
        cargo.insert(new ItemStack(Items.STONE),(long)(cargo.capacity()-cargo.load())-2);
        CargoMenu.click(menu,p,near,near.indexOf(new ItemStack(Items.STONE)),0,false);
        h.assertTrue(menu.getCarried().getCount()==1&&stored(cargo,Items.COBBLESTONE)==3,"Only what fits is stacked and the rest stays on the cursor: "+menu.getCarried().getCount()+" left");
        CargoMenu.click(menu,p,near,-1,0,false);
        h.assertTrue(menu.getCarried().getCount()==1&&stored(cargo,Items.COBBLESTONE)==3,"A full store takes nothing and the cursor keeps it");
        h.succeed();
    }

    @GameTest(template="industrial") public static void recipePlacementFillsTheGridFromHandPackAndNearbyStores(GameTestHelper h){
        var pallet=pallet(h);pallet.cargo().insert(new ItemStack(Items.OAK_PLANKS),2);
        var p=player(h);var pack=Backpack.ITEM.toStack();p.getData(Accessories.INVENTORY).setStackInSlot(0,pack);
        Backpack.worn(p).setItem(0,new ItemStack(Items.OAK_PLANKS,1));p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.OAK_PLANKS,1));
        var at=h.absolutePos(new BlockPos(7,1,9));h.getLevel().setBlockAndUpdate(at,Blocks.CRAFTING_TABLE.defaultBlockState());
        p.openMenu(new SimpleMenuProvider((id,inv,player)->new CraftingMenu(id,inv,ContainerLevelAccess.create(h.getLevel(),at)),Component.literal("Crafting")));
        var menu=(CraftingMenu)p.containerMenu;
        @SuppressWarnings("unchecked") var table=(RecipeHolder<CraftingRecipe>)h.getLevel().getRecipeManager().byKey(ResourceLocation.withDefaultNamespace("crafting_table")).orElseThrow();
        h.assertTrue(!p.getRecipeBook().contains(table),"The recipe is not unlocked in this player's recipe book");
        menu.handlePlacement(false,table,p);
        int placed=0;for(int i=1;i<10;i++)if(menu.getSlot(i).getItem().is(Items.OAK_PLANKS))placed+=menu.getSlot(i).getItem().getCount();
        h.assertTrue(placed==4&&menu.getSlot(0).getItem().is(Items.CRAFTING_TABLE),"Placement fills the grid with four planks and the table shows: "+placed);
        h.assertTrue(p.getOffhandItem().isEmpty()&&Backpack.worn(p).isEmpty()&&stored(pallet.cargo(),Items.OAK_PLANKS)==0,"The planks came from the Hand, the pack and the Pallet, none duplicated");
        h.succeed();
    }

    /** A crafting table beside the stores, opened with the Hand already full of cobblestone so crafted planks have nowhere on the player. */
    private static CraftingMenu fullHandedCrafting(GameTestHelper h,ServerPlayer p,int logs){
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.COBBLESTONE,BlockCarry.LIMIT));
        var at=h.absolutePos(new BlockPos(7,1,9));h.getLevel().setBlockAndUpdate(at,Blocks.CRAFTING_TABLE.defaultBlockState());
        p.openMenu(new SimpleMenuProvider((id,inv,player)->new CraftingMenu(id,inv,ContainerLevelAccess.create(h.getLevel(),at)),Component.literal("Crafting")));
        var menu=(CraftingMenu)p.containerMenu;menu.getSlot(1).set(new ItemStack(Items.OAK_LOG,logs));return menu;
    }
    private static int dropped(GameTestHelper h,ServerPlayer p,Item item){
        return h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,p.getBoundingBox().inflate(4),e->e.getItem().is(item)).stream().mapToInt(e->e.getItem().getCount()).sum();
    }

    @GameTest(template="industrial") public static void bulkCraftingStacksWhatTheHandCannotHoldOntoTheNearestStore(GameTestHelper h){
        var pallet=pallet(h);pallet.cargo().insert(new ItemStack(Items.STONE),10);
        var p=player(h);var menu=fullHandedCrafting(h,p,3);
        menu.clicked(0,0,ClickType.QUICK_MOVE,p);
        h.assertTrue(stored(pallet.cargo(),Items.OAK_PLANKS)==3&&menu.getSlot(1).getItem().isEmpty(),"Shift-clicking planks crafts all three logs onto the Pallet: "+stored(pallet.cargo(),Items.OAK_PLANKS));
        h.assertTrue(p.getOffhandItem().is(Items.COBBLESTONE)&&p.getOffhandItem().getCount()==BlockCarry.LIMIT&&dropped(h,p,Items.OAK_PLANKS)==0&&stored(pallet.cargo(),Items.STONE)==10,"The Hand and the Pallet's stone are untouched and nothing drops");
        h.succeed();
    }

    @GameTest(template="industrial") public static void bulkCraftingStopsWhenEveryStoreIsFull(GameTestHelper h){
        var pallet=pallet(h);var cargo=pallet.cargo();
        cargo.insert(new ItemStack(Items.STONE),(long)(cargo.capacity()-cargo.load())-2);
        var p=player(h);var menu=fullHandedCrafting(h,p,3);
        menu.clicked(0,0,ClickType.QUICK_MOVE,p);
        int stacked=(int)stored(cargo,Items.OAK_PLANKS),fell=dropped(h,p,Items.OAK_PLANKS),left=menu.getSlot(1).getItem().getCount();
        h.assertTrue(stacked==2&&fell==0&&left==1,"Room for two planks crafts two logs and the third stays on the table: "+stacked+" stacked, "+fell+" dropped, "+left+" left");
        h.succeed();
    }

    @GameTest(template="industrial") public static void bulkCraftingSkipsAStoreThePlayerMayNoLongerUse(GameTestHelper h){
        var pallet=pallet(h);var p=player(h);var menu=fullHandedCrafting(h,p,2);menu.broadcastChanges();
        var data=CivicData.get(h.getLevel().getServer());var c=new CivicData.Claim();
        c.at=new CivicData.Address(h.getLevel().dimension().location().toString(),h.absolutePos(new BlockPos(5,1,5)));c.owner=new CivicData.Owner(UUID.randomUUID(),false);c.radius=2;c.height=32;c.upkeep=1;c.coalUnit=3600000;c.energy=360000000;c.lastUpdate=System.currentTimeMillis();data.claims.put(c.at,c);data.rebuildIndex();
        try{
            menu.clicked(0,0,ClickType.QUICK_MOVE,p);
            h.assertTrue(stored(pallet.cargo(),Items.OAK_PLANKS)==0&&menu.getSlot(1).getItem().getCount()==2&&dropped(h,p,Items.OAK_PLANKS)==0,"A Pallet claimed after the menu opened takes no planks and no logs are spent");
        }finally{data.claims.remove(c.at);data.rebuildIndex();}
        h.succeed();
    }

    @GameTest(template="industrial") public static void bulkStonecuttingStacksOntoTheNearestStore(GameTestHelper h){
        var pallet=pallet(h);var p=player(h);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.COBBLESTONE,BlockCarry.LIMIT));
        var at=h.absolutePos(new BlockPos(7,1,9));h.getLevel().setBlockAndUpdate(at,Blocks.STONECUTTER.defaultBlockState());
        // Opened as the server opens it, less the initial sync: the stonecutter's recipe index is a data slot, which the stand-in connection cannot send.
        var menu=new StonecutterMenu(1,p.getInventory(),ContainerLevelAccess.create(h.getLevel(),at));p.containerMenu=menu;
        BlockCarry.attachMenu(menu,p.getInventory());NearbyStores.attach(menu,p);menu.getSlot(0).set(new ItemStack(Items.STONE,5));
        int bricks=-1;var recipes=menu.getRecipes();
        for(int i=0;i<recipes.size();i++)if(recipes.get(i).value().getResultItem(h.getLevel().registryAccess()).is(Items.STONE_BRICKS))bricks=i;
        h.assertTrue(bricks>=0&&menu.clickMenuButton(p,bricks),"The stonecutter offers stone bricks");
        menu.clicked(1,0,ClickType.QUICK_MOVE,p);
        h.assertTrue(stored(pallet.cargo(),Items.STONE_BRICKS)==5&&menu.getSlot(0).getItem().isEmpty(),"Shift-clicking cuts all five stone into bricks on the Pallet: "+stored(pallet.cargo(),Items.STONE_BRICKS));
        h.succeed();
    }

    @GameTest(template="industrial") public static void craftingTakesTheItemItAsksForAfterTheListChanged(GameTestHelper h){
        var pallet=pallet(h);pallet.cargo().insert(new ItemStack(Items.OAK_PLANKS),4);pallet.cargo().insert(new ItemStack(Items.COBBLESTONE),4);
        var barrow=barrow(h);barrow.cargo().insert(new ItemStack(Items.STONE),1);
        var p=player(h);var menu=chest(p);menu.broadcastChanges();
        h.assertTrue(NearbyStores.of(menu).indexOf(new ItemStack(Items.OAK_PLANKS))==1,"The list holds the wheelbarrow's stone, then the Pallet's planks and cobblestone");
        // The nearer wheelbarrow leaves before the next synchronization, so every later entry of the list last seen moves up one.
        barrow.discard();
        int taken=CraftingSources.take(p,new ItemStack(Items.OAK_PLANKS),2);
        h.assertTrue(taken==2&&stored(pallet.cargo(),Items.OAK_PLANKS)==2&&stored(pallet.cargo(),Items.COBBLESTONE)==4,
                "Planks for the grid come from the Pallet's planks and its cobblestone stays: took "+taken+", planks "+stored(pallet.cargo(),Items.OAK_PLANKS)+", cobblestone "+stored(pallet.cargo(),Items.COBBLESTONE));
        h.succeed();
    }
}
