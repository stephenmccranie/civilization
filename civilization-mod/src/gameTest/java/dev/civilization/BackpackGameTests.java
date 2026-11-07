package dev.civilization;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class BackpackGameTests {
    private static ServerPlayer player(GameTestHelper h){
        var profile=new GameProfile(UUID.randomUUID(),"backpacker");
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,ClientInformation.createDefault());
        p.connection=new FakePlayer(h.getLevel(),profile).connection;p.setGameMode(GameType.SURVIVAL);p.setPos(h.absolutePos(new BlockPos(7,1,7)).getCenter());return p;
    }
    private static StockpileEntity pallet(GameTestHelper h){
        for(int x=3;x<=9;x++)for(int z=3;z<=9;z++)for(int y=0;y<=4;y++)h.setBlock(new BlockPos(x,y,z),y==0?Blocks.STONE:Blocks.AIR);
        h.setBlock(new BlockPos(5,1,5),CargoContent.STOCKPILE.get());return (StockpileEntity)h.getBlockEntity(new BlockPos(5,1,5));
    }
    private static Backpack.Contents wear(ServerPlayer p){
        var pack=Backpack.ITEM.toStack();p.getData(Accessories.INVENTORY).setStackInSlot(0,pack);return Backpack.worn(p);
    }
    private static int held(Backpack.Contents pack,Item item){int n=0;for(int i=0;i<pack.getContainerSize();i++)if(pack.getItem(i).is(item))n+=pack.getItem(i).getCount();return n;}
    private static long stored(CargoInventory store,Item item){int at=store.indexOf(new ItemStack(item));return at<0?0:store.count(at);}
    private static void bind(GameTestHelper h,ServerPlayer p,CargoHolder store){
        new CargoMenu(1,p.getInventory(),store).clickMenuButton(p,4);
        h.assertTrue(Backpack.binding(p,store)==2&&Backpack.worn(p).pack().has(Backpack.SUPPLY.get()),"The store's Bind button binds the worn pack");
    }

    @GameTest(template="industrial") public static void everyPackHasEightSlotsAndTheBlockCap(GameTestHelper h){
        var made=Backpack.ITEM.toStack();EquipmentGrade.ensure(made,h.getLevel().random);
        h.assertTrue(!EquipmentGrade.hasGrade(made)&&!made.isDamageableItem(),"A finished backpack has no grade and no durability");
        var p=player(h);var pack=wear(p);var stone=new ItemStack(Items.STONE,32);var more=new ItemStack(Items.STONE,8);pack.insert(stone);pack.insert(more);
        h.assertTrue(pack.getContainerSize()==Backpack.SLOTS&&held(pack,Items.STONE)==Backpack.BLOCKS&&stone.isEmpty()&&more.getCount()==8&&pack.blockRoom()==0,"A pack has 8 slots and stops at 32 blocks");
        var apples=new ItemStack(Items.APPLE,32);pack.insert(apples);
        h.assertTrue(apples.isEmpty()&&held(pack,Items.APPLE)==32,"Ordinary goods fill the slots blocks leave free");
        h.assertTrue(!pack.canPlaceItem(0,Backpack.ITEM.toStack())&&!pack.canPlaceItem(0,new ItemStack(Items.DIRT)),"Neither another backpack nor blocks beyond the cap go in");
        var saved=p.getData(Accessories.INVENTORY).serializeNBT(h.getLevel().registryAccess());
        var loaded=new Accessories.AccessoryInventory();loaded.deserializeNBT(h.getLevel().registryAccess(),saved);
        h.assertTrue(held(loaded.backpack(),Items.STONE)==Backpack.BLOCKS&&held(loaded.backpack(),Items.APPLE)==32,"Contents are saved on the worn item and survive a reload");
        h.succeed();
    }

    @GameTest(template="industrial") public static void pickupsFillTheHandThenThePack(GameTestHelper h){
        var p=player(h);var pack=wear(p);
        var stone=new ItemEntity(h.getLevel(),0,2,0,new ItemStack(Items.STONE,32));stone.setNoPickUpDelay();stone.playerTouch(p);
        var more=new ItemEntity(h.getLevel(),0,2,0,new ItemStack(Items.STONE,16));more.setNoPickUpDelay();more.playerTouch(p);
        h.assertTrue(p.getOffhandItem().getCount()==BlockCarry.LIMIT&&held(pack,Items.STONE)==Backpack.BLOCKS&&!more.isRemoved()&&more.getItem().getCount()==8,"Blocks fill the 8-block hand, then the pack to its 32-block cap; the rest stays on the ground");
        for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STICK,32));
        var apples=new ItemEntity(h.getLevel(),0,2,0,new ItemStack(Items.APPLE,5));apples.setNoPickUpDelay();apples.playerTouch(p);
        h.assertTrue(apples.isRemoved()&&held(pack,Items.APPLE)==5,"Goods the full pockets cannot take go into the pack");
        h.succeed();
    }

    @GameTest(template="industrial") public static void placingRefillsTheHandAndTopsUpPackStacksFromTheBoundStore(GameTestHelper h){
        var store=pallet(h);store.cargo().insert(new ItemStack(Items.STONE),64);store.cargo().insert(new ItemStack(Items.GLASS),10);
        var p=player(h);var pack=wear(p);pack.setItem(0,new ItemStack(Items.STONE,5));pack.setItem(1,new ItemStack(Items.OAK_PLANKS,2));
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STONE,BlockCarry.LIMIT));bind(h,p,store);
        long before=stored(store.cargo(),Items.STONE)+held(pack,Items.STONE)+p.getOffhandItem().getCount();
        var at=h.absolutePos(new BlockPos(8,1,8));
        var placed=p.getOffhandItem().useOn(new UseOnContext(p,InteractionHand.OFF_HAND,new BlockHitResult(Vec3.atCenterOf(at.below()),Direction.UP,at.below(),false)));
        h.assertTrue(placed.consumesAction()&&h.getLevel().getBlockState(at).is(Blocks.STONE)&&p.getOffhandItem().getCount()==BlockCarry.LIMIT-1,"A real placement uses one stone from the hand");
        Backpack.tick(new PlayerTickEvent.Post(p));
        long after=stored(store.cargo(),Items.STONE)+held(pack,Items.STONE)+p.getOffhandItem().getCount();
        h.assertTrue(p.getOffhandItem().getCount()==BlockCarry.LIMIT&&held(pack,Items.STONE)==Backpack.BLOCKS-2,"The hand is topped off and the pack's stone stack topped up to the cap");
        h.assertTrue(after==before-1,"Exactly the placed block leaves the store, pack and hand: "+before+" then "+after);
        h.assertTrue(held(pack,Items.GLASS)==0&&held(pack,Items.OAK_PLANKS)==2&&stored(store.cargo(),Items.GLASS)==10,"The pack takes on no block it did not already hold");
        pack.setItem(0,new ItemStack(Items.STONE,3));p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STONE,1));Backpack.tick(new PlayerTickEvent.Post(p));
        var next=at.east();p.getOffhandItem().useOn(new UseOnContext(p,InteractionHand.OFF_HAND,new BlockHitResult(Vec3.atCenterOf(next.below()),Direction.UP,next.below(),false)));
        h.assertTrue(p.getOffhandItem().isEmpty(),"Placing the last block empties the hand");
        Backpack.tick(new PlayerTickEvent.Post(p));
        h.assertTrue(p.getOffhandItem().is(Items.STONE)&&p.getOffhandItem().getCount()==BlockCarry.LIMIT,"An emptied hand refills with the block it last held");
        h.assertTrue(held(pack,Items.STONE)==Backpack.BLOCKS-2,"The hand draws on the store first, so the pack's own stack stays and is topped up");
        h.succeed();
    }

    @GameTest(template="industrial") public static void supplyStopsOutOfReachUnboundAndWithoutAccess(GameTestHelper h){
        var store=pallet(h);store.cargo().insert(new ItemStack(Items.STONE),64);
        var p=player(h);wear(p);bind(h,p,store);var stone=new ItemStack(Items.STONE);
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STONE,3));
        var near=p.position();p.setPos(store.cargoPosition().add(Backpack.RADIUS+1,0,0));Backpack.refill(p,stone);
        h.assertTrue(p.getOffhandItem().getCount()==3&&stored(store.cargo(),Items.STONE)==64,"Nothing comes from a store beyond reach");
        p.setPos(near);Backpack.unbind(p);Backpack.refill(p,stone);
        h.assertTrue(p.getOffhandItem().getCount()==3&&stored(store.cargo(),Items.STONE)==64&&Backpack.binding(p,store)==1,"Nothing comes once unbound");
        bind(h,p,store);Backpack.refill(p,stone);
        h.assertTrue(p.getOffhandItem().getCount()==BlockCarry.LIMIT&&stored(store.cargo(),Items.STONE)==59,"Bound again within reach, the hand refills");
        var data=CivicData.get(h.getLevel().getServer());var claim=new CivicData.Claim();
        claim.at=new CivicData.Address(h.getLevel().dimension().location().toString(),store.getBlockPos());claim.owner=new CivicData.Owner(UUID.randomUUID(),false);claim.radius=8;claim.height=32;claim.upkeep=1;claim.coalUnit=3600000;claim.energy=claim.coalUnit*200;claim.lastUpdate=System.currentTimeMillis();data.claims.put(claim.at,claim);data.rebuildIndex();
        try{p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STONE,3));Backpack.refill(p,stone);h.assertTrue(p.getOffhandItem().getCount()==3&&stored(store.cargo(),Items.STONE)==59,"Access is checked at the pull: another owner's claim refuses it");}
        finally{data.claims.remove(claim.at);data.rebuildIndex();}
        h.succeed();
    }

    @GameTest(template="industrial") public static void aBoundWheelbarrowSuppliesWhereverItIsParked(GameTestHelper h){
        pallet(h);var barrow=new WheelbarrowEntity(WheelbarrowContent.ENTITY.get(),h.getLevel());var at=h.absolutePos(new BlockPos(7,1,5));
        barrow.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);h.getLevel().addFreshEntity(barrow);barrow.cargo().insert(new ItemStack(Items.COBBLESTONE),30);
        var p=player(h);wear(p);bind(h,p,barrow);var cobble=new ItemStack(Items.COBBLESTONE);
        barrow.setPos(barrow.position().add(0,0,-2));Backpack.refill(p,cobble);
        h.assertTrue(p.getOffhandItem().getCount()==BlockCarry.LIMIT&&barrow.cargo().count(0)==22,"The binding follows the wheelbarrow by identity as it moves");
        var handler=player(h);handler.setPos(barrow.handlerPosition(barrow.position(),barrow.getYRot()));barrow.interact(handler,InteractionHand.MAIN_HAND);
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.COBBLESTONE,2));Backpack.refill(p,cobble);
        h.assertTrue(handler.getVehicle()==barrow&&p.getOffhandItem().getCount()==2&&barrow.cargo().count(0)==22,"A wheelbarrow being pushed supplies nothing");
        handler.stopRiding();barrow.discard();Backpack.refill(p,cobble);
        h.assertTrue(p.getOffhandItem().getCount()==2,"A store that is gone supplies nothing");
        h.succeed();
    }

    @GameTest(template="industrial") public static void aLoadedPackStaysOnAndOnlyOneIsWorn(GameTestHelper h){
        var p=player(h);var pack=wear(p);var worn=p.getData(Accessories.INVENTORY);
        h.assertTrue(!worn.isItemValid(1,Backpack.ITEM.toStack())&&worn.isItemValid(1,FrontierContent.GEIGER_COUNTER.get().getDefaultInstance()),"One backpack at a time, beside other accessories");
        var slot=new AccessorySlot(p,0,0,0);pack.setItem(0,new ItemStack(Items.APPLE));
        h.assertTrue(!slot.mayPickup(p),"A pack holding anything cannot be taken off");
        pack.setItem(0,ItemStack.EMPTY);h.assertTrue(slot.mayPickup(p),"An empty pack comes off");
        h.succeed();
    }

    @GameTest(template="industrial") public static void theInventoryShowsThePackAndMovesBlocksBetweenHandAndPack(GameTestHelper h){
        var p=player(h);var menu=p.inventoryMenu;
        h.assertTrue(packSlots(menu)==Backpack.SLOTS&&activePackSlots(menu)==0,"Without a pack the inventory's pack slots stand empty and hidden");
        var pack=wear(p);pack.setItem(0,new ItemStack(Items.STONE,12));pack.setItem(1,new ItemStack(Items.APPLE,3));
        h.assertTrue(activePackSlots(menu)==Backpack.SLOTS,"Putting on a pack shows its eight slots in the open inventory");
        menu.clicked(slot(menu,0).index,0,ClickType.QUICK_MOVE,p);
        h.assertTrue(p.getOffhandItem().getCount()==BlockCarry.LIMIT&&held(pack,Items.STONE)==4,"Shift-clicking pack blocks fills the Hand and keeps the rest");
        menu.clicked(BlockCarry.handSlot(menu).index,0,ClickType.QUICK_MOVE,p);
        h.assertTrue(p.getOffhandItem().isEmpty()&&held(pack,Items.STONE)==12,"Shift-clicking the Hand stows its blocks in the pack");
        menu.clicked(slot(menu,1).index,0,ClickType.QUICK_MOVE,p);
        h.assertTrue(p.getInventory().countItem(Items.APPLE)==3&&held(pack,Items.APPLE)==0,"Shift-clicking pack goods moves them to the pockets");
        h.succeed();
    }

    private static long packSlots(net.minecraft.world.inventory.AbstractContainerMenu menu){return menu.slots.stream().filter(s->s instanceof Backpack.PackSlot).count();}
    private static long activePackSlots(net.minecraft.world.inventory.AbstractContainerMenu menu){return menu.slots.stream().filter(s->s instanceof Backpack.PackSlot&&s.isActive()).count();}
    private static net.minecraft.world.inventory.Slot slot(net.minecraft.world.inventory.AbstractContainerMenu menu,int pack){return menu.slots.stream().filter(s->s instanceof Backpack.PackSlot&&s.getContainerSlot()==pack).findFirst().orElseThrow();}
    @GameTest(template="industrial") public static void thePackShowsBesideEveryMenuAndFillsAfterThePockets(GameTestHelper h){
        var store=pallet(h);var p=player(h);var pack=wear(p);var chest=new net.minecraft.world.SimpleContainer(27);
        p.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,player)->net.minecraft.world.inventory.ChestMenu.threeRows(id,inv,chest),net.minecraft.network.chat.Component.literal("Chest")));
        var menu=p.containerMenu;p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STONE,BlockCarry.LIMIT));
        h.assertTrue(menu instanceof net.minecraft.world.inventory.ChestMenu&&packSlots(menu)==Backpack.SLOTS&&activePackSlots(menu)==Backpack.SLOTS,"A chest menu shows the worn pack's eight slots");
        chest.setItem(0,new ItemStack(Items.APPLE,32));menu.clicked(0,0,ClickType.QUICK_MOVE,p);
        h.assertTrue(p.getInventory().countItem(Items.APPLE)==32&&held(pack,Items.APPLE)==0,"Shift-clicked goods fill the pockets first");
        for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STICK,32));
        chest.setItem(0,new ItemStack(Items.APPLE,32));menu.clicked(0,0,ClickType.QUICK_MOVE,p);
        h.assertTrue(chest.getItem(0).isEmpty()&&held(pack,Items.APPLE)==32,"With the pockets full they go into the pack");
        pack.insert(new ItemStack(Items.STONE,8));chest.setItem(1,new ItemStack(Items.STONE,32));menu.clicked(1,0,ClickType.QUICK_MOVE,p);
        h.assertTrue(chest.getItem(1).getCount()==8&&p.getOffhandItem().getCount()==BlockCarry.LIMIT&&held(pack,Items.STONE)==Backpack.BLOCKS,"With the hand full, blocks go into the pack up to its cap and the rest stays: chest "+chest.getItem(1)+", hand "+p.getOffhandItem()+", pack "+held(pack,Items.STONE));
        // As the server's menu opening does: the Hand, then the pack.
        menu=new CargoMenu(2,p.getInventory(),store);BlockCarry.attachMenu(menu,p.getInventory());Backpack.attachMenu(menu,p);int first=-1;
        for(int i=0;i<menu.slots.size();i++)if(menu.slots.get(i) instanceof Backpack.PackSlot s&&s.getItem().is(Items.STONE)){first=i;break;}
        h.assertTrue(menu instanceof CargoMenu&&activePackSlots(menu)==Backpack.SLOTS&&first>=0,"A Pallet menu shows the pack too");
        menu.clicked(first,0,ClickType.QUICK_MOVE,p);
        h.assertTrue(held(pack,Items.STONE)==0&&stored(store.cargo(),Items.STONE)==Backpack.BLOCKS,"Shift-clicking a pack stack puts it on the Pallet");
        Backpack.attachMenu(p.inventoryMenu,p);
        h.assertTrue(packSlots(p.inventoryMenu)==Backpack.SLOTS,"The inventory shows the pack's slots once");
        h.succeed();
    }

    @GameTest(template="industrial") public static void deathDropsThePackAndItsContentsSeparately(GameTestHelper h){
        var p=player(h);var pack=wear(p);pack.setItem(0,new ItemStack(Items.STONE,10));pack.setItem(1,new ItemStack(Items.APPLE,3));
        var drops=new ArrayList<ItemEntity>();NeoForge.EVENT_BUS.post(new LivingDropsEvent(p,h.getLevel().damageSources().generic(),drops,false));
        h.assertTrue(drops.stream().anyMatch(d->d.getItem().is(Items.STONE)&&d.getItem().getCount()==10)&&drops.stream().anyMatch(d->d.getItem().is(Items.APPLE)&&d.getItem().getCount()==3),"The contents drop as ordinary stacks");
        h.assertTrue(drops.stream().anyMatch(d->d.getItem().is(Backpack.ITEM)&&!BlockCarry.filledContainer(d.getItem()))&&Backpack.worn(p)==null,"The pack drops empty, so it can be picked up again");
        h.succeed();
    }
}
