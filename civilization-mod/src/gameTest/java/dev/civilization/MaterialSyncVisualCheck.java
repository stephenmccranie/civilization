package dev.civilization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.*;
import net.minecraft.world.level.block.Blocks;

/** Deliberately separates block-state delivery from material delivery by several rendered frames. */
final class MaterialSyncVisualCheck {
    private static int ticks;
    private static final net.minecraft.world.level.block.Block[] MATERIALS={Blocks.BRICKS,Blocks.COPPER_BLOCK,Blocks.OAK_PLANKS};
    static void tick(Minecraft mc) {
        ticks++;var server=mc.getSingleplayerServer();
        if(ticks==100) {
            mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(8);
            server.execute(()->{
                var level=server.overworld();level.setDayTime(6000);level.setWeatherParameters(100000,0,false,false);
                for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++){level.setBlockAndUpdate(new BlockPos(x,100,z),Blocks.STONE.defaultBlockState());for(int y=101;y<=107;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
                for(int i=0;i<3;i++)level.setBlockAndUpdate(pos(i),CuttingContent.PIECE.get().defaultBlockState().setValue(CutBlock.UNITS,1));
                var player=server.getPlayerList().getPlayers().getFirst();player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.getInventory().clearContent();player.getAbilities().flying=true;player.onUpdateAbilities();player.teleportTo(level,3,102,-4.5,java.util.Set.of(),32f,20f);
            });
        }
        if(ticks==180)shot(mc,"before");
        if(ticks==190)server.execute(()->{for(int i=0;i<3;i++)((CutBlockEntity)server.overworld().getBlockEntity(pos(i))).cells(CutCells.filled(CutBlock.bounds(server.overworld().getBlockState(pos(i))),MATERIALS[i].defaultBlockState()));});
        if(ticks==260){
            for(int i=0;i<3;i++)if(!(mc.level.getBlockEntity(pos(i)) instanceof CutBlockEntity cut) || !cut.material().is(MATERIALS[i]))throw new IllegalStateException("Material packet did not reach client for display "+i);
            shot(mc,"after");com.mojang.logging.LogUtils.getLogger().info("Material-sync: all three delayed material packets reached client; hidden=true mouseGrabbed={}",mc.mouseHandler.isMouseGrabbed());
        }
        if(ticks>280)mc.stop();
    }
    private static BlockPos pos(int i){return new BlockPos((i-1)*2,101,0);}
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"material-sync-"+name+".png",mc.getMainRenderTarget(),message->com.mojang.logging.LogUtils.getLogger().info("{}",message.getString()));}
}
