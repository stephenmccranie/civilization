package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A prime mover. Each server tick it gives one machine within eight blocks up to the share of its full power that
 * machine asks for, and burns its own fuel for exactly the work it gave. Engines differ only in fuel and full power.
 */
public interface Engine {
    int RANGE=8;
    /** At most once per server tick; a second request in the same tick receives nothing. */
    double requestWork(BlockPos consumer,double demand);
    /** The engines: the Hot-Bulb Engine and the Coal Engine. */
    static Engine of(BlockEntity entity){
        return entity instanceof OilEngineEntity oil?oil:entity instanceof IndustrialBlockEntity m&&m.kind==IndustrialBlock.Kind.COAL_ENGINE?m:null;
    }
    /** The nearest engine within range of `at` in chunks that have finished loading, or null. */
    static BlockPos near(Level level,BlockPos at){
        BlockPos best=null;
        for(int cx=(at.getX()-RANGE)>>4;cx<=(at.getX()+RANGE)>>4;cx++)for(int cz=(at.getZ()-RANGE)>>4;cz<=(at.getZ()+RANGE)>>4;cz++){
            var chunk=Chunks.chunk(level,cx,cz);if(chunk==null)continue;
            for(var entity:chunk.getBlockEntities().values()){
                var p=entity.getBlockPos();
                if(of(entity)!=null&&p.distSqr(at)<=RANGE*RANGE&&(best==null||p.distSqr(at)<best.distSqr(at)))best=p;
            }
        }
        return best;
    }
}
