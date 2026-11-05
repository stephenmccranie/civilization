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
            k.setItem(2,new ItemStack(Items.BRICK,32));k.setItem(5,new ItemStack(Items.BRICK,32));k.setItem(1,KilnContent.MINERAL_COAL.toStack(32));k.setItem(3,KilnContent.MINERAL_COAL.toStack(32));
            // Fresh, three-quarters cooled and cold metal side by side in the hotbar.
            var now=l.getGameTime();p.getInventory().setItem(0,HotMetal.heated(new ItemStack(Items.IRON_INGOT,8),now));
            p.getInventory().setItem(1,HotMetal.heated(new ItemStack(Items.COPPER_INGOT,8),now-HotMetal.COOL_TICKS*3/4));p.getInventory().setItem(2,new ItemStack(Items.IRON_INGOT,8));p.openMenu(k);
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
        if(ticks==145)server.execute(()->{
            var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();var pos=new BlockPos(84,101,0);l.setDayTime(6000);l.setWeatherParameters(6000,0,false,false);
            // The smelt-hammer-quench stations, and rough metal from glowing to cold in the hotbar.
            l.setBlockAndUpdate(pos,KilnContent.STONE_ANVIL.get().defaultBlockState());for(int x=-2;x<=2;x++)for(int z=-1;z<=1;z++)l.setBlockAndUpdate(pos.offset(x,-1,z),net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState());l.setBlockAndUpdate(pos.east(),net.minecraft.world.level.block.Blocks.ANVIL.defaultBlockState());
            l.setBlockAndUpdate(pos.west(),net.minecraft.world.level.block.Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL,3));
            var now=l.getGameTime();var inventory=p.getInventory();
            inventory.setItem(0,HotMetal.heated(KilnContent.IRON_BLOOM.toStack(8),now));inventory.setItem(1,KilnContent.BLISTER_COPPER.toStack(8));
            inventory.setItem(2,KilnContent.GOLD_DORE.toStack(4));inventory.setItem(3,KilnContent.BLISTER_STEEL.toStack(4));inventory.setItem(4,KilnContent.STONE_ANVIL_ITEM.toStack());
            inventory.setItem(5,HotMetal.heated(new ItemStack(Items.IRON_INGOT,8),now-HotMetal.COOL_TICKS+HotMetal.DULL_TICKS/2));inventory.setItem(6,new ItemStack(Items.DIAMOND,2));inventory.selected=0;
            p.teleportTo(l,86.0,101,2.4,java.util.Set.of(),142,8);
        });
        if(ticks==180){mc.gui.getChat().clearMessages(false);mc.getToasts().clear();}
        if(ticks==185)shot(mc,"forge");
        if(ticks>195)mc.stop();
    }
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"storage-"+name+".png",mc.getMainRenderTarget(),m->System.out.println(m.getString()));}
}
