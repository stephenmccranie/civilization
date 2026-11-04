package dev.civilization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Real fueled structures, with matching cold references and unchanged world light emission. */
final class MachineLightingVisualCheck {
    private static int ticks;
    static void tick(Minecraft mc) {
        ticks++;var server=mc.getSingleplayerServer();
        if(ticks==100){
            mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(8);mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            server.execute(()->{
                var level=server.overworld();level.setDayTime(6000);level.setWeatherParameters(100000,0,false,false);
                for(int x=-10;x<=10;x++)for(int z=-6;z<=8;z++){level.setBlockAndUpdate(new BlockPos(x,100,z),Blocks.STONE.defaultBlockState());for(int y=101;y<=107;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
                // A cold and a fuelled, struck Kiln, Fertilizer Furnace and Cooking Stove.
                for(int i=0;i<6;i++) {
                    var pos=pos(i);
                    level.setBlockAndUpdate(pos,(switch(i/2){case 0->KilnContent.KILN.get();case 1->KilnContent.RETORT.get();default->CookingContent.STATION.get();}).defaultBlockState());
                    if(i<4)MachineFixture.build(level,pos);
                    if((i&1)==1 && level.getBlockEntity(pos) instanceof KilnBlockEntity fired){
                        fired.setItem(0,switch(i/2){case 0->new ItemStack(Items.CLAY,32);case 1->IndustrialContent.ENRICHED_BLEND.toStack(32);default->new ItemStack(Items.BEEF,32);});
                        fired.setItem(1,KilnContent.MINERAL_COAL.toStack(8));
                        MachineFixture.spark(level,true);
                        if(!fired.fire.strike(true))throw new IllegalStateException("Lighting fixture spark refused");
                    }
                }
                var player=server.getPlayerList().getPlayers().getFirst();player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.getInventory().clearContent();player.getAbilities().flying=true;player.onUpdateAbilities();player.teleportTo(level,3,104,-8,java.util.Set.of(),15f,17f);
            });
        }
        if(ticks==240){
            for(int i=0;i<6;i++){
                var state=mc.level.getBlockState(pos(i));if(!state.hasProperty(BlockStateProperties.LIT) || state.getValue(BlockStateProperties.LIT)!=((i&1)==1))throw new IllegalStateException("Lighting fixture incorrect lit state "+i);
                if((i&1)==1 && state.getLightEmission(mc.level,pos(i))<=0)throw new IllegalStateException("Controller world light was lost");
            }
            shot(mc,"day");server.execute(()->server.overworld().setDayTime(18000));
        }
        if(ticks==300)shot(mc,"night");if(ticks>310)mc.stop();
    }
    private static BlockPos pos(int i){return i<4?new BlockPos(-7+i*4,101,0):new BlockPos(-2+(i-4)*2,101,-3);}
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"machine-lighting-"+name+".png",mc.getMainRenderTarget(),message->com.mojang.logging.LogUtils.getLogger().info("{}",message.getString()));}
}
