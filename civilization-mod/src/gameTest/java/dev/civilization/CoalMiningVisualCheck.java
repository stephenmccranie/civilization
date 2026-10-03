package dev.civilization;

import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Actual input, synchronized stock and rail hauling in the guarded disposable world. */
final class CoalMiningVisualCheck {
    private static final BlockPos FACE=new BlockPos(0,101,0),RAIL=new BlockPos(3,100,-1),SCREEN=new BlockPos(4,100,3),STOVE=new BlockPos(-3,100,-1);
    private static int ticks;
    private static volatile String failure;
    private static volatile boolean verified;
    private static double cameraShape;
    private static double idleLag;
    private static boolean windupShot,contactShot,followShot;
    private static int auditedContacts;
    static void tick(Minecraft mc){
        ticks++;if(failure!=null)throw new IllegalStateException(failure);var server=mc.getSingleplayerServer();
        if(ticks==100){mc.options.pauseOnLostFocus=false;mc.options.hideGui=false;mc.options.fov().set(60);mc.options.guiScale().set(3);mc.gui.getChat().clearMessages(true);mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);check(server,()->{
            var l=server.overworld();l.setDayTime(6000);var weather=RegionalWeather.get(l);var d=weather.district(l,RegionalWeather.key(0,0));d.rain=false;d.end=weather.clock+72000;weather.setDirty();
            for(int x=-6;x<=8;x++)for(int z=-7;z<=6;z++){l.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.STONE_BRICKS.defaultBlockState());for(int y=100;y<=108;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
            for(var e:l.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,new AABB(-7,98,-8,9,109,7)))if(e instanceof LooseCoalEntity||e instanceof CoalMinecart||e instanceof net.minecraft.world.entity.item.ItemEntity)e.discard();
            for(int x=-1;x<=1;x++)for(int y=100;y<=102;y++)l.setBlockAndUpdate(new BlockPos(x,y,0),CoalMiningContent.FACE.get().defaultBlockState());
            for(int z=-1;z<=3;z++)l.setBlockAndUpdate(new BlockPos(3,100,z),Blocks.RAIL.defaultBlockState());
            l.setBlockAndUpdate(SCREEN,CoalMiningContent.SCREEN.get().defaultBlockState());l.setBlockAndUpdate(STOVE,CookingContent.STATION.get().defaultBlockState());
            var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getInventory().clearContent();select(p,0);p.getInventory().setItem(0,CoalMiningContent.PICK.toStack());EquipmentGrade.apply(p.getMainHandItem(),100,12345);p.getInventory().setItem(1,CoalMiningContent.SHOVEL.toStack());p.getInventory().setItem(2,CoalMiningContent.CART_ITEM.toStack());p.getInventory().setItem(4,CoalMiningContent.SCREEN_ITEM.toStack());p.getInventory().setItem(5,CoalMiningContent.FACE_ITEM.toStack());CalorieFoodData.of(p).reserve().set(2400);CalorieFoodData.of(p).resetCounters();p.teleportTo(l,.5,100,-3,java.util.Set.of(),0,5);
        });}
        if(ticks==120)aim(server,FACE.getCenter().add(-.25,-.25,-.5));
        if(ticks==125){mc.player.setYRot(mc.player.getYRot()+24);mc.player.setXRot(mc.player.getXRot()-8);}
        if(ticks>=126&&ticks<=131)idleLag=Math.max(idleLag,Math.abs(dev.civilization.client.CoalMiningClient.lastSide));
        if(ticks==129)shot(mc,"camera-drag-idle");
        if(ticks==132)aim(server,FACE.getCenter().add(-.25,-.25,-.5));
        if(ticks==135&&idleLag<8)throw new IllegalStateException("Resting pick did not trail a camera turn: "+idleLag);
        if(ticks==135)check(server,()->assertFace(server,64,0)); // Camera movement alone never mines.
        if(ticks==140){click(mc);click(mc);}
        if(ticks>140&&ticks<163){
            double age=dev.civilization.client.CoalMiningClient.lastAge;
            if(!windupShot&&age>=6.5&&age<8){shot(mc,"windup");windupShot=true;}
            if(!contactShot&&age>=12&&age<15){shot(mc,"contact");contactShot=true;}
            if(!followShot&&age>=15&&age<19){shot(mc,"follow-through");followShot=true;}
        }
        if(ticks==165)check(server,()->assertFace(server,60,1));
        if(ticks==170)click(mc); // Duplicate input during recovery cannot accelerate the held cadence.
        if(ticks==177)check(server,()->assertFace(server,60,1));
        if(ticks==180)release(mc);
        if(ticks==200)check(server,()->assertFace(server,56,2)); // Held input repeated; releasing prevents a third swing.
        if(ticks==210)click(mc);
        if(ticks==213){mc.player.setYRot(mc.player.getYRot()+18);mc.player.setXRot(mc.player.getXRot()-10);}
        if(ticks>=214&&ticks<=224)cameraShape=Math.max(cameraShape,Math.abs(dev.civilization.client.CoalMiningClient.lastSide));
        if(ticks==217)shot(mc,"camera-shaped");
        if(ticks==218){release(mc);aim(server,FACE.getCenter().add(-.25,-.25,-.5));}
        if(ticks==235){if(cameraShape<8)throw new IllegalStateException("Camera did not visibly drag the rendered pick: "+cameraShape);check(server,()->assertFace(server,52,3));}
        if(ticks==250)mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        if(ticks==252)mc.options.keyAttack.setDown(true);
        if(ticks==270)check(server,()->assertFace(server,52,3)); // GUI input never schedules another swing.
        if(ticks==275){release(mc);mc.player.closeContainer();}
        if(ticks==275)aim(server,FACE.getCenter().add(.25,-.25,-.5));
        if(ticks==290)click(mc);
        if(ticks==300)release(mc);
        if(ticks==315){shot(mc,"notched-vein");check(server,()->{
            assertFace(server,48,4);var p=server.getPlayerList().getPlayers().getFirst();if(CalorieFoodData.of(p).broken!=4||auditedContacts!=4)throw new IllegalStateException("Chipping labor is not charged once per accepted contact: "+auditedContacts+" audited");select(p,1);
        });}
        if(ticks==335){var pile=findPile(mc);shot(mc,"loose-pile");use(mc,pile,Direction.UP);}
        if(ticks==345){shot(mc,"loaded-shovel");check(server,()->{var p=server.getPlayerList().getPlayers().getFirst();if(CoalMiningContent.load(p.getMainHandItem())!=16)throw new IllegalStateException("Actual shovel scoop did not collect 16 raw units: "+CoalMiningContent.load(p.getMainHandItem())+" loaded, "+server.overworld().getEntitiesOfClass(LooseCoalEntity.class,new AABB(FACE).inflate(4)).stream().mapToInt(LooseCoalEntity::units).sum()+" still falling");select(p,2);});}
        if(ticks==360)use(mc,RAIL,Direction.UP);
        if(ticks==375)check(server,()->select(server.getPlayerList().getPlayers().getFirst(),1));
        if(ticks==390)mc.gameMode.interact(mc.player,cart(mc),InteractionHand.MAIN_HAND);
        if(ticks==405){shot(mc,"loaded-cart");check(server,()->{var p=server.getPlayerList().getPlayers().getFirst();var c=cart(server);if(c.load()!=16||CoalMiningContent.load(p.getMainHandItem())!=0)throw new IllegalStateException("Actual shovel-to-cart transfer failed");select(p,3);p.teleportTo(server.overworld(),3.5,100,-2.5,java.util.Set.of(),0,20);});}
        if(ticks==425)shot(mc,"cart-cargo");
        if(ticks==440)mc.gameMode.interact(mc.player,cart(mc),InteractionHand.MAIN_HAND);
        if(ticks==500)check(server,()->{var c=cart(server);if(c.getZ()<1)throw new IllegalStateException("Native rail cart did not travel along the route: "+c.position());var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),3.5,100,1.3,java.util.Set.of(),-30,20);});
        if(ticks==520||ticks==535||ticks==550||ticks==565)use(mc,SCREEN,Direction.NORTH);
        if(ticks==577){shot(mc,"preparation-screen");check(server,()->{var s=(CoalScreenEntity)server.overworld().getBlockEntity(SCREEN);if(s.raw()!=0||s.coal()!=4||cart(server).load()!=0)throw new IllegalStateException("Actual paced preparation did not make four Coal");});}
        if(ticks==580)mc.options.keyShift.setDown(true);
        if(ticks==585)use(mc,SCREEN,Direction.NORTH);
        if(ticks==592)mc.options.keyShift.setDown(false);
        if(ticks==600)check(server,()->{
            var p=server.getPlayerList().getPlayers().getFirst();if(p.getInventory().countItem(KilnContent.MINERAL_COAL.get())!=4)throw new IllegalStateException("Actual crouch-click did not collect the existing usable Coal item");
            int slot=p.getInventory().findSlotMatchingItem(KilnContent.MINERAL_COAL.toStack());var fuel=p.getInventory().removeItem(slot,1);var stove=(KilnBlockEntity)server.overworld().getBlockEntity(STOVE);stove.setItem(1,fuel);
            // Seed only the disposable ignition fixture, retaining real CoalFire consumption.
            for(long seed=0;;seed++){var r=net.minecraft.util.RandomSource.create(seed);r.nextFloat();if(r.nextInt(3)==0){server.overworld().random.setSeed(seed);break;}}
            if(!stove.fire.strike(p,true)||!stove.fire.lit()||!stove.getItem(1).isEmpty()||stove.fireHeat()<=0)throw new IllegalStateException("Prepared Coal did not fuel the existing cooking station");verified=true;
        });
        if(ticks==610)mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        if(ticks==625)shot(mc,"inventory");
        if(ticks==630){mc.player.closeContainer();mc.options.hideGui=true;shaders(false);check(server,()->{var p=server.getPlayerList().getPlayers().getFirst();select(p,0);p.teleportTo(server.overworld(),.5,100,-3,java.util.Set.of(),0,10);});}
        if(ticks==680)shot(mc,"notched-vanilla");
        if(ticks==685){mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);check(server,()->server.getPlayerList().getPlayers().getFirst().teleportTo(server.overworld(),-1.5,100,-2,java.util.Set.of(),-40,5));}
        if(ticks==715)shot(mc,"player-scale");
        if(ticks==720){mc.options.setCameraType(CameraType.FIRST_PERSON);shaders(true);}
        if(ticks>730){if(!verified)throw new IllegalStateException("Server loop acceptance missing");com.mojang.logging.LogUtils.getLogger().info("COAL MINING VISUAL VERIFIED: held repeat cadence, release/GUI guards, camera-only guard, overhead rendered arc, partial synchronized face, exact stock/wear/labor, visible shovel load, native rail push, paced screen, existing Coal consumption, inventory, Photon and shaders disabled");mc.stop();}
    }
    private static void select(net.minecraft.server.level.ServerPlayer p,int slot){p.getInventory().selected=slot;p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(slot));}
    private static void assertFace(net.minecraft.server.MinecraftServer s,int cells,int wear){var e=(CoalWorkfaceEntity)s.overworld().getBlockEntity(FACE);if(e==null||Long.bitCount(e.mask())!=cells||s.getPlayerList().getPlayers().getFirst().getMainHandItem().getDamageValue()!=wear)throw new IllegalStateException("Face/contact mismatch: "+(e==null?"missing":Long.bitCount(e.mask()))+" cells, expected "+cells);}
    private static BlockPos findPile(Minecraft mc){for(int x=-1;x<=1;x++)for(int z=-2;z<=0;z++){var p=new BlockPos(x,100,z);if(mc.level.getBlockState(p).is(CoalMiningContent.PILE.get()))return p;}throw new IllegalStateException("Settled raw pile not synchronized");}
    private static CoalMinecart cart(Minecraft mc){return mc.level.getEntitiesOfClass(CoalMinecart.class,new AABB(2,99,-3,5,103,6)).getFirst();}
    private static CoalMinecart cart(net.minecraft.server.MinecraftServer s){return s.overworld().getEntitiesOfClass(CoalMinecart.class,new AABB(2,99,-3,5,103,6)).getFirst();}
    private static void click(Minecraft mc){mc.options.keyAttack.setDown(true);var e=new net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered(0,mc.options.keyAttack,InteractionHand.MAIN_HAND);net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(e);if(!e.isCanceled()||e.shouldSwingHand())throw new IllegalStateException("Ordinary pick action was not suppressed");}
    private static void release(Minecraft mc){mc.options.keyAttack.setDown(false);}
    private static void use(Minecraft mc,BlockPos p,Direction face){mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(p.getCenter(),face,p,false));}
    private static void aim(net.minecraft.server.MinecraftServer s,Vec3 target){check(s,()->{var p=s.getPlayerList().getPlayers().getFirst();var d=target.subtract(p.getEyePosition());p.teleportTo(p.serverLevel(),p.getX(),p.getY(),p.getZ(),java.util.Set.of(),(float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))));});}
    private static void check(net.minecraft.server.MinecraftServer s,Runnable r){s.execute(()->{try{r.run();}catch(Throwable e){failure=e.toString();}});}
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"coal-mining-"+name+".png",mc.getMainRenderTarget(),m->{});}
    private static void shaders(boolean enabled){try{var iris=Class.forName("net.irisshaders.iris.Iris");var config=iris.getMethod("getIrisConfig").invoke(null);config.getClass().getMethod("setShadersEnabled",boolean.class).invoke(config,enabled);config.getClass().getMethod("save").invoke(config);iris.getMethod("reload").invoke(null);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    @net.neoforged.fml.common.EventBusSubscriber(modid="civilization",value=net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class LaborAudit {
        private static long broken;private static double labor,cost;
        @net.neoforged.bus.api.SubscribeEvent public static void before(net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre e){
            if(!System.getProperty("civilization.previewCheck","").equals("coal-mining")||!(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)||!p.getMainHandItem().is(CoalMiningContent.PICK.get()))return;
            var d=CalorieFoodData.of(p);broken=d.broken;labor=d.laborSpent;cost=2*ThermalRules.calorieFactor(ThermalSystem.comfort(p))*d.workFactor();
        }
        @net.neoforged.bus.api.SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST) public static void after(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e){
            if(!System.getProperty("civilization.previewCheck","").equals("coal-mining")||!(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)||!p.getMainHandItem().is(CoalMiningContent.PICK.get()))return;
            var d=CalorieFoodData.of(p);if(d.broken==broken)return;
            if(d.broken!=broken+1||Math.abs(d.laborSpent-labor-cost)>.00001)failure="Contact labor differs from one comfort/meal-adjusted charge";else auditedContacts++;
        }
    }
}
