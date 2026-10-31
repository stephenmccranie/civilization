package dev.civilization;

import net.minecraft.client.*;
import net.minecraft.world.level.GameType;

/** Read-only tour of a separate laboratory copy, using the existing hidden-window guards. */
final class ThermalLaboratoryVisual {
    private static int ticks;
    static void tick(Minecraft mc){
        ticks++;
        if(ticks==1)mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        if(ticks==80)mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
            p.setGameMode(GameType.SPECTATOR);
            p.teleportTo(mc.getSingleplayerServer().overworld(),49,113,-28,java.util.Set.of(),0,39);
        });
        if(ticks==160)Screenshot.grab(mc.gameDirectory,"thermal-lab-campus.png",mc.getMainRenderTarget(),m->{});
        if(ticks==170)mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
            p.teleportTo(mc.getSingleplayerServer().overworld(),99,82,21.5,java.util.Set.of(),40,20);
        });
        if(ticks==230)Screenshot.grab(mc.gameDirectory,"thermal-lab-foundry.png",mc.getMainRenderTarget(),m->{});
        if(ticks>240){System.out.println("THERMAL_LAB_VISUAL_PASS");mc.stop();}
    }
}
