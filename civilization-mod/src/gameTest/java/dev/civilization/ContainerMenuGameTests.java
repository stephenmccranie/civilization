package dev.civilization;

import com.mojang.authlib.GameProfile;
import java.util.*;
import java.util.function.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

/**
 * Every container's real menu under the same carrying rules. A player with a full Hand, a worn pack, stocked pockets
 * and a Pallet in reach opens each container, shift-clicks everything across and back and takes from the Nearby panel:
 * each item's total never changes, building blocks never reach the pockets, the Hand and the pack keep their limits,
 * and once a claim shuts the player out no click moves anything.
 */
@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class ContainerMenuGameTests {
    private static final BlockPos PALLET=new BlockPos(5,1,5),CONTAINER=new BlockPos(8,1,8);
    /** One container under test: what opens it, the inventory it shows, and what the docs say it holds after the deposits. */
    private record Case(String name,Supplier<Block> block,boolean armful){}
    private static final List<Case> CASES=List.of(
            new Case("chest",()->Blocks.CHEST,true),new Case("trapped chest",()->Blocks.TRAPPED_CHEST,true),new Case("barrel",()->Blocks.BARREL,true),
            new Case("chest minecart",null,true),
            new Case("hopper",()->Blocks.HOPPER,false),new Case("dropper",()->Blocks.DROPPER,false),new Case("dispenser",()->Blocks.DISPENSER,false),
            new Case("shulker box",()->Blocks.SHULKER_BOX,false),new Case("crafter",()->Blocks.CRAFTER,false),new Case("brewing stand",()->Blocks.BREWING_STAND,false),
            new Case("retired furnace",()->Blocks.FURNACE,false),
            new Case("Cooking Stove",()->CookingContent.STATION.get(),false),new Case("Smithy",()->WorkshopContent.SMITHY.get(),false),
            new Case("Coal Engine",()->IndustrialContent.COAL_ENGINE.get(),false),new Case("Prototype Stove",()->PrototypeStoveContent.STOVE.get(),false));
    private static final List<Item> GOODS=List.of(Items.STONE,Items.COBBLESTONE,Items.OAK_PLANKS,Items.APPLE,Items.STICK,KilnContent.MINERAL_COAL.get());

    private static ServerPlayer player(GameTestHelper h){
        var profile=new GameProfile(UUID.randomUUID(),"container-menus");
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,ClientInformation.createDefault());
        p.connection=new FakePlayer(h.getLevel(),profile).connection;p.setGameMode(GameType.SURVIVAL);p.setPos(h.absolutePos(new BlockPos(7,1,7)).getCenter());return p;
    }
    /** Opens the menu as the server does, without the data-slot packets a test connection cannot send. */
    private static AbstractContainerMenu open(ServerPlayer p,MenuProvider provider){
        var menu=provider.createMenu(1,p.getInventory(),p);
        BlockCarry.attachMenu(menu,p.getInventory());Backpack.attachMenu(menu,p);NearbyStores.attach(menu,p);
        p.containerMenu=menu;return menu;
    }
    private static int count(Container c,Item item){int n=0;for(int i=0;i<c.getContainerSize();i++)if(c.getItem(i).is(item))n+=c.getItem(i).getCount();return n;}
    private static int blocks(Container c){int n=0;for(int i=0;i<c.getContainerSize();i++)if(BlockCarry.carried(c.getItem(i)))n+=c.getItem(i).getCount();return n;}
    private static long stored(CargoInventory store,Item item){int at=store.indexOf(new ItemStack(item));return at<0?0:store.count(at);}
    /** Where every one of the goods is: the container, the player's pockets and Hand, the pack, the cursor and the Pallet. */
    private static List<Long> census(Container container,ServerPlayer p,AbstractContainerMenu menu,CargoInventory pallet){
        var all=new ArrayList<Long>();
        for(var item:GOODS){
            all.add((long)count(container,item));all.add((long)count(p.getInventory(),item));all.add((long)count(Backpack.worn(p),item));
            all.add(menu.getCarried().is(item)?(long)menu.getCarried().getCount():0L);all.add(stored(pallet,item));
        }
        return all;
    }
    private static long total(List<Long> census,int good){long n=0;for(int i=0;i<5;i++)n+=census.get(good*5+i);return n;}
    /** Shift-clicks every slot of the menu that holds something and belongs, or does not belong, to the player. */
    private static void shiftClick(AbstractContainerMenu menu,ServerPlayer p,boolean players){
        for(int i=0;i<menu.slots.size();i++){
            var slot=menu.slots.get(i);boolean own=slot.container instanceof Inventory||slot instanceof Backpack.PackSlot;
            if(own==players&&slot.hasItem())menu.clicked(i,0,ClickType.QUICK_MOVE,p);
        }
    }
    private static void rules(GameTestHelper h,String when,Case c,Container container,ServerPlayer p,List<Long> before,List<Long> now){
        for(int good=0;good<GOODS.size();good++)h.assertTrue(total(before,good)==total(now,good),c.name()+", "+when+": "+GOODS.get(good)+" went from "+total(before,good)+" to "+total(now,good)+" "+now);
        for(int i=0;i<Inventory.INVENTORY_SIZE;i++)h.assertTrue(!BlockCarry.carried(p.getInventory().getItem(i)),c.name()+", "+when+": building blocks reached pocket "+i+": "+p.getInventory().getItem(i));
        var hand=p.getOffhandItem();h.assertTrue(!BlockCarry.carried(hand)||hand.getCount()<=BlockCarry.LIMIT,c.name()+", "+when+": the Hand holds more than an armful: "+hand);
        h.assertTrue(blocks(Backpack.worn(p))<=Backpack.BLOCKS,c.name()+", "+when+": the pack holds more than its block cap: "+blocks(Backpack.worn(p)));
        h.assertTrue(!c.armful()||blocks(container)<=BlockCarry.LIMIT,c.name()+", "+when+": holds more than an armful of building blocks: "+blocks(container));
    }

    @GameTest(template="industrial") public static void everyContainerMenuConservesGoodsKeepsTheCarryingLimitsAndRechecksAccess(GameTestHelper h){
        var level=h.getLevel();
        for(int x=3;x<=9;x++)for(int z=3;z<=9;z++)for(int y=0;y<=4;y++)h.setBlock(new BlockPos(x,y,z),y==0?Blocks.STONE:Blocks.AIR);
        h.setBlock(PALLET,CargoContent.STOCKPILE.get());var pallet=((StockpileEntity)h.getBlockEntity(PALLET)).cargo();var at=h.absolutePos(CONTAINER);
        for(var c:CASES){
            MinecartChest cart=null;Container container;MenuProvider provider;
            if(c.block()==null){cart=new MinecartChest(level,at.getX()+.5,at.getY(),at.getZ()+.5);level.addFreshEntity(cart);container=cart;provider=cart;}
            else{level.setBlockAndUpdate(at,c.block().get().defaultBlockState());var entity=level.getBlockEntity(at);container=(Container)entity;provider=(MenuProvider)entity;}
            var p=player(h);p.getData(Accessories.INVENTORY).setStackInSlot(0,Backpack.ITEM.toStack());var pack=Backpack.worn(p);
            p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STONE,BlockCarry.LIMIT));
            pack.setItem(0,new ItemStack(Items.COBBLESTONE,20));pack.setItem(1,new ItemStack(Items.APPLE,10));
            p.getInventory().setItem(9,new ItemStack(Items.STICK,5));p.getInventory().setItem(0,KilnContent.MINERAL_COAL.toStack(6));
            pallet.insert(new ItemStack(Items.OAK_PLANKS),40);
            var menu=open(p,provider);var near=NearbyStores.of(menu);menu.broadcastChanges();
            h.assertTrue(BlockCarry.handSlot(menu)!=null&&menu.slots.stream().filter(s->s instanceof Backpack.PackSlot).count()==Backpack.SLOTS&&near!=null&&near.stores()==1,
                    c.name()+": its menu shows the Hand, the pack and the Pallet in reach");
            var start=census(container,p,menu,pallet);

            shiftClick(menu,p,true);
            rules(h,"after shift-clicking the Hand, the pack and the pockets",c,container,p,start,census(container,p,menu,pallet));
            if(c.armful())h.assertTrue(count(container,Items.STONE)==BlockCarry.LIMIT&&count(pack,Items.COBBLESTONE)==20&&count(container,Items.APPLE)==10&&count(container,Items.STICK)==5&&count(container,KilnContent.MINERAL_COAL.get())==6,
                    c.name()+": takes the Hand's armful and every ordinary stack, and leaves the pack's blocks packed: "+census(container,p,menu,pallet));

            CargoMenu.click(menu,p,near,near.indexOf(new ItemStack(Items.OAK_PLANKS)),0,true);
            rules(h,"after shift-clicking the Pallet's planks from Nearby",c,container,p,start,census(container,p,menu,pallet));
            CargoMenu.click(menu,p,near,near.indexOf(new ItemStack(Items.OAK_PLANKS)),0,false);
            if(!menu.getCarried().isEmpty())CargoMenu.click(menu,p,near,-1,0,false);
            rules(h,"after lifting a stack from Nearby and stacking it back",c,container,p,start,census(container,p,menu,pallet));

            shiftClick(menu,p,false);
            rules(h,"after shift-clicking its own slots back",c,container,p,start,census(container,p,menu,pallet));

            var d=CivicData.get(level.getServer());var claim=new CivicData.Claim();claim.at=new CivicData.Address("minecraft:overworld",at);claim.owner=new CivicData.Owner(UUID.randomUUID(),false);
            claim.radius=4;claim.height=32;claim.upkeep=1;claim.coalUnit=3600000;claim.energy=7200000;claim.lastUpdate=System.currentTimeMillis();
            p.getInventory().setItem(10,new ItemStack(Items.STICK,3));container.setItem(0,new ItemStack(Items.APPLE,2));var shut=census(container,p,menu,pallet);
            d.claims.put(claim.at,claim);d.rebuildIndex();
            try{
                menu.clicked(0,0,ClickType.QUICK_MOVE,p);menu.clicked(0,0,ClickType.PICKUP,p);shiftClick(menu,p,true);
                menu.broadcastChanges();CargoMenu.click(menu,p,near,near.indexOf(new ItemStack(Items.OAK_PLANKS)),0,true);
                h.assertTrue(shut.equals(census(container,p,menu,pallet))&&near.stores()==0,c.name()+": a player shut out by a claim moves nothing through the open menu: "+shut+" became "+census(container,p,menu,pallet));
            }finally{d.claims.remove(claim.at);d.rebuildIndex();}

            p.closeContainer();container.clearContent();
            if(cart!=null)cart.discard();else level.removeBlock(at,false);
            for(int i=pallet.size()-1;i>=0;i--)pallet.remove(i,pallet.count(i));
        }
        h.succeed();
    }
}
