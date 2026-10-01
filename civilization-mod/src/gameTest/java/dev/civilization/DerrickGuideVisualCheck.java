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
        if(ticks==5&&mc.player.isDeadOrDying()){mc.player.respawn();mc.setScreen(null);}
        if(ticks==125){if(mc.player.isDeadOrDying())throw new IllegalStateException("Visual player must be alive");if(mc.screen instanceof net.minecraft.client.gui.screens.DeathScreen)mc.setScreen(null);}
        if(ticks==100){mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(8);mc.options.fov().set(40);mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            check(mc,()->{var l=server.overworld();l.setDayTime(6000);l.setWeatherParameters(100000,0,false,false);
                for(int x=-10;x<=12;x++)for(int z=-4;z<=13;z++){l.getChunkAt(new BlockPos(x,101,z));l.setBlockAndUpdate(new BlockPos(x,100,z),Blocks.STONE_BRICKS.defaultBlockState());for(int y=101;y<=129;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
                l.setBlockAndUpdate(AT,IndustrialContent.PUMP.get().defaultBlockState());var p=server.getPlayerList().getPlayers().getFirst();p.setHealth(p.getMaxHealth());p.fallDistance=0;p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.getAbilities().flying=false;p.onUpdateAbilities();p.teleportTo(l,.5,102,-3,java.util.Set.of(),0,15);ready=true;
            });}
        if(ticks>100&&!ready){ticks=101;return;}
        if(ticks==135)shot(mc,"construction-guide");
        if(ticks==140)check(mc,()->server.getPlayerList().getPlayers().getFirst().setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE,7)));
        if(ticks==145)useController(mc);
        if(ticks==160)check(mc,()->{var m=(IndustrialBlockEntity)server.overworld().getBlockEntity(AT);var p=server.getPlayerList().getPlayers().getFirst();if(m.derrickSections!=1||!p.getMainHandItem().isEmpty())throw new IllegalStateException("Actual held-stack footing construction failed");p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_PLANKS,60));});
        if(ticks==165)useController(mc);
        if(ticks==175)check(mc,()->{var p=server.getPlayerList().getPlayers().getFirst();var m=(IndustrialBlockEntity)server.overworld().getBlockEntity(AT);if(Integer.bitCount(m.derrickSections)!=11||!p.getMainHandItem().isEmpty())throw new IllegalStateException("Held wood did not build partial tower");p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(server.overworld(),.5,110,-3,java.util.Set.of(),0,-15);});
        if(ticks==180){mc.options.fov().set(80);mc.gui.getChat().clearMessages(false);}
        if(ticks==183)profileGuide(mc);
        if(ticks==185)shot(mc,"partial-guide");
        if(ticks==200)check(mc,()->{var l=server.overworld();var m=(IndustrialBlockEntity)l.getBlockEntity(AT);var cell=ModeledDerrick.CELLS.stream().filter(c->c.pieces().size()==1&&c.pieces().containsKey(1)).findFirst().orElseThrow();l.destroyBlock(ModeledDerrick.position(AT,m.front(),cell),true);if(l.getBlockEntity(AT)!=m||Integer.bitCount(m.derrickSections)!=10)throw new IllegalStateException("Panel damage collapsed tower");});
        if(ticks==220)shot(mc,"damaged-guide");
        if(ticks==240)check(mc,()->{var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(l,.5,102,-3,java.util.Set.of(),0,15);var m=(IndustrialBlockEntity)l.getBlockEntity(AT);for(var stock:java.util.List.of(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_PLANKS,6),new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_PLANKS,60),new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_PLANKS,29),new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_BLOCK,8),IndustrialContent.PORT.toStack()))ModeledDerrick.build(m,p,stock);});
        if(ticks==260)check(mc,()->{var l=server.overworld();var m=(IndustrialBlockEntity)l.getBlockEntity(AT);if(!m.derrickBuilt||!IndustrialStructure.bind(m))throw new IllegalStateException("Progressive completed construction failed");
            var site=new Deposits.Site(0,101,0,Deposits.Kind.OIL,2);int stock=0;for(int i=0;i<site.cells()&&stock<20;i++){var pos=site.cell(i);if(site.body(pos)){l.setBlockAndUpdate(pos,IndustrialContent.RESERVOIR_OIL.get().defaultBlockState());stock++;}}
            try{var f=IndustrialBlockEntity.class.getDeclaredField("site");f.setAccessible(true);f.set(m,site);f=IndustrialBlockEntity.class.getDeclaredField("surveyed");f.setAccessible(true);f.setBoolean(m,true);}catch(Exception e){throw new RuntimeException(e);}
            m.setItem(0,KilnContent.MINERAL_COAL.toStack(16));CoalFireFixture.light(m);var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();p.teleportTo(l,8,114,-40,java.util.Set.of(),9.5f,0);
        });
        if(ticks==265){mc.options.fov().set(40);mc.options.hideGui=true;}
        if(ticks==310)shot(mc,"front");
        if(ticks==315)check(mc,()->{var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),-21,114,42,java.util.Set.of(),208,0);});
        if(ticks==355)shot(mc,"rear");
        if(ticks==360)check(mc,()->{var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),13,119,-9,java.util.Set.of(),43,10);});
        if(ticks==395)shot(mc,"gallery-detail");
        if(ticks==400)check(mc,()->{var p=server.getPlayerList().getPlayers().getFirst();p.getAbilities().flying=false;p.onUpdateAbilities();p.teleportTo(server.overworld(),.5,116.5,1.9,java.util.Set.of(),0,5);});
        if(ticks==425)check(mc,()->{var p=server.getPlayerList().getPlayers().getFirst();if(!p.onGround()||Math.abs(p.getY()-116.40625)>.04)throw new IllegalStateException("Gallery collision does not support the actual player: "+p.getY()+" ground="+p.onGround());galleryChecked=true;});
        if(ticks==430)shot(mc,"on-gallery");
        if(ticks==435)check(mc,()->{var p=server.getPlayerList().getPlayers().getFirst();p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(server.overworld(),7,104,-6,java.util.Set.of(),34,0);});
        if(ticks==465)shot(mc,"working-base");
        if(ticks==476)shot(mc,"working-next");
        if(ticks==480){mc.options.hideGui=false;check(mc,()->{var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),.5,102,-3,java.util.Set.of(),0,15);p.openMenu((IndustrialBlockEntity)server.overworld().getBlockEntity(AT));});}
        if(ticks==510){if(((IndustrialMenu)mc.player.containerMenu).data.get(13)!=1)throw new IllegalStateException("Built flag did not reach cabinet");shot(mc,"assembled-cabinet");}
        if(ticks>520){if(!galleryChecked)throw new IllegalStateException("Gallery review incomplete");com.mojang.logging.LogUtils.getLogger().info("DERRICK GUIDE VERIFIED: actual held-stack build, partial/damaged guides, complete native tower, working cycle and actual gallery support");mc.stop();}
    }
    private static void profileGuide(Minecraft mc){
        var m=(IndustrialBlockEntity)mc.level.getBlockEntity(AT);var log=com.mojang.logging.LogUtils.getLogger();
        for(int mask:new int[]{0,m.derrickSections}){
            long oldTime=0,newTime=0;int count=0;
            for(int repeat=0;repeat<3;repeat++){
                long start=System.nanoTime();var expected=new java.util.HashMap<Integer,net.minecraft.world.phys.AABB>();
                for(int i=0;i<ModeledDerrick.CELLS.size();i++){var cell=ModeledDerrick.CELLS.get(i);var shape=cell.shape(m.front(),ModeledDerrick.ALL^mask);if(!shape.isEmpty())expected.put(i,shape.bounds().move(ModeledDerrick.position(AT,m.front(),cell)));}
                oldTime+=System.nanoTime()-start;start=System.nanoTime();var snapshot=dev.civilization.client.DerrickGuideGeometry.build(AT,m.front(),mask);newTime+=System.nanoTime()-start;count=snapshot.cells().size();
                if(count!=expected.size())throw new IllegalStateException("Cached guide lost missing cells");
                for(var cell:snapshot.cells())if(!cell.box().equals(expected.get(cell.index())))throw new IllegalStateException("Cached guide changed collision outline bounds");
            }
            log.info("DERRICK GUIDE CPU: mask={}, cells={}, old voxel unions={} ms/build, cached bounds snapshot={} ms/build; render performs zero voxel unions",mask,count,oldTime/3e6,newTime/3e6);
        }
    }
    private static void useController(Minecraft mc){mc.gameMode.useItemOn(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(.5,101.5,0),net.minecraft.core.Direction.NORTH,AT,false));}
    private static void check(Minecraft mc,Runnable action){mc.getSingleplayerServer().execute(()->{try{action.run();}catch(Throwable e){failure=e.toString();}});}
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"derrick-"+name+".png",mc.getMainRenderTarget(),m->{});}
}
