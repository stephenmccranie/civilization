package dev.civilization;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;

/** Incremental physical search: at most 256 cells per work step, no chunk loads or reserve counters. */
public final class DepositWork {
    public int cursor;
    public record Target(BlockPos pos,int units,int status){}
    public Target find(ServerLevel l,Deposits.Site site,BlockPos machine){
        for(int n=0;n<1024&&cursor<site.workSize();n++,cursor++){
            var p=site.workCell(cursor);if(!site.body(p))continue;
            if(!l.hasChunkAt(p))continue;
            var state=l.getBlockState(p);
            boolean coal=site.kind()==Deposits.Kind.COAL&&state.is(IndustrialContent.COAL_SEAM.get());
            boolean oil=site.kind()==Deposits.Kind.OIL&&state.is(IndustrialContent.RESERVOIR_OIL.get());
            if(!coal&&!oil)continue;
            if(!allowed(l,machine,p))return new Target(p,0,MachineStatus.Industry.CLAIM_BLOCKED);
            return new Target(p,coal?16:IndustrialRates.PUMP_MB,MachineStatus.Industry.WORKING);
        }
        if(cursor>=site.workSize()){
            // An unloaded slice may still hold stock. Retry it when nearby chunks return.
            if(!site.loaded(l)){cursor=0;return new Target(null,0,MachineStatus.Industry.UNLOADED);}
            return new Target(null,0,MachineStatus.Industry.DEPLETED);
        }
        return new Target(null,0,MachineStatus.Industry.SEARCHING);
    }
    public static boolean allowed(ServerLevel l,BlockPos machine,BlockPos target){var claim=CivicAccess.claim(l,target);if(claim==null)return true;var source=CivicAccess.claim(l,machine);return source!=null&&source.owner.equals(claim.owner);}
    public boolean extract(ServerLevel l,Deposits.Site site,BlockPos machine,Target target){
        if(target.pos()==null||!l.hasChunkAt(target.pos())||!allowed(l,machine,target.pos()))return false;
        var state=l.getBlockState(target.pos());
        if(site.kind()==Deposits.Kind.COAL){
            if(!state.is(IndustrialContent.COAL_SEAM.get())||!l.setBlock(target.pos(),Blocks.AIR.defaultBlockState(),3))return false;
            l.levelEvent(2001,target.pos(),Block.getId(state));
        }else{
            if(!state.is(IndustrialContent.RESERVOIR_OIL.get()))return false;
            int height=state.getValue(LiquidBlock.LEVEL);
            if(!l.setBlock(target.pos(),height==7?Blocks.AIR.defaultBlockState():state.setValue(LiquidBlock.LEVEL,height+1),3))return false;
            if(height<7)return true;
        }
        cursor++;return true;
    }
}
