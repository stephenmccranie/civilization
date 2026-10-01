package dev.civilization.client;

import dev.civilization.AirshipPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid="civilization",value=Dist.CLIENT)
public final class AirshipClient {
    @SubscribeEvent public static void input(MovementInputUpdateEvent e){
        var mc=Minecraft.getInstance();var p=mc.player;if(p==null||!p.getPersistentData().getBoolean("civ_airship_pilot"))return;
        boolean loading=mc.screen instanceof net.minecraft.client.gui.screens.ReceivingLevelScreen;
        boolean exit=mc.options.keyShift.isDown()||mc.screen!=null&&!loading;
        if(p.tickCount%2==0||exit)PacketDistributor.sendToServer(new AirshipPayload.Input((mc.options.keyUp.isDown()?1:0)-(mc.options.keyDown.isDown()?1:0),(mc.options.keyRight.isDown()?1:0)-(mc.options.keyLeft.isDown()?1:0),(mc.options.keyJump.isDown()?1:0)-(mc.options.keySprint.isDown()?1:0),exit));
        var input=e.getInput();input.forwardImpulse=input.leftImpulse=0;input.up=input.down=input.left=input.right=input.jumping=input.shiftKeyDown=false;
        if(exit)p.getPersistentData().putBoolean("civ_airship_pilot",false);
    }
}
