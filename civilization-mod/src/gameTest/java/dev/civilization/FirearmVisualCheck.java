package dev.civilization;

import net.minecraft.client.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** Uses actual client input events/packets; the window stays hidden and cannot grab the mouse. */
final class FirearmVisualCheck {
    private static int ticks,sounds;private static volatile Throwable failure;private static volatile boolean verified;
    private static float beforePitch;private static int fpsTotal,fpsSamples;
    private static void server(Minecraft mc,Runnable action){mc.getSingleplayerServer().execute(()->{try{action.run();}catch(Throwable t){failure=t;}});}
    static void tick(Minecraft mc){
        ticks++;if(failure!=null)throw new IllegalStateException("Firearm visual check",failure);
        if(ticks==100){net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.sound.PlaySoundEvent e)->{if(e.getSound()!=null&&e.getSound().getLocation().equals(FirearmContent.SHOT.get().getLocation()))sounds++;});mc.options.hideGui=false;mc.options.fov().set(65);mc.options.pauseOnLostFocus=false;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);server(mc,()->{
            var l=mc.getSingleplayerServer().overworld();l.setDayTime(6000);var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
            for(int x=-6;x<=6;x++)for(int z=-6;z<=20;z++){l.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.STONE_BRICKS.defaultBlockState());for(int y=100;y<=106;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.setHealth(p.getMaxHealth());p.getInventory().clearContent();p.getInventory().selected=0;p.setItemInHand(InteractionHand.MAIN_HAND,FirearmContent.PATERSON.toStack());p.getInventory().setItem(1,FirearmContent.AMMO.toStack(12));p.teleportTo(l,.5,100,-3,java.util.Set.of(),0,0);
        });}
        if(ticks==150)shot(mc,"held-empty");
        if(ticks==155)net.minecraft.client.KeyMapping.click(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(org.lwjgl.glfw.GLFW.GLFW_KEY_R));
        if(ticks==180){if(!PatersonItem.reloading(mc.player.getMainHandItem()))throw new IllegalStateException("R key did not start synchronized reload");shot(mc,"reload-open");}
        if(ticks==250)shot(mc,"reload-cylinder");
        if(ticks==335){server(mc,()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();if(PatersonItem.rounds(p.getMainHandItem())!=5||PatersonItem.inventoryAmmo(p)!=7)throw new IllegalStateException("Inventory reload conservation failed");});shot(mc,"held-loaded");}
        if(ticks>=320&&ticks<345){fpsTotal+=mc.getFps();fpsSamples++;}
        if(ticks==345){beforePitch=mc.player.getXRot();var e=new net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered(0,mc.options.keyAttack,InteractionHand.MAIN_HAND);dev.civilization.client.FirearmClient.input(e);if(!e.isCanceled()||e.shouldSwingHand())throw new IllegalStateException("Left click did not suppress melee/mining");}
        if(ticks==348)shot(mc,"recoil");
        if(ticks==370){if(mc.player.getXRot()>=beforePitch-.8f||sounds!=1)throw new IllegalStateException("Confirmed recoil/sound failed: pitch="+mc.player.getXRot()+" sound="+sounds);server(mc,()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();if(PatersonItem.rounds(p.getMainHandItem())!=4)throw new IllegalStateException("Actual fire payload failed");verified=true;});}
        if(ticks==380){var renderer=(dev.civilization.client.PatersonRenderer)net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(FirearmContent.PATERSON.get()).getCustomRenderer();if(!renderer.getGeoModel().getBone("loaded_bullet_0").orElseThrow().isHidden()||renderer.getGeoModel().getBone("loaded_bullet_1").orElseThrow().isHidden())throw new IllegalStateException("Spent chamber visibility did not follow cylinder phase");}
        if(ticks==390){mc.setScreen(new InventoryScreen(mc.player));}
        if(ticks==410)shot(mc,"inventory");
        if(ticks==420)mc.setScreen(null);
        if(ticks>435&&verified){var message="FIREARM_VISUAL_PASS reload=5 ammo=7 fire=4 actual_input=true sound="+sounds+" recoil="+(beforePitch-mc.player.getXRot())+" heldFPS="+(fpsTotal/Math.max(1,fpsSamples));System.out.println(message);try{java.nio.file.Files.writeString(mc.gameDirectory.toPath().resolve("paterson-check.txt"),message);}catch(java.io.IOException e){throw new IllegalStateException(e);}mc.stop();}
    }
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,"paterson-"+name+".png",mc.getMainRenderTarget(),m->{});}
}
