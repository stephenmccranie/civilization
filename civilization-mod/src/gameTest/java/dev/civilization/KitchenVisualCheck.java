package dev.civilization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

/** Actual stove/menu/dial interaction in the disposable, guarded visual world. */
final class KitchenVisualCheck {
    private static int ticks;
    private static volatile boolean dialChecked;
    private static final BlockPos POS=new BlockPos(0,100,0);
    static void tick(Minecraft mc){
        ticks++;var server=mc.getSingleplayerServer();
        if(ticks==100){mc.options.pauseOnLostFocus=false;mc.options.guiScale().set(3);mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            server.execute(()->{var l=server.overworld();l.setDayTime(6000);var weather=RegionalWeather.get(l);var district=weather.district(l,RegionalWeather.key(0,0));district.rain=false;district.end=weather.clock+72000;weather.setDirty();
                for(int x=-4;x<=7;x++)for(int z=-6;z<=3;z++){l.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.STONE_BRICKS.defaultBlockState());for(int y=100;y<=105;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
                for(var e:l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(-5,98,-7,8,107,4)))e.discard();
                for(int i=0;i<3;i++){var pos=POS.east(i*2);l.setBlockAndUpdate(pos,PrototypeStoveContent.STOVE.get().defaultBlockState());var s=(PrototypeStoveEntity)l.getBlockEntity(pos);s.setItem(0,new ItemStack(Items.POTATO,2));s.setItem(1,new ItemStack(Items.CARROT,2));s.setItem(2,new ItemStack(Items.BREAD));s.start();var tag=s.saveWithoutMetadata(l.registryAccess());tag.putDouble("work",new double[]{100,900,1550}[i]);tag.putDouble("dial",new double[]{.3,.6,1}[i]);s.loadWithComponents(tag,l.registryAccess());l.sendBlockUpdated(pos,s.getBlockState(),s.getBlockState(),3);}
                l.setBlockAndUpdate(POS.east(6),CookingContent.STATION.get().defaultBlockState());
                var p=server.getPlayerList().getPlayers().getFirst();p.getInventory().clearContent();p.getInventory().setItem(0,PrototypeStoveContent.STOVE_ITEM.toStack());p.getInventory().setItem(1,PrototypeStoveContent.meal(1,1600));p.teleportTo(l,2.5,100,-3.1,java.util.Set.of(),0,20);
            });
        }
        if(ticks==140)mc.gui.getChat().clearMessages(true);
        if(ticks==150)shot(mc,"lineup");
        if(ticks==155)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),.5,100,-1.8,java.util.Set.of(),0,20);});
        if(ticks==175)shot(mc,"close");
        if(ticks==180)server.execute(()->{var s=(PrototypeStoveEntity)server.overworld().getBlockEntity(POS);var p=server.getPlayerList().getPlayers().getFirst();p.openMenu(s,b->b.writeBlockPos(POS));});
        if(ticks==205)shot(mc,"menu");
        if(ticks==210&&mc.screen instanceof dev.civilization.client.PrototypeStoveScreen screen){int left=(mc.getWindow().getGuiScaledWidth()-176)/2,top=(mc.getWindow().getGuiScaledHeight()-238)/2;screen.mouseClicked(left+76,top+85,0);screen.mouseDragged(left+87,top+96,0,11,11);screen.mouseReleased(left+87,top+96,0);}
        if(ticks==230){server.execute(()->{if(Math.abs(((PrototypeStoveEntity)server.overworld().getBlockEntity(POS)).dial()-(225.0/270))>.01)throw new IllegalStateException("Continuous rotary input did not reach the server");dialChecked=true;});shot(mc,"dial");}
        if(ticks==250&&!dialChecked)throw new IllegalStateException("Rotary server check did not complete");
        if(ticks==235){mc.player.closeContainer();server.execute(()->{var s=(PrototypeStoveEntity)server.overworld().getBlockEntity(POS);s.setItem(3,KilnContent.MINERAL_COAL.toStack(10));var t=s.saveWithoutMetadata(server.overworld().registryAccess());t.putDouble("work",780);t.putInt("heat",400);var f=t.getCompound("coalFire");f.putBoolean("lit",true);f.putInt("budget",400);t.put("coalFire",f);s.loadWithComponents(t,server.overworld().registryAccess());});}
        if(ticks==265)shot(mc,"cooking");
        if(ticks==270)server.execute(()->{var s=(PrototypeStoveEntity)server.overworld().getBlockEntity(POS);if(!s.finish())throw new IllegalStateException("Visual batch could not be served");});
        if(ticks==275)mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        if(ticks==295)shot(mc,"inventory");
        if(ticks==300){mc.player.closeContainer();server.execute(()->server.getPlayerList().getPlayers().getFirst().teleportTo(server.overworld(),2.5,100,3.1,java.util.Set.of(),180,20));}
        if(ticks==325)shot(mc,"rear");
        if(ticks==330)server.execute(()->server.getPlayerList().getPlayers().getFirst().teleportTo(server.overworld(),-2.5,100,.5,java.util.Set.of(),-90,20));
        if(ticks==350)shot(mc,"side");
        if(ticks>355){com.mojang.logging.LogUtils.getLogger().info("KITCHEN VISUAL VERIFIED: rotary input, working skillet, serving");mc.stop();}
    }
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"kitchen-"+name+".png",mc.getMainRenderTarget(),m->{});}
}
