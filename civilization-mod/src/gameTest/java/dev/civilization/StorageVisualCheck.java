package dev.civilization;

import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;

/** Short real-menu snapshot of paired material storage. */
final class StorageVisualCheck {
    private static int ticks;
    static void tick(Minecraft mc){
        ticks++;var server=mc.getSingleplayerServer();
        if(ticks==60){mc.options.hideGui=false;mc.options.guiScale().set(3);server.execute(()->{
            var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();var pos=new BlockPos(80,101,0);
            p.teleportTo(l,80.5,101,-2,java.util.Set.of(),0,0);l.setBlockAndUpdate(pos,KilnContent.KILN.get().defaultBlockState());
            var k=(KilnBlockEntity)l.getBlockEntity(pos);k.setItem(0,new ItemStack(Items.CLAY,32));k.setItem(4,new ItemStack(Items.CLAY,32));
            k.setItem(2,new ItemStack(Items.BRICK,32));k.setItem(5,new ItemStack(Items.BRICK,32));k.setItem(1,KilnContent.MINERAL_COAL.toStack(32));k.setItem(3,KilnContent.MINERAL_COAL.toStack(32));p.openMenu(k);
        });}
        if(ticks==95){
            if(!(mc.player.containerMenu instanceof KilnMenu m)||m.slots.size()!=42||m.getSlot(4).getItem().getCount()!=32||m.getSlot(5).getItem().getCount()!=32)throw new IllegalStateException("Paired kiln storage failed menu sync");
            shot(mc,"kiln");
        }
        if(ticks==105)server.execute(()->{
            var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();var pos=new BlockPos(80,101,0);
            l.setBlockAndUpdate(pos,WorkshopContent.TEXTILE.get().defaultBlockState());var w=(WorkshopBlockEntity)l.getBlockEntity(pos);
            w.setItem(0,new ItemStack(Items.WHITE_WOOL,32));w.setItem(1,new ItemStack(Items.WHITE_WOOL,32));w.setItem(4,KilnContent.MINERAL_COAL.toStack(32));w.setItem(6,KilnContent.MINERAL_COAL.toStack(32));w.setItem(5,WorkshopContent.CLOTH.toStack(32));w.setItem(7,WorkshopContent.CLOTH.toStack(32));p.openMenu(w);
        });
        if(ticks==135){
            if(!(mc.player.containerMenu instanceof WorkshopMenu m)||m.slots.size()!=44||!m.getSlot(1).isActive()||m.getSlot(7).getItem().getCount()!=32)throw new IllegalStateException("Paired workshop storage failed menu sync");
            shot(mc,"workshop");
        }
        if(ticks>150)mc.stop();
    }
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"storage-"+name+".png",mc.getMainRenderTarget(),m->System.out.println(m.getString()));}
}
