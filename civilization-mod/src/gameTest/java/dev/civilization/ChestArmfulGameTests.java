package dev.civilization;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class ChestArmfulGameTests {
    /** A survival player whose action-bar messages the test can read. */
    private static final class Player extends ServerPlayer {
        final List<String> said=new ArrayList<>();
        Player(GameTestHelper h,GameProfile profile){super(h.getLevel().getServer(),h.getLevel(),profile,ClientInformation.createDefault());}
        @Override public void displayClientMessage(Component message,boolean overlay){said.add(message.getString());}
    }
    private static Player player(GameTestHelper h){
        var profile=new GameProfile(UUID.randomUUID(),"chest-armful");var p=new Player(h,profile);
        p.connection=new FakePlayer(h.getLevel(),profile).connection;p.setGameMode(GameType.SURVIVAL);p.setPos(h.absolutePos(new BlockPos(5,2,5)).getCenter());return p;
    }
    private static <T extends BlockEntity> T place(GameTestHelper h,BlockPos at,Block block){
        var pos=h.absolutePos(at);h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState());@SuppressWarnings("unchecked") var entity=(T)h.getLevel().getBlockEntity(pos);return entity;
    }
    private static int blocks(Container c){int n=0;for(int i=0;i<c.getContainerSize();i++)if(BlockCarry.carried(c.getItem(i)))n+=c.getItem(i).getCount();return n;}
    private static int count(Container c,Item item){int n=0;for(int i=0;i<c.getContainerSize();i++)if(c.getItem(i).is(item))n+=c.getItem(i).getCount();return n;}
    private static int hand(AbstractContainerMenu menu,ServerPlayer p){
        for(int i=0;i<menu.slots.size();i++)if(menu.slots.get(i).container==p.getInventory()&&menu.slots.get(i).getContainerSlot()==BlockCarry.OFFHAND)return i;
        throw new IllegalStateException("The menu has no Hand slot");
    }
    private static void hopperTick(HopperBlockEntity hopper){hopper.setCooldown(0);HopperBlockEntity.pushItemsTick(hopper.getLevel(),hopper.getBlockPos(),hopper.getBlockState(),hopper);}
    private static HopperBlockEntity hopperOver(GameTestHelper h,BlockPos chest,ItemStack stack){
        var pos=h.absolutePos(chest.above());h.getLevel().setBlockAndUpdate(pos,Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING,Direction.DOWN));
        var hopper=(HopperBlockEntity)h.getLevel().getBlockEntity(pos);hopper.setItem(0,stack);return hopper;
    }

    @GameTest(template="industrial") public static void clicksShiftClicksAndHoppersStopAtTheArmful(GameTestHelper h){
        var at=new BlockPos(3,1,3);ChestBlockEntity chest=place(h,at,Blocks.CHEST);chest.setItem(0,new ItemStack(Items.DIRT,3));
        var p=player(h);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STONE,BlockCarry.LIMIT));p.openMenu(chest);var menu=p.containerMenu;int hand=hand(menu,p);
        menu.clicked(hand,0,ClickType.QUICK_MOVE,p);
        h.assertTrue(blocks(chest)==BlockCarry.LIMIT&&count(chest,Items.STONE)==5&&p.getOffhandItem().getCount()==3,"Shift-click from the Hand fills the chest to 8 building blocks and leaves the rest in hand: chest "+blocks(chest)+", hand "+p.getOffhandItem());
        h.assertTrue(p.said.size()==1&&p.said.getFirst().startsWith("Building blocks 8/8"),"The refused remainder is explained on the action bar: "+p.said);
        menu.clicked(hand,0,ClickType.PICKUP,p);menu.clicked(5,0,ClickType.PICKUP,p);
        h.assertTrue(menu.getCarried().getCount()==3&&chest.getItem(5).isEmpty()&&p.said.size()==2,"A click cannot put a ninth block in: cursor "+menu.getCarried());
        menu.clicked(5,1,ClickType.PICKUP,p);
        h.assertTrue(menu.getCarried().getCount()==3&&chest.getItem(5).isEmpty(),"Nor can a right-click place one");
        menu.clicked(hand,0,ClickType.PICKUP,p);
        var hopper=hopperOver(h,at,new ItemStack(Items.COBBLESTONE,2));hopperTick(hopper);
        h.assertTrue(hopper.getItem(0).getCount()==2&&blocks(chest)==BlockCarry.LIMIT,"A hopper cannot push a block into a full chest");
        hopper.setItem(0,new ItemStack(Items.APPLE,2));hopperTick(hopper);
        h.assertTrue(hopper.getItem(0).getCount()==1&&count(chest,Items.APPLE)==1,"A hopper still feeds ordinary goods");
        var handler=h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,chest.getBlockPos(),Direction.UP);
        h.assertTrue(handler.insertItem(9,new ItemStack(Items.STONE),false).getCount()==1&&handler.insertItem(9,new ItemStack(Items.BUCKET),false).getCount()==1,"Automation adds neither a block nor a bucket");
        menu.clicked(1,0,ClickType.QUICK_MOVE,p);
        h.assertTrue(p.getOffhandItem().getCount()==BlockCarry.LIMIT&&blocks(chest)==3,"Taking blocks out still works: hand "+p.getOffhandItem());
        hopper.setItem(0,new ItemStack(Items.COBBLESTONE,1));hopperTick(hopper);
        h.assertTrue(hopper.isEmpty()&&blocks(chest)==4,"Once there is room a hopper feeds blocks again");
        h.succeed();
    }

    @GameTest(template="industrial") public static void ordinaryGoodsKeepTheirStacks(GameTestHelper h){
        ChestBlockEntity chest=place(h,new BlockPos(3,1,3),Blocks.CHEST);
        var p=player(h);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STONE,BlockCarry.LIMIT));p.openMenu(chest);var menu=p.containerMenu;
        menu.clicked(hand(menu,p),0,ClickType.QUICK_MOVE,p);
        for(int i=0;i<9;i++)p.getInventory().setItem(i,new ItemStack(i%2==0?Items.APPLE:Items.STICK,32));
        for(int i=0;i<9;i++)menu.clicked(54+i,0,ClickType.QUICK_MOVE,p);
        h.assertTrue(blocks(chest)==BlockCarry.LIMIT&&count(chest,Items.APPLE)==5*32&&count(chest,Items.STICK)==4*32&&p.said.isEmpty(),"A chest full of blocks still takes 32-item stacks of other goods, with nothing to explain");
        menu.setCarried(new ItemStack(Items.IRON_PICKAXE));menu.clicked(20,0,ClickType.PICKUP,p);
        h.assertTrue(chest.getItem(20).is(Items.IRON_PICKAXE)&&menu.getCarried().isEmpty(),"Tools go in by click");
        h.succeed();
    }

    @GameTest(template="industrial") public static void anOverfullChestEmptiesButTakesNoMore(GameTestHelper h){
        var at=new BlockPos(3,1,3);ChestBlockEntity chest=place(h,at,Blocks.CHEST);chest.setItem(0,new ItemStack(Items.STONE,20));chest.setItem(1,new ItemStack(Items.WATER_BUCKET));
        var p=player(h);p.openMenu(chest);var menu=p.containerMenu;int hand=hand(menu,p);
        h.assertTrue(blocks(chest)==21,"Stock from before the limit is kept, every block and bucket");
        menu.clicked(0,0,ClickType.QUICK_MOVE,p);
        h.assertTrue(p.getOffhandItem().getCount()==BlockCarry.LIMIT&&count(chest,Items.STONE)==12,"Blocks come out into the hand as usual");
        menu.clicked(hand,0,ClickType.QUICK_MOVE,p);
        h.assertTrue(p.getOffhandItem().getCount()==BlockCarry.LIMIT&&count(chest,Items.STONE)==12&&p.said.size()==1&&p.said.getFirst().startsWith("Building blocks 13/8"),"An overfull chest takes none back and says why: "+p.said);
        var hopper=hopperOver(h,at,new ItemStack(Items.STONE));hopperTick(hopper);
        h.assertTrue(hopper.getItem(0).getCount()==1&&count(chest,Items.STONE)==12,"Nor does a hopper add any");
        menu.clicked(0,0,ClickType.PICKUP,p);menu.clicked(0,0,ClickType.PICKUP,p);
        h.assertTrue(chest.getItem(0).getCount()==BlockCarry.LIMIT-1&&menu.getCarried().getCount()==12-(BlockCarry.LIMIT-1),"A stack lifted out goes back only up to the armful, the bucket counting: chest "+chest.getItem(0)+", cursor "+menu.getCarried());
        int total=chest.getItem(0).getCount()+menu.getCarried().getCount()+p.getOffhandItem().getCount()+hopper.getItem(0).getCount();
        h.assertTrue(total==21,"Every stone is conserved: "+total);
        h.succeed();
    }

    @GameTest(template="industrial") public static void doubleChestsBarrelsAndChestVehiclesHoldAnArmfulEach(GameTestHelper h){
        var level=h.getLevel();var left=h.absolutePos(new BlockPos(3,1,3));
        var state=Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE,ChestType.LEFT);level.setBlockAndUpdate(left,state);
        var right=left.relative(ChestBlock.getConnectedDirection(state));level.setBlockAndUpdate(right,Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE,ChestType.RIGHT));
        var handler=level.getCapability(Capabilities.ItemHandler.BLOCK,left,Direction.UP);int accepted=0;
        for(int n=0;n<40;n++)for(int slot=0;slot<handler.getSlots();slot++)if(handler.insertItem(slot,new ItemStack(Items.STONE),false).isEmpty()){accepted++;break;}
        h.assertTrue(handler.getSlots()==54&&accepted==2*BlockCarry.LIMIT,"A double chest is two chests, an armful each: "+accepted);
        var p=player(h);p.openMenu(level.getBlockState(left).getMenuProvider(level,left));var menu=p.containerMenu;
        for(int i=0;i<54;i++)menu.getSlot(i).set(ItemStack.EMPTY);
        menu.setCarried(new ItemStack(Items.OAK_PLANKS,30));
        menu.clicked(-999,AbstractContainerMenu.getQuickcraftMask(0,0),ClickType.QUICK_CRAFT,p);
        for(int slot:new int[]{0,1,27})menu.clicked(slot,AbstractContainerMenu.getQuickcraftMask(1,0),ClickType.QUICK_CRAFT,p);
        menu.clicked(-999,AbstractContainerMenu.getQuickcraftMask(2,0),ClickType.QUICK_CRAFT,p);
        int first=menu.getSlot(0).getItem().getCount()+menu.getSlot(1).getItem().getCount(),second=menu.getSlot(27).getItem().getCount();
        h.assertTrue(first==BlockCarry.LIMIT&&second==BlockCarry.LIMIT&&menu.getCarried().getCount()==30-2*BlockCarry.LIMIT,"A drag stops at each half's armful: "+first+" + "+second+", cursor "+menu.getCarried());
        p.closeContainer();

        BarrelBlockEntity barrel=place(h,new BlockPos(7,1,3),Blocks.BARREL);barrel.setItem(0,new ItemStack(Items.DIRT,6));
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STONE,BlockCarry.LIMIT));p.openMenu(barrel);menu=p.containerMenu;
        menu.clicked(2,BlockCarry.OFFHAND,ClickType.SWAP,p);
        h.assertTrue(barrel.getItem(2).getCount()==2&&p.getOffhandItem().getCount()==BlockCarry.LIMIT-2,"The offhand key swaps only what fits a barrel's armful: barrel "+barrel.getItem(2)+", hand "+p.getOffhandItem());
        p.closeContainer();

        for(Container vehicle:new Container[]{new MinecartChest(level,left.getX(),left.getY()+2,left.getZ()),new ChestBoat(EntityType.CHEST_BOAT,level)}){
            h.assertTrue(vehicle.canPlaceItem(0,new ItemStack(Items.STONE,BlockCarry.LIMIT))&&!vehicle.canPlaceItem(0,new ItemStack(Items.STONE,BlockCarry.LIMIT+1))
                    &&vehicle.canPlaceItem(0,new ItemStack(Items.APPLE,32)),"A chest vehicle holds an armful of blocks and any ordinary goods: "+vehicle);
            vehicle.setItem(3,new ItemStack(Items.STONE,BlockCarry.LIMIT));
            h.assertTrue(!vehicle.canPlaceItem(4,new ItemStack(Items.DIRT))&&new ChestMenu(MenuType.GENERIC_9x3,1,p.getInventory(),vehicle,3).getSlot(4).getMaxStackSize(new ItemStack(Items.DIRT))==0,"A full chest vehicle takes no more by automation or menu");
        }
        h.succeed();
    }

    @GameTest(template="industrial") public static void thePackPanelRespectsTheArmful(GameTestHelper h){
        ChestBlockEntity chest=place(h,new BlockPos(3,1,3),Blocks.CHEST);
        var p=player(h);var pack=Backpack.ITEM.toStack();p.getData(Accessories.INVENTORY).setStackInSlot(0,pack);
        Backpack.worn(p).setItem(0,new ItemStack(Items.STONE,20));
        p.openMenu(chest);var menu=p.containerMenu;int slot=-1;
        for(int i=0;i<menu.slots.size();i++)if(menu.slots.get(i) instanceof Backpack.PackSlot s&&s.getItem().is(Items.STONE)){slot=i;break;}
        menu.clicked(slot,0,ClickType.QUICK_MOVE,p);
        int packed=count(Backpack.worn(p),Items.STONE);
        h.assertTrue(blocks(chest)==BlockCarry.LIMIT&&packed==20-BlockCarry.LIMIT&&p.said.size()==1,"Shift-clicking a pack stack fills the chest to its armful and the rest stays packed: chest "+blocks(chest)+", pack "+packed);
        h.succeed();
    }

    @GameTest(template="industrial") public static void droppersAndHoppersOverChestMinecartsStopAtTheArmful(GameTestHelper h){
        var level=h.getLevel();ChestBlockEntity chest=place(h,new BlockPos(3,1,3),Blocks.CHEST);chest.setItem(0,new ItemStack(Items.DIRT,BlockCarry.LIMIT-1));
        var beside=chest.getBlockPos().west();level.setBlockAndUpdate(beside,Blocks.DROPPER.defaultBlockState().setValue(DispenserBlock.FACING,Direction.EAST));
        var dropper=(DispenserBlockEntity)level.getBlockEntity(beside);dropper.setItem(0,new ItemStack(Items.COBBLESTONE,3));
        for(int n=0;n<3;n++)level.getBlockState(beside).tick(level,beside,level.random);
        h.assertTrue(blocks(chest)==BlockCarry.LIMIT&&count(chest,Items.COBBLESTONE)==1&&dropper.getItem(0).getCount()==2,"A dropper fills a chest to its armful and keeps the rest: chest "+blocks(chest)+", dropper "+dropper.getItem(0));
        dropper.setItem(0,new ItemStack(Items.APPLE,2));level.getBlockState(beside).tick(level,beside,level.random);
        h.assertTrue(dropper.getItem(0).getCount()==1&&count(chest,Items.APPLE)==1,"A dropper still feeds ordinary goods");

        var under=new BlockPos(7,1,3);var at=h.absolutePos(under);var cart=new MinecartChest(level,at.getX()+.5,at.getY(),at.getZ()+.5);level.addFreshEntity(cart);
        cart.setItem(0,new ItemStack(Items.STONE,BlockCarry.LIMIT-1));
        var hopper=hopperOver(h,under,new ItemStack(Items.COBBLESTONE,3));hopperTick(hopper);hopperTick(hopper);
        h.assertTrue(blocks(cart)==BlockCarry.LIMIT&&count(cart,Items.COBBLESTONE)==1&&hopper.getItem(0).getCount()==2,"A hopper fills a chest minecart to its armful and keeps the rest: cart "+blocks(cart)+", hopper "+hopper.getItem(0));
        hopper.setItem(0,new ItemStack(Items.APPLE,2));hopperTick(hopper);
        h.assertTrue(hopper.getItem(0).getCount()==1&&count(cart,Items.APPLE)==1,"A hopper still feeds a chest minecart ordinary goods");
        cart.clearContent();cart.discard();h.succeed();
    }
}
