package dev.civilization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

/** Actual stove/menu/dial interaction in the disposable, guarded visual world. */
final class KitchenVisualCheck {
    private static int ticks;
    private static volatile boolean dialChecked,carryChecked,restServed;
    private static volatile String failure;
    private static final BlockPos COUNTER=new BlockPos(1,100,0);
    private static final BlockPos POS=new BlockPos(0,100,0);
    static void tick(Minecraft mc){
        ticks++;var server=mc.getSingleplayerServer();if(failure!=null)throw new IllegalStateException(failure);
        if(ticks==100){mc.options.pauseOnLostFocus=false;mc.options.guiScale().set(3);mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            check(server,()->{var l=server.overworld();l.setDayTime(6000);var weather=RegionalWeather.get(l);var district=weather.district(l,RegionalWeather.key(0,0));district.rain=false;district.end=weather.clock+72000;weather.setDirty();
                for(int x=-4;x<=7;x++)for(int z=-6;z<=3;z++){l.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.STONE_BRICKS.defaultBlockState());for(int y=100;y<=105;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
                for(var e:l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(-5,98,-7,8,107,4)))e.discard();
                for(int i=0;i<3;i++){var pos=POS.east(i*2);l.setBlockAndUpdate(pos,PrototypeStoveContent.STOVE.get().defaultBlockState());var s=(PrototypeStoveEntity)l.getBlockEntity(pos);s.setItem(0,new ItemStack(Items.POTATO,2));s.setItem(1,new ItemStack(Items.CARROT,2));s.setItem(2,new ItemStack(Items.BREAD));s.start();var tag=s.saveWithoutMetadata(l.registryAccess());tag.putDouble("work",new double[]{100,900,1550}[i]);tag.putDouble("dial",new double[]{.3,.6,1}[i]);s.loadWithComponents(tag,l.registryAccess());l.sendBlockUpdated(pos,s.getBlockState(),s.getBlockState(),3);}
                l.setBlockAndUpdate(POS.east(6),CookingContent.STATION.get().defaultBlockState());
                var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=false;p.onUpdateAbilities();p.getInventory().clearContent();p.getInventory().setItem(0,PrototypeStoveContent.STOVE_ITEM.toStack());p.getInventory().setItem(1,PrototypeStoveContent.meal(1,1600));p.getInventory().setItem(2,PrototypeStoveContent.SKILLET.toStack());p.teleportTo(l,2.5,100,-3.1,java.util.Set.of(),0,20);
            });
        }
        if(ticks==140)mc.gui.getChat().clearMessages(true);
        if(ticks==150)shot(mc,"lineup");
        if(ticks==155)check(server,()->{var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),.5,100,-1.8,java.util.Set.of(),0,20);});
        if(ticks==175)shot(mc,"close");
        if(ticks==180)check(server,()->{var s=(PrototypeStoveEntity)server.overworld().getBlockEntity(POS);var p=server.getPlayerList().getPlayers().getFirst();p.openMenu(s,b->b.writeBlockPos(POS));});
        if(ticks==205)shot(mc,"menu");
        if(ticks==210&&mc.screen instanceof dev.civilization.client.PrototypeStoveScreen screen){int left=(mc.getWindow().getGuiScaledWidth()-176)/2,top=(mc.getWindow().getGuiScaledHeight()-238)/2;screen.mouseClicked(left+76,top+85,0);screen.mouseDragged(left+87,top+96,0,11,11);screen.mouseReleased(left+87,top+96,0);}
        if(ticks==230){check(server,()->{if(Math.abs(((PrototypeStoveEntity)server.overworld().getBlockEntity(POS)).dial()-(225.0/270))>.01)throw new IllegalStateException("Continuous rotary input did not reach the server");dialChecked=true;});shot(mc,"dial");}
        if(ticks==250&&!dialChecked)throw new IllegalStateException("Rotary server check did not complete");
        if(ticks==235){mc.player.closeContainer();check(server,()->{var s=(PrototypeStoveEntity)server.overworld().getBlockEntity(POS);s.setItem(3,KilnContent.MINERAL_COAL.toStack(10));var t=s.saveWithoutMetadata(server.overworld().registryAccess());t.putDouble("work",780);t.putDouble("warmth",130);t.putInt("heat",400);var f=t.getCompound("coalFire");f.putBoolean("lit",true);f.putInt("budget",400);t.put("coalFire",f);s.loadWithComponents(t,server.overworld().registryAccess());});}
        if(ticks==265)shot(mc,"cooking");
        if(ticks==270)check(server,()->{var s=(PrototypeStoveEntity)server.overworld().getBlockEntity(POS);if(!s.finish())throw new IllegalStateException("Visual batch could not be served");});
        if(ticks==275)mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        if(ticks==295)shot(mc,"inventory");
        if(ticks==300){mc.player.closeContainer();server.execute(()->server.getPlayerList().getPlayers().getFirst().teleportTo(server.overworld(),2.5,100,3.1,java.util.Set.of(),180,20));}
        if(ticks==325)shot(mc,"rear");
        if(ticks==330)server.execute(()->server.getPlayerList().getPlayers().getFirst().teleportTo(server.overworld(),-2.5,100,.5,java.util.Set.of(),-90,20));
        if(ticks==350)shot(mc,"side");
        if(ticks==355)check(server,()->{
            var l=server.overworld();l.setBlockAndUpdate(COUNTER,Blocks.STONE_BRICKS.defaultBlockState());
            var stove=(PrototypeStoveEntity)l.getBlockEntity(POS);stove.setItem(4,ItemStack.EMPTY);
            var t=stove.saveWithoutMetadata(l.registryAccess());t.putBoolean("batch",true);t.putDouble("budget",1600);t.putDouble("work",730);t.putDouble("warmth",140);stove.loadWithComponents(t,l.registryAccess());l.sendBlockUpdated(POS,stove.getBlockState(),stove.getBlockState(),3);
            var p=server.getPlayerList().getPlayers().getFirst();p.getInventory().selected=0;p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.teleportTo(l,.5,100,-1.8,java.util.Set.of(),0,20);
        });
        if(ticks==370)mc.options.keyShift.setDown(true);
        if(ticks==375)use(mc,POS);
        if(ticks==380)check(server,()->{var p=server.getPlayerList().getPlayers().getFirst();if(((PrototypeStoveEntity)server.overworld().getBlockEntity(POS)).hasSkillet()||!p.getMainHandItem().is(PrototypeStoveContent.SKILLET.get()))throw new IllegalStateException("Actual stove lift failed");});
        if(ticks==385)use(mc,COUNTER);
        if(ticks==400)check(server,()->{var p=server.getPlayerList().getPlayers().getFirst();if(!(server.overworld().getBlockEntity(COUNTER.above()) instanceof RestingSkilletEntity pan)||!pan.skillet().batch()||!p.getMainHandItem().isEmpty())throw new IllegalStateException("Actual resting placement failed");});
        if(ticks==410)shot(mc,"resting");
        if(ticks==425)use(mc,COUNTER.above());
        if(ticks==435){shot(mc,"carried");mc.options.keyShift.setDown(false);}
        if(ticks==440)use(mc,POS);
        if(ticks==450)check(server,()->{var stove=(PrototypeStoveEntity)server.overworld().getBlockEntity(POS);if(!stove.hasSkillet()||!stove.batch()||stove.work()<=730||!server.overworld().getBlockState(COUNTER.above()).isAir())throw new IllegalStateException("Actual return or carryover failed");carryChecked=true;});
        if(ticks==455)shot(mc,"returned");
        if(ticks==460)mc.options.keyShift.setDown(true);
        if(ticks==465)use(mc,POS);
        if(ticks==475)use(mc,COUNTER);
        if(ticks==480)mc.options.keyShift.setDown(false);
        if(ticks==490)use(mc,COUNTER.above());
        if(ticks==500){shot(mc,"rest-served");check(server,()->{var p=server.getPlayerList().getPlayers().getFirst();var pan=(RestingSkilletEntity)server.overworld().getBlockEntity(COUNTER.above());if(pan.skillet().batch()||p.getInventory().countItem(PrototypeStoveContent.MEAL.get())!=8)throw new IllegalStateException("Actual resting serve failed");restServed=true;});}
        if(ticks>510){if(failure!=null)throw new IllegalStateException(failure);if(!restServed)throw new IllegalStateException("Resting serve check did not complete");if(!carryChecked)throw new IllegalStateException("Carryover review did not complete");mc.options.keyShift.setDown(false);com.mojang.logging.LogUtils.getLogger().info("KITCHEN VISUAL VERIFIED: rotary input, working skillet, lift, rest, carried food, return and resting serve");mc.stop();}
    }
    private static void check(net.minecraft.server.MinecraftServer server,Runnable action){server.execute(()->{try{action.run();}catch(Throwable e){failure=e.toString();}});}
    private static void use(Minecraft mc,BlockPos pos){mc.gameMode.useItemOn(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(pos.getCenter().add(0,.5,0),Direction.UP,pos,false));}
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"kitchen-"+name+".png",mc.getMainRenderTarget(),m->{});}
}
