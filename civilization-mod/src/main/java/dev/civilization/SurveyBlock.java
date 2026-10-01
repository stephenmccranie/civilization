package dev.civilization;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class SurveyBlock extends Block implements EntityBlock {
    public final boolean controller;
    private static final VoxelShape SHAPE=Shapes.or(box(0,12,0,16,16,16),box(6,0,6,10,12,10),box(3,0,3,13,2,13));
    public SurveyBlock(Properties p,boolean controller){super(p);this.controller=controller;registerDefaultState(stateDefinition.any().setValue(CivicBlock.FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(CivicBlock.FACING);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(CivicBlock.FACING,c.getHorizontalDirection().getOpposite());}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(CivicBlock.FACING,r.rotate(s.getValue(CivicBlock.FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(CivicBlock.FACING)));}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPE;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return controller?new SurveyBlockEntity(p,s):null;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return !l.isClientSide&&controller&&type==CivicContent.TABLE_ENTITY.get()?(world,pos,state,entity)->((SurveyBlockEntity)entity).tick():null;}
    @Override protected ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        return controller?MachineConstruction.useOn(stack,state,level,pos,player):ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){
        if(p instanceof ServerPlayer server){
            BlockPos anchor=controller?pos:find(l,pos);
            if(anchor!=null && SurveyTable.complete(l,anchor))SurveyMenu.open(server,anchor);
            else server.displayClientMessage(net.minecraft.network.chat.Component.literal("Build a 2×2 table: controller + 3 Survey Table Sections."),true);
        }return InteractionResult.sidedSuccess(l.isClientSide);
    }
    private static BlockPos find(Level l,BlockPos pos){for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var a=pos.offset(x,0,z);if(l.hasChunkAt(a)&&l.getBlockState(a).is(CivicContent.TABLE.get())&&SurveyTable.positions(l,a).contains(pos))return a;}return null;}
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.Item.TooltipContext context,java.util.List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag flag){if(controller)lines.add(net.minecraft.network.chat.Component.literal("2×2 build: add 3 Survey Table Sections").withStyle(net.minecraft.ChatFormatting.GRAY));}
}
