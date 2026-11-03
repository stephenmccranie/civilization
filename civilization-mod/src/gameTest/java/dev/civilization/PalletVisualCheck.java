package dev.civilization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Pallet stacks at each fill level, mixed goods and pieces, an overloaded pre-limit pallet and a 3x3 yard, through real clicks and menus. */
final class PalletVisualCheck {
    private static int ticks;
    private static volatile boolean ready;
    private static volatile Throwable failure;
    private static final int Y=101;
    private static final int[] FILLS={0,1,8,16,33,64};
    private static final BlockPos MIXED=new BlockPos(142,Y,3),PIECES=new BlockPos(144,Y,3),GLASS=new BlockPos(146,Y,3),OVERLOADED=new BlockPos(148,Y,3),
            EMPTY=new BlockPos(150,Y,3),FULL=new BlockPos(152,Y,0),YARD=new BlockPos(142,Y,6),LAST=YARD.offset(2,0,2);
    private static BlockPos fill(int i){return new BlockPos(142+2*i,Y,0);}
    static void tick(Minecraft mc){
        ticks++;var server=mc.getSingleplayerServer();if(failure!=null)throw new IllegalStateException("Pallet scene",failure);
        if(ticks==80){mc.options.fov().set(60);mc.options.hideGui=true;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);check(server,()->{
            var l=server.overworld();l.setDayTime(6000);var weather=RegionalWeather.get(l);var d=weather.district(l,RegionalWeather.key(146,4));d.rain=false;d.end=weather.clock+72000;weather.setDirty();
            for(int x=136;x<=158;x++)for(int z=-8;z<=14;z++){l.setBlockAndUpdate(new BlockPos(x,Y-1,z),Blocks.STONE_BRICKS.defaultBlockState());for(int y=Y;y<=Y+8;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
            for(int i=0;i<FILLS.length;i++)pallet(l,fill(i)).cargo().insert(new ItemStack(Items.STONE),FILLS[i]);
            var mixed=pallet(l,MIXED).cargo();mixed.insert(new ItemStack(Items.STONE),16);mixed.insert(new ItemStack(Items.OAK_PLANKS),20);mixed.insert(new ItemStack(Items.BRICKS),10);
            var pieces=pallet(l,PIECES).cargo();pieces.insert(CuttingContent.stack(Blocks.STONE.defaultBlockState(),2,1),7);pieces.insert(CuttingContent.stack(Blocks.OAK_PLANKS.defaultBlockState(),1,1),6);pieces.insert(CuttingContent.stack(Blocks.BRICKS.defaultBlockState(),3,1),21);
            pallet(l,GLASS).cargo().insert(new ItemStack(Items.GLASS),24);
            var legacy=new MassCargo(()->1e12,()->{});legacy.insert(new ItemStack(Items.STONE),700);legacy.insert(new ItemStack(Items.OAK_PLANKS),1000);
            var tag=new CompoundTag();tag.put("Cargo",legacy.save(l.registryAccess()));pallet(l,OVERLOADED).loadWithComponents(tag,l.registryAccess());
            pallet(l,EMPTY);
            for(var at:BlockPos.betweenClosed(YARD,LAST))pallet(l,at.immutable());
            var yard=((StockpileEntity)l.getBlockEntity(YARD)).cargo();yard.insert(new ItemStack(Items.COBBLESTONE),300);yard.insert(new ItemStack(Items.OAK_PLANKS),100);
            var p=player(server);p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().clearContent();
            p.teleportTo(l,147.5,104.5,-5,java.util.Set.of(),0,28);ready=true;
        });}
        if(ticks>80&&!ready){ticks=81;return;}
        if(ticks==140){
            for(int i=0;i<FILLS.length;i++)if(!(mc.level.getBlockEntity(fill(i)) instanceof StockpileEntity e)||e.layers()!=(FILLS[i]+15)/16||e.stack().load()!=FILLS[i])throw new IllegalStateException("Fill level "+FILLS[i]+" did not reach the client stack");
            if(mc.level.getBlockState(FULL).getShape(mc.level,FULL).max(Direction.Axis.Y)!=1)throw new IllegalStateException("A full stack is not a full block on the client");
            shot(mc,"fill-levels");
        }
        if(ticks==145)check(server,()->player(server).teleportTo(server.overworld(),139.2,104.2,10.6,java.util.Set.of(),-128,32));
        if(ticks==170){
            int full=0;for(var at:BlockPos.betweenClosed(YARD,LAST))if(mc.level.getBlockEntity(at) instanceof StockpileEntity e&&e.stack().load()==StockpileEntity.CAPACITY)full++;
            if(full!=5)throw new IllegalStateException("The yard did not fill one pallet at a time: "+full+" full");
            shot(mc,"yard");
        }
        if(ticks==175)check(server,()->player(server).teleportTo(server.overworld(),145.5,103.4,0.6,java.util.Set.of(),0,38));
        if(ticks==200)shot(mc,"pieces");
        if(ticks==205){shaders(false);check(server,()->player(server).teleportTo(server.overworld(),147.5,104.5,-5,java.util.Set.of(),0,28));}
        if(ticks==250)shot(mc,"fill-levels-no-shaders");
        if(ticks==255)shaders(true);
        if(ticks==300){mc.options.hideGui=false;check(server,()->{
            var p=player(server);p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STONE,32));p.inventoryMenu.broadcastFullState();
            p.teleportTo(server.overworld(),150.5,Y,1.6,java.util.Set.of(),0,45);
        });}
        if(ticks==315)click(mc,EMPTY);
        if(ticks==330){
            if(!mc.player.getOffhandItem().isEmpty()||!(mc.level.getBlockEntity(EMPTY) instanceof StockpileEntity e)||e.stack().count(0)!=32||e.layers()!=2||mc.player.containerMenu instanceof CargoMenu)throw new IllegalStateException("Right-click did not stack the offhand blocks onto the pallet");
            shot(mc,"put-held");
        }
        if(ticks==335)check(server,()->{var p=player(server);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STONE,32));p.inventoryMenu.broadcastFullState();p.teleportTo(server.overworld(),152.5,Y,-1.6,java.util.Set.of(),0,40);});
        if(ticks==345)click(mc,FULL);
        if(ticks==360){
            if(!(mc.player.containerMenu instanceof CargoMenu m)||m.count(0)!=64||m.load()!=64||m.capacity()!=64||mc.player.getOffhandItem().getCount()!=32)throw new IllegalStateException("A full pallet did not open its menu and leave the hand stack alone");
            shot(mc,"full-menu");
        }
        if(ticks==365){mc.player.closeContainer();check(server,()->player(server).teleportTo(server.overworld(),143.5,Y,4.6,java.util.Set.of(),0,40));}
        if(ticks==375)click(mc,YARD);
        if(ticks==385){
            if(!mc.player.getOffhandItem().isEmpty()||!(mc.level.getBlockEntity(YARD.offset(1,0,2)) instanceof StockpileEntity e)||e.stack().count(0)!=32)throw new IllegalStateException("Yard stone did not start the first empty pallet");
            click(mc,YARD);
        }
        if(ticks==400){
            if(!(mc.player.containerMenu instanceof CargoMenu m)||m.palletCount()!=9||m.capacity()!=576||m.count(0)!=300||m.count(1)!=100||m.count(2)!=32)throw new IllegalStateException("The yard menu did not combine its nine stacks");
            shot(mc,"yard-menu");
            mc.gameMode.handleInventoryMouseClick(m.containerId,0,0,ClickType.QUICK_MOVE,mc.player);
        }
        if(ticks==410){
            if(!mc.player.getOffhandItem().is(Items.COBBLESTONE)||mc.player.getOffhandItem().getCount()!=32)throw new IllegalStateException("Shared withdrawal did not reach the offhand");
            mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId,CargoMenu.OFFHAND_SLOT,0,ClickType.QUICK_MOVE,mc.player);
        }
        // Fixture-only removal of the empty last pallet changes the topology under the open menu.
        if(ticks==420){if(!mc.player.getOffhandItem().isEmpty()||((CargoMenu)mc.player.containerMenu).count(0)!=300)throw new IllegalStateException("The Hand slot did not return the cobblestone");check(server,()->server.overworld().setBlockAndUpdate(LAST,Blocks.AIR.defaultBlockState()));}
        if(ticks==430&&(!(mc.player.containerMenu instanceof CargoMenu m)||m.palletCount()!=8||m.capacity()!=512||m.load()!=432))throw new IllegalStateException("An open menu did not adopt its smaller yard");
        if(ticks==435)check(server,()->server.overworld().setBlockAndUpdate(LAST,CargoContent.STOCKPILE.get().defaultBlockState()));
        if(ticks==445&&(!(mc.player.containerMenu instanceof CargoMenu m)||m.palletCount()!=9||m.capacity()!=576||m.load()!=432))throw new IllegalStateException("An open menu duplicated or lost stock when the yard rejoined");
        if(ticks==450){mc.player.closeContainer();check(server,()->player(server).teleportTo(server.overworld(),148.5,Y,1.6,java.util.Set.of(),0,40));}
        if(ticks==460)click(mc,OVERLOADED);
        if(ticks==475){
            if(!(mc.player.containerMenu instanceof CargoMenu m)||m.load()!=1700||m.capacity()!=64||m.count(0)!=700||m.count(1)!=1000)throw new IllegalStateException("The overloaded pallet lost or hid pre-limit stock");
            shot(mc,"overloaded-menu");
        }
        if(ticks==480){mc.player.closeContainer();mc.options.hideGui=true;check(server,()->{var p=player(server);p.setGameMode(GameType.SPECTATOR);p.teleportTo(server.overworld(),148.5,105,12.5,java.util.Set.of(),170,30);});}
        if(ticks==505)shot(mc,"after");
        if(ticks>510){com.mojang.logging.LogUtils.getLogger().info("PALLET VISUAL VERIFIED: fill levels, mixed goods and pieces, overloaded pre-limit stock, yard fill order, right-click stacking, full-pallet and shared menus, Hand-slot transfers, topology changes under an open menu");mc.stop();}
    }
    private static StockpileEntity pallet(net.minecraft.server.level.ServerLevel l,BlockPos at){l.setBlockAndUpdate(at,CargoContent.STOCKPILE.get().defaultBlockState());return (StockpileEntity)l.getBlockEntity(at);}
    private static net.minecraft.server.level.ServerPlayer player(net.minecraft.server.MinecraftServer s){return s.getPlayerList().getPlayers().getFirst();}
    private static void click(Minecraft mc,BlockPos at){mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at),Direction.UP,at,false));}
    private static void check(net.minecraft.server.MinecraftServer s,Runnable action){s.execute(()->{try{action.run();}catch(Throwable e){failure=e;}});}
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"pallet-"+name+".png",mc.getMainRenderTarget(),m->{});}
    private static void shaders(boolean enabled){try{var iris=Class.forName("net.irisshaders.iris.Iris");var config=iris.getMethod("getIrisConfig").invoke(null);config.getClass().getMethod("setShadersEnabled",boolean.class).invoke(config,enabled);config.getClass().getMethod("save").invoke(config);iris.getMethod("reload").invoke(null);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
}
