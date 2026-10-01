package dev.civilization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.*;

/** Native 27/54-slot menus, real shift-click and the shared skin. Disposable client only. */
final class ChestVisualCheck {
    private static int ticks;
    static void tick(Minecraft mc){
        ticks++;var server=mc.getSingleplayerServer();
        if(ticks==100){mc.options.pauseOnLostFocus=false;mc.options.guiScale().set(3);mc.options.hideGui=false;open(mc,3);}
        if(ticks==140){check(mc,3);shot(mc,"single");mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId,0,0,net.minecraft.world.inventory.ClickType.QUICK_MOVE,mc.player);}
        if(ticks==155){
            var m=mc.player.containerMenu;
            if(m.getSlot(0).hasItem()||m.slots.subList(27,63).stream().filter(s->s.getItem().is(Items.COBBLESTONE)).mapToInt(s->s.getItem().getCount()).sum()!=17)throw new IllegalStateException("Chest shift-click changed stock");
            open(mc,6);
        }
        if(ticks==185){check(mc,6);shot(mc,"double");}
        if(ticks>190){com.mojang.logging.LogUtils.getLogger().info("CHEST UI VERIFIED");mc.stop();}
    }
    private static void open(Minecraft mc,int rows){mc.getSingleplayerServer().execute(()->{
        var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.closeContainer();p.getInventory().clearContent();
        var stock=new SimpleContainer(rows*9);stock.setItem(0,new ItemStack(Items.COBBLESTONE,17));stock.setItem(rows*9-1,new ItemStack(Items.DIAMOND,3));
        p.openMenu(new SimpleMenuProvider((id,inv,player)->rows==3?ChestMenu.threeRows(id,inv,stock):ChestMenu.sixRows(id,inv,stock),Component.literal(rows==3?"Chest":"Large Chest")));
    });}
    private static void check(Minecraft mc,int rows){if(!(mc.screen instanceof ContainerScreen)||!(mc.player.containerMenu instanceof ChestMenu m)||m.getRowCount()!=rows||!m.getSlot(rows*9-1).getItem().is(Items.DIAMOND))throw new IllegalStateException("Chest menu failed to synchronize");}
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"chest-"+name+".png",mc.getMainRenderTarget(),m->com.mojang.logging.LogUtils.getLogger().info("{}",m.getString()));}
}
