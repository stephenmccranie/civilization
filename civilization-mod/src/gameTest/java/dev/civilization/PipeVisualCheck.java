package dev.civilization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;

/** Short art fixture: transparent background, six directions, transfer expiry and item. */
final class PipeVisualCheck {
    private static int ticks;
    private static volatile boolean ready;
    static void tick(Minecraft mc) {
        ticks++;
        var server=mc.getSingleplayerServer();
        if(ticks==100){
            mc.options.hideGui=true;mc.options.fov().set(45);mc.options.guiScale().set(3);
            server.execute(()->{
                var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();
                for(int x=90;x<=105;x++)for(int z=-10;z<=5;z++)for(int y=100;y<=108;y++)
                    l.setBlockAndUpdate(new BlockPos(x,y,z),y==100?Blocks.STONE_BRICKS.defaultBlockState():Blocks.AIR.defaultBlockState());
                for(int x=92;x<=102;x++)for(int y=101;y<=107;y++)l.setBlockAndUpdate(new BlockPos(x,y,4),((x+y)%2==0?Blocks.WHITE_CONCRETE:Blocks.RED_TERRACOTTA).defaultBlockState());
                for(int x=94;x<=100;x++)for(int y:new int[]{103,105})l.setBlockAndUpdate(new BlockPos(x,y,0),IndustrialContent.PIPE.get().defaultBlockState());
                l.setBlockAndUpdate(new BlockPos(94,104,0),IndustrialContent.PIPE.get().defaultBlockState());
                for(int z=1;z<=2;z++)l.setBlockAndUpdate(new BlockPos(100,103,z),IndustrialContent.PIPE.get().defaultBlockState());
                l.setBlockAndUpdate(new BlockPos(100,103,3),IndustrialContent.PORT.get().defaultBlockState());
                p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();
                p.getInventory().clearContent();p.getInventory().setItem(0,IndustrialContent.PIPE.toStack());
                p.teleportTo(l,97.5,103,-9,java.util.Set.of(),0,-3);
                l.setDayTime(6000);l.setWeatherParameters(100000,0,false,false);ready=true;
            });
        }
        if(ticks>100&&!ready){ticks=101;return;}
        if(ticks>=140&&ticks<=200&&ticks%6==0)server.execute(()->{
            var l=server.overworld();
            for(int x=94;x<=100;x++)l.blockEvent(new BlockPos(x,103,0),IndustrialContent.PIPE.get(),2,
                (x==94?Direction.UP:Direction.WEST).ordinal()|((x==100?Direction.SOUTH:Direction.EAST).ordinal()<<3));
            l.blockEvent(new BlockPos(94,104,0),IndustrialContent.PIPE.get(),2,Direction.UP.ordinal()|(Direction.DOWN.ordinal()<<3));
            for(int z=1;z<=2;z++)l.blockEvent(new BlockPos(100,103,z),IndustrialContent.PIPE.get(),2,Direction.NORTH.ordinal()|(Direction.SOUTH.ordinal()<<3));
        });
        if(ticks==180){var flow=(PipeFlowEntity)mc.level.getBlockEntity(new BlockPos(97,103,0));
            if(flow==null||flow.until[Direction.EAST.ordinal()]<=mc.level.getGameTime())throw new IllegalStateException("Pipe flow telemetry missing");
            shot(mc,"flow");}
        if(ticks==188)shot(mc,"motion");
        if(ticks==240){var flow=(PipeFlowEntity)mc.level.getBlockEntity(new BlockPos(97,103,0));
            if(flow.until[Direction.EAST.ordinal()]>mc.level.getGameTime())throw new IllegalStateException("Pipe flow did not clear");shot(mc,"empty");}
        if(ticks==245)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),103,105,-5,java.util.Set.of(),45,12);});
        if(ticks==275)shot(mc,"corners");
        if(ticks==280){mc.options.hideGui=false;mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));}
        if(ticks==295)shot(mc,"item");
        if(ticks>300)mc.stop();
    }
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"pipes-"+name+".png",mc.getMainRenderTarget(),m->com.mojang.logging.LogUtils.getLogger().info("{}",m.getString()));}
}
