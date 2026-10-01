package dev.civilization;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.*;
import net.minecraft.world.level.block.Blocks;
final class ThermalArtVisualCheck {
    private static int ticks;
    private static volatile boolean ready;
    private static volatile Throwable failure;
    static void tick(Minecraft mc){
        ticks++;var server=mc.getSingleplayerServer();
        if(failure!=null)throw new IllegalStateException("Thermal art fixture",failure);
        if(ticks==100){mc.options.pauseOnLostFocus=false;mc.options.fov().set(45);mc.options.hideGui=true;
            server.execute(()->{try{
                var l=server.overworld();l.setDayTime(6000);l.setWeatherParameters(100000,0,false,false);
                for(int x=-4;x<26;x++)for(int z=-3;z<8;z++){l.setBlockAndUpdate(new BlockPos(x,100,z),Blocks.STONE_BRICKS.defaultBlockState());for(int y=101;y<110;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
                var blocks=java.util.List.of(KilnContent.KILN.get(),KilnContent.FOUNDRY.get(),CookingContent.STATION.get(),KilnContent.RETORT.get(),WorkshopContent.SMITHY.get());
                var player=server.getPlayerList().getPlayers().getFirst();player.getInventory().clearContent();
                for(int i=0;i<blocks.size();i++){
                    var at=new BlockPos(i*5,101,0);var state=blocks.get(i).defaultBlockState();l.setBlockAndUpdate(at,state);
                    if(i!=2)for(var part:MachineStructure.parts(state))MachineStructure.placePart(l,at,Direction.NORTH,part);
                    player.getInventory().setItem(i,new net.minecraft.world.item.ItemStack(blocks.get(i)));
                }
                player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();player.teleportTo(l,7,105,-13,java.util.Set.of(),5,15);ready=true;
            }catch(Throwable t){failure=t;}});
        }
        if(ticks>100&&!ready){ticks=101;return;}
        if(ticks==155)shot(mc,"cold");
        if(ticks==160)server.execute(()->{try{for(int i=0;i<5;i++){
            var b=server.overworld().getBlockEntity(new BlockPos(i*5,101,0));
            if(b instanceof KilnBlockEntity k){k.setItem(1,KilnContent.MINERAL_COAL.toStack(8));CoalFireFixture.light(k);}
            else if(b instanceof WorkshopBlockEntity w){w.setItem(4,KilnContent.MINERAL_COAL.toStack(8));CoalFireFixture.light(w);}
        }}catch(Throwable t){failure=t;}});
        if(ticks==220)shot(mc,"lit");
        if(ticks==225)server.execute(()->server.getPlayerList().getPlayers().getFirst().teleportTo(server.overworld(),21,104,-9,java.util.Set.of(),17,15));
        if(ticks==260)shot(mc,"forge-fertilizer");
        if(ticks==265)server.execute(()->server.getPlayerList().getPlayers().getFirst().teleportTo(server.overworld(),14,104,8,java.util.Set.of(),170,15));
        if(ticks==300)shot(mc,"rear");
        if(ticks==305){mc.options.hideGui=false;mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));}
        if(ticks==325)shot(mc,"inventory");
        if(ticks>335)mc.stop();
    }
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"thermal-art-"+name+".png",mc.getMainRenderTarget(),m->com.mojang.logging.LogUtils.getLogger().info("{}",m.getString()));}
}
