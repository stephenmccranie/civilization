package dev.civilization;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
/** Transient flow telemetry and shared server transfer budget; no stored liquid or ticker. */
public final class PipeFlowEntity extends BlockEntity {
    private long budgetStep=Long.MIN_VALUE;
    private int transferred;
    int available(long time){
        long step=time/IndustrialRates.STEP_TICKS;
        if(step!=budgetStep){budgetStep=step;transferred=0;}
        return IndustrialRates.PIPE_MB_PER_STEP-transferred;
    }
    void spend(long time,int amount){available(time);transferred+=amount;}
    public final int[] fluids=new int[6],signs=new int[6];
    public final long[] until=new long[6];
    public PipeFlowEntity(BlockPos p,BlockState s){super(IndustrialContent.PIPE_ENTITY.get(),p,s);}
    @Override public void onLoad(){super.onLoad();if(level!=null&&!level.isClientSide)level.scheduleTick(worldPosition,getBlockState().getBlock(),1);}
    public void flow(int fluid,int route){
        if(level==null||fluid<1||fluid>5)return;
        int in=route&7,out=(route>>3)&7;if(in>5||out>5)return;
        for(int d:new int[]{in,out}){fluids[d]=fluid;signs[d]=d==out?1:-1;until[d]=level.getGameTime()+16;}
    }
    public static String name(int id){return switch(id){case 1->"Crude Oil";case 2->"Refined Fuel";case 3->"Heated Crude";case 4->"Distillate Vapor";case 5->"Lubricating Oil";default->"Empty tank";};}
    public static int color(int id){return switch(id){case 1->0xff614838;case 2->0xffe7b34e;case 3->0xffc46c34;case 4->0xffdce8e5;case 5->0xff879344;default->0xff273c45;};}
}
