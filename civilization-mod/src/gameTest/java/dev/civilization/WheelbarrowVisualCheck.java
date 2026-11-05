package dev.civilization;

import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Real placement/interaction packets, actual client controls and synchronized geometry. */
final class WheelbarrowVisualCheck {
    // The cargo, pallet and Hand checks keep their own clock, which starts where handling ends.
    private static final int REST=520;
    /** A full stack of dirt, more than the Survival armful. */
    private static final int FULL=new ItemStack(Blocks.DIRT).getMaxStackSize();
    private static int ticks;
    private static volatile String failure;
    private static volatile boolean verified;
    private static volatile int entityId;
    private static volatile double stopZ;
    private static float headingBefore,walkStart;
    private static volatile boolean cargoVerified;
    private static boolean handMenuVerified;
    private static volatile boolean creativeVerified;
    private static boolean palletViewsVerified,handLayoutsVerified;
    static void tick(Minecraft mc){
        ticks++;if(failure!=null)throw new IllegalStateException(failure);var server=mc.getSingleplayerServer();
        if(ticks<REST)handling(mc,server,ticks);else rest(mc,server,ticks-REST+360);
    }
    /** Taking the handles, the first-person view, walking with its bob, steering by keys and facing, a hill, the measured hitbox, and the tray filling one for one. */
    private static void handling(Minecraft mc,MinecraftServer server,int t){
        if(t==100){mc.options.pauseOnLostFocus=false;mc.options.hideGui=false;mc.options.fov().set(65);mc.options.bobView().set(true);mc.gui.getChat().clearMessages(true);mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);check(server,()->{
            var l=server.overworld();l.setDayTime(6000);var weather=RegionalWeather.get(l);var d=weather.district(l,RegionalWeather.key(0,0));d.rain=false;d.end=weather.clock+72000;weather.setDirty();
            for(int x=-26;x<=9;x++)for(int z=-8;z<=9;z++){l.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.STONE_BRICKS.defaultBlockState());for(int y=100;y<=106;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
            for(var e:l.getEntitiesOfClass(WheelbarrowEntity.class,new AABB(-28,98,-12,12,110,12)))e.discard();
            for(int x=-4;x<=4;x++)for(int y=100;y<=102;y++)l.setBlockAndUpdate(new BlockPos(x,y,5),Blocks.STONE_BRICKS.defaultBlockState());
            // A hill to cross westward: one-block rises every two blocks up to three high, then back down.
            for(int x=-18;x<=-8;x++)for(int z=-7;z<=-2;z++)for(int y=0;y<Math.min(3,Math.min((-x-6)/2,(x+20)/2));y++)l.setBlockAndUpdate(new BlockPos(x,100+y,z),Blocks.STONE_BRICKS.defaultBlockState());
            var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();p.getInventory().selected=0;p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));p.setItemInHand(InteractionHand.MAIN_HAND,WheelbarrowContent.ITEM.toStack());p.inventoryMenu.broadcastFullState();p.teleportTo(l,.5,100,-2,java.util.Set.of(),0,20);
        });}
        if(t==130)mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(.5,100,.5),Direction.UP,new BlockPos(0,99,0),false));
        if(t==150){var e=barrow(mc);entityId=e.getId();check(server,()->{var b=barrow(server);b.setYRot(45.125f);b.setPos(b.position());var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,WheelbarrowContent.ITEM.toStack());p.inventoryMenu.broadcastFullState();var at=b.handlerPosition(b.position(),45);p.teleportTo(server.overworld(),at.x,at.y,at.z,java.util.Set.of(),45,20);});}
        if(t==160&&Math.abs(barrow(mc).getYRot()-45.125f)>.002f)throw new IllegalStateException("Client heading was rounded to native angle bytes: "+barrow(mc).getYRot());
        if(t==165){var e=barrow(mc);var point=e.position().add(WheelbarrowEntity.GRIP.yRot((180-e.getYRot())*Mth.DEG_TO_RAD));mc.gameMode.interactAt(mc.player,e,new EntityHitResult(e,point),InteractionHand.MAIN_HAND);}
        if(t==175&&Math.abs(Mth.wrapDegrees(mc.player.getYRot()-barrow(mc).getYRot()))>.01f)throw new IllegalStateException("Taking handles did not align the first-person heading");
        // Handler teleports turn the view by nothing (a relative turn to their own yaw): it comes round with the handles.
        if(t==180){if(!(mc.player.getVehicle() instanceof WheelbarrowEntity))throw new IllegalStateException("Taking diagonal handles while holding the placement item did not synchronize");check(server,()->{var e=barrow(server);e.setYRot(0);e.setPos(e.position());var p=server.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.inventoryMenu.broadcastFullState();var at=e.handlerPosition(e.position(),0);p.connection.teleport(at.x,at.y,at.z,p.getYRot(),20,java.util.Set.of(net.minecraft.world.entity.RelativeMovement.Y_ROT));});}
        if(t==192)shot(mc,"start");
        if(t==193)pitch(mc,40);
        if(t==197)shot(mc,"start-grips");
        if(t==198)pitch(mc,20);
        if(t==200){dev.civilization.client.WheelbarrowRenderCheck.gaitCheck=true;walkStart=mc.player.walkDist;mc.options.keyUp.setDown(true);}
        if(t==209)shot(mc,"walk-bob-a");
        if(t==214){
            // Pushing at a steady pace is walking: vanilla's walk bob holds a full stride, so View Bobbing applies as on foot.
            if(mc.player.bob<.09f||Math.abs(mc.player.bob-mc.player.oBob)>.005f||mc.player.walkDist-walkStart<.6f)throw new IllegalStateException("Pushing did not bob like walking: bob="+mc.player.oBob+".."+mc.player.bob+" walked="+(mc.player.walkDist-walkStart));
            shot(mc,"walk-bob-b");
        }
        if(t==216){var e=barrow(mc);observe(mc,new Vec3(e.getX()+5,101.4,e.getZ()-.5),90,12);}
        if(t==218)shot(mc,"walk-side");
        if(t==219)observe(mc,null,0,0);
        if(t==228){mc.options.keyUp.setDown(false);check(server,()->{var e=barrow(server);if(e.getZ()<3.5||e.getBoundingBox().maxZ>5.01)throw new IllegalStateException("Wall collision or push failed: "+e.position()+" / "+e.getBoundingBox());stopZ=e.getZ();});}
        if(t==236){check(server,()->{if(Math.abs(barrow(server).getZ()-stopZ)>.03)throw new IllegalStateException("Released forward key kept driving");});mc.options.keyDown.setDown(true);}
        if(t==246){mc.options.keyDown.setDown(false);check(server,()->{if(barrow(server).getZ()>stopZ-.6)throw new IllegalStateException("S did not back the carrier up");});}
        if(t==247){dev.civilization.client.WheelbarrowRenderCheck.cameraCheck=true;headingBefore=barrow(mc).getYRot();mc.options.keyRight.setDown(true);}
        // The server turns once per its own tick, a tick or two out of step with the keys; the view comes round
        // with the handles a couple of ticks behind them and eases out after them.
        if(t==263){mc.options.keyRight.setDown(false);mc.options.keyLeft.setDown(true);}
        if(t==269)mc.options.keyLeft.setDown(false);
        if(t==275){
            dev.civilization.client.WheelbarrowRenderCheck.cameraCheck=false;
            if(dev.civilization.client.WheelbarrowRenderCheck.rightCameraSamples<10||dev.civilization.client.WheelbarrowRenderCheck.leftCameraSamples<5)throw new IllegalStateException("Insufficient right/left key-turn samples");
        }
        if(t==280){followed(mc,"A/D");float steered=Mth.wrapDegrees(barrow(mc).getYRot()-headingBefore);if(steered<15)throw new IllegalStateException("A/D steered the carrier only "+steered+" degrees");dev.civilization.client.WheelbarrowRenderCheck.assertHeading(barrow(mc));shot(mc,"keys-turned");}
        if(t==281){headingBefore=barrow(mc).getYRot();mc.player.turn(-70/.15,0);}
        if(t==284)shot(mc,"mouse-look");
        if(t==314){lookedAway(mc,-70,"a mouse turn");dev.civilization.client.WheelbarrowRenderCheck.assertHeading(barrow(mc));}
        if(t==315){mc.player.turn(160/.15,0);float offset=Mth.wrapDegrees(mc.player.getYRot()-barrow(mc).getYRot());if(Math.abs(offset)>WheelbarrowEntity.LOOK+.01f)throw new IllegalStateException("The handler looked "+offset+" degrees away from the handles");}
        if(t==317)shot(mc,"look-limit");
        if(t==342){lookedAway(mc,WheelbarrowEntity.LOOK,"the look limit");mc.player.turn(-WheelbarrowEntity.LOOK/.15f,0);}
        if(t==344)check(server,()->{var e=barrow(server);e.setYRot(90);e.setPos(-4.5,100,-4.5);var p=server.getPlayerList().getPlayers().getFirst();var at=e.handlerPosition(e.position(),90);p.connection.teleport(at.x,at.y,at.z,p.getYRot(),20,java.util.Set.of(net.minecraft.world.entity.RelativeMovement.Y_ROT));});
        if(t==350)mc.options.keyUp.setDown(true);
        if(t==368){if(barrow(mc).carriagePitch(1)<10)throw new IllegalStateException("Climbing did not tip the carriage up: "+barrow(mc).carriagePitch(1));shot(mc,"climb");}
        if(t==369)observe(mc,new Vec3(barrow(mc).getX()-.5,102.6,1.5),180,15);
        if(t==371)shot(mc,"climb-side");
        if(t==372)observe(mc,null,0,0);
        if(t==412){if(barrow(mc).carriagePitch(1)>-10)throw new IllegalStateException("Descending did not tip the carriage down: "+barrow(mc).carriagePitch(1));shot(mc,"descend");}
        if(t==413)observe(mc,new Vec3(barrow(mc).getX()-.5,103.6,1.5),180,15);
        if(t==415)shot(mc,"descend-side");
        if(t==416)observe(mc,null,0,0);
        if(t==452){mc.options.keyUp.setDown(false);check(server,()->{var e=barrow(server);if(e.getX()>-19||Math.abs(e.getY()-100)>1e-6)throw new IllegalStateException("The carrier did not cross the hill: "+e.position());});}
        if(t==455)mc.options.keyShift.setDown(true);
        if(t==460){
            mc.options.keyShift.setDown(false);dev.civilization.client.WheelbarrowRenderCheck.gaitCheck=false;
            if(dev.civilization.client.WheelbarrowRenderCheck.gaitSamples<150)throw new IllegalStateException("Insufficient real walking pose samples");
            check(server,()->{
                var e=barrow(server);if(e.isVehicle())throw new IllegalStateException("Release did not end pushing");
                // Parked diagonally beside a block that an axis-aligned envelope would cover.
                e.setYRot(45);e.setPos(-21.5,100,-4.5);server.overworld().setBlockAndUpdate(new BlockPos(-21,100,-4),Blocks.STONE_BRICKS.defaultBlockState());
            });
        }
        if(t==462){mc.getEntityRenderDispatcher().setRenderHitBoxes(true);mc.options.hideGui=true;observe(mc,new Vec3(-21.5,104.5,-8.5),0,48);}
        if(t==468){check(server,()->{if(Math.abs(barrow(server).getY()-100)>1e-6)throw new IllegalStateException("A block beside the body lifted the carrier");});shot(mc,"hitbox-diagonal");}
        if(t==470){mc.getEntityRenderDispatcher().setRenderHitBoxes(false);observe(mc,null,0,0);check(server,()->server.overworld().setBlockAndUpdate(new BlockPos(-21,100,-4),Blocks.AIR.defaultBlockState()));}
        // The tray draws every block it holds: two 4x4 layers below the rim, a third rising past it, then a mixed heap.
        if(t==472){check(server,()->{var e=barrow(server);e.setYRot(0);e.setPos(-21.5,100,-4.5);e.cargo().insert(new ItemStack(Blocks.STONE),16);});observe(mc,new Vec3(-19.6,101.9,-2.3),139,30);}
        if(t==483){
            // Walk across the front, through the tyre's point: the real client, with the pack's Lithium, rides around the taper onto the tread line.
            var e=barrow(mc);if(Math.abs(Mth.wrapDegrees(e.getYRot()))>.01f)throw new IllegalStateException("The parked barrow had not settled facing south: "+e.getYRot());
            // A step that rounds the point keeps its motion along the taper, so it may end up to one step's rise beyond the tread line.
            var back=mc.player.position();mc.player.setPos(e.getX()+.9,e.getY(),e.getZ()+1.3);
            for(int i=0;i<18;i++){mc.player.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(-.1,0,0));if(e.body().overlaps(Solid.of(mc.player.getBoundingBox())))throw new IllegalStateException("Walking across the wheel entered its body at "+mc.player.position());}
            double beyond=mc.player.getZ()-mc.player.getBbWidth()/2-e.body().bounds().maxZ;
            if(beyond<-1e-6||beyond>.05||mc.player.getX()>e.getX()-.4)throw new IllegalStateException("Walking across the wheel did not ride around its point: "+mc.player.position()+" beside "+e.position());
            mc.player.setPos(back);
        }
        if(t==478)shot(mc,"tray-layer");
        if(t==479)check(server,()->{var c=barrow(server).cargo();c.insert(new ItemStack(Blocks.OAK_PLANKS),16);c.insert(new ItemStack(Blocks.STONE_BRICKS),16);});
        if(t==485)shot(mc,"tray-rim");
        if(t==486)check(server,()->{
            var c=barrow(server).cargo();c.insert(new ItemStack(Blocks.COBBLESTONE),6);c.insert(new ItemStack(Blocks.STONE_SLAB),8);c.insert(CuttingContent.stack(Blocks.STONE.defaultBlockState(),3,1),48);
            if(c.load()!=WheelbarrowEntity.GRID.capacity()||c.insert(new ItemStack(Blocks.STONE),1)!=0)throw new IllegalStateException("The heaped tray did not hold exactly its drawn 64 blocks: "+c.load());
        });
        if(t==492)shot(mc,"tray-heaped");
        if(t==494){mc.options.hideGui=false;observe(mc,null,0,0);check(server,()->{var e=barrow(server);var p=server.getPlayerList().getPlayers().getFirst();var at=e.handlerPosition(e.position(),0);p.teleportTo(server.overworld(),at.x,at.y,at.z,java.util.Set.of(),0,20);});}
        if(t==500){var e=barrow(mc);var point=e.position().add(WheelbarrowEntity.GRIP.yRot((180-e.getYRot())*Mth.DEG_TO_RAD));mc.gameMode.interactAt(mc.player,e,new EntityHitResult(e,point),InteractionHand.MAIN_HAND);}
        if(t==506){if(mc.player.getVehicle()!=barrow(mc))throw new IllegalStateException("Could not take the heaped barrow's handles");shot(mc,"heaped-first-person");mc.options.keyShift.setDown(true);}
        if(t==510){mc.options.keyShift.setDown(false);check(server,()->{var c=barrow(server).cargo();while(!c.isEmpty())c.remove(0,Long.MAX_VALUE);});}
    }
    private static void rest(Minecraft mc,MinecraftServer server,int t){
        if(t==360){mc.options.keyShift.setDown(false);mc.options.hideGui=true;mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);check(server,()->{var e=barrow(server);if(e.isVehicle())throw new IllegalStateException("Release did not end pushing");e.setYRot(0);e.setPos(.5,100,.5);verified=true;var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),-.9,100,.5,java.util.Set.of(),135,12);});}
        if(t==390){shot(mc,"standing-scale");shot(mc,"photon-front");}
        if(t==400){mc.options.setCameraType(CameraType.FIRST_PERSON);check(server,()->{var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SPECTATOR);p.teleportTo(server.overworld(),3.7,102,4.2,java.util.Set.of(),140,18);});}
        if(t==430)shot(mc,"photon-rear");
        if(t==440){mc.options.hideGui=false;check(server,()->{var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.teleportTo(server.overworld(),-3,101.5,-3.7,java.util.Set.of(),-40,10);p.setItemInHand(InteractionHand.MAIN_HAND,WheelbarrowContent.ITEM.toStack());});}
        if(t==465){mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));}
        if(t==480)shot(mc,"inventory");
        if(t==485)mc.setScreen(null);
        if(t==495)shaders(false);
        if(t==535)shot(mc,"shaders-disabled");
        if(t==545)shaders(true);
        if(t==560){check(server,()->{
            var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.teleportTo(server.overworld(),2.5,100,-1,java.util.Set.of(),0,20);
            p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(CargoContent.STOCKPILE.get()));p.inventoryMenu.broadcastFullState();
        });}
        if(t==570){if(!mc.player.getMainHandItem().is(CargoContent.STOCKPILE.get().asItem())||!mc.player.getOffhandItem().isEmpty())throw new IllegalStateException("Pallet was forced out of the selected hotbar slot");shot(mc,"pallet-main-hand");}
        if(t==575)mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(2.5,100,.5),Direction.UP,new BlockPos(2,99,0),false));
        if(t==580)check(server,()->{
            var s=(StockpileEntity)server.overworld().getBlockEntity(new BlockPos(2,100,0));if(s==null)throw new IllegalStateException("Real stockpile placement failed");
            // A full pallet (1,024 eighths) tops up a part-loaded barrow (224 eighths) to exactly its 512-eighth capacity.
            s.cargo().insert(CuttingContent.stack(Blocks.STONE.defaultBlockState(),3,1),1024);barrow(server).cargo().insert(CuttingContent.stack(Blocks.STONE.defaultBlockState(),3,1),224);
        });
        // An empty-handed tray hit opens the cargo; one holding goods would stack them instead.
        if(t==584){var e=barrow(mc);mc.gameMode.interactAt(mc.player,e,new EntityHitResult(e,e.position().add(0,.8,0)),InteractionHand.MAIN_HAND);}
        if(t==590)check(server,()->{
            var p=server.getPlayerList().getPlayers().getFirst();var mined=new net.minecraft.world.entity.item.ItemEntity(server.overworld(),p.getX(),p.getY(),p.getZ(),new ItemStack(Blocks.STONE,BlockCarry.LIMIT));mined.setNoPickUpDelay();server.overworld().addFreshEntity(mined);
        });
        if(t==600){
            if(!(mc.player.containerMenu instanceof CargoMenu))throw new IllegalStateException("Tray hit did not open cargo");
            if(mc.player.getOffhandItem().getCount()!=BlockCarry.LIMIT||!mc.player.getOffhandItem().is(Blocks.STONE.asItem()))throw new IllegalStateException("Mined ground blocks did not synchronize into offhand");
            mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId,0);
        }
        if(t==612){var m=(CargoMenu)mc.player.containerMenu;if(m.count(0)!=512||m.load()!=WheelbarrowEntity.GRID.capacity())throw new IllegalStateException("Cargo quantity or load did not synchronize beyond native stack counts");shot(mc,"cargo-menu");}
        if(t==620)mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId,0,1,net.minecraft.world.inventory.ClickType.PICKUP,mc.player);
        if(t==625){if(mc.player.containerMenu.getCarried().getCount()!=16)throw new IllegalStateException("Real cargo right-click cursor did not synchronize");mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId,0,0,net.minecraft.world.inventory.ClickType.PICKUP,mc.player);}
        if(t==630)mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId,CargoMenu.OFFHAND_SLOT,0,net.minecraft.world.inventory.ClickType.QUICK_MOVE,mc.player);
        if(t==635)check(server,()->{var p=server.getPlayerList().getPlayers().getFirst();if(p.getOffhandItem().getCount()!=BlockCarry.LIMIT||barrow(server).cargo().entry(0).count()!=512)throw new IllegalStateException("Full cargo accepted an oversized transfer");});
        if(t==640)mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId,1);
        if(t==644&&((CargoMenu)mc.player.containerMenu).count(0)!=224)throw new IllegalStateException("Unload did not refill the pallet and keep the rest");
        if(t==650){mc.player.closeContainer();check(server,()->{var e=barrow(server);var p=server.getPlayerList().getPlayers().getFirst();var grip=e.handlerPosition(e.position(),0);p.teleportTo(server.overworld(),grip.x,grip.y,grip.z,java.util.Set.of(),0,20);});}
        if(t==660){var e=barrow(mc);mc.gameMode.interactAt(mc.player,e,new EntityHitResult(e,e.position().add(0,.95,-.83)),InteractionHand.MAIN_HAND);}
        if(t==665){if(mc.player.getVehicle()!=barrow(mc))throw new IllegalStateException("Rear/handle hit did not take loaded handles");mc.options.keyUp.setDown(true);mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);}
        if(t==672)shot(mc,"loaded-pushing");
        if(t==675){mc.options.keyUp.setDown(false);mc.options.keyShift.setDown(true);}
        if(t==680){mc.options.keyShift.setDown(false);mc.options.setCameraType(CameraType.FIRST_PERSON);check(server,()->{
            var e=barrow(server);e.setYRot(0);e.setPos(.5,100,.5);var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),2.5,100,-1,java.util.Set.of(),0,20);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.inventoryMenu.broadcastFullState();
        });}
        if(t==695)mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(2.5,100.125,.5),Direction.UP,new BlockPos(2,100,0),false));
        if(t==710){if(!(mc.player.containerMenu instanceof CargoMenu m)||m.count(0)!=1024||mc.player.getOffhandItem().getCount()!=BlockCarry.LIMIT)throw new IllegalStateException("A full pallet did not open its synchronized menu and leave the hand stack alone");shot(mc,"stockpile-menu");cargoVerified=true;}
        if(t==715){mc.player.closeContainer();check(server,()->{var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SPECTATOR);p.teleportTo(server.overworld(),3.7,102,4.2,java.util.Set.of(),140,18);});}
        if(t==735)shot(mc,"loaded-world-photon");
        if(t==745)shaders(false);
        if(t==785)shot(mc,"loaded-world-no-shaders");
        if(t==795)shaders(true);
        if(t==805)check(server,()->{
            var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.teleportTo(server.overworld(),2.5,100,1.4,java.util.Set.of(),0,20);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            server.overworld().setBlockAndUpdate(new BlockPos(2,100,2),Blocks.CHEST.defaultBlockState());
            ((net.minecraft.world.level.block.entity.ChestBlockEntity)server.overworld().getBlockEntity(new BlockPos(2,100,2))).setItem(0,new ItemStack(Blocks.DIRT,5));
            ((net.minecraft.world.level.block.entity.ChestBlockEntity)server.overworld().getBlockEntity(new BlockPos(2,100,2))).setItem(2,new ItemStack(net.minecraft.world.item.Items.APPLE,1));
        });
        if(t==815)mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(2.5,100.5,2),Direction.NORTH,new BlockPos(2,100,2),false));
        if(t==825){
            if(!(mc.player.containerMenu instanceof net.minecraft.world.inventory.ChestMenu)||mc.player.containerMenu.slots.size()!=64||mc.player.containerMenu.getSlot(63).getItem().getCount()!=BlockCarry.LIMIT)throw new IllegalStateException("Native chest hand-slot synchronization failed");
            mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId,0,0,net.minecraft.world.inventory.ClickType.QUICK_MOVE,mc.player);
        }
        if(t==831){
            if(mc.player.containerMenu.getSlot(0).getItem().getCount()!=5||mc.player.getOffhandItem().getCount()!=BlockCarry.LIMIT)throw new IllegalStateException("Chest shift-click bypassed a full offhand");
            mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId,63,0,net.minecraft.world.inventory.ClickType.QUICK_MOVE,mc.player);
        }
        if(t==836)mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId,0,0,net.minecraft.world.inventory.ClickType.QUICK_MOVE,mc.player);
        if(t==845){
            if(!mc.player.getOffhandItem().is(Blocks.DIRT.asItem())||mc.player.getOffhandItem().getCount()!=5||mc.player.containerMenu.getSlot(1).getItem().getCount()!=BlockCarry.LIMIT)throw new IllegalStateException("Native chest/hand transfers did not conserve the two block types");
            for(int i=0;i<36;i++)if(BlockCarry.block(mc.player.getInventory().getItem(i)))throw new IllegalStateException("Blocks escaped into normal player pockets");
            dev.civilization.client.CarryUiCheck.assertLayout(mc,false);shot(mc,"offhand-chest");handMenuVerified=true;
        }
        if(t==846)dev.civilization.client.CarryUiCheck.clickSlot(mc,2);
        if(t==848)dev.civilization.client.CarryUiCheck.clickLabel(mc);
        if(t==852){
            if(!mc.player.containerMenu.getCarried().is(net.minecraft.world.item.Items.APPLE)||mc.player.containerMenu.getCarried().getCount()!=1)throw new IllegalStateException("Clicking the Hand label dropped or consumed cursor stock");
            check(server,()->{var p=server.getPlayerList().getPlayers().getFirst();if(!p.containerMenu.getCarried().is(net.minecraft.world.item.Items.APPLE)||p.containerMenu.getCarried().getCount()!=1)throw new IllegalStateException("Server treated the Hand frame as an outside drop");});
        }
        if(t==855)mc.player.closeContainer();
        if(t==865)check(server,()->{var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);dev.civilization.client.CreativeInventoryCheck.seed(p);p.inventoryMenu.broadcastFullState();});
        if(t==880)dev.civilization.client.CreativeInventoryCheck.open(mc);
        if(t==885)dev.civilization.client.CreativeInventoryCheck.click(mc,45,0);
        if(t==895){dev.civilization.client.CreativeInventoryCheck.verify(mc.player,false);check(server,()->dev.civilization.client.CreativeInventoryCheck.verify(server.getPlayerList().getPlayers().getFirst(),false));}
        if(t==900)dev.civilization.client.CreativeInventoryCheck.click(mc,46,0);
        if(t==905)dev.civilization.client.CreativeInventoryCheck.click(mc,48,0);
        if(t==910)dev.civilization.client.CreativeInventoryCheck.click(mc,46,0);
        if(t==920){dev.civilization.client.CreativeInventoryCheck.verify(mc.player,true);check(server,()->dev.civilization.client.CreativeInventoryCheck.verify(server.getPlayerList().getPlayers().getFirst(),true));}
        if(t==925)dev.civilization.client.CreativeInventoryCheck.click(mc,47,1);
        if(t==930)dev.civilization.client.CreativeInventoryCheck.click(mc,47,0);
        if(t==940){dev.civilization.client.CreativeInventoryCheck.verify(mc.player,true);check(server,()->{dev.civilization.client.CreativeInventoryCheck.verify(server.getPlayerList().getPlayers().getFirst(),true);creativeVerified=true;});shot(mc,"creative-hotbar");}
        if(t==950)mc.player.closeContainer();
        if(t==965)check(server,()->{
            var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(CargoContent.STOCKPILE.get()));p.inventoryMenu.broadcastFullState();p.teleportTo(server.overworld(),3.5,100,-1,java.util.Set.of(),0,20);
        });
        if(t==1090){mc.player.closeContainer();check(server,()->{var p=server.getPlayerList().getPlayers().getFirst();p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(CargoContent.STOCKPILE.get(),2));p.inventoryMenu.broadcastFullState();});}
        if(t==1105)mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        if(t==1115){if(!mc.player.getMainHandItem().getHoverName().getString().equals("Pallet"))throw new IllegalStateException("Pallet name did not reach the ordinary inventory item");shot(mc,"pallet-inventory");}
        if(t==1120)mc.setScreen(null);
        if(t==1135)shot(mc,"pallet-main-hand-clear");
        if(t==1150)check(server,()->{var p=server.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(CargoContent.STOCKPILE.get()));p.inventoryMenu.broadcastFullState();});
        if(t==1165){shot(mc,"pallet-offhand");palletViewsVerified=true;}
        // A worn pack puts its panel above the Hand, so both are reviewed beside open recipe panels. The Hand holds dirt,
        // which the Kiln does not take as a building material, so an empty-handed click opens its menu.
        if(t==1170)check(server,()->{
            var p=server.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Blocks.DIRT,BlockCarry.LIMIT));
            var pack=Backpack.ITEM.toStack();EquipmentGrade.apply(pack,1000,7);p.getData(Accessories.INVENTORY).setStackInSlot(0,pack);Backpack.worn(p).setItem(0,new ItemStack(Blocks.STONE,6));
            p.inventoryMenu.broadcastFullState();p.teleportTo(server.overworld(),3.5,100,1,java.util.Set.of(),0,20);server.overworld().setBlockAndUpdate(new BlockPos(4,100,2),Blocks.CRAFTING_TABLE.defaultBlockState());
        });
        if(t==1185)mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(4.5,100.5,2),Direction.NORTH,new BlockPos(4,100,2),false));
        if(t==1200)dev.civilization.client.CarryUiCheck.toggleRecipes(mc);
        if(t>=1210&&t<1290)handLayouts(mc,t-1210,"hand-crafting-recipes");
        if(t==1290)mc.player.closeContainer();
        if(t==1295)check(server,()->server.overworld().setBlockAndUpdate(new BlockPos(4,100,2),KilnContent.KILN.get().defaultBlockState()));
        if(t==1310)mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(4.5,100.5,2),Direction.NORTH,new BlockPos(4,100,2),false));
        if(t==1325&&!(mc.player.containerMenu instanceof KilnMenu))throw new IllegalStateException("Native machine menu did not open for Hand-layout review: "+mc.player.containerMenu.getClass().getSimpleName());
        if(t>=1325&&t<1405)handLayouts(mc,t-1325,"hand-machine-recipes");
        // Creative shows the same Hand and Pack panel, and its Hand takes a full stack by real clicks.
        if(t==1405){mc.player.closeContainer();check(server,()->{var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Blocks.DIRT,FULL));p.inventoryMenu.broadcastFullState();});}
        if(t==1420)mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(4.5,100.5,2),Direction.NORTH,new BlockPos(4,100,2),false));
        if(t==1435){
            if(!(mc.player.containerMenu instanceof KilnMenu)||!mc.player.isCreative())throw new IllegalStateException("Creative Kiln menu did not open: "+mc.player.containerMenu.getClass().getSimpleName());
            dev.civilization.client.CarryUiCheck.assertLayout(mc,true);shot(mc,"creative-machine-recipes");
            dev.civilization.client.CarryUiCheck.clickHand(mc);
        }
        if(t==1440){
            if(mc.player.containerMenu.getCarried().getCount()!=FULL)throw new IllegalStateException("Creative Hand click did not pick up its full stack: "+mc.player.containerMenu.getCarried());
            dev.civilization.client.CarryUiCheck.clickHand(mc);
        }
        if(t==1445){
            if(!mc.player.containerMenu.getCarried().isEmpty()||mc.player.getOffhandItem().getCount()!=FULL)throw new IllegalStateException("Creative Hand did not take back its full stack");
            check(server,()->{var p=server.getPlayerList().getPlayers().getFirst();if(p.getOffhandItem().getCount()!=FULL||!p.containerMenu.getCarried().isEmpty())throw new IllegalStateException("Server capped the Creative Hand: "+p.getOffhandItem());});
            dev.civilization.client.CarryUiCheck.toggleRecipes(mc);
        }
        if(t==1450){dev.civilization.client.CarryUiCheck.assertLayout(mc,false);shot(mc,"creative-machine");}
        if(t==1455){mc.player.closeContainer();handLayoutsVerified=true;}
        if(t>1460){if(!verified||!cargoVerified||!handMenuVerified||!creativeVerified||!palletViewsVerified||!handLayoutsVerified)throw new IllegalStateException("Wheelbarrow/pallet/Hand acceptance missing");com.mojang.logging.LogUtils.getLogger().info("WHEELBARROW VISUAL VERIFIED: handle hits, movement/camera/gait, normal main-hand pallet placement and icon/held views, mined offhand pickup, cargo/chest/Creative conservation, full Hand frame cursor safety, crafting and machine recipe clearance in Survival and Creative, Creative full-stack Hand, Photon/shaders-disabled views");mc.stop();}
    }
    /** The open menu's Hand and Pack panel at 1080p, at GUI scale 2 there, in a 768p window and at 1440p, then back to 1080p. */
    private static void handLayouts(Minecraft mc,int t,String name){
        var window=mc.getWindow().getWindow();
        switch(t){
            case 0->{dev.civilization.client.CarryUiCheck.assertLayout(mc,true);shot(mc,name);mc.options.guiScale().set(2);mc.resizeDisplay();}
            case 10->{dev.civilization.client.CarryUiCheck.assertLayout(mc,true);shot(mc,name+"-gui2");mc.options.guiScale().set(0);org.lwjgl.glfw.GLFW.glfwSetWindowSize(window,1366,768);}
            case 35->{dev.civilization.client.CarryUiCheck.assertLayout(mc,true);shot(mc,name+"-768p");org.lwjgl.glfw.GLFW.glfwSetWindowSize(window,2560,1440);}
            case 60->{dev.civilization.client.CarryUiCheck.assertLayout(mc,true);shot(mc,name+"-1440p");org.lwjgl.glfw.GLFW.glfwSetWindowSize(window,1920,1080);}
            case 79->{if(mc.getWindow().getHeight()!=1080)throw new IllegalStateException("The window did not return to 1080p");}
            default->{}
        }
    }
    private static void followed(Minecraft mc,String turn){if(Math.abs(Mth.wrapDegrees(mc.player.getYRot()-barrow(mc).getYRot()))>1)throw new IllegalStateException("The view did not stay on the handles after "+turn+": player="+mc.player.getYRot()+" carrier="+barrow(mc).getYRot());}
    /** The mouse only looks: the carrier keeps its heading and the view stays `look` degrees off the handles. */
    private static void lookedAway(Minecraft mc,float look,String turn){
        float heading=Mth.wrapDegrees(barrow(mc).getYRot()-headingBefore),offset=Mth.wrapDegrees(mc.player.getYRot()-barrow(mc).getYRot());
        if(Math.abs(heading)>.01f||Math.abs(offset-look)>.5f)throw new IllegalStateException("After "+turn+" the carrier turned "+heading+" degrees and the view sits "+offset+" off the handles");
    }
    private static void pitch(Minecraft mc,float degrees){mc.player.setXRot(degrees);mc.player.xRotO=degrees;}
    /** Views the scene from a fixed eye, or returns the view to the player. */
    private static void observe(Minecraft mc,Vec3 eye,float yaw,float pitch){
        if(eye==null){mc.setCameraEntity(mc.player);return;}
        var marker=new net.minecraft.world.entity.Marker(net.minecraft.world.entity.EntityType.MARKER,mc.level);marker.moveTo(eye.x,eye.y,eye.z,yaw,pitch);mc.setCameraEntity(marker);
    }
    private static WheelbarrowEntity barrow(Minecraft mc){if(entityId!=0&&mc.level.getEntity(entityId) instanceof WheelbarrowEntity e)return e;return mc.level.getEntitiesOfClass(WheelbarrowEntity.class,new AABB(-10,98,-10,10,108,10)).getFirst();}
    private static WheelbarrowEntity barrow(MinecraftServer s){return (WheelbarrowEntity)s.overworld().getEntity(entityId);}
    private static void check(MinecraftServer s,Runnable action){s.execute(()->{try{action.run();}catch(Throwable e){failure=e.toString();}});}
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"wheelbarrow-"+name+".png",mc.getMainRenderTarget(),m->{});}
    private static void shaders(boolean enabled){try{var iris=Class.forName("net.irisshaders.iris.Iris");var config=iris.getMethod("getIrisConfig").invoke(null);config.getClass().getMethod("setShadersEnabled",boolean.class).invoke(config,enabled);config.getClass().getMethod("save").invoke(config);iris.getMethod("reload").invoke(null);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
}
