package dev.civilization;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
public final class BoatHelmBlock extends HorizontalDirectionalBlock {
    public static final BooleanProperty PANEL=BooleanProperty.create("panel");
    public static final IntegerProperty GEAR=IntegerProperty.create("gear",0,3);
    public BoatHelmBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(PANEL,false).setValue(GEAR,1));}
    @Override protected com.mojang.serialization.MapCodec<BoatHelmBlock> codec(){return simpleCodec(BoatHelmBlock::new);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(FACING,PANEL,GEAR);}
    public static BlockPos other(BlockPos p,BlockState s){return p.relative(s.getValue(PANEL)?s.getValue(FACING).getCounterClockWise():s.getValue(FACING).getClockWise());}
    public static BlockPos root(BlockPos p,BlockState s){return s.getValue(PANEL)?other(p,s):p;}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){
        var s=defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());var second=other(c.getClickedPos(),s);
        return c.getLevel().getWorldBorder().isWithinBounds(second)&&c.getLevel().getBlockState(second).canBeReplaced(c)&&CivicAccess.allowed(c.getLevel(),second,c.getPlayer())?s:null;
    }
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity entity,ItemStack stack){
        if(!l.isClientSide&&!s.getValue(PANEL))l.setBlock(other(p,s),s.setValue(PANEL,true),3);
    }
    @Override public BlockState playerWillDestroy(Level l,BlockPos p,BlockState s,Player player){
        var q=other(p,s);var mate=l.getBlockState(q);
        if(mate.is(this)&&mate.getValue(PANEL)!=s.getValue(PANEL))l.destroyBlock(q,s.getValue(PANEL)&&!player.isCreative());
        return super.playerWillDestroy(l,p,s,player);
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        return s.getValue(PANEL)?Block.box(0,0,0,16,22,16):Block.box(0,0,0,16,28,16);
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(l instanceof net.minecraft.server.level.ServerLevel server && player instanceof net.minecraft.server.level.ServerPlayer sp){
            BoatSystem.use(server,root(p,s),sp,InteractionHand.MAIN_HAND);
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override public void appendHoverText(ItemStack s,Item.TooltipContext c,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag f){
        lines.add(net.minecraft.network.chat.Component.literal("Two-wide console. Attach it to any connected hull on water."));
        lines.add(net.minecraft.network.chat.Component.literal("Running Hot-Bulb Engines provide thrust; mass slows the vessel."));
        lines.add(net.minecraft.network.chat.Component.literal("W/S: gear up/down. A/D: steer. Shift: leave helm."));
        lines.add(net.minecraft.network.chat.Component.literal("Build freely aboard; new blocks must touch the vessel."));
    }
}
