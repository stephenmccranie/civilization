package dev.civilization.client;

import dev.civilization.PrototypeStoveContent;
import dev.civilization.SkilletItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.util.*;

/** Exposed held pans keep cooking audibly even when their stove is out of view. */
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT)
public final class CarriedSkilletSounds {
    private static final Map<Player,Map<InteractionHand,List<StoveSound>>> SOUNDS=new HashMap<>();
    private CarriedSkilletSounds() {}

    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();var manager=mc.getSoundManager();var heard=new HashSet<Player>();
        if(mc.level!=null&&mc.player!=null){
            var listener=mc.gameRenderer.getMainCamera().getPosition();
            for(var player:mc.level.players()){
                if(!player.isAlive()||player.position().add(0,1,0).distanceToSqr(listener)>=16*16)continue;
                for(var hand:InteractionHand.values()){
                    var stack=player.getItemInHand(hand);
                    if(!stack.is(PrototypeStoveContent.SKILLET.get()))continue;
                    var pan=SkilletItem.contents(stack,mc.level);
                    if(!pan.batch()||pan.warmth()==0)continue;
                    heard.add(player);
                    var hands=SOUNDS.computeIfAbsent(player,p->new EnumMap<>(InteractionHand.class));
                    var loops=hands.get(hand);
                    if(loops==null||loops.getFirst().ended()){
                        if(loops!=null)loops.forEach(StoveSound::end);
                        loops=List.of(new StoveSound(player,hand,0),new StoveSound(player,hand,1),new StoveSound(player,hand,2));hands.put(hand,loops);
                    }
                    for(var sound:loops)if(!manager.isActive(sound))manager.play(sound);
                }
            }
        }
        // Stop old hands/players after placement, switching items, death or world change.
        SOUNDS.entrySet().removeIf(entry->{
            entry.getValue().entrySet().removeIf(hand->{
                var stack=entry.getKey().getItemInHand(hand.getKey());
                var pan=stack.is(PrototypeStoveContent.SKILLET.get())?SkilletItem.contents(stack,entry.getKey().level()):null;
                boolean remove=!heard.contains(entry.getKey())||pan==null||!pan.batch()||pan.warmth()==0;
                if(remove)hand.getValue().forEach(StoveSound::end);
                return remove;
            });
            return entry.getValue().isEmpty();
        });
    }
}
