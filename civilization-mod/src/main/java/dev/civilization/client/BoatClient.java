package dev.civilization.client;
import dev.civilization.BoatPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT)
public final class BoatClient {
    private static boolean up,down;
    @SubscribeEvent public static void input(MovementInputUpdateEvent e){
        var mc=Minecraft.getInstance();var p=mc.player;if(p==null||!p.getPersistentData().getBoolean("civ_boat_pilot")){up=false;down=false;return;}
        if(p.tickCount-p.getPersistentData().getLong("civ_boat_pilot_tick")>40){p.getPersistentData().putBoolean("civ_boat_pilot",false);return;}
        boolean exit=mc.options.keyShift.isDown()||mc.screen!=null;
        boolean nextUp=mc.options.keyUp.isDown(),nextDown=mc.options.keyDown.isDown();
        int shift=(nextUp&&!up?1:0)-(nextDown&&!down?1:0);up=nextUp;down=nextDown;
        if(p.tickCount%4==0||exit||shift!=0)PacketDistributor.sendToServer(new BoatPayload(shift,(mc.options.keyRight.isDown()?1:0)-(mc.options.keyLeft.isDown()?1:0),exit));
        var input=e.getInput();input.forwardImpulse=0;input.leftImpulse=0;input.up=false;input.down=false;input.left=false;input.right=false;input.jumping=false;
        if(exit)p.getPersistentData().putBoolean("civ_boat_pilot",false);
    }
}
