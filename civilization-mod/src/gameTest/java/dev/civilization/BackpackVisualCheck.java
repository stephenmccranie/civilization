package dev.civilization;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/**
 * A worn backpack in its slot, binding it in a Pallet menu, building that the store refills, the pack worn on the back,
 * the pack in the inventory, building beyond reach, the pack and nearby Pallets beside a chest, and unbinding.
 */
final class BackpackVisualCheck {
    private static int ticks;
    private static volatile boolean ready;
    private static volatile Throwable failure;
    private static final int Y=101;
    private static final BlockPos PALLET=new BlockPos(174,Y,4),BESIDE=PALLET.east(),CHEST=new BlockPos(172,Y,0),MACHINES=new BlockPos(177,Y,3);
    private static final Vec3 BUILDER=new Vec3(174.5,Y,0.5),AWAY=new Vec3(195.5,Y,0.5);
    /** Stone in the pallets, the pack and the hand at the start. */
    private static final int STORED=100,PACKED=6,HAND=BlockCarry.LIMIT;
    static void tick(Minecraft mc){
        ticks++;var server=mc.getSingleplayerServer();if(failure!=null)throw new IllegalStateException("Backpack scene",failure);
        if(ticks==80){mc.options.fov().set(60);mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);check(server,()->{
            var l=server.overworld();l.setDayTime(6000);var weather=RegionalWeather.get(l);var d=weather.district(l,RegionalWeather.key(180,2));d.rain=false;d.end=weather.clock+72000;weather.setDirty();
            for(int x=166;x<=200;x++)for(int z=-6;z<=10;z++){l.setBlockAndUpdate(new BlockPos(x,Y-1,z),Blocks.STONE_BRICKS.defaultBlockState());for(int y=Y;y<=Y+6;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
            for(var at:new BlockPos[]{PALLET,BESIDE})l.setBlockAndUpdate(at,CargoContent.STOCKPILE.get().defaultBlockState());
            var store=((StockpileEntity)l.getBlockEntity(PALLET)).cargo();store.insert(new ItemStack(Items.STONE),STORED);store.insert(new ItemStack(Items.GLASS),20);
            // Machines and other hand-carried blocks go on a store too, one cell each; the oven draws as its item.
            l.setBlockAndUpdate(MACHINES,CargoContent.STOCKPILE.get().defaultBlockState());var machines=((StockpileEntity)l.getBlockEntity(MACHINES)).cargo();
            machines.insert(CookingContent.STATION_ITEM.toStack(),2);machines.insert(new ItemStack(Items.CHEST),1);machines.insert(new ItemStack(Items.OAK_STAIRS),3);
            machines.insert(new ItemStack(Items.OAK_DOOR),1);machines.insert(BakingOvenContent.ITEM.toStack(),1);
            l.setBlockAndUpdate(CHEST,Blocks.CHEST.defaultBlockState());((net.minecraft.world.level.block.entity.ChestBlockEntity)l.getBlockEntity(CHEST)).setItem(0,new ItemStack(Items.APPLE,12));
            var p=player(server);p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();
            var pack=Backpack.ITEM.toStack();p.getData(Accessories.INVENTORY).setStackInSlot(0,pack);
            var contents=Backpack.worn(p);contents.setItem(0,new ItemStack(Items.STONE,PACKED));contents.setItem(1,new ItemStack(Items.OAK_PLANKS,4));
            contents.setItem(2,new ItemStack(Items.APPLE,5));contents.setItem(3,new ItemStack(Items.TORCH,16));
            p.teleportTo(l,174.5,Y,1.5,java.util.Set.of(),0,35);ready=true;
        });}
        if(ticks>80&&!ready){ticks=81;return;}
        if(ticks==90){mc.options.hideGui=true;check(server,()->player(server).teleportTo(server.overworld(),174.5,Y,1.5,java.util.Set.of(),-56,40));}
        if(ticks==104)shot(mc,"pallet-goods");
        if(ticks==106){mc.options.hideGui=false;check(server,()->player(server).teleportTo(server.overworld(),174.5,Y,1.5,java.util.Set.of(),0,35));}
        if(ticks==110)mc.setScreen(new InventoryScreen(mc.player));
        if(ticks==125){
            if(Backpack.worn(mc.player)==null)throw new IllegalStateException("The worn backpack did not reach the client's accessory slot");
            shot(mc,"slot");
        }
        if(ticks==130){mc.setScreen(null);click(mc,PALLET);}
        if(ticks==145){
            if(!(mc.player.containerMenu instanceof CargoMenu m)||m.binding()!=1)throw new IllegalStateException("An empty-handed click did not open the Pallet menu with its Bind button");
            shot(mc,"store-menu");mc.gameMode.handleInventoryButtonClick(m.containerId,4);
        }
        if(ticks==160){
            if(!(mc.player.containerMenu instanceof CargoMenu m)||m.binding()!=2)throw new IllegalStateException("Bind backpack did not bind the pack to the Pallet");
            shot(mc,"store-bound");
        }
        if(ticks==165){mc.player.closeContainer();check(server,()->{var p=player(server);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STONE,HAND));p.inventoryMenu.broadcastFullState();p.teleportTo(server.overworld(),BUILDER.x,BUILDER.y,BUILDER.z,java.util.Set.of(),-90,40);});}
        if(ticks==185)shot(mc,"hud");
        // A two-high wall of eight stones, each placed from the hand by a real click.
        if(ticks>=190&&ticks<238&&(ticks-190)%6==0){int i=(ticks-190)/6;place(mc,new BlockPos(176,Y-1+i/4,i%4-1));}
        if(ticks==250)check(server,()->{
            var l=server.overworld();var p=player(server);int built=0;
            for(int i=0;i<8;i++)if(l.getBlockState(new BlockPos(176,Y+i/4,i%4-1)).is(Blocks.STONE))built++;
            var store=((StockpileEntity)l.getBlockEntity(PALLET)).cargo();long stored=store.count(store.indexOf(new ItemStack(Items.STONE)));
            int packed=Backpack.worn(p).getItem(0).getCount();
            if(built!=8||p.getOffhandItem().getCount()!=HAND||packed!=Backpack.BLOCKS-4)throw new IllegalStateException("Building did not keep the hand and the pack's stone topped up: built "+built+", hand "+p.getOffhandItem().getCount()+", pack "+packed);
            if(stored+packed+p.getOffhandItem().getCount()+built!=STORED+PACKED+HAND)throw new IllegalStateException("Stone was created or lost: "+stored+" stored");
        });
        if(ticks==255)shot(mc,"built");
        if(ticks==258)check(server,()->player(server).teleportTo(server.overworld(),AWAY.x,AWAY.y,AWAY.z,java.util.Set.of(),-90,20));
        if(ticks==262){mc.options.hideGui=true;mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);}
        // The body turns under a fixed camera: straight on, three-quarters and in profile.
        if(ticks>=262&&ticks<=325){float turn=ticks<285?0:ticks<305?45:90;mc.player.yBodyRot=mc.player.yBodyRotO=mc.player.getYRot()+turn;}
        if(ticks==280)shot(mc,"worn-back");
        if(ticks==300)shot(mc,"worn-quarter");
        if(ticks==322)shot(mc,"worn-side");
        if(ticks==326){mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.hideGui=false;check(server,()->player(server).teleportTo(server.overworld(),BUILDER.x,BUILDER.y,BUILDER.z,java.util.Set.of(),-90,40));}
        // The inventory shows the pack beside the Hand, with its binding.
        if(ticks==332)mc.setScreen(new InventoryScreen(mc.player));
        if(ticks==345){
            if(active(mc.player.inventoryMenu)!=Backpack.SLOTS||!pack(mc.player.inventoryMenu,2).getItem().is(Items.APPLE))throw new IllegalStateException("The inventory did not show the pack's nine slots");
            dev.civilization.client.CarryUiCheck.assertLayout(mc,false);
            shot(mc,"inventory");
        }
        if(ticks==370){mc.setScreen(null);check(server,()->player(server).teleportTo(server.overworld(),AWAY.x,AWAY.y,AWAY.z,java.util.Set.of(),-90,40));}
        if(ticks==385)place(mc,new BlockPos(197,Y-1,0));
        if(ticks==400)check(server,()->{
            var p=player(server);var store=((StockpileEntity)server.overworld().getBlockEntity(PALLET)).cargo();
            if(p.getOffhandItem().getCount()!=HAND||Backpack.worn(p).getItem(0).getCount()!=Backpack.BLOCKS-5)throw new IllegalStateException("Beyond reach the hand did not refill from the pack alone");
            if(store.count(store.indexOf(new ItemStack(Items.STONE)))!=STORED-HAND-(Backpack.BLOCKS-4-PACKED))throw new IllegalStateException("The store supplied a builder beyond its reach");
        });
        if(ticks==405)shot(mc,"beyond-reach");
        if(ticks==410)check(server,()->{var p=player(server);p.setItemInHand(InteractionHand.MAIN_HAND,Backpack.ITEM.toStack());p.inventoryMenu.broadcastFullState();});
        if(ticks==425)shot(mc,"held");
        // Facing open floor with an empty offhand, so nothing hides the right hand's pack.
        if(ticks==428){mc.options.hideGui=true;mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);check(server,()->{
            var p=player(server);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);p.inventoryMenu.broadcastFullState();p.teleportTo(server.overworld(),AWAY.x,AWAY.y,AWAY.z,java.util.Set.of(),90,0);
        });}
        if(ticks==443)shot(mc,"held-third");
        // Any other menu shows the pack beside the Hand, here a chest, with the Pallets in reach above it.
        if(ticks==446){mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.hideGui=false;check(server,()->player(server).teleportTo(server.overworld(),BUILDER.x,BUILDER.y,BUILDER.z,java.util.Set.of(),90,20));}
        if(ticks==456)click(mc,CHEST);
        if(ticks==470){
            if(!(mc.player.containerMenu instanceof net.minecraft.world.inventory.ChestMenu m)||active(m)!=Backpack.SLOTS)throw new IllegalStateException("A chest menu did not show the worn pack");
            if(!(NearbyStores.of(m) instanceof NearbyStores near)||near.stores()!=2||near.indexOf(new ItemStack(Items.STONE))<0)throw new IllegalStateException("A chest menu did not list both Pallet groups in reach");
            dev.civilization.client.CarryUiCheck.assertLayout(mc,false);
            shot(mc,"chest-menu");
        }
        // Real clicks on the Nearby panel: one takes a stack of stone onto the cursor, one on an empty cell puts it back.
        if(ticks==471){nearbyStone=nearbyCount(mc);clickNearby(mc,NearbyStores.of(mc.player.containerMenu).indexOf(new ItemStack(Items.STONE)));}
        if(ticks==474){
            var carried=mc.player.containerMenu.getCarried();
            if(!carried.is(Items.STONE)||nearbyCount(mc)!=nearbyStone-carried.getCount())throw new IllegalStateException("A Nearby click did not take stone: cursor "+carried+", listed "+nearbyCount(mc)+" of "+nearbyStone);
            check(server,()->{if(!player(server).containerMenu.getCarried().is(Items.STONE))throw new IllegalStateException("The server did not move the stone onto the cursor");});
            clickNearby(mc,NearbyStores.of(mc.player.containerMenu).size());
        }
        if(ticks==477&&(!mc.player.containerMenu.getCarried().isEmpty()||nearbyCount(mc)!=nearbyStone))throw new IllegalStateException("A Nearby click did not put the stone back: listed "+nearbyCount(mc)+" of "+nearbyStone);
        // The Pack panel's Unbind button, by a real click.
        if(ticks==478){
            var s=(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)mc.screen;var b=dev.civilization.client.SidePanels.unbind(dev.civilization.client.SidePanels.layout(s).pack());
            dev.civilization.client.CarryUiCheck.click(mc,s.getGuiLeft()+b.getX()+5,s.getGuiTop()+b.getY()+5);
        }
        if(ticks==490){
            check(server,()->{if(Backpack.worn(player(server)).pack().has(Backpack.SUPPLY.get()))throw new IllegalStateException("Unbind left the pack bound");});
            shot(mc,"unbound");
        }
        if(ticks>492){com.mojang.logging.LogUtils.getLogger().info("BACKPACK VISUAL VERIFIED: hand-carried machines on a Pallet, worn in an accessory slot, bound in a Pallet menu, building refilled from the store with stone conserved, worn from behind, three-quarters and the side, the pack in the inventory, the pack alone beyond reach, held item, the pack and nearby Pallets beside a chest menu, unbinding from the Pack panel");mc.stop();}
    }
    private static long nearbyStone;
    private static long nearbyCount(Minecraft mc){var near=NearbyStores.of(mc.player.containerMenu);return near.count(near.indexOf(new ItemStack(Items.STONE)));}
    /** Clicks the Nearby panel's cell for an entry on its first page. */
    private static void clickNearby(Minecraft mc,int entry){
        var s=(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)mc.screen;var r=dev.civilization.client.SidePanels.layout(s).nearby();int columns=dev.civilization.client.SidePanels.COLUMNS;
        dev.civilization.client.CarryUiCheck.click(mc,s.getGuiLeft()+r.getX()+7+entry%columns*18+8,s.getGuiTop()+r.getY()+15+entry/columns*18+8);
    }
    private static ServerPlayer player(MinecraftServer s){return s.getPlayerList().getPlayers().getFirst();}
    private static long active(net.minecraft.world.inventory.AbstractContainerMenu menu){return menu.slots.stream().filter(s->s instanceof Backpack.PackSlot&&s.isActive()).count();}
    private static net.minecraft.world.inventory.Slot pack(net.minecraft.world.inventory.AbstractContainerMenu menu,int i){return menu.slots.stream().filter(s->s instanceof Backpack.PackSlot&&s.getContainerSlot()==i).findFirst().orElseThrow();}
    private static void click(Minecraft mc,BlockPos at){mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at),Direction.UP,at,false));}
    /** Places the offhand block on top of `below`, as a right-click with the hand does. */
    private static void place(Minecraft mc,BlockPos below){mc.gameMode.useItemOn(mc.player,InteractionHand.OFF_HAND,new BlockHitResult(Vec3.atCenterOf(below).add(0,.5,0),Direction.UP,below,false));}
    private static void check(MinecraftServer s,Runnable action){s.execute(()->{try{action.run();}catch(Throwable e){failure=e;}});}
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"backpack-"+name+".png",mc.getMainRenderTarget(),m->{});}
}
