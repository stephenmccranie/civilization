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
                l.setBlockAndUpdate(AT,IndustrialContent.DERRICK.get().defaultBlockState());var p=server.getPlayerList().getPlayers().getFirst();p.setHealth(p.getMaxHealth());p.fallDistance=0;p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.getAbilities().flying=false;p.onUpdateAbilities();p.teleportTo(l,.5,102,-3,java.util.Set.of(),0,15);ready=true;
            });}
        if(ticks>100&&!ready){ticks=101;return;}
        if(ticks==135)shot(mc,"construction-guide");
        if(ticks==140)check(mc,()->server.getPlayerList().getPlayers().getFirst().setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE,7)));
        if(ticks==145)useController(mc);
        if(ticks==160)check(mc,()->{var m=(IndustrialBlockEntity)server.overworld().getBlockEntity(AT);var p=server.getPlayerList().getPlayers().getFirst();if(m.build().sections!=1||!p.getOffhandItem().isEmpty())throw new IllegalStateException("Actual held-stack footing construction failed");p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_PLANKS,BlockCarry.LIMIT));});
        if(ticks==165)useController(mc);
        if(ticks==175)check(mc,()->{var p=server.getPlayerList().getPlayers().getFirst();var m=(IndustrialBlockEntity)server.overworld().getBlockEntity(AT);if(Integer.bitCount(m.build().sections)!=2||!p.getOffhandItem().isEmpty()||m.build().paidCount(2)!=2)throw new IllegalStateException("An armful of planks did not build one frame panel and pay two planks toward the next");ModeledMachine.build(m,p,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_PLANKS,54));if(Integer.bitCount(m.build().sections)!=11)throw new IllegalStateException("Further armfuls did not build the partial tower");p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(server.overworld(),.5,110,-3,java.util.Set.of(),0,-15);});
        if(ticks==180){mc.options.fov().set(80);mc.gui.getChat().clearMessages(false);}
        if(ticks==183)profileGuide(mc);
        if(ticks==185)shot(mc,"partial-guide");
        if(ticks==200)check(mc,()->{var l=server.overworld();var m=(IndustrialBlockEntity)l.getBlockEntity(AT);var cell=MachineModel.OIL_DERRICK.cells.stream().filter(c->c.pieces().size()==1&&c.pieces().containsKey(1)).findFirst().orElseThrow();l.destroyBlock(ModeledMachine.position(AT,m.front(),cell),true);if(l.getBlockEntity(AT)!=m||Integer.bitCount(m.build().sections)!=10)throw new IllegalStateException("Panel damage collapsed tower");});
        if(ticks==220)shot(mc,"damaged-guide");
        if(ticks==240)check(mc,()->{var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(l,.5,102,-3,java.util.Set.of(),0,15);var m=(IndustrialBlockEntity)l.getBlockEntity(AT);for(var stock:java.util.List.of(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_PLANKS,6),new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_PLANKS,60),new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_PLANKS,64),new net.minecraft.world.item.ItemStack(IndustrialBlockEntity.supply(IndustrialBlock.Kind.DERRICK),4),new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("civilization:machine_parts")),2),IndustrialContent.PORT.toStack()))ModeledMachine.build(m,p,stock);});
        if(ticks==260)check(mc,()->{var l=server.overworld();var m=(IndustrialBlockEntity)l.getBlockEntity(AT);if(m.build().sections!=MachineModel.OIL_DERRICK.all||!IndustrialStructure.bind(m))throw new IllegalStateException("Progressive construction left sections unbuilt: "+Integer.toBinaryString(MachineModel.OIL_DERRICK.all&~m.build().sections));
            var samples=new StringBuilder();for(int y:new int[]{0,4,12,20,28}){
                var center=AT.above(y);samples.append(" y=").append(y).append(':').append(l.getBrightness(net.minecraft.world.level.LightLayer.SKY,center));
                for(var side:net.minecraft.core.Direction.Plane.HORIZONTAL)samples.append('/').append(l.getBrightness(net.minecraft.world.level.LightLayer.SKY,center.relative(side,5)));
            }
            System.out.println("DERRICK_SKY_SAMPLES controllerPacked="+net.minecraft.client.renderer.LevelRenderer.getLightColor(l,AT)+samples);
            var site=new Deposits.Site(0,101,0,Deposits.Kind.OIL,2);int stock=0;for(int i=0;i<site.cells()&&stock<20;i++){var pos=site.cell(i);if(site.body(pos)){l.setBlockAndUpdate(pos,IndustrialContent.RESERVOIR_OIL.get().defaultBlockState());stock++;}}
            try{var f=IndustrialBlockEntity.class.getDeclaredField("site");f.setAccessible(true);f.set(m,site);f=IndustrialBlockEntity.class.getDeclaredField("surveyed");f.setAccessible(true);f.setBoolean(m,true);}catch(Exception e){throw new RuntimeException(e);}
            m.struck=true;MachineFixture.coalEngine(l,m);var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();p.teleportTo(l,8,114,-40,java.util.Set.of(),9.5f,0);
        });
        if(ticks==265){mc.options.fov().set(40);mc.options.hideGui=true;}
        if(ticks==310)shot(mc,"front");
        if(ticks==315)check(mc,()->{var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),-21,114,42,java.util.Set.of(),208,0);});
        if(ticks==355)shot(mc,"rear");
        if(ticks==360)check(mc,()->{var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),13,119,-9,java.util.Set.of(),43,10);});
        if(ticks==395)shot(mc,"gallery-detail");
        if(ticks==400)check(mc,()->{var p=server.getPlayerList().getPlayers().getFirst();p.getAbilities().flying=false;p.onUpdateAbilities();p.teleportTo(server.overworld(),.5,116.5,1.9,java.util.Set.of(),0,5);});
        if(ticks==425)check(mc,()->{var p=server.getPlayerList().getPlayers().getFirst();if(!p.onGround()||Math.abs(p.getY()-116.46875)>.04)throw new IllegalStateException("Gallery collision does not support the actual player: "+p.getY()+" ground="+p.onGround());profileCornerCollision(mc);galleryChecked=true;});
        if(ticks==430)shot(mc,"on-gallery");
        if(ticks==435)check(mc,()->{var p=server.getPlayerList().getPlayers().getFirst();p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(server.overworld(),7,104,-6,java.util.Set.of(),34,0);});
        if(ticks==465)shot(mc,"working-base");
        if(ticks==476)shot(mc,"working-next");
        if(ticks==480){mc.options.hideGui=false;check(mc,()->{var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),.5,102,-3,java.util.Set.of(),0,15);p.openMenu((IndustrialBlockEntity)server.overworld().getBlockEntity(AT));});}
        if(ticks==510){if(((IndustrialMenu)mc.player.containerMenu).data.get(12)!=1)throw new IllegalStateException("Built flag did not reach cabinet");shot(mc,"assembled-cabinet");}
        if(ticks==520){mc.setScreen(null);mc.options.hideGui=true;mc.options.fov().set(70);check(mc,()->{var p=server.getPlayerList().getPlayers().getFirst();p.closeContainer();p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(server.overworld(),.5,121,-1,java.util.Set.of(),0,-30);});}
        if(ticks==545)shot(mc,"crown-level-photon");
        if(ticks==550)check(mc,()->server.getPlayerList().getPlayers().getFirst().teleportTo(server.overworld(),.5,121,-1,java.util.Set.of(),0,-55));
        if(ticks==575){verifyCrownRenderer(mc);shot(mc,"crown-up-photon");}
        if(ticks==580){shaders(false);check(mc,()->server.getPlayerList().getPlayers().getFirst().teleportTo(server.overworld(),.5,102,-3,java.util.Set.of(),0,0));}
        // Shader reload discards Sodium's compiled sections. Warm the controller
        // section again before repeating the same crown camera regression.
        if(ticks==620)check(mc,()->server.getPlayerList().getPlayers().getFirst().teleportTo(server.overworld(),.5,121,-1,java.util.Set.of(),0,-55));
        if(ticks==650){verifyCrownRenderer(mc);shot(mc,"crown-up-sodium");}
        if(ticks==655)check(mc,()->server.getPlayerList().getPlayers().getFirst().teleportTo(server.overworld(),.5,121,-1,java.util.Set.of(),0,-30));
        if(ticks==680)shot(mc,"crown-level-sodium");
        if(ticks==685)shaders(true);
        if(ticks==700){
            int light=dev.civilization.client.ModeledMachineRenderer.cellLight(mc.level,AT);
            if(net.minecraft.client.renderer.LightTexture.sky(light)!=15)throw new IllegalStateException("Exposed derrick's controller cell must take full skylight, not "+net.minecraft.client.renderer.LightTexture.sky(light));
            check(mc,()->{server.overworld().setDayTime(18000);server.getPlayerList().getPlayers().getFirst().teleportTo(server.overworld(),7,104,-6,java.util.Set.of(),34,0);});
        }
        if(ticks==745)shot(mc,"night-photon");
        if(ticks>760){if(!galleryChecked)throw new IllegalStateException("Gallery review incomplete");com.mojang.logging.LogUtils.getLogger().info("DERRICK GUIDE VERIFIED: held-stack build, partial/damaged guides, working cycle, gallery support and crown look-up with Sodium, both Photon and shaders disabled, exposed ambient skylight and night response");mc.stop();}
    }
    private static void verifyCrownRenderer(Minecraft mc){
        var m=(IndustrialBlockEntity)mc.level.getBlockEntity(AT);var renderer=mc.getBlockEntityRenderDispatcher().getRenderer(m);
        if(renderer==null||!renderer.shouldRenderOffScreen(m)||!renderer.shouldRender(m,mc.gameRenderer.getMainCamera().getPosition()))throw new IllegalStateException("Derrick must remain eligible when its controller section leaves view");
        if(renderer.shouldRenderOffScreen(new IndustrialBlockEntity(AT,IndustrialContent.REFINERY.get().defaultBlockState())))throw new IllegalStateException("Only derricks should bypass controller-section culling");
        // The crown's top rail stands 25.875 blocks above the controller.
        if(!renderer.getRenderBoundingBox(m).contains(.5,126.8,5))throw new IllegalStateException("Render bounds omit the crown");
        if(renderer.shouldRender(m,new net.minecraft.world.phys.Vec3(500,121,0)))throw new IllegalStateException("Global registration bypassed the view-distance limit");
        if(mc.levelRenderer.getFrustum().isVisible(new net.minecraft.world.phys.AABB(AT)))throw new IllegalStateException("Crown regression camera must exclude the ground controller");
        try{
            var sodium=Class.forName("net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer");var world=sodium.getMethod("instance").invoke(null);
            var seen=new java.util.concurrent.atomic.AtomicBoolean();
            java.util.function.Consumer<net.minecraft.world.level.block.entity.BlockEntity> visitor=b->{if(b==m)seen.set(true);};
            sodium.getMethod("iterateVisibleBlockEntities",java.util.function.Consumer.class).invoke(world,visitor);
            if(!seen.get())throw new IllegalStateException("Sodium omitted the derrick at the crown look-up camera");
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Crown regression requires the configured Sodium runtime",e);}
    }
    private static void shaders(boolean enabled){
        try{
            var iris=Class.forName("net.irisshaders.iris.Iris");var config=iris.getMethod("getIrisConfig").invoke(null);
            config.getClass().getMethod("setShadersEnabled",boolean.class).invoke(config,enabled);
            // Only this disposable client's copied configuration is changed.
            config.getClass().getMethod("save").invoke(config);iris.getMethod("reload").invoke(null);
            if((boolean)iris.getMethod("isPackInUseQuick").invoke(null)!=enabled)throw new IllegalStateException("Shader toggle did not apply");
            com.mojang.logging.LogUtils.getLogger().info("DERRICK CROWN SHADERS: {}",enabled);
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Crown regression requires the configured Iris runtime",e);}
    }
    private static net.minecraft.world.phys.shapes.VoxelShape uncachedShape(MachineModel.Cell cell,net.minecraft.core.Direction front,int mask){
        var shape=net.minecraft.world.phys.shapes.Shapes.empty();for(var entry:cell.pieces().entrySet())if((mask&(1<<entry.getKey()))!=0)shape=net.minecraft.world.phys.shapes.Shapes.or(shape,entry.getValue()[front.get2DDataValue()]);return shape;
    }
    private static void profileCornerCollision(Minecraft mc){
        var level=mc.getSingleplayerServer().overworld();var m=(IndustrialBlockEntity)level.getBlockEntity(AT);
        var corner=MachineModel.OIL_DERRICK.cells.stream().filter(c->c.y()==15&&c.pieces().containsKey(21)).max(java.util.Comparator.comparingInt(c->c.pieces().size())).orElseThrow();
        var at=ModeledMachine.position(AT,m.front(),corner);var state=level.getBlockState(at);var getter=level.getChunkAt(at);
        var expected=uncachedShape(corner,m.front(),m.build().sections);var actual=state.getCollisionShape(getter,at);
        if(net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(expected,actual,net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME))throw new IllegalStateException("Corner collision changed geometry");
        long start=System.nanoTime();for(int i=0;i<20;i++)uncachedShape(corner,m.front(),m.build().sections);double old=(System.nanoTime()-start)/20e6;
        start=System.nanoTime();for(int i=0;i<1000;i++)if(state.getCollisionShape(getter,at)!=actual)throw new IllegalStateException("Corner collision rebuilt while walking");double cached=(System.nanoTime()-start)/1000e6;
        // Exercise Minecraft's actual movement collision path at all four gallery corners.
        var player=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();double max=0;
        for(double x:new double[]{-2.4,3.4})for(double z:new double[]{2.6,7.4}){
            player.teleportTo(level,x,116.46875,z,java.util.Set.of(),0,0);
            for(int step=0;step<20;step++){start=System.nanoTime();player.move(net.minecraft.world.entity.MoverType.SELF,new net.minecraft.world.phys.Vec3(step%2==0?.06:-.06,-.01,0));max=Math.max(max,(System.nanoTime()-start)/1e6);}
        }
        player.teleportTo(level,.5,116.46875,1.9,java.util.Set.of(),0,5);
        com.mojang.logging.LogUtils.getLogger().info("DERRICK CORNER CPU: sections={}, old={} ms/query, cached={} ms/query; 1000 identical chunk collision shapes; max of 80 corner movement calls={} ms",corner.pieces().size(),old,cached,max);
    }
    private static void profileGuide(Minecraft mc){
        var m=(IndustrialBlockEntity)mc.level.getBlockEntity(AT);var log=com.mojang.logging.LogUtils.getLogger();
        for(int mask:new int[]{0,m.build().sections}){
            long oldTime=0,newTime=0;int count=0;
            for(int repeat=0;repeat<3;repeat++){
                long start=System.nanoTime();var expected=new java.util.HashMap<Integer,net.minecraft.world.phys.AABB>();
                for(int i=0;i<MachineModel.OIL_DERRICK.cells.size();i++){var cell=MachineModel.OIL_DERRICK.cells.get(i);var shape=uncachedShape(cell,m.front(),MachineModel.OIL_DERRICK.all^mask);if(!shape.isEmpty())expected.put(i,shape.bounds().move(ModeledMachine.position(AT,m.front(),cell)));}
                // A missing port is shown as its whole block, under no cell index.
                for(var port:MachineModel.OIL_DERRICK.ports)if((mask&(1<<port.section()))==0)expected.put(-1,new net.minecraft.world.phys.AABB(MachineStructure.position(AT,m.front(),port.part())));
                oldTime+=System.nanoTime()-start;start=System.nanoTime();var snapshot=dev.civilization.client.MachineGuide.geometry(AT,m.front(),MachineModel.OIL_DERRICK,mask,java.util.List.of());newTime+=System.nanoTime()-start;count=snapshot.cells().size();
                if(count!=expected.size())throw new IllegalStateException("Cached guide lost missing cells");
                for(var cell:snapshot.cells())if(!cell.box().equals(expected.get(cell.index())))throw new IllegalStateException("Cached guide changed collision outline bounds");
            }
            log.info("DERRICK GUIDE CPU: mask={}, cells={}, old voxel unions={} ms/build, cached bounds snapshot={} ms/build; render performs zero voxel unions",mask,count,oldTime/3e6,newTime/3e6);
        }
    }
    private static void useController(Minecraft mc){mc.gameMode.useItemOn(mc.player,net.minecraft.world.InteractionHand.OFF_HAND,new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(.5,101.5,0),net.minecraft.core.Direction.NORTH,AT,false));}
    private static void check(Minecraft mc,Runnable action){mc.getSingleplayerServer().execute(()->{try{action.run();}catch(Throwable e){failure=e.toString();}});}
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"derrick-"+name+".png",mc.getMainRenderTarget(),m->{});}
}
