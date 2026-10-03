package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.*;

/** Exact quarter-Coal quantity lives in the blockstate; settled piles never tick. */
public final class RawCoalPileBlock extends Block {
    public static final IntegerProperty UNITS=IntegerProperty.create("units",1,64);
    private static final VoxelShape[] SHAPES=new VoxelShape[5];
    static {for(int i=1;i<=4;i++)SHAPES[i]=Shapes.or(box(1,0,1,15,i*2,15),box(3,0,3,13,i*2+1,13)).optimize();}
    public RawCoalPileBlock(){super(BlockBehaviour.Properties.ofFullCopy(Blocks.COAL_BLOCK).strength(-1,3600000).noLootTable().noOcclusion().pushReaction(PushReaction.BLOCK));registerDefaultState(stateDefinition.any().setValue(UNITS,1));}
    public static int stage(int units){return Math.clamp((units+15)/16,1,4);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(UNITS);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPES[stage(s.getValue(UNITS))];}
    @Override protected VoxelShape getOcclusionShape(BlockState s,BlockGetter l,BlockPos p){return Shapes.empty();}
    @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block source,BlockPos from,boolean moving){if(!l.isClientSide&&!supported(l,p))l.scheduleTick(p,this,1);}
    public static boolean supported(Level l,BlockPos p){return l.hasChunkAt(p.below())&&l.getBlockState(p.below()).isFaceSturdy(l,p.below(),net.minecraft.core.Direction.UP);}
    @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,net.minecraft.util.RandomSource r){
        if(!supported(l,p)&&LooseCoalEntity.spawn(l,p.getCenter(),s.getValue(UNITS)))l.removeBlock(p,false);
    }
    public static int insert(ServerLevel l,BlockPos p,int offered){
        if(offered<=0||!l.hasChunkAt(p)||l.isOutsideBuildHeight(p)||!l.getWorldBorder().isWithinBounds(p)||!supported(l,p))return 0;
        var s=l.getBlockState(p);int old=s.is(CoalMiningContent.PILE.get())?s.getValue(UNITS):0;
        if(old==0&&(!s.isAir()||!l.getFluidState(p).isEmpty()))return 0;
        int n=Math.min(64-old,offered);if(n<=0)return 0;
        return l.setBlock(p,CoalMiningContent.PILE.get().defaultBlockState().setValue(UNITS,old+n),3)?n:0;
    }
}
