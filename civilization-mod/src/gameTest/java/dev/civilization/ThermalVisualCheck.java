package dev.civilization;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.EquipmentSlot;
@net.neoforged.fml.common.EventBusSubscriber(modid="civilization",value=net.neoforged.api.distmarker.Dist.CLIENT)
final class ThermalVisualCheck {
    @net.neoforged.bus.api.SubscribeEvent public static void tooltip(net.neoforged.neoforge.client.event.ScreenEvent.Render.Post e){
        if(ticks>=255&&ticks<=270&&e.getScreen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen s)
            dev.civilization.client.ComfortHud.renderInventory(s,e.getGuiGraphics(),s.getGuiLeft()+s.getXSize()/2,Math.max(19,s.getGuiTop()-9));
    }
    private static int ticks;
    static void tick(Minecraft mc){ticks++;var server=mc.getSingleplayerServer();
        if(ticks==100){mc.options.guiScale().set(3);mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);server.execute(()->{
            var l=server.overworld();l.setDayTime(6000);RegionalWeather.get(l).clear(l,BlockPos.ZERO);
            var f=ThermalField.get(l);f.energy.clear();f.pending.clear();
            for(int x=-5;x<=5;x++)for(int z=-3;z<=8;z++)for(int y=99;y<=105;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),y==99?Blocks.STONE_BRICKS.defaultBlockState():Blocks.AIR.defaultBlockState());
            for(int x=-3;x<=3;x++)for(int y=100;y<=104;y++)l.setBlockAndUpdate(new BlockPos(x,y,4),Blocks.BRICKS.defaultBlockState());
            for(var entity:l.getEntitiesOfClass(net.minecraft.world.entity.decoration.ItemFrame.class,new net.minecraft.world.phys.AABB(-6,98,-4,6,107,9)))entity.discard();
            var p=server.getPlayerList().getPlayers().getFirst();p.getInventory().clearContent();p.setItemSlot(EquipmentSlot.HEAD,ThermalContent.HELMET.toStack());p.teleportTo(l,.5,100,-2.5,java.util.Set.of(),0,0);
            for(int x=-3;x<=3;x++)for(int z=0;z<=3;z++){var at=new BlockPos(x,102,z);f.add(l,at,(double)(x+3)*90);}
            // The diagnostic wall itself spans ambient to hot, separately from the warm air in front of it.
            for(int x=-3;x<=3;x++)for(int y=100;y<=104;y++){
                var at=new BlockPos(x,y,4);
                f.add(l,at,(double)(x+3)*10*ThermalField.capacity(l.getBlockState(at)));
            }
            f.setDirty();
        });}
        if(ticks==155)Screenshot.grab(mc.gameDirectory,"thermal-helmet.png",mc.getMainRenderTarget(),m->{});
        if(ticks==160)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),1.5,100,-2.5,java.util.Set.of(),0,0);});
        if(ticks==190)Screenshot.grab(mc.gameDirectory,"thermal-grid-moved.png",mc.getMainRenderTarget(),m->{});
        if(ticks==200)server.execute(()->server.getPlayerList().getPlayers().getFirst().setItemSlot(EquipmentSlot.HEAD,net.minecraft.world.item.ItemStack.EMPTY));
        if(ticks==230)Screenshot.grab(mc.gameDirectory,"thermal-helmet-off.png",mc.getMainRenderTarget(),m->{});
        if(ticks==210)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);var l=server.overworld();l.setBlockAndUpdate(p.blockPosition().below(),Blocks.BRICKS.defaultBlockState());});
        if(ticks==245){Screenshot.grab(mc.gameDirectory,"comfort-hud.png",mc.getMainRenderTarget(),m->{});}
        if(ticks==250)mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        if(ticks==265)Screenshot.grab(mc.gameDirectory,"comfort-inventory.png",mc.getMainRenderTarget(),m->{});
        if(ticks==272){mc.setScreen(null);server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();server.overworld().setBlockAndUpdate(p.blockPosition().below(),Blocks.STONE.defaultBlockState());});}
        if(ticks>=275&&ticks<=315)dev.civilization.client.ThermalVision.accept(new ThermalPayload(0,0,0,0,true,java.util.List.of()));
        if(ticks==315)Screenshot.grab(mc.gameDirectory,"comfort-cold.png",mc.getMainRenderTarget(),m->{});
        if(ticks>=320&&ticks<=360)dev.civilization.client.ThermalVision.accept(new ThermalPayload(22,22,1,0,true,java.util.List.of()));
        if(ticks==360)Screenshot.grab(mc.gameDirectory,"comfort-warm.png",mc.getMainRenderTarget(),m->{});
        if(ticks>=365&&ticks<=410)dev.civilization.client.ThermalVision.accept(new ThermalPayload(38,38,.17f,0,true,java.util.List.of()));
        if(ticks==410)Screenshot.grab(mc.gameDirectory,"comfort-hot.png",mc.getMainRenderTarget(),m->{});
        if(ticks>415){com.mojang.logging.LogUtils.getLogger().info("THERMAL VISUAL VERIFIED");mc.stop();}
    }
}
