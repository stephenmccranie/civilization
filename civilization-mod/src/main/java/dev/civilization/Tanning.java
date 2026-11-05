package dev.civilization;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Bark tanning: stripping a tree's log peels one Tanbark, and the Tannery's pit steeps up to eight hides in water,
 * one Tanbark each, for twenty loaded minutes with no fire.
 */
@EventBusSubscriber(modid="civilization")
public final class Tanning {
    public static final int PIT=8, TICKS=24000;
    private Tanning(){}
    /** Runs last, so a strip that claim protection or anything else cancels never reaches it and peels nothing. */
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void stripped(BlockEvent.BlockToolModificationEvent e){
        if(e.isSimulated()||e.getItemAbility()!=ItemAbilities.AXE_STRIP||!(e.getLevel() instanceof ServerLevel level))return;
        if(!e.getState().is(BlockTags.LOGS_THAT_BURN)||AxeItem.getAxeStrippingState(e.getState())==null)return;
        Block.popResourceFromFace(level,e.getPos(),e.getContext().getClickedFace(),WorkshopContent.TANBARK.toStack());
    }
}
