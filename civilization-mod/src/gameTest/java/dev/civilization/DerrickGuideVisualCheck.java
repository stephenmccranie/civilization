package dev.civilization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Focused controller construction and full native derrick review under Photon. */
final class DerrickGuideVisualCheck {
    private static int ticks;private static volatile String failure;private static volatile boolean ready,galleryChecked;
    private static final BlockPos AT=new BlockPos(0,101,0);
    static void tick(Minecraft mc){
        ticks++;var server=mc.getSingleplayerServer();if(failure!=null)throw new IllegalStateException(failure);
        if(ticks==100){mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(8);mc.options.fov().set(40);mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            check(mc,()->{var l=server.overworld();l.setDayTime(6000);l.setWeatherParameters(100000,0,false,false);
                for(int x=-10;x<=12;x++)for(int z=-4;z<=13;z++){l.getChunkAt(new BlockPos(x,101,z));l.setBlockAndUpdate(new BlockPos(x,100,z),Blocks.STONE_BRICKS.defaultBlockState());for(int y=101;y<=129;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
                l.setBlockAndUpdate(AT,IndustrialContent.PUMP.get().defaultBlockState());var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(l,.5,102,-3,java.util.Set.of(),0,15);p.openMenu((IndustrialBlockEntity)l.getBlockEntity(AT));ready=true;
            });}
        if(ticks>100&&!ready){ticks=101;return;}
        if(ticks==135){if(!(mc.player.containerMenu instanceof IndustrialMenu m)||m.data.get(13)!=0)throw new IllegalStateException("Construction cabinet did not synchronize");shot(mc,"construction");}
        if(ticks==140){int x=(mc.getWindow().getGuiScaledWidth()-176)/2,y=(mc.getWindow().getGuiScaledHeight()-214)/2;mc.screen.mouseClicked(x+88,y+49,0);mc.screen.mouseReleased(x+88,y+49,0);}
        if(ticks==160)check(mc,()->{var l=server.overworld();var m=(IndustrialBlockEntity)l.getBlockEntity(AT);if(!m.derrickBuilt||!IndustrialStructure.bind(m))throw new IllegalStateException("Actual Assemble button failed");
            var site=new Deposits.Site(0,101,0,Deposits.Kind.OIL,2);int stock=0;for(int i=0;i<site.cells()&&stock<20;i++){var pos=site.cell(i);if(site.body(pos)){l.setBlockAndUpdate(pos,IndustrialContent.RESERVOIR_OIL.get().defaultBlockState());stock++;}}
            try{var f=IndustrialBlockEntity.class.getDeclaredField("site");f.setAccessible(true);f.set(m,site);f=IndustrialBlockEntity.class.getDeclaredField("surveyed");f.setAccessible(true);f.setBoolean(m,true);}catch(Exception e){throw new RuntimeException(e);}
            m.setItem(0,KilnContent.MINERAL_COAL.toStack(16));CoalFireFixture.light(m);var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();p.teleportTo(l,8,114,-40,java.util.Set.of(),9.5f,0);
        });
        if(ticks==165)mc.options.hideGui=true;
        if(ticks==210)shot(mc,"front");
        if(ticks==215)check(mc,()->{var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),-21,114,42,java.util.Set.of(),208,0);});
        if(ticks==255)shot(mc,"rear");
        if(ticks==260)check(mc,()->{var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),13,119,-9,java.util.Set.of(),43,10);});
        if(ticks==295)shot(mc,"gallery-detail");
        if(ticks==300)check(mc,()->{var p=server.getPlayerList().getPlayers().getFirst();p.getAbilities().flying=false;p.onUpdateAbilities();p.teleportTo(server.overworld(),.5,116.5,1.9,java.util.Set.of(),0,5);});
        if(ticks==325)check(mc,()->{var p=server.getPlayerList().getPlayers().getFirst();if(!p.onGround()||Math.abs(p.getY()-116.40625)>.04)throw new IllegalStateException("Gallery collision does not support the actual player: "+p.getY()+" ground="+p.onGround());galleryChecked=true;});
        if(ticks==330)shot(mc,"on-gallery");
        if(ticks==335)check(mc,()->{var p=server.getPlayerList().getPlayers().getFirst();p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(server.overworld(),7,104,-6,java.util.Set.of(),34,0);});
        if(ticks==365)shot(mc,"working-base");
        if(ticks==376)shot(mc,"working-next");
        if(ticks==380){mc.options.hideGui=false;check(mc,()->{var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),.5,102,-3,java.util.Set.of(),0,15);p.openMenu((IndustrialBlockEntity)server.overworld().getBlockEntity(AT));});}
        if(ticks==410){if(((IndustrialMenu)mc.player.containerMenu).data.get(13)!=1)throw new IllegalStateException("Built flag did not reach cabinet");shot(mc,"assembled-cabinet");}
        if(ticks>420){if(!galleryChecked)throw new IllegalStateException("Gallery review incomplete");com.mojang.logging.LogUtils.getLogger().info("DERRICK GUIDE VERIFIED: actual Assemble button, complete native tower, working cycle and actual gallery support");mc.stop();}
    }
    private static void check(Minecraft mc,Runnable action){mc.getSingleplayerServer().execute(()->{try{action.run();}catch(Throwable e){failure=e.toString();}});}
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"derrick-"+name+".png",mc.getMainRenderTarget(),m->{});}
}
