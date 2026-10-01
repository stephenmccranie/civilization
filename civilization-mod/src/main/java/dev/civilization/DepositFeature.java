package dev.civilization;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Every chunk generates only its own slice, so adjoining chunks cannot resurrect extracted blocks. */
public final class DepositFeature extends Feature<NoneFeatureConfiguration> {
    public DepositFeature(){super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c){
        var w=c.level();var o=c.origin();var s=Deposits.candidate(w.getLevel(),Math.floorDiv(o.getX(),256),Math.floorDiv(o.getZ(),256));if(s==null)return false;
        int minX=Math.max(o.getX(),s.x()-s.radius()),maxX=Math.min(o.getX()+15,s.x()+s.radius());
        int minZ=Math.max(o.getZ(),s.z()-s.radius()),maxZ=Math.min(o.getZ()+15,s.z()+s.radius());
        boolean placed=false;
        for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++)for(int y=s.bottom();y<=s.ceiling();y++){
            var p=new BlockPos(x,y,z);if(!s.body(p)||!Deposits.naturalRock(w.getBlockState(p)))continue;
            var state=s.kind()==Deposits.Kind.COAL?IndustrialContent.COAL_SEAM.get().defaultBlockState():y<=s.top()?IndustrialContent.RESERVOIR_OIL.get().defaultBlockState():Blocks.AIR.defaultBlockState();
            w.setBlock(p,state,2);placed=true;
        }
        if(s.kind()==Deposits.Kind.OIL && s.x()>>4==o.getX()>>4 && s.z()>>4==o.getZ()>>4){
            w.setBlock(new BlockPos(s.x(),s.y()-1,s.z()),IndustrialContent.OIL_SEEP.get().defaultBlockState(),2);
            var source=new BlockPos(s.x(),s.y(),s.z());
            w.setBlock(source,IndustrialContent.SURFACE_OIL.get().defaultBlockState(),2);
            SurfaceOilFlow.activate(w,source);
            placed=true;
        }
        return placed;
    }
}
