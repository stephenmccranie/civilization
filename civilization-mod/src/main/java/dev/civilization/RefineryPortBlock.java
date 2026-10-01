package dev.civilization;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
public final class RefineryPortBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public RefineryPortBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getClickedFace());}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new RefineryPortEntity(p,s);}
    @Override protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState s,net.minecraft.world.level.storage.loot.LootParams.Builder b){
        return b.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY) instanceof RefineryPortEntity p&&p.derrickAssembly()?java.util.List.of():super.getDrops(s,b);
    }
    @Override protected void onRemove(BlockState s,Level l,BlockPos at,BlockState next,boolean moving){
        if(!l.isClientSide&&!s.is(next.getBlock())&&l.getBlockEntity(at) instanceof RefineryPortEntity p)p.breakAssembly();
        super.onRemove(s,l,at,next,moving);
    }
    @Override protected void tick(BlockState s,net.minecraft.server.level.ServerLevel l,BlockPos at,net.minecraft.util.RandomSource random){
        if(l.getBlockEntity(at) instanceof RefineryPortEntity p&&p.keepAssemblyCheck())l.scheduleTick(at,this,1200);
    }
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState s,Level l,BlockPos at,net.minecraft.world.entity.player.Player p,net.minecraft.world.phys.BlockHitResult hit){if(!l.isClientSide&&CivicAccess.allowed(l,at,p)&&l.getBlockEntity(at) instanceof RefineryPortEntity port)p.displayClientMessage(port.description(),true);return net.minecraft.world.InteractionResult.sidedSuccess(l.isClientSide);}
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack s,net.minecraft.world.item.Item.TooltipContext c,java.util.List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag f){lines.add(net.minecraft.network.chat.Component.literal("Build into the guide. Automatically faces its pipe connection."));}
}
