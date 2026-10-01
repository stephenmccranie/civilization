package dev.civilization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in screenshot fixture, only in the development source set and isolated preview world. */
@EventBusSubscriber(modid = "civilization", value = Dist.CLIENT)
public final class PreviewVisualCheck {
    private static final String CHECK = System.getProperty("civilization.previewCheck", "");
    private static final boolean FULL = Boolean.getBoolean("civilization.visualFull");
    private static boolean opened, prepared, captured;
    private static int ticks;
    private static int setupTicks;
    private static String campusScreen="";

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (CHECK.isEmpty()) return;
        var mc = Minecraft.getInstance();
        if (!Boolean.getBoolean("civilization.visualHeadless") || !dev.civilization.VisualTestGuard.windowPrepared)
            throw new IllegalStateException("Automated screenshots require runVisualClient and its input/window guards");
        long window = mc.getWindow().getWindow();
        if (org.lwjgl.glfw.GLFW.glfwGetWindowAttrib(window, org.lwjgl.glfw.GLFW.GLFW_VISIBLE) != 0
                || org.lwjgl.glfw.GLFW.glfwGetWindowAttrib(window, org.lwjgl.glfw.GLFW.GLFW_FOCUSED) != 0
                || mc.mouseHandler.isMouseGrabbed())
            throw new IllegalStateException("Visual client must stay hidden, unfocused and unable to grab the mouse");
        if(CHECK.equals("weather-peer")){WeatherVisualCheck.peer(mc);return;}
        if (!opened && mc.screen instanceof TitleScreen) {
            opened = true;
            mc.createWorldOpenFlows().openWorld(System.getProperty("civilization.previewWorld","preview-compatibility"), mc::stop);
        }
        if((CHECK.equals("campus")||CHECK.equals("flight-benchmark")) && opened && mc.level==null && mc.screen!=null) {
            if(!campusScreen.equals(mc.screen.getClass().getName())){campusScreen=mc.screen.getClass().getName();System.out.println("Campus screen: "+campusScreen+" / "+mc.screen.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.Button).map(c->((net.minecraft.client.gui.components.Button)c).getMessage().getString()).toList());}
            for(var child:mc.screen.children())if(child instanceof net.minecraft.client.gui.components.Button button) {
                var label=button.getMessage().getString();
                if(label.equals("Proceed")||label.equals("I know what I'm doing!")||label.equals("Load Anyway")||label.equals("Yes")){System.out.println("Campus test confirmation: "+mc.screen.getClass().getSimpleName()+" / "+label);button.onPress();break;}
            }
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if(CHECK.equals("road")){RoadVisualCheck.tick(mc);return;}
        if(CHECK.equals("uranium")){UraniumVisualCheck.tick(mc);return;}
        if(CHECK.equals("derrick-guide")){DerrickGuideVisualCheck.tick(mc);return;}
        if(CHECK.equals("material-sync")){MaterialSyncVisualCheck.tick(mc);return;}
        if(CHECK.equals("thermal-art")){ThermalArtVisualCheck.tick(mc);return;}
        if(CHECK.equals("machine-lighting")){MachineLightingVisualCheck.tick(mc);return;}
        if(CHECK.equals("deposits")){PhysicalDepositVisualCheck.tick(mc);return;}
        if(CHECK.equals("campus")){if(System.getProperty("civilization.previewWorld","").equals("compact-campus"))CompactCampusVisual.tick(mc);else CampusVisualCheck.tick(mc);return;}
        if(CHECK.equals("flight-benchmark")){FlightBenchmarkClient.tick(mc);return;}
        if(CHECK.equals("airship")){AirshipVisualCheck.tick(mc);return;}
        if(CHECK.equals("boat")){BoatVisualCheck.tick(mc);return;}
        if(CHECK.equals("weather")){WeatherVisualCheck.tick(mc);return;}
        if(CHECK.equals("engine") || CHECK.equals("models")){OilEngineVisualCheck.tick(mc);return;}
        if(CHECK.equals("crafting")){CraftingVisualCheck.tick(mc);return;}
        if(CHECK.equals("chests")){ChestVisualCheck.tick(mc);return;}
        if(CHECK.equals("inventory")){InventoryVisualCheck.tick(mc);return;}
        if(CHECK.equals("workshops")){WorkshopVisualCheck.tick(mc);return;}
        if(CHECK.equals("industry")){IndustryVisualCheck.tick(mc);return;}
        if(CHECK.equals("pipes")){PipeVisualCheck.tick(mc);return;}
        if(CHECK.equals("jei")){JeiVisualCheck.tick(mc);return;}
        if(CHECK.equals("storage")){StorageVisualCheck.tick(mc);return;}
        if(CHECK.equals("bulk")){BulkVisualCheck.tick(mc);return;}
        if(CHECK.equals("thermal")){ThermalVisualCheck.tick(mc);return;}
        if(CHECK.equals("cloth")||CHECK.equals("sulfur")||CHECK.equals("parts")||CHECK.equals("supplies")||CHECK.equals("foods")||CHECK.equals("manufactured")){ItemArtVisualCheck.tick(mc,CHECK);return;}
        if(CHECK.equals("canisters")){CanisterVisualCheck.tick(mc);return;}
        if(CHECK.equals("paterson")){FirearmVisualCheck.tick(mc);return;}
        if(CHECK.equals("oven")){OvenVisualCheck.tick(mc);return;}
        if(CHECK.equals("kitchen")){KitchenVisualCheck.tick(mc);return;}
        if(CHECK.equals("civic")){CivicVisualCheck.tick(mc);return;}
        if (!prepared && ++setupTicks < 100) return; // Let the initial chunk snapshot arrive before editing the display.
        if (!prepared) {
            prepared = true;
            PreviewConfig.MODE.set(PreviewConfig.Mode.TEXTURED);
            mc.options.pauseOnLostFocus = false;
            mc.options.hideGui = false;
            mc.options.fov().set(70);
            mc.options.guiScale().set(3);
            mc.options.renderDistance().set(8);
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            var server = mc.getSingleplayerServer();
            server.execute(() -> {
                try {
                var level = server.overworld();
                for (int cx = -1; cx <= 0; cx++) for (int cz = -1; cz <= 0; cz++) level.getChunk(cx, cz);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
                level.setDayTime(6000);
                level.setWeatherParameters(100000, 0, false, false);
                for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
                    level.setBlockAndUpdate(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState());
                    for (int y = 101; y <= 129; y++) level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
                level.setBlockAndUpdate(new BlockPos(0, 101, 0), KilnContent.KILN.get().defaultBlockState());
                level.setBlockAndUpdate(new BlockPos(-1, 101, 1), Blocks.COBBLESTONE.defaultBlockState());
                // A bright wall behind the guides makes opacity and occlusion easy to inspect.
                for (int x = -3; x <= 3; x++) for (int y = 101; y <= 105; y++)
                    level.setBlockAndUpdate(new BlockPos(x, y, 5), ((x + y) % 2 == 0 ? Blocks.WHITE_CONCRETE : Blocks.RED_CONCRETE).defaultBlockState());
                var player = server.getPlayerList().getPlayers().getFirst();
                player.closeContainer();
                player.setGameMode(GameType.CREATIVE);
                if (CHECK.equals("textures") || CHECK.equals("machines")) {
                    player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(KilnContent.MINERAL_COAL.get()));
                    player.getInventory().setItem(1, new net.minecraft.world.item.ItemStack(FarmingContent.FERTILIZER.get()));
                    player.getInventory().setItem(2, new net.minecraft.world.item.ItemStack(IndustrialContent.ENRICHED_BLEND.get()));
                    player.getInventory().setItem(3, new net.minecraft.world.item.ItemStack(CookingContent.BREAD_DOUGH.get()));
                    player.getInventory().setItem(4, new net.minecraft.world.item.ItemStack(CookingContent.COOKIE_DOUGH.get()));
                    player.getInventory().setItem(5, new net.minecraft.world.item.ItemStack(CookingContent.CAKE_BATTER.get()));
                    player.getInventory().setItem(6, new net.minecraft.world.item.ItemStack(CookingContent.UNBAKED_PIE.get()));
                    for (int slot = 7; slot <= 8; slot++) {
                        var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("civilization", slot == 7 ? "field_ration" : "foraged_morsel");
                        player.getInventory().setItem(slot, new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id)));
                    }
                }
                player.teleportTo(level, 2.5, 101, -3.0, java.util.Set.of(), 29.745f, 15.53f);
                if(CHECK.equals("modular")) {
                    for(int x=-4;x<=4;x++)for(int y=101;y<=105;y++)level.setBlockAndUpdate(new BlockPos(x,y,5),Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(new BlockPos(-1,101,1),Blocks.AIR.defaultBlockState());
                    for(int display=0;display<3;display++) {
                        var pos=new BlockPos((display-1)*2,101,0);level.setBlockAndUpdate(pos,CuttingContent.PIECE.get().defaultBlockState());
                        var cells=new net.minecraft.world.level.block.state.BlockState[8];
                        for(int i=0;i<8;i++)if((new int[]{23,219,255}[display]&(1<<i))!=0)cells[i]=display==0?Blocks.BRICKS.defaultBlockState():i%2==0?Blocks.COPPER_BLOCK.defaultBlockState():Blocks.STONE_BRICKS.defaultBlockState();
                        ((CutBlockEntity)level.getBlockEntity(pos)).cells(cells);
                    }
                    player.getInventory().clearContent();player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,CuttingContent.stack(Blocks.BRICKS.defaultBlockState(),3,8));
                    player.getAbilities().flying=true;player.onUpdateAbilities();player.teleportTo(level,3,102,-4.5,java.util.Set.of(),32f,20f);
                }
                if (CHECK.equals("machines")) {
                    player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(KilnContent.KILN.get()));
                    player.getInventory().setItem(1, new net.minecraft.world.item.ItemStack(KilnContent.RETORT.get()));
                    player.getInventory().setItem(2, new net.minecraft.world.item.ItemStack(CookingContent.STATION.get()));
                    player.getInventory().setItem(3, CuttingContent.STONE_SAW.toStack());
                    player.getInventory().setItem(4, CuttingContent.IRON_SAW.toStack());
                    player.getInventory().setItem(5, CuttingContent.DIAMOND_SAW.toStack());
                    player.getInventory().setItem(6, CuttingContent.stack(Blocks.BRICKS.defaultBlockState(), 2, 8));
                    player.getInventory().setItem(7, CuttingContent.stack(Blocks.COPPER_BLOCK.defaultBlockState(), 1, 8));
                    player.getInventory().setItem(8, net.minecraft.world.item.ItemStack.EMPTY);
                    player.getInventory().selected = 8;
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                    // Display all faces and both model states, without requiring fueled structures.
                    for (int x = -4; x <= 4; x++) for (int y = 101; y <= 105; y++)
                        level.setBlockAndUpdate(new BlockPos(x, y, 5), Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(new BlockPos(0, 101, 0), Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(new BlockPos(-1, 101, 1), Blocks.AIR.defaultBlockState());
                    var display = java.util.List.of(KilnContent.KILN.get(), KilnContent.RETORT.get(), CookingContent.STATION.get());
                    for (int i = 0; i < display.size(); i++) {
                        level.setBlockAndUpdate(new BlockPos(-4 + i * 4, 101, 0), display.get(i).defaultBlockState());
                        level.setBlockAndUpdate(new BlockPos(-4 + i * 4, 102, 3), display.get(i).defaultBlockState());
                        level.setBlockAndUpdate(new BlockPos(-4 + i * 4, 101, 3), Blocks.STONE.defaultBlockState());
                    }
                    player.teleportTo(level, 3.5, 103, -5.0, java.util.Set.of(), 35f, 22f);
                    if(!FULL)for(int i=0;i<2;i++) {
                        var controller=new BlockPos(-4+i*4,101,0);
                        for(var part:MachineStructure.parts(i==1))MachineStructure.placePart(level,controller,net.minecraft.core.Direction.NORTH,part);
                        if(MachineStructure.check(level,controller,net.minecraft.core.Direction.NORTH).status()!=MachineStructure.COMPLETE)throw new IllegalStateException("Quick machine fixture is incomplete");
                    }
                }
                } catch(Throwable failure){com.mojang.logging.LogUtils.getLogger().error("Visual fixture setup failed",failure);}
            });
        }
        if(CHECK.equals("modular")) {
            if(++ticks==240){if(!(mc.level.getBlockEntity(new BlockPos(0,101,0)) instanceof CutBlockEntity cut) || CutCells.mask(cut.cells())!=219)throw new IllegalStateException("Modular fixture did not reach client");screenshot(mc,"cells");com.mojang.logging.LogUtils.getLogger().info("Modular visual: hidden=true mouseGrabbed={} framebuffer={}x{}",mc.mouseHandler.isMouseGrabbed(),mc.getMainRenderTarget().width,mc.getMainRenderTarget().height);}
            if(ticks>250)mc.stop();return;
        }
        if (CHECK.equals("machines")) {
            // Visual fixture only: hold the hot model on the back row despite empty inventories.
            for (int i = 0; i < 3; i++) {
                var pos = new BlockPos(-4 + i * 4, 102, 3);
                var state = mc.level.getBlockState(pos);
                if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT))
                    mc.level.setBlock(pos, state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, true), 3);
            }
        }
        // A focused check needs one settled daylight image; night/outline/placement tours are opt-in.
        if(CHECK.equals("guide")&&!FULL&&ticks>=240){
            ++ticks;
            if(ticks==242)mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().overworld().setBlockAndUpdate(new BlockPos(1,101,0),Blocks.BRICKS.defaultBlockState()));
            if(ticks==247||ticks==267)screenshot(mc,"wrong-"+ticks);
            if(ticks==270)mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().overworld().removeBlock(new BlockPos(1,101,0),false));
            if(ticks==278)screenshot(mc,"removed");
            if(ticks>=282)mc.stop();return;
        }
        if(!FULL && ticks>=240){if(ticks++>=250)mc.stop();return;}
        if (++ticks == 240 && !captured) {
            captured = true;
            if (mc.getMainRenderTarget().width != 1920 || mc.getMainRenderTarget().height != 1080)
                throw new IllegalStateException("Expected a native 1920x1080 visual-test framebuffer");
            com.mojang.logging.LogUtils.getLogger().info("Visual safety: hidden=true focused=false mouseGrabbed=false preventedMouseCalls={} framebuffer=1920x1080", dev.civilization.VisualTestGuard.preventedMouseCalls);
            if (CHECK.equals("machines")) {
                var kiln = mc.level.getBlockState(new BlockPos(-4, 101, 0));
                if (!kiln.is(KilnContent.KILN.get())) throw new IllegalStateException("Kiln display did not reach client: " + kiln);
                com.mojang.logging.LogUtils.getLogger().info("Machine display camera: {} yaw {} pitch {}", mc.player.position(), mc.player.getYRot(), mc.player.getXRot());
            }
            if (!CHECK.equals("machines") && (!(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit)
                    || !hit.getBlockPos().equals(new BlockPos(0, 101, 0))))
                throw new IllegalStateException("Preview fixture must be aimed at the controller within interaction reach");
            Screenshot.grab(mc.gameDirectory, "preview-" + CHECK + ".png", mc.getMainRenderTarget(), message ->
                    com.mojang.logging.LogUtils.getLogger().info("Preview visual check: {}", message.getString()));
        }
        if (ticks == 250) mc.getSingleplayerServer().execute(() -> mc.getSingleplayerServer().overworld().setDayTime(18000));
        if (ticks == 400) screenshot(mc, "night");
        if (ticks == 410) PreviewConfig.MODE.set(PreviewConfig.Mode.OUTLINE);
        if (ticks == 440) screenshot(mc, "outline");
        if (ticks == 450 && !CHECK.equals("machines")) mc.getSingleplayerServer().execute(() -> {
            var level = mc.getSingleplayerServer().overworld();
            for (var part : MachineStructure.parts(false))
                MachineStructure.placePart(level, new BlockPos(0, 101, 0), net.minecraft.core.Direction.NORTH, part);
        });
        if (ticks == 450 && CHECK.equals("machines")) mc.getSingleplayerServer().execute(() -> {
            var level = mc.getSingleplayerServer().overworld();
            level.setDayTime(6000);
            // Inspect controllers in their actual shells, including the cobblestone kiln.
            for (int i = 0; i < 2; i++) {
                var controller = new BlockPos(-4 + i * 4, 101, 0);
                for (var part : MachineStructure.parts(i == 1)) {
                    MachineStructure.placePart(level, controller, net.minecraft.core.Direction.NORTH, part);
                }
                if (MachineStructure.check(level, controller, net.minecraft.core.Direction.NORTH).status() != MachineStructure.COMPLETE)
                    throw new IllegalStateException("Assembled visual fixture is incomplete");
            }
        });
        if (ticks == 480) screenshot(mc, "complete");
        if (CHECK.equals("machines") && ticks == 505) {
            PreviewConfig.MODE.set(PreviewConfig.Mode.TEXTURED);
            mc.getSingleplayerServer().execute(() -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                player.teleportTo(mc.getSingleplayerServer().overworld(), 3, 102, -4, java.util.Set.of(), 38.1f, 39.5f);
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, CuttingContent.stack(Blocks.BRICKS.defaultBlockState(), 1, 8));
            });
        }
        if (CHECK.equals("machines") && ticks > 515) {
            mc.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, CuttingContent.stack(Blocks.BRICKS.defaultBlockState(), ticks>620?3:ticks>570?1:2, 8));
            mc.player.setShiftKeyDown(false);
            mc.player.input.shiftKeyDown = false;
        }
        if (CHECK.equals("machines") && ticks == 555) screenshot(mc, "cut-vertical-preview");
        if (CHECK.equals("machines") && ticks == 605) screenshot(mc, "cut-beam-preview");
        if (CHECK.equals("machines") && ticks == 655) screenshot(mc, "cut-eighth-preview");
        if (ticks > (CHECK.equals("machines") ? 665 : 500)) mc.stop();
    }

    private static void screenshot(Minecraft mc, String variant) {
        Screenshot.grab(mc.gameDirectory, "preview-" + CHECK + "-" + variant + ".png", mc.getMainRenderTarget(), message ->
                com.mojang.logging.LogUtils.getLogger().info("Preview visual check: {}", message.getString()));
    }
}
