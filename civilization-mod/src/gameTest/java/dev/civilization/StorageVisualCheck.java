package dev.civilization;

import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;

/** Short real-menu snapshot of paired material storage, then hot metal through its colours in slots, in hand (white-hot and cherry) and dropped, by day and night, then at night without shaders. */
final class StorageVisualCheck {
    private static int ticks;
    /** Seconds out of the fire of the hotbar's iron ingots, from white-yellow to cold (-1), and of the three dropped ingots. */
    private static final int[] HOTBAR={2,18,30,35,40,45,51,58,-1},DROPPED={2,35,45};
    private static final java.util.List<net.minecraft.world.entity.item.ItemEntity> DROPS=new java.util.ArrayList<>();
    private static ItemStack aged(ItemStack stack,long now,int seconds){if(seconds<0)stack.remove(HotMetal.HOT.get());else stack.set(HotMetal.HOT.get(),now+HotMetal.COOL_TICKS-seconds*20L);return stack;}
    /** Sets every shown ingot to its age again, so each view shows the same temperatures. */
    private static void age(net.minecraft.server.MinecraftServer server){server.execute(()->{
        var l=server.overworld();var now=l.getGameTime();var inventory=server.getPlayerList().getPlayers().getFirst().getInventory();
        for(int i=0;i<HOTBAR.length;i++)aged(inventory.getItem(i),now,HOTBAR[i]);
        for(int i=0;i<DROPPED.length;i++)DROPS.get(i).setItem(aged(DROPS.get(i).getItem().copy(),now,DROPPED[i]));
    });}
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
        // Each menu holds its native slots, the Hand and the pack's slots.
        if(ticks==95){
            if(!(mc.player.containerMenu instanceof KilnMenu m)||m.slots.size()!=42+1+Backpack.SLOTS||m.getSlot(4).getItem().getCount()!=32||m.getSlot(5).getItem().getCount()!=32)throw new IllegalStateException("Paired kiln storage failed menu sync");
            shot(mc,"kiln");
        }
        if(ticks==105)server.execute(()->{
            var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();var pos=new BlockPos(80,101,0);
            l.setBlockAndUpdate(pos,WorkshopContent.TEXTILE.get().defaultBlockState());var w=(WorkshopBlockEntity)l.getBlockEntity(pos);
            w.setItem(0,new ItemStack(Items.WHITE_WOOL,32));w.setItem(1,new ItemStack(Items.WHITE_WOOL,32));w.setItem(5,WorkshopContent.CLOTH.toStack(32));w.setItem(7,WorkshopContent.CLOTH.toStack(32));p.openMenu(w);
        });
        if(ticks==135){
            if(!(mc.player.containerMenu instanceof WorkshopMenu m)||m.slots.size()!=44+1+Backpack.SLOTS||!m.getSlot(1).isActive()||m.getSlot(7).getItem().getCount()!=32)throw new IllegalStateException("Paired workshop storage failed menu sync");
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
            inventory.setItem(5,HotMetal.heated(new ItemStack(Items.IRON_INGOT,8),now-HotMetal.COOL_TICKS*5/6));inventory.setItem(6,new ItemStack(Items.DIAMOND,2));inventory.selected=0;
            p.teleportTo(l,86.0,101,2.4,java.util.Set.of(),142,8);
        });
        if(ticks==180){mc.gui.getChat().clearMessages(false);mc.getToasts().clear();}
        if(ticks==185)shot(mc,"forge");
        if(ticks==190)server.execute(()->{
            var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();var inventory=p.getInventory();
            for(int x=82;x<=87;x++)for(int z=1;z<=6;z++){l.setBlockAndUpdate(new BlockPos(x,100,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());for(int y=101;y<=103;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());}
            for(int i=0;i<HOTBAR.length;i++)inventory.setItem(i,new ItemStack(Items.IRON_INGOT,4));inventory.selected=3;
            for(int i=0;i<DROPPED.length;i++){var drop=new net.minecraft.world.entity.item.ItemEntity(l,83.7+i*.8,101,2.6,new ItemStack(Items.IRON_INGOT));drop.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);drop.setNeverPickUp();l.addFreshEntity(drop);DROPS.add(drop);}
            p.teleportTo(l,84.5,101,4.6,java.util.Set.of(),180,40);
        });
        if(ticks==195)mc.player.getInventory().selected=3;
        if(ticks==192||ticks==212||ticks==242||ticks==262)age(server);
        if(ticks==200){mc.gui.getChat().clearMessages(false);shot(mc,"hot-day");mc.player.getInventory().selected=0;}
        if(ticks==208){shot(mc,"hot-day-held");mc.player.getInventory().selected=3;}
        if(ticks==210){mc.options.hideGui=true;mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);mc.player.setXRot(0);}
        if(ticks==220)shot(mc,"hot-day-front");
        if(ticks==225){mc.options.hideGui=false;mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);mc.player.setXRot(40);server.execute(()->server.overworld().setDayTime(18000));}
        if(ticks==250){shot(mc,"hot-night");mc.player.getInventory().selected=0;}
        if(ticks==258){shot(mc,"hot-night-held");mc.player.getInventory().selected=3;}
        if(ticks==260){mc.options.hideGui=true;mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);mc.player.setXRot(0);}
        if(ticks==270)shot(mc,"hot-night-front");
        // The same night without a shader pack, held white-hot.
        if(ticks==272){mc.options.hideGui=false;mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);mc.player.setXRot(40);mc.player.getInventory().selected=0;WheelbarrowVisualCheck.shaders(false);}
        if(ticks==310)age(server);
        if(ticks==315)shot(mc,"hot-night-plain");
        if(ticks>325)mc.stop();
    }
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"storage-"+name+".png",mc.getMainRenderTarget(),m->System.out.println(m.getString()));}
}
