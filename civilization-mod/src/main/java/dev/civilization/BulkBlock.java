package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid="civilization")
public final class BulkBlock extends Block implements EntityBlock {
    public final boolean liquid;
    public BulkBlock(boolean liquid){super(Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());this.liquid=liquid;registerDefaultState(stateDefinition.any().setValue(CivicBlock.FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(CivicBlock.FACING);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(CivicBlock.FACING,c.getHorizontalDirection().getOpposite());}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(CivicBlock.FACING,r.rotate(s.getValue(CivicBlock.FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(CivicBlock.FACING)));}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new BulkEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return !l.isClientSide&&t==BulkContent.ENTITY.get()?(w,p,b,e)->((BulkEntity)e).tick():null;}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){if(!l.isClientSide&&l.getBlockEntity(pos) instanceof BulkEntity b&&b.valid(p))p.openMenu(b);return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){
        if(!(stack.is(KilnContent.MINERAL_COAL.get())||BulkEntity.canister(stack)))return MachineConstruction.useOn(stack,s,l,pos,p);
        if(!l.isClientSide&&l.getBlockEntity(pos) instanceof BulkEntity b&&b.valid(p))b.useHeld(p,hand);
        return ItemInteractionResult.sidedSuccess(l.isClientSide);
    }
    @SubscribeEvent public static void breaking(BlockEvent.BreakEvent e){if(e.getLevel().getBlockEntity(e.getPos()) instanceof BulkEntity b&&b.amount()>0){e.setCanceled(true);e.getPlayer().displayClientMessage(Component.literal("Empty the cargo store before removing its controller."),true);}}
    @Override public void appendHoverText(ItemStack s,Item.TooltipContext c,java.util.List<Component> lines,TooltipFlag f){
        lines.add(Component.literal(liquid?"64,000 mB · crude, fuel or lubricant":"8,192 coal · visible bulk storage"));
        var parts=BulkStructure.parts(liquid);for(int units:new int[]{4,2,1}){
            long n=parts.stream().filter(p->p.material().equals("casing")&&p.units()==units).count();
            lines.add(Component.literal(n+" Industrial Casing "+(units==4?"blocks":units==2?"halves":"beams")));
        }
        if(liquid)lines.add(Component.literal("1 Glass block"));
        lines.add(Component.literal("3×5×3 · place for construction guide"));lines.add(Component.literal("Empty before removing; destruction loses contents."));
    }
}
