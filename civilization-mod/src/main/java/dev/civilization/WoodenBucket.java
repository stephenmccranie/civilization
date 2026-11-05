package dev.civilization;

import javax.annotation.Nullable;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/**
 * The bucket is a cooper's wooden pail of planks with a stick bail, carried one at a time like the filled ones. It carries
 * water, milk, powder snow and live fish, but lava would burn it: it scoops none by hand or dispenser and draws none from
 * a lava cauldron.
 */
@EventBusSubscriber(modid="civilization",bus=EventBusSubscriber.Bus.MOD)
public final class WoodenBucket {
    private WoodenBucket(){}
    /** Whether scooping this fluid is refused; a player is told why. */
    public static boolean refuses(Fluid fluid,@Nullable Player player){
        if(!fluid.is(FluidTags.LAVA))return false;
        if(player instanceof ServerPlayer p)p.displayClientMessage(Component.literal("Lava would burn a wooden bucket"),true);
        return true;
    }
    @SubscribeEvent public static void setup(FMLCommonSetupEvent e){e.enqueueWork(()->CauldronInteraction.LAVA.map().remove(Items.BUCKET));}
    @SubscribeEvent public static void components(ModifyDefaultComponentsEvent e){e.modify(Items.BUCKET,b->b.set(DataComponents.MAX_STACK_SIZE,1));}
}
