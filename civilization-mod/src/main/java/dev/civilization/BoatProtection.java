package dev.civilization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
@EventBusSubscriber(modid="civilization")
public final class BoatProtection {
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void interact(PlayerInteractEvent.RightClickBlock e){if(e.getLevel() instanceof ServerLevel l){var s=BoatSystem.at(l,e.getPos());if(s!=null&&!BoatSystem.owner(s,e.getEntity()))e.setCanceled(true);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void breaking(BlockEvent.BreakEvent e){if(e.getLevel() instanceof ServerLevel l){var s=BoatSystem.at(l,e.getPos());if(s!=null&&(!BoatSystem.owner(s,e.getPlayer())||l.getBlockState(e.getPos()).is(BoatContent.HELM.get())))e.setCanceled(true);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void placing(BlockEvent.EntityPlaceEvent e){if(e.getLevel() instanceof ServerLevel l){var s=BoatSystem.at(l,e.getPos());if(s==null)return;
        var p=e.getEntity() instanceof net.minecraft.world.entity.player.Player player?player:null;
        if(e instanceof BlockEvent.EntityMultiPlaceEvent multi){
            for(var snapshot:multi.getReplacedBlockSnapshots())if(!canPlace(l,snapshot.getPos(),p))e.setCanceled(true);
        }else if(!canPlace(l,e.getPos(),p))e.setCanceled(true);
    }}
    public static boolean canPlace(ServerLevel l,net.minecraft.core.BlockPos pos,net.minecraft.world.entity.player.Player p){
        var s=BoatSystem.at(l,pos);if(s==null)return CivicAccess.allowed(l,pos,p);
        var state=l.getBlockState(pos);boolean container=state.is(Blocks.CHEST)||state.is(Blocks.BARREL);
        boolean supported=!state.hasBlockEntity()||container||state.is(CuttingContent.PIECE.get())||state.getBlock() instanceof OilEngineBlock||state.getBlock() instanceof BulkBlock;
        return BoatSystem.owner(s,p)&&BoatSystem.attached(s,pos)&&supported&&!state.is(BoatContent.HELM.get())&&!state.is(net.minecraft.tags.BlockTags.PORTALS)&&state.getFluidState().isEmpty();
    }
}
