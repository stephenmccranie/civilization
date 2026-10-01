package dev.civilization;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.*;
/** Bounded, unbuffered conduits. Transfer occurs only after the entire loaded route is validated. */
public final class PipeRouting {
    private record Edge(BlockPos pos,Direction side){}
    public static int transfer(Level l,BlockPos start,Direction side,FluidTank tank,boolean push){
        var parent=new HashMap<BlockPos,BlockPos>();var queue=new ArrayDeque<BlockPos>();var visited=new HashSet<BlockPos>();var endpoints=new LinkedHashSet<Edge>();
        var first=start.relative(side);if(!l.hasChunkAt(first)||!CivicAccess.boundary(l,start,first))return 0;
        if(l.getBlockState(first).is(IndustrialContent.PIPE.get())){queue.add(first);parent.put(first,start);}else endpoints.add(new Edge(first,side.getOpposite()));
        while(!queue.isEmpty()){
            var at=queue.removeFirst();if(!visited.add(at))continue;if(visited.size()>128)return 0;
            var s=l.getBlockState(at);if(!s.is(IndustrialContent.PIPE.get()))continue;
            for(var d:Direction.values()){
                var n=at.relative(d);if(n.equals(start)||!l.hasChunkAt(n)||!CivicAccess.boundary(l,at,n))continue;
                if(l.getBlockState(n).is(IndustrialContent.PIPE.get())){if(!parent.containsKey(n)){parent.put(n,at);queue.add(n);}}
                else if(l.getBlockState(n).is(IndustrialContent.PORT.get())||l.getBlockState(n).getBlock() instanceof BulkBlock||l.getBlockState(n).getBlock() instanceof OilEngineBlock||l.getBlockState(n).getBlock() instanceof IndustrialBlock)endpoints.add(new Edge(n,d.getOpposite()));
            }
        }
        int left=IndustrialRates.PIPE_MB_PER_STEP;
        for(var edge:endpoints){
            IFluidHandler target=l.getCapability(Capabilities.FluidHandler.BLOCK,edge.pos,edge.side);if(target==null)continue;
            var route=route(l,start,edge,parent);if(route==null)continue;
            long time=l.getGameTime();int limit=left;
            for(var pipe:route)limit=Math.min(limit,pipe.available(time));
            if(limit<=0)continue;
            int n=0,fluid=0;
            if(push){var sample=tank.drain(limit,SIMULATE);int accepted=target.fill(sample,SIMULATE);if(accepted>0){n=target.fill(sample.copyWithAmount(accepted),EXECUTE);tank.drain(n,EXECUTE);fluid=IndustrialContent.fluidId(sample);}}
            else {var sample=target.drain(limit,SIMULATE);int accepted=tank.fill(sample,SIMULATE);if(accepted>0){var drained=target.drain(sample.copyWithAmount(accepted),EXECUTE);n=tank.fill(drained,EXECUTE);fluid=IndustrialContent.fluidId(sample);}}
            if(n>0){left-=n;for(var pipe:route)pipe.spend(time,n);showFlow(l,start,edge,parent,fluid,push);}
            if(left==0)break;
        }
        return IndustrialRates.PIPE_MB_PER_STEP-left;
    }
    private static List<PipeFlowEntity> route(Level l,BlockPos start,Edge edge,Map<BlockPos,BlockPos> parent){
        var route=new ArrayList<PipeFlowEntity>();var at=edge.pos.relative(edge.side);
        for(int count=0;count<=128;count++){
            if(at.equals(start))return route;
            if(!(l.getBlockEntity(at) instanceof PipeFlowEntity pipe))return null;
            route.add(pipe);at=parent.get(at);if(at==null)return null;
        }
        return null;
    }
    private static void showFlow(Level l,BlockPos start,Edge edge,Map<BlockPos,BlockPos> parent,int fluid,boolean push){
        var next=edge.pos;var at=edge.pos.relative(edge.side);
        for(int count=0;count<128&&!at.equals(start);count++){
            var previous=parent.get(at);if(previous==null)return;
            var in=Direction.fromDelta(previous.getX()-at.getX(),previous.getY()-at.getY(),previous.getZ()-at.getZ());
            var out=Direction.fromDelta(next.getX()-at.getX(),next.getY()-at.getY(),next.getZ()-at.getZ());
            if(in==null||out==null)return;
            int route=push?in.ordinal()|(out.ordinal()<<3):out.ordinal()|(in.ordinal()<<3);
            l.blockEvent(at,IndustrialContent.PIPE.get(),fluid,route);
            next=at;at=previous;
        }
    }

}
